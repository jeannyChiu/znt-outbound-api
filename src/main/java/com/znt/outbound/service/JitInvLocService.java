package com.znt.outbound.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znt.outbound.model.jit.JitInvLocRequest;
import com.znt.outbound.model.jit.JitInvLocApiRequest;
import com.znt.outbound.model.jit.JitInvLocApiResponse;
import com.znt.outbound.model.jit.JitInvLoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JitInvLocService {

    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final ApiConfigService apiConfigService;
    private final JitAuthService jitAuthService;
    private final EnvelopeService envelopeService;
    private final StatusNotificationService statusNotificationService;

    // 狀態常數定義
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    // 日期時間格式
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    /**
     * 執行庫存查詢的主要方法
     * 
     * @param whName 倉庫名稱
     * @param zoneName 儲區名稱
     * @param storerAbbrName 貨主名稱
     * @param sku 料號
     * @return 處理結果訊息
     */
    public String queryInventoryLocation(String whName, String zoneName, String storerAbbrName, String sku) {
        String operationId = generateOperationId();
        log.info("[{}] 開始執行庫存查詢作業。參數: WhName={}, ZoneName={}, StorerAbbrName={}, Sku={}", 
                operationId, whName, zoneName, storerAbbrName, sku);

        Long requestId = null;
        String seqId = null;
        try {
            // 步驟 1: 寫入請求記錄到 JIT_INV_LOC_REQUEST 表
            requestId = insertRequestRecord(operationId, whName, zoneName, storerAbbrName, sku);
            if (requestId == null) {
                return "寫入請求記錄失敗";
            }

            // 步驟 2: 建立 B2B 封套記錄
            try {
                String providerName = apiConfigService.getProviderName();
                seqId = envelopeService.createJitInvLocEnvelope(requestId.toString(), providerName);
                log.debug("[{}] 成功建立封套，RequestId: {}, SEQ_ID: {}", operationId, requestId, seqId);
            } catch (Exception e) {
                log.error("[{}] 建立 B2B 封套時發生異常，RequestId: {}", operationId, requestId, e);
                updateRequestStatus(requestId, STATUS_FAILED);
                return "建立 B2B 封套失敗: " + e.getMessage();
            }

            // 步驟 3: 更新狀態為 PROCESSING
            updateRequestStatus(requestId, STATUS_PROCESSING);

            // 步驟 4: 調用 JIT API 查詢庫存
            JitInvLocApiResponse apiResponse = callJitInventoryApi(operationId, whName, zoneName, storerAbbrName, sku);
            if (apiResponse == null || apiResponse.getInvLocs() == null || apiResponse.getInvLocs().isEmpty()) {
                updateRequestStatus(requestId, STATUS_FAILED);
                envelopeService.updateEnvelopeStatus(seqId, "F");
                sendNotificationSafely(seqId, "失敗");
                return "JIT API 查詢失敗或無庫存資料";
            }

            // 步驟 5: 將回應資料寫入 JIT_INV_LOC_LIST 表
            int insertedCount = insertInventoryLocationData(operationId, requestId, apiResponse.getInvLocs());
            if (insertedCount > 0) {
                updateRequestStatus(requestId, STATUS_COMPLETED);
                envelopeService.updateEnvelopeStatus(seqId, "S");
                sendNotificationSafely(seqId, "成功");
                log.info("[{}] 庫存查詢作業完成。成功寫入 {} 筆庫存資料", operationId, insertedCount);
                return String.format("庫存查詢成功，共找到 %d 筆庫存資料", insertedCount);
            } else {
                updateRequestStatus(requestId, STATUS_FAILED);
                envelopeService.updateEnvelopeStatus(seqId, "F");
                sendNotificationSafely(seqId, "失敗");
                return "庫存資料寫入失敗";
            }

        } catch (Exception e) {
            log.error("[{}] 庫存查詢作業發生錯誤", operationId, e);
            if (requestId != null) {
                updateRequestStatus(requestId, STATUS_FAILED);
            }
            if (seqId != null) {
                envelopeService.updateEnvelopeStatus(seqId, "F");
                sendNotificationSafely(seqId, "失敗");
            }
            return "庫存查詢失敗: " + e.getMessage();
        }
    }

    /**
     * 寫入請求記錄到 JIT_INV_LOC_REQUEST 表
     */
    private Long insertRequestRecord(String operationId, String whName, String zoneName, String storerAbbrName, String sku) {
        log.info("[{}] 開始寫入請求記錄到 JIT_INV_LOC_REQUEST 表", operationId);
        
        try {
            // 先獲取下一個序列值
            String queryIdSql = "SELECT B2B.JIT_INV_LOC_REQUEST_REQUEST_ID_SEQ.NEXTVAL FROM DUAL";
            Long requestId = jdbcTemplate.queryForObject(queryIdSql, Long.class);
            
            // 執行插入操作
            String insertSql = """
                INSERT INTO B2B.JIT_INV_LOC_REQUEST (REQUEST_ID, WH_NAME, ZONE_NAME, STORER_ABBR_NAME, SKU, STATUS, CREATED_AT) 
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
            
            jdbcTemplate.update(insertSql, requestId, whName, zoneName, storerAbbrName, sku, STATUS_PENDING, 
                               Timestamp.valueOf(LocalDateTime.now()));
            
            log.info("[{}] 請求記錄寫入成功，REQUEST_ID: {}", operationId, requestId);
            return requestId;
            
        } catch (DataAccessException e) {
            log.error("[{}] 寫入請求記錄失敗", operationId, e);
            return null;
        }
    }

    /**
     * 更新請求狀態
     */
    private void updateRequestStatus(Long requestId, String status) {
        String updateSql = "UPDATE B2B.JIT_INV_LOC_REQUEST SET STATUS = ?, UPDATED_AT = ? WHERE REQUEST_ID = ?";
        try {
            jdbcTemplate.update(updateSql, status, Timestamp.valueOf(LocalDateTime.now()), requestId);
            log.debug("更新請求狀態成功。REQUEST_ID: {}, STATUS: {}", requestId, status);
        } catch (DataAccessException e) {
            log.error("更新請求狀態失敗。REQUEST_ID: {}, STATUS: {}", requestId, status, e);
        }
    }

    /**
     * 調用 JIT API 查詢庫存 (包含重試機制)
     */
    private JitInvLocApiResponse callJitInventoryApi(String operationId, String whName, String zoneName, String storerAbbrName, String sku) {
        log.info("[{}] 開始調用 JIT 庫存查詢 API", operationId);
        
        // 嘗試第一次調用
        ResponseEntity<String> response = attemptToCallJitInventoryApi(operationId, whName, zoneName, storerAbbrName, sku, false);
        
        // 如果因為未授權而失敗，則清除 token 並重試一次
        if (response != null && response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.warn("[{}] 接收到 401 Unauthorized 回應，Token 可能已過期。正在清除 token 並重試...", operationId);
            jitAuthService.clearAuthToken();
            response = attemptToCallJitInventoryApi(operationId, whName, zoneName, storerAbbrName, sku, true);
        }
        
        if (response != null && response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return parseApiResponse(operationId, response.getBody());
        }
        
        return null;
    }

    /**
     * 嘗試調用 JIT API 查詢庫存 (使用 GET 方法)
     */
    private ResponseEntity<String> attemptToCallJitInventoryApi(String operationId, String whName, String zoneName, 
                                                               String storerAbbrName, String sku, boolean isRetry) {
        String url = apiConfigService.getInvLocApiUrl();
        if (url == null || url.isEmpty()) {
            log.error("[{}] JIT 庫存查詢 API URL 未設定", operationId);
            return null;
        }

        // 獲取認證 Token
        String authToken = jitAuthService.getAuthToken();
        if (authToken == null) {
            log.error("[{}] 無法獲取 JIT auth-token", operationId);
            return null;
        }

        // 建立 URL 參數
        StringBuilder urlWithParams = new StringBuilder(url);
        urlWithParams.append("?");
        
        // 添加查詢參數（如果有值）
        boolean hasParams = false;
        if (whName != null && !whName.isEmpty()) {
            urlWithParams.append("whName=").append(whName);
            hasParams = true;
        }
        if (zoneName != null && !zoneName.isEmpty()) {
            if (hasParams) urlWithParams.append("&");
            urlWithParams.append("zoneName=").append(zoneName);
            hasParams = true;
        }
        if (storerAbbrName != null && !storerAbbrName.isEmpty()) {
            if (hasParams) urlWithParams.append("&");
            urlWithParams.append("storerAbbrName=").append(storerAbbrName);
            hasParams = true;
        }
        if (sku != null && !sku.isEmpty()) {
            if (hasParams) urlWithParams.append("&");
            urlWithParams.append("sku=").append(sku);
            hasParams = true;
        }

        // 設定 HTTP Headers (移除 Content-Type，只保留認證相關 headers)
        HttpHeaders headers = new HttpHeaders();
        headers.set("username", apiConfigService.getLoginUsername());
        headers.set("auth-token", authToken);

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        String attemptLog = isRetry ? "重試" : "首次嘗試";
        log.info("[{}] ({}) 準備發送庫存查詢請求到 JIT API (GET)。URL: {}", operationId, attemptLog, urlWithParams.toString());

        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                urlWithParams.toString(), 
                org.springframework.http.HttpMethod.GET, 
                requestEntity, 
                String.class
            );
            log.info("[{}] ({}) 成功接收到 JIT API 的回應。Status: {}", operationId, attemptLog, responseEntity.getStatusCode());
            return responseEntity;
        } catch (HttpClientErrorException e) {
            // 捕獲 HTTP 錯誤，例如 401, 404 等
            log.error("[{}] ({}) 發送庫存查詢到 JIT API 時發生 HTTP 錯誤。Status: {}, Response: {}", 
                    operationId, attemptLog, e.getStatusCode(), e.getResponseBodyAsString(), e);
            // 將 HttpClientErrorException 包裝成 ResponseEntity 回傳，以便上層判斷
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            log.error("[{}] ({}) 發送庫存查詢到 JIT API 時發生連接錯誤。URL: {}", operationId, attemptLog, urlWithParams.toString(), e);
            return null;
        }
    }

    /**
     * 解析 JIT API 回應
     */
    private JitInvLocApiResponse parseApiResponse(String operationId, String responseBody) {
        log.debug("[{}] 開始解析 JIT API 回應", operationId);
        
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules(); // 註冊 JavaTimeModule
            
            JitInvLocApiResponse apiResponse = objectMapper.readValue(responseBody, JitInvLocApiResponse.class);
            log.info("[{}] JIT API 回應解析成功，找到 {} 筆庫存資料", operationId, 
                    apiResponse.getInvLocs() != null ? apiResponse.getInvLocs().size() : 0);
            return apiResponse;
            
        } catch (JsonProcessingException e) {
            log.error("[{}] JIT API 回應解析失敗", operationId);
            log.error("[{}] 解析錯誤詳情: {}", operationId, e.getMessage());
            return null;
        }
    }

    /**
     * 將庫存資料寫入 JIT_INV_LOC_LIST 表 (支援批次處理)
     */
    private int insertInventoryLocationData(String operationId, Long requestId, List<JitInvLoc> invLocs) {
        log.info("[{}] 開始寫入庫存資料到 JIT_INV_LOC_LIST 表，共 {} 筆", operationId, invLocs.size());
        
        if (invLocs.isEmpty()) {
            return 0;
        }
        
        // 使用批次插入提高效率
        String insertSql = """
            INSERT INTO B2B.JIT_INV_LOC_LIST (
                INV_LOC_ID, REQUEST_ID, WH_NAME, STORER_ABBR_NAME, ZONE_NAME, LOC, LPN, LOT, SKU, 
                SKU_NAME, SKU_NAME_E, SPEC, QTY, QTY_ALLOCATED, QTY_PICKED, QTY_HOLD, QTY_AVAILABLE, 
                DESCRIPTIONS, CREATE_DT, CREATE_P, CREATE_PN, UPDATE_DT, UPDATE_P, UPDATE_PN, 
                RECEIVE_DT, INV_AGE_DAYS, DATE_CODE, EXPIRED_DT, COO, PACKAGE_TYPE, INBOUND_TYPE, 
                VLOT, QTY_P_EA, QTY_P_CASE, QTY_PIP, QTY_P_IIP, 
                LOT_ATTR01, LOT_ATTR02, LOT_ATTR03, LOT_ATTR04, LOT_ATTR05, LOT_ATTR06, LOT_ATTR07, 
                LOT_ATTR08, LOT_ATTR09, LOT_ATTR10, LOT_ATTR11, LOT_ATTR12, LOT_ATTR13, LOT_ATTR14, 
                LOT_ATTR15, LOT_ATTR16, LOT_ATTR17, LOT_ATTR18, LOT_ATTR19, LOT_ATTR20
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        final int batchSize = 500; // 批次大小
        int totalInserted = 0;
        
        try {
            // 分批處理數據
            for (int i = 0; i < invLocs.size(); i += batchSize) {
                int endIndex = Math.min(i + batchSize, invLocs.size());
                List<JitInvLoc> batch = invLocs.subList(i, endIndex);
                
                log.info("[{}] 處理批次 {}-{} (共 {} 筆)", operationId, i + 1, endIndex, batch.size());
                
                // 準備批次參數
                List<Object[]> batchArgs = new java.util.ArrayList<>();
                for (JitInvLoc invLoc : batch) {
                    Object[] args = {
                        invLoc.getInvLocId(), requestId, invLoc.getWhName(), invLoc.getStorerAbbrName(), 
                        invLoc.getZoneName(), invLoc.getLoc(), invLoc.getLpn(), invLoc.getLot(), invLoc.getSku(),
                        invLoc.getSkuName(), invLoc.getSkuNameE(), invLoc.getSpec(), invLoc.getQty(), 
                        invLoc.getQtyAllocated(), invLoc.getQtyPicked(), invLoc.getQtyHold(), invLoc.getQtyAvailable(),
                        invLoc.getDescriptions(), convertToTimestamp(invLoc.getCreateDt()), invLoc.getCreateP(), 
                        invLoc.getCreatePn(), convertToTimestamp(invLoc.getUpdateDt()), invLoc.getUpdateP(), 
                        invLoc.getUpdatePn(), convertToTimestamp(invLoc.getReceiveDt()), invLoc.getInvAgeDays(),
                        invLoc.getDateCode(), convertToTimestamp(invLoc.getExpiredDt()), invLoc.getCoo(), 
                        invLoc.getPackageType(), invLoc.getInboundType(), invLoc.getVLot(), invLoc.getQtyPEa(),
                        invLoc.getQtyPCase(), invLoc.getQtyPip(), invLoc.getQtyPPip(),
                        invLoc.getLotAttr01(), invLoc.getLotAttr02(), invLoc.getLotAttr03(), invLoc.getLotAttr04(),
                        invLoc.getLotAttr05(), invLoc.getLotAttr06(), invLoc.getLotAttr07(), invLoc.getLotAttr08(),
                        invLoc.getLotAttr09(), invLoc.getLotAttr10(), invLoc.getLotAttr11(), invLoc.getLotAttr12(),
                        invLoc.getLotAttr13(), invLoc.getLotAttr14(), invLoc.getLotAttr15(), invLoc.getLotAttr16(),
                        invLoc.getLotAttr17(), invLoc.getLotAttr18(), invLoc.getLotAttr19(), invLoc.getLotAttr20()
                    };
                    batchArgs.add(args);
                }
                
                // 執行批次插入
                int[] updateCounts = jdbcTemplate.batchUpdate(insertSql, batchArgs);
                int batchInserted = java.util.Arrays.stream(updateCounts).sum();
                totalInserted += batchInserted;
                
                log.info("[{}] 批次 {}-{} 完成，成功寫入 {} 筆", operationId, i + 1, endIndex, batchInserted);
                
                // 如果批次插入失敗的記錄數量過多，記錄警告
                if (batchInserted < batch.size()) {
                    log.warn("[{}] 批次 {}-{} 中有 {} 筆資料寫入失敗", 
                            operationId, i + 1, endIndex, batch.size() - batchInserted);
                }
            }
            
        } catch (DataAccessException e) {
            log.error("[{}] 批次寫入庫存資料時發生錯誤", operationId, e);
            // 如果批次插入失敗，降級為逐筆插入
            log.info("[{}] 降級為逐筆插入模式", operationId);
            return insertInventoryLocationDataOneByOne(operationId, requestId, invLocs);
        }
        
        log.info("[{}] 庫存資料寫入完成，成功寫入 {} 筆", operationId, totalInserted);
        return totalInserted;
    }
    
    /**
     * 逐筆插入庫存資料 (降級方案)
     */
    private int insertInventoryLocationDataOneByOne(String operationId, Long requestId, List<JitInvLoc> invLocs) {
        log.info("[{}] 使用逐筆插入模式寫入庫存資料", operationId);
        
        String insertSql = """
            INSERT INTO B2B.JIT_INV_LOC_LIST (
                INV_LOC_ID, REQUEST_ID, WH_NAME, STORER_ABBR_NAME, ZONE_NAME, LOC, LPN, LOT, SKU, 
                SKU_NAME, SKU_NAME_E, SPEC, QTY, QTY_ALLOCATED, QTY_PICKED, QTY_HOLD, QTY_AVAILABLE, 
                DESCRIPTIONS, CREATE_DT, CREATE_P, CREATE_PN, UPDATE_DT, UPDATE_P, UPDATE_PN, 
                RECEIVE_DT, INV_AGE_DAYS, DATE_CODE, EXPIRED_DT, COO, PACKAGE_TYPE, INBOUND_TYPE, 
                VLOT, QTY_P_EA, QTY_P_CASE, QTY_PIP, QTY_P_IIP, 
                LOT_ATTR01, LOT_ATTR02, LOT_ATTR03, LOT_ATTR04, LOT_ATTR05, LOT_ATTR06, LOT_ATTR07, 
                LOT_ATTR08, LOT_ATTR09, LOT_ATTR10, LOT_ATTR11, LOT_ATTR12, LOT_ATTR13, LOT_ATTR14, 
                LOT_ATTR15, LOT_ATTR16, LOT_ATTR17, LOT_ATTR18, LOT_ATTR19, LOT_ATTR20
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        int insertedCount = 0;
        for (JitInvLoc invLoc : invLocs) {
            try {
                jdbcTemplate.update(insertSql,
                    invLoc.getInvLocId(), requestId, invLoc.getWhName(), invLoc.getStorerAbbrName(), 
                    invLoc.getZoneName(), invLoc.getLoc(), invLoc.getLpn(), invLoc.getLot(), invLoc.getSku(),
                    invLoc.getSkuName(), invLoc.getSkuNameE(), invLoc.getSpec(), invLoc.getQty(), 
                    invLoc.getQtyAllocated(), invLoc.getQtyPicked(), invLoc.getQtyHold(), invLoc.getQtyAvailable(),
                    invLoc.getDescriptions(), convertToTimestamp(invLoc.getCreateDt()), invLoc.getCreateP(), 
                    invLoc.getCreatePn(), convertToTimestamp(invLoc.getUpdateDt()), invLoc.getUpdateP(), 
                    invLoc.getUpdatePn(), convertToTimestamp(invLoc.getReceiveDt()), invLoc.getInvAgeDays(),
                    invLoc.getDateCode(), convertToTimestamp(invLoc.getExpiredDt()), invLoc.getCoo(), 
                    invLoc.getPackageType(), invLoc.getInboundType(), invLoc.getVLot(), invLoc.getQtyPEa(),
                    invLoc.getQtyPCase(), invLoc.getQtyPip(), invLoc.getQtyPPip(),
                    invLoc.getLotAttr01(), invLoc.getLotAttr02(), invLoc.getLotAttr03(), invLoc.getLotAttr04(),
                    invLoc.getLotAttr05(), invLoc.getLotAttr06(), invLoc.getLotAttr07(), invLoc.getLotAttr08(),
                    invLoc.getLotAttr09(), invLoc.getLotAttr10(), invLoc.getLotAttr11(), invLoc.getLotAttr12(),
                    invLoc.getLotAttr13(), invLoc.getLotAttr14(), invLoc.getLotAttr15(), invLoc.getLotAttr16(),
                    invLoc.getLotAttr17(), invLoc.getLotAttr18(), invLoc.getLotAttr19(), invLoc.getLotAttr20()
                );
                insertedCount++;
            } catch (DataAccessException e) {
                log.error("[{}] 寫入庫存資料失敗。InvLocId: {}", operationId, invLoc.getInvLocId(), e);
            }
        }
        
        return insertedCount;
    }

    /**
     * 轉換 LocalDateTime 為 Timestamp
     */
    private Timestamp convertToTimestamp(LocalDateTime localDateTime) {
        return localDateTime != null ? Timestamp.valueOf(localDateTime) : null;
    }

    /**
     * 產生操作ID
     */
    private String generateOperationId() {
        return "INV-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 安全地發送通知，確保異常不會影響主流程
     * @param seqId 封套序列ID
     * @param statusDescription 狀態描述
     */
    private void sendNotificationSafely(String seqId, String statusDescription) {
        try {
            log.info("準備為 SEQ_ID: {} 發送 {} 通知郵件", seqId, statusDescription);
            statusNotificationService.sendNotification(seqId);
            log.info("已成功為 SEQ_ID: {} 發送 {} 通知郵件", seqId, statusDescription);
        } catch (Exception e) {
            log.error("為 SEQ_ID: {} 發送 {} 通知郵件時發生錯誤", seqId, statusDescription, e);
            // 不重新拋出異常，以免影響主流程
        }
    }
}