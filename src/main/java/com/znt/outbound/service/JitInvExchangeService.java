package com.znt.outbound.service;

import com.znt.outbound.model.jit.JitInvExchangeRequest;
import com.znt.outbound.model.jit.JitInvExchangeSkuFinalLineByApi;
import com.znt.outbound.model.jit.JitInvExchangeMaterialLineByApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * JIT庫內換料服務
 * 處理庫內換料請求的映射和發送
 * 支援三種換料類型：Exchange(1:1), Combine(1:N), Separate(1:N)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JitInvExchangeService {

    private final JdbcTemplate jdbcTemplate;
    private final JitApiClient jitApiClient;
    private final EnvelopeService envelopeService;
    private final ApiConfigService apiConfigService;
    private final StatusNotificationService statusNotificationService;

    // 狀態常數定義
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    // 換料類型常數
    private static final String EXCHANGE_TYPE_COMBINE = "Combine";
    private static final String EXCHANGE_TYPE_SEPARATE = "Separate";
    private static final String EXCHANGE_TYPE_EXCHANGE = "Exchange";

    // 重試相關常數
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1秒

    /**
     * 處理統計資訊內部類別
     */
    private static class ProcessingStatistics {
        int processedCount = 0;
        int successCount = 0;
        int failedCount = 0;
        int databaseErrors = 0;
        int apiErrors = 0;
        int mappingErrors = 0;
        int criticalErrors = 0;

        @Override
        public String toString() {
            return String.format("ProcessingStatistics{processed=%d, success=%d, failed=%d, dbErrors=%d, apiErrors=%d, mappingErrors=%d, criticalErrors=%d}",
                    processedCount, successCount, failedCount, databaseErrors, apiErrors, mappingErrors, criticalErrors);
        }
    }

    /**
     * 處理並發送 JIT 庫內換料的主要方法
     * 該方法將持續查詢並處理待處理的庫內換料資料
     */
    public void processAndSendJitInvExchange() {
        String operationId = generateOperationId();
        log.info("[{}] 開始執行 JIT 庫內換料處理與發送作業...", operationId);

        long startTime = System.currentTimeMillis();
        ProcessingStatistics stats = new ProcessingStatistics();

        try {
            // 步驟 1: 準備庫內換料數據
            prepareInvExchangeData(operationId);
            
            // 步驟 2: 讀取 SQL 查詢語句
            String sql = loadSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取 SQL 查詢語句，終止處理作業。", operationId);
                return;
            }

            // 步驟 3: 迴圈處理所有待處理的庫內換料資料
            processInvExchangeDataLoop(operationId, sql, stats);

        } catch (Exception e) {
            log.error("[{}] JIT 庫內換料處理作業發生嚴重錯誤", operationId, e);
            stats.criticalErrors++;
        } finally {
            // 記錄最終統計和性能指標
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logFinalStatistics(operationId, stats, duration);

            // 如果有失敗的記錄，記錄詳細資訊以便後續分析
            if (stats.failedCount > 0) {
                log.warn("[{}] 本次處理有 {} 筆庫內換料失敗，建議檢查失敗原因並考慮重新處理",
                        operationId, stats.failedCount);
            }
        }
    }

    /**
     * 處理庫內換料資料的主要迴圈
     */
    private void processInvExchangeDataLoop(String operationId, String sql, ProcessingStatistics stats) {
        // SQL 現在只查詢 PENDING 狀態，不需要防止無限循環的機制

        while (true) {
            try {
                // 查詢待處理的庫內換料資料（按 ExternalId 分組）
                List<Map<String, Object>> pendingHeaders = queryPendingInvExchangeHeaders(sql);

                if (pendingHeaders.isEmpty()) {
                    log.info("[{}] 沒有待處理的庫內換料資料，結束本次處理作業。", operationId);
                    break;
                }

                log.info("[{}] 查詢到 {} 個待處理的換料單", operationId, pendingHeaders.size());

                // 處理第一個換料單的資料
                Map<String, Object> header = pendingHeaders.get(0);
                String externalId = extractString(header, "EXTERNAL_ID");
                Long headerId = extractLong(header, "HEADER_ID");

                // 處理單個換料單
                boolean success = processSingleExchangeOrder(operationId, header, headerId, stats);
                
                if (!success) {
                    stats.failedCount++;
                    log.error("[{}] 庫內換料 (ExternalId: {}) 處理失敗，狀態已更新為 FAILED", operationId, externalId);
                } else {
                    log.info("[{}] 庫內換料 (ExternalId: {}) 處理成功", operationId, externalId);
                }

                stats.processedCount++;

            } catch (Exception e) {
                log.error("[{}] 處理庫內換料資料時發生錯誤", operationId, e);
                stats.criticalErrors++;
                break;
            }
        }
    }

    /**
     * 查詢待處理的庫內換料主表資料
     */
    private List<Map<String, Object>> queryPendingInvExchangeHeaders(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (DataAccessException e) {
            log.error("查詢待處理的庫內換料資料時發生資料庫錯誤", e);
            return new ArrayList<>();
        }
    }

    /**
     * 處理單個換料單
     */
    private boolean processSingleExchangeOrder(String operationId, Map<String, Object> header, 
                                             Long headerId, ProcessingStatistics stats) {
        String externalId = extractString(header, "EXTERNAL_ID");
        String seqId = null;
        
        try {
            // 建立 B2B 封套
            try {
                String providerName = apiConfigService.getProviderName();
                seqId = envelopeService.createJitInvExchangeEnvelope(externalId, providerName);
                log.info("[{}] 成功建立 B2B 封套，SEQ_ID: {}, ExternalId: {}", operationId, seqId, externalId);
            } catch (Exception e) {
                log.error("[{}] 建立 B2B 封套失敗，ExternalId: {}", operationId, externalId, e);
                updateInvExchangeStatus(externalId, STATUS_FAILED);
                stats.databaseErrors++;
                return false;
            }
            
            // 更新狀態為處理中
            updateInvExchangeStatus(externalId, STATUS_PROCESSING);

            // 查詢成品明細
            List<Map<String, Object>> finalLines = queryFinalLinesByHeaderId(headerId);
            if (finalLines.isEmpty()) {
                log.error("[{}] ExternalId: {} 沒有成品明細資料", operationId, externalId);
                updateInvExchangeStatus(externalId, STATUS_FAILED);
                // 更新 B2B 封套狀態為失敗
                if (seqId != null) {
                    envelopeService.updateEnvelopeStatus(seqId, "F");
                    // 發送失敗通知
                    try {
                        statusNotificationService.sendNotification(seqId);
                        log.info("[{}] 已發送失敗通知，SeqId: {}", operationId, seqId);
                    } catch (Exception e) {
                        log.error("[{}] 發送失敗通知時發生錯誤，但不影響主流程", operationId, e);
                    }
                }
                stats.mappingErrors++;
                return false;
            }

            // 構建 JIT 請求物件
            JitInvExchangeRequest request = buildJitInvExchangeRequest(header, finalLines);
            
            if (request == null) {
                log.error("[{}] 無法構建 JIT 庫內換料請求，ExternalId: {}", operationId, externalId);
                updateInvExchangeStatus(externalId, STATUS_FAILED);
                // 更新 B2B 封套狀態為失敗
                if (seqId != null) {
                    envelopeService.updateEnvelopeStatus(seqId, "F");
                    // 發送失敗通知
                    try {
                        statusNotificationService.sendNotification(seqId);
                        log.info("[{}] 已發送失敗通知，SeqId: {}", operationId, seqId);
                    } catch (Exception e) {
                        log.error("[{}] 發送失敗通知時發生錯誤，但不影響主流程", operationId, e);
                    }
                }
                stats.mappingErrors++;
                return false;
            }

            // 發送到 JIT API
            ResponseEntity<String> response = jitApiClient.sendInvExchange(request);
            
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                log.info("[{}] 成功發送庫內換料請求到 JIT，ExternalId: {}", operationId, externalId);
                updateInvExchangeStatus(externalId, STATUS_COMPLETED);
                
                // 更新 B2B 封套狀態為成功
                if (seqId != null) {
                    envelopeService.updateEnvelopeStatus(seqId, "S");
                }
                
                stats.successCount++;
                
                // 發送成功通知
                sendSuccessNotification(externalId, request, seqId);
                return true;
            } else {
                handleApiError(operationId, externalId, response, stats, seqId);
                return false;
            }

        } catch (Exception e) {
            log.error("[{}] 處理 ExternalId: {} 時發生錯誤", operationId, externalId, e);
            updateInvExchangeStatus(externalId, STATUS_FAILED);
            
            // 更新 B2B 封套狀態為失敗
            if (seqId != null) {
                envelopeService.updateEnvelopeStatus(seqId, "F");
                // 發送失敗通知
                try {
                    statusNotificationService.sendNotification(seqId);
                    log.info("[{}] 已發送失敗通知，SeqId: {}", operationId, seqId);
                } catch (Exception notificationError) {
                    log.error("[{}] 發送失敗通知時發生錯誤，但不影響主流程", operationId, notificationError);
                }
            }
            
            stats.mappingErrors++;
            return false;
        }
    }

    /**
     * 查詢成品明細資料
     */
    private List<Map<String, Object>> queryFinalLinesByHeaderId(Long headerId) {
        String sql = "SELECT * FROM JIT_INV_EXCHANGE_FINAL WHERE HEADER_ID = ? ORDER BY FINAL_ID";
        try {
            return jdbcTemplate.queryForList(sql, headerId);
        } catch (DataAccessException e) {
            log.error("查詢成品明細資料時發生錯誤，HeaderId: {}", headerId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 查詢原材料明細資料
     */
    private List<Map<String, Object>> queryMaterialLinesByFinalId(Long finalId) {
        String sql = "SELECT * FROM JIT_INV_EXCHANGE_MATERIAL WHERE FINAL_ID = ? ORDER BY MATERIAL_ID";
        try {
            return jdbcTemplate.queryForList(sql, finalId);
        } catch (DataAccessException e) {
            log.error("查詢原材料明細資料時發生錯誤，FinalId: {}", finalId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 構建 JIT 庫內換料請求物件
     */
    private JitInvExchangeRequest buildJitInvExchangeRequest(Map<String, Object> header, 
                                                           List<Map<String, Object>> finalLines) {
        JitInvExchangeRequest request = new JitInvExchangeRequest();

        // 設置主要欄位
        request.setExternalId(extractString(header, "EXTERNAL_ID"));
        request.setExternalNo(extractString(header, "EXTERNAL_NO"));
        request.setWhName(extractString(header, "WH_NAME"));
        request.setStorer(extractString(header, "STORER"));
        request.setExchangeType(extractString(header, "EXCHANGE_TYPE"));
        request.setApplyDate(extractLocalDateTime(header, "APPLY_DATE"));
        request.setRefNo(extractString(header, "REF_NO"));
        request.setRemark(extractString(header, "REMARK"));

        // 構建成品明細
        List<JitInvExchangeSkuFinalLineByApi> jitFinalLines = new ArrayList<>();
        for (Map<String, Object> finalLine : finalLines) {
            JitInvExchangeSkuFinalLineByApi jitFinalLine = buildFinalLine(finalLine);
            if (jitFinalLine != null) {
                jitFinalLines.add(jitFinalLine);
            }
        }
        request.setFinalLines(jitFinalLines);

        return request;
    }

    /**
     * 構建單個成品明細
     */
    private JitInvExchangeSkuFinalLineByApi buildFinalLine(Map<String, Object> finalLine) {
        JitInvExchangeSkuFinalLineByApi jitFinalLine = new JitInvExchangeSkuFinalLineByApi();
        
        jitFinalLine.setProduct(extractString(finalLine, "PRODUCT"));
        jitFinalLine.setMfSku(extractString(finalLine, "MF_SKU"));  // 設置原廠料號（必填）
        jitFinalLine.setQty(extractInteger(finalLine, "QTY"));
        jitFinalLine.setZoneName(extractString(finalLine, "ZONE_NAME"));
        jitFinalLine.setLot(extractString(finalLine, "LOT"));
        jitFinalLine.setBatch(extractString(finalLine, "BATCH"));
        jitFinalLine.setDateCode(extractString(finalLine, "DATE_CODE"));
        jitFinalLine.setCoo(extractString(finalLine, "COO"));

        // 查詢並設置原材料明細
        Long finalId = extractLong(finalLine, "FINAL_ID");
        if (finalId != null) {
            List<Map<String, Object>> materialLines = queryMaterialLinesByFinalId(finalId);
            List<JitInvExchangeMaterialLineByApi> jitMaterialLines = new ArrayList<>();
            
            for (Map<String, Object> materialLine : materialLines) {
                JitInvExchangeMaterialLineByApi jitMaterialLine = buildMaterialLine(materialLine);
                if (jitMaterialLine != null) {
                    jitMaterialLines.add(jitMaterialLine);
                }
            }
            jitFinalLine.setMaterialLines(jitMaterialLines);
        }

        return jitFinalLine;
    }

    /**
     * 構建單個原材料明細
     */
    private JitInvExchangeMaterialLineByApi buildMaterialLine(Map<String, Object> materialLine) {
        JitInvExchangeMaterialLineByApi jitMaterialLine = new JitInvExchangeMaterialLineByApi();
        
        jitMaterialLine.setMaterial(extractString(materialLine, "MATERIAL"));
        jitMaterialLine.setMfSku(extractString(materialLine, "MF_SKU"));  // 設置原廠料號（必填）
        jitMaterialLine.setQty(extractInteger(materialLine, "QTY"));
        jitMaterialLine.setZoneName(extractString(materialLine, "ZONE_NAME"));
        jitMaterialLine.setLot(extractString(materialLine, "LOT"));
        jitMaterialLine.setBatch(extractString(materialLine, "BATCH"));
        jitMaterialLine.setDateCode(extractString(materialLine, "DATE_CODE"));
        jitMaterialLine.setCoo(extractString(materialLine, "COO"));

        return jitMaterialLine;
    }

    /**
     * 處理 API 錯誤回應
     */
    private void handleApiError(String operationId, String externalId, 
                               ResponseEntity<String> response, ProcessingStatistics stats, String seqId) {
        if (response == null) {
            log.error("[{}] JIT API 回應為 null，ExternalId: {}", operationId, externalId);
            updateInvExchangeStatus(externalId, STATUS_FAILED);
            
            // 更新 B2B 封套狀態為失敗
            if (seqId != null) {
                envelopeService.updateEnvelopeStatus(seqId, "F");
                // 發送失敗通知
                try {
                    statusNotificationService.sendNotification(seqId);
                    log.info("[{}] 已發送失敗通知，SeqId: {}", operationId, seqId);
                } catch (Exception e) {
                    log.error("[{}] 發送失敗通知時發生錯誤，但不影響主流程", operationId, e);
                }
            }
            
            stats.apiErrors++;
            return;
        }

        HttpStatus statusCode = HttpStatus.valueOf(response.getStatusCode().value());
        String responseBody = response.getBody();

        log.error("[{}] JIT API 回應錯誤，ExternalId: {}, Status: {}, Response: {}", 
                operationId, externalId, statusCode, responseBody);

        // 根據不同的錯誤類型進行處理
        if (statusCode == HttpStatus.BAD_REQUEST) {
            // 400 錯誤通常是資料問題，不應重試
            updateInvExchangeStatus(externalId, STATUS_FAILED);
            sendFailureNotification(externalId, "資料驗證失敗: " + responseBody);
        } else if (statusCode == HttpStatus.UNAUTHORIZED) {
            // 401 錯誤已經由 JitApiClient 處理重試
            updateInvExchangeStatus(externalId, STATUS_FAILED);
        } else if (statusCode.is5xxServerError()) {
            // 5xx 錯誤可能是暫時性的，保持 FAILED 狀態以便後續重試
            updateInvExchangeStatus(externalId, STATUS_FAILED);
        } else {
            updateInvExchangeStatus(externalId, STATUS_FAILED);
        }
        
        // 更新 B2B 封套狀態為失敗
        if (seqId != null) {
            envelopeService.updateEnvelopeStatus(seqId, "F");
            // 發送失敗通知
            try {
                statusNotificationService.sendNotification(seqId);
                log.info("[{}] 已發送失敗通知，SeqId: {}", operationId, seqId);
            } catch (Exception e) {
                log.error("[{}] 發送失敗通知時發生錯誤，但不影響主流程", operationId, e);
            }
        }
        
        stats.apiErrors++;
    }

    /**
     * 更新庫內換料狀態
     */
    private void updateInvExchangeStatus(String externalId, String status) {
        String sql = "UPDATE JIT_INV_EXCHANGE_HEADER SET STATUS = ?, UPDATED_AT = SYSTIMESTAMP WHERE EXTERNAL_ID = ?";
        try {
            int updatedRows = jdbcTemplate.update(sql, status, externalId);
            if (updatedRows > 0) {
                log.info("庫內換料狀態已更新。ExternalId: {}, Status: {}", externalId, status);
            } else {
                log.warn("未找到 ExternalId: {} 的庫內換料記錄，無法更新狀態", externalId);
            }
        } catch (DataAccessException e) {
            log.error("更新庫內換料狀態失敗，ExternalId: {}, Status: {}", externalId, status, e);
        }
    }

    /**
     * 發送成功通知
     */
    private void sendSuccessNotification(String externalId, JitInvExchangeRequest request, String seqId) {
        try {
            // 使用通用的通知服務
            if (seqId != null) {
                statusNotificationService.sendNotification(seqId);
            }
            
            log.info("JIT庫內換料成功 - SeqId: {}, ExternalId: {}, ExternalNo: {}, 換料類型: {}, 貨主: {}, 成品明細數: {}", 
                seqId,
                externalId, 
                request.getExternalNo(),
                request.getExchangeType(),
                request.getStorer(),
                request.getFinalLines() != null ? request.getFinalLines().size() : 0
            );
        } catch (Exception e) {
            log.error("發送成功通知時發生錯誤，但不影響主流程", e);
        }
    }

    /**
     * 發送失敗通知
     */
    private void sendFailureNotification(String externalId, String errorMessage) {
        try {
            // StatusNotificationService 需要 seqId，但我們沒有這個值
            // 所以只記錄日誌
            log.error("JIT庫內換料失敗 - ExternalId: {}, 錯誤訊息: {}", externalId, errorMessage);
        } catch (Exception e) {
            log.error("記錄失敗通知時發生錯誤，但不影響主流程", e);
        }
    }

    /**
     * 準備庫內換料數據
     * 執行 prepare_inv_exchange_data.sql 和 prepare_inv_exchange_separate_data.sql 來插入待處理的數據
     */
    private void prepareInvExchangeData(String operationId) {
        log.info("[{}] 開始執行庫內換料資料預處理作業...", operationId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 步驟 1: 執行雜項收發資料預處理 (Miscellaneous 類型) (20250808新增)
            log.info("[{}] 執行雜項收發資料預處理...", operationId);
            prepareMiscellaneousTypeData(operationId);
            
            // 步驟 2: 執行共用料換料資料預處理 (Exchange 類型)
            log.info("[{}] 執行共用料換料資料預處理...", operationId);
            prepareExchangeTypeData(operationId);
            
            // 步驟 3: 執行拆解工單資料預處理 (Separate 類型)
            log.info("[{}] 執行拆解工單資料預處理...", operationId);
            prepareSeparateTypeData(operationId);
            
            // 步驟 4: 執行組合工單資料預處理 (Combine 類型)
            log.info("[{}] 執行組合工單資料預處理...", operationId);
            prepareCombineTypeData(operationId);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            log.info("[{}] 所有庫內換料資料預處理完成，總執行時間: {}ms", operationId, duration);
            
        } catch (Exception e) {
            log.error("[{}] 庫內換料資料預處理發生錯誤", operationId, e);
            throw e;
        }
    }
    
    /**
     * 準備 Exchange 類型的換料數據
     */
    private void prepareExchangeTypeData(String operationId) {
        try {
            // 讀取預處理 SQL
            String sql = loadPrepareInvExchangeSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取共用料換料預處理 SQL 查詢語句，終止作業。", operationId);
                throw new RuntimeException("無法讀取共用料換料預處理 SQL");
            }
            
            log.info("[{}] 開始執行共用料換料資料預處理 SQL...", operationId);
            
            // 執行預處理 SQL（包含 BEGIN...END 區塊）
            jdbcTemplate.execute(sql);
            
            log.info("[{}] 共用料換料資料預處理完成", operationId);
            
        } catch (DataAccessException e) {
            log.error("[{}] 共用料換料資料預處理資料庫操作失敗", operationId, e);
            throw new RuntimeException("共用料換料資料預處理失敗", e);
        } catch (Exception e) {
            log.error("[{}] 共用料換料資料預處理發生未預期錯誤", operationId, e);
            throw new RuntimeException("共用料換料資料預處理失敗", e);
        }
    }
    
    /**
     * 準備 Separate 類型的拆解工單數據
     */
    private void prepareSeparateTypeData(String operationId) {
        try {
            // 讀取預處理 SQL
            String sql = loadPrepareSeparateSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取拆解工單預處理 SQL 查詢語句，終止作業。", operationId);
                throw new RuntimeException("無法讀取拆解工單預處理 SQL");
            }
            
            log.info("[{}] 開始執行拆解工單資料預處理 SQL...", operationId);
            
            // 執行預處理 SQL（包含 BEGIN...END 區塊）
            jdbcTemplate.execute(sql);
            
            log.info("[{}] 拆解工單資料預處理完成", operationId);
            
        } catch (DataAccessException e) {
            log.error("[{}] 拆解工單資料預處理資料庫操作失敗", operationId, e);
            throw new RuntimeException("拆解工單資料預處理失敗", e);
        } catch (Exception e) {
            log.error("[{}] 拆解工單資料預處理發生未預期錯誤", operationId, e);
            throw new RuntimeException("拆解工單資料預處理失敗", e);
        }
    }
    
    /**
     * 讀取庫內換料預處理 SQL 查詢語句
     */
    private String loadPrepareInvExchangeSqlQuery() {
        try {
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("sql/prepare_inv_exchange_data.sql"),
                    StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("無法讀取庫內換料預處理 SQL 檔案: prepare_inv_exchange_data.sql", e);
            return null;
        }
    }
    
    /**
     * 準備 Combine 類型的組合工單數據
     */
    private void prepareCombineTypeData(String operationId) {
        try {
            // 讀取預處理 SQL
            String sql = loadPrepareCombineSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取組合工單預處理 SQL 查詢語句，終止作業。", operationId);
                throw new RuntimeException("無法讀取組合工單預處理 SQL");
            }
            
            log.info("[{}] 開始執行組合工單資料預處理 SQL...", operationId);
            
            // 執行預處理 SQL（包含 BEGIN...END 區塊）
            jdbcTemplate.execute(sql);
            
            log.info("[{}] 組合工單資料預處理完成", operationId);
            
        } catch (DataAccessException e) {
            log.error("[{}] 組合工單資料預處理資料庫操作失敗", operationId, e);
            throw new RuntimeException("組合工單資料預處理失敗", e);
        } catch (Exception e) {
            log.error("[{}] 組合工單資料預處理發生未預期錯誤", operationId, e);
            throw new RuntimeException("組合工單資料預處理失敗", e);
        }
    }
    
    /**
     * 讀取拆解工單預處理 SQL 查詢語句
     */
    private String loadPrepareSeparateSqlQuery() {
        try {
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("sql/prepare_inv_exchange_separate_data.sql"),
                    StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("無法讀取拆解工單預處理 SQL 檔案: prepare_inv_exchange_separate_data.sql", e);
            return null;
        }
    }
    
    /**
     * 讀取組合工單預處理 SQL 查詢語句
     */
    private String loadPrepareCombineSqlQuery() {
        try {
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("sql/prepare_inv_exchange_combine_data.sql"),
                    StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("無法讀取組合工單預處理 SQL 檔案: prepare_inv_exchange_combine_data.sql", e);
            return null;
        }
    }
    
    /**
     * 準備 Miscellaneous 類型的雜項收發數據 (20250808新增)
     */
    private void prepareMiscellaneousTypeData(String operationId) {
        try {
            // 讀取預處理 SQL
            String sql = loadPrepareMiscellaneousSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取雜項收發預處理 SQL 查詢語句，終止作業。", operationId);
                throw new RuntimeException("無法讀取雜項收發預處理 SQL");
            }
            
            log.info("[{}] 開始執行雜項收發資料預處理 SQL...", operationId);
            
            // 執行預處理 SQL（包含 BEGIN...END 區塊）
            jdbcTemplate.execute(sql);
            
            log.info("[{}] 雜項收發資料預處理完成", operationId);
            
        } catch (DataAccessException e) {
            log.error("[{}] 雜項收發資料預處理資料庫操作失敗", operationId, e);
            throw new RuntimeException("雜項收發資料預處理失敗", e);
        } catch (Exception e) {
            log.error("[{}] 雜項收發資料預處理發生未預期錯誤", operationId, e);
            throw new RuntimeException("雜項收發資料預處理失敗", e);
        }
    }
    
    /**
     * 讀取雜項收發預處理 SQL 查詢語句 (20250808新增)
     */
    private String loadPrepareMiscellaneousSqlQuery() {
        try {
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("sql/prepare_inv_exchange_miscellaneous_data.sql"),
                    StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("無法讀取雜項收發預處理 SQL 檔案: prepare_inv_exchange_miscellaneous_data.sql", e);
            return null;
        }
    }
    
    /**
     * 載入 SQL 查詢語句
     */
    private String loadSqlQuery() {
        try (Reader reader = new InputStreamReader(
                getClass().getResourceAsStream("/sql/select_inv_exchange_for_jit.sql"),
                StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("載入 SQL 查詢語句時發生錯誤", e);
            // 如果無法載入文件，使用預設的 SQL
            return "SELECT * FROM JIT_INV_EXCHANGE_HEADER WHERE STATUS = 'PENDING' ORDER BY CREATED_AT";
        }
    }

    /**
     * 記錄最終統計資訊
     */
    private void logFinalStatistics(String operationId, ProcessingStatistics stats, long duration) {
        log.info("[{}] JIT 庫內換料處理作業完成。處理時間: {} ms", operationId, duration);
        log.info("[{}] 處理統計: {}", operationId, stats);

        if (stats.processedCount > 0) {
            double successRate = (double) stats.successCount / stats.processedCount * 100;
            log.info("[{}] 成功率: {:.2f}% ({}/{})", 
                    operationId, successRate, stats.successCount, stats.processedCount);
        }

        if (stats.criticalErrors > 0) {
            log.error("[{}] 發生 {} 個嚴重錯誤，需要立即關注！", operationId, stats.criticalErrors);
        }
    }

    /**
     * 產生唯一的作業 ID
     */
    private String generateOperationId() {
        return "INV-EXCH-" + System.currentTimeMillis();
    }

    // 輔助方法：提取字串值
    private String extractString(Map<String, Object> row, String key) {
        return extractString(row, key, null);
    }

    private String extractString(Map<String, Object> row, String key, String defaultValue) {
        Object value = row.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    // 輔助方法：提取整數值
    private Integer extractInteger(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("無法轉換欄位 {} 的值為整數: {}", key, value);
            return null;
        }
    }

    // 輔助方法：提取 Long 值
    private Long extractLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("無法轉換欄位 {} 的值為Long: {}", key, value);
            return null;
        }
    }

    // 輔助方法：提取 LocalDateTime
    private LocalDateTime extractLocalDateTime(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime();
        } else if (value instanceof Date) {
            return new Timestamp(((Date) value).getTime()).toLocalDateTime();
        }
        
        return LocalDateTime.now(); // 預設值
    }
}