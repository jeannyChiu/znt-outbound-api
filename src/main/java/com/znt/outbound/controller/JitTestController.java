package com.znt.outbound.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.znt.outbound.model.jit.JitAsnRequest;
import com.znt.outbound.model.jit.JitInvMoveOrTradeRequest;
import com.znt.outbound.scheduler.JitAsnScheduledTask;
import com.znt.outbound.scheduler.JitInvMoveOrTradeScheduledTask;
import com.znt.outbound.scheduler.JitInvLocScheduledTask;
import com.znt.outbound.scheduler.JitInvExchangeScheduledTask;
import com.znt.outbound.service.JitAsnMappingService;
import com.znt.outbound.service.JitInvMoveOrTradeMappingService;
import com.znt.outbound.service.JitInvLocService;
import com.znt.outbound.service.JitInvExchangeService;
import com.znt.outbound.service.JitAuthService;
import com.znt.outbound.service.ApiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/jit")
@RequiredArgsConstructor
@Slf4j
public class JitTestController {

    private final JitAsnMappingService jitAsnMappingService;
    private final JitInvMoveOrTradeMappingService jitInvMoveOrTradeMappingService;
    private final JitInvLocService jitInvLocService;
    private final JitInvExchangeService jitInvExchangeService;
    private final JdbcTemplate jdbcTemplate;
    private final RestTemplate restTemplate;
    private final JitAuthService jitAuthService;
    private final ApiConfigService apiConfigService;
    private final JitAsnScheduledTask jitAsnScheduledTask;
    private final JitInvMoveOrTradeScheduledTask jitInvMoveOrTradeScheduledTask;
    private final JitInvLocScheduledTask jitInvLocScheduledTask;
    private final JitInvExchangeScheduledTask jitInvExchangeScheduledTask;

    /**
     * 健康檢查端點
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        log.info("收到健康檢查請求");
        return ResponseEntity.ok()
            .body(new TestResponse(true, "JIT Test Controller 運行正常",
                "當前時間: " + java.time.LocalDateTime.now()));
    }

    /**
     * 測試JIT認證功能 - 原始回應版本
     */
    @GetMapping("/test-auth-raw")
    public ResponseEntity<?> testJitAuthRaw() {
        log.info("=== JIT 認證測試（原始回應）開始 ===");
        try {
            // 檢查配置
            String loginUrl = apiConfigService.getLoginApiUrl();
            String username = apiConfigService.getLoginUsername();
            String password = apiConfigService.getLoginPassword();

            log.info("JIT 登入配置檢查:");
            log.info("Login URL: {}", loginUrl);
            log.info("Username: {}", username);
            log.info("Password: {}", password != null ? "已設定" : "未設定");

            if (loginUrl == null || username == null || password == null) {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "JIT 認證配置不完整",
                        "請檢查資料庫中的 JIT-LOGIN_URLT, JIT-LOGIN_USERNAME, JIT-LOGIN_PASSWORD 設定"));
            }

            // 直接呼叫JIT API並獲取原始回應
            com.znt.outbound.model.jit.auth.JitLoginRequest loginRequest =
                new com.znt.outbound.model.jit.auth.JitLoginRequest(username, password);
            org.springframework.http.HttpEntity<com.znt.outbound.model.jit.auth.JitLoginRequest> requestEntity =
                new org.springframework.http.HttpEntity<>(loginRequest);

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();

            // 獲取原始字串回應
            org.springframework.http.ResponseEntity<String> response =
                restTemplate.postForEntity(loginUrl, requestEntity, String.class);

            log.info("JIT API 原始回應狀態: {}", response.getStatusCode());
            log.info("JIT API 原始回應內容: {}", response.getBody());

            return ResponseEntity.ok()
                .body(new TestResponse(true, "JIT API 原始回應獲取成功",
                    "狀態碼: " + response.getStatusCode() + ", 回應內容: " + response.getBody()));

        } catch (Exception e) {
            log.error("JIT 認證測試過程中發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "JIT 認證測試失敗",
                    "測試過程中發生錯誤: " + e.getMessage()));
        } finally {
            log.info("=== JIT 認證測試（原始回應）結束 ===");
        }
    }

    /**
     * 測試JIT認證功能
     */
    @GetMapping("/test-auth")
    public ResponseEntity<?> testJitAuth() {
        log.info("=== JIT 認證測試開始 ===");
        try {
            // 檢查配置
            String loginUrl = apiConfigService.getLoginApiUrl();
            String username = apiConfigService.getLoginUsername();
            String password = apiConfigService.getLoginPassword();

            log.info("JIT 登入配置檢查:");
            log.info("Login URL: {}", loginUrl);
            log.info("Username: {}", username);
            log.info("Password: {}", password != null ? "已設定" : "未設定");

            if (loginUrl == null || username == null || password == null) {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "JIT 認證配置不完整",
                        "請檢查資料庫中的 JIT-LOGIN_URLT, JIT-LOGIN_USERNAME, JIT-LOGIN_PASSWORD 設定"));
            }

            // 清除快取的token，強制重新登入
            jitAuthService.clearAuthToken();

            // 嘗試獲取認證token
            String authToken = jitAuthService.getAuthToken();

            if (authToken != null) {
                log.info("JIT 認證測試成功，獲取到 auth-token");
                return ResponseEntity.ok()
                    .body(new TestResponse(true, "JIT 認證測試成功",
                        "成功獲取 auth-token: " + authToken.substring(0, Math.min(10, authToken.length())) + "..."));
            } else {
                log.error("JIT 認證測試失敗，無法獲取 auth-token");
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "JIT 認證測試失敗",
                        "無法獲取 auth-token，請檢查登入URL和憑證是否正確"));
            }

        } catch (Exception e) {
            log.error("JIT 認證測試過程中發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "JIT 認證測試失敗",
                    "測試過程中發生錯誤: " + e.getMessage()));
        } finally {
            log.info("=== JIT 認證測試結束 ===");
        }
    }

    /**
     * 測試資料庫連線和基本查詢
     */
    @GetMapping("/test-db")
    public ResponseEntity<?> testDatabase() {
        log.info("=== 資料庫連線測試開始 ===");
        try {
            // 測試基本連線
            String currentTime = jdbcTemplate.queryForObject("SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') FROM DUAL", String.class);
            log.info("資料庫連線正常，當前時間: {}", currentTime);

            // 測試 JIT_ASN_HEADER 表格
            String headerCountSql = "SELECT COUNT(*) FROM B2B.JIT_ASN_HEADER";
            Integer headerCount = jdbcTemplate.queryForObject(headerCountSql, Integer.class);
            log.info("JIT_ASN_HEADER 表格共有 {} 筆資料", headerCount);

            // 測試 JIT_ASN_LINE 表格
            String lineCountSql = "SELECT COUNT(*) FROM B2B.JIT_ASN_LINE";
            Integer lineCount = jdbcTemplate.queryForObject(lineCountSql, Integer.class);
            log.info("JIT_ASN_LINE 表格共有 {} 筆資料", lineCount);

            // 測試 PENDING 狀態的資料
            String pendingCountSql = "SELECT COUNT(*) FROM B2B.JIT_ASN_HEADER WHERE STATUS = 'PENDING'";
            Integer pendingCount = jdbcTemplate.queryForObject(pendingCountSql, Integer.class);
            log.info("STATUS='PENDING' 的 Header 資料共有 {} 筆", pendingCount);

            // 測試庫內移倉/交易表格
            String invMoveHeaderCountSql = "SELECT COUNT(*) FROM B2B.JIT_MOVE_TRADE_HEADER";
            Integer invMoveHeaderCount = jdbcTemplate.queryForObject(invMoveHeaderCountSql, Integer.class);
            log.info("JIT_MOVE_TRADE_HEADER 表格共有 {} 筆資料", invMoveHeaderCount);

            String invMoveLineCountSql = "SELECT COUNT(*) FROM B2B.JIT_MOVE_TRADE_LINE";
            Integer invMoveLineCount = jdbcTemplate.queryForObject(invMoveLineCountSql, Integer.class);
            log.info("JIT_MOVE_TRADE_LINE 表格共有 {} 筆資料", invMoveLineCount);

            String invMovePendingCountSql = "SELECT COUNT(*) FROM B2B.JIT_MOVE_TRADE_HEADER WHERE STATUS = 'PENDING'";
            Integer invMovePendingCount = jdbcTemplate.queryForObject(invMovePendingCountSql, Integer.class);
            log.info("STATUS='PENDING' 的庫內移倉/交易 Header 資料共有 {} 筆", invMovePendingCount);

            return ResponseEntity.ok()
                .body(new TestResponse(true, "資料庫連線測試成功",
                    String.format("ASN Header: %d 筆, ASN Line: %d 筆, ASN Pending: %d 筆, InvMove Header: %d 筆, InvMove Line: %d 筆, InvMove Pending: %d 筆",
                        headerCount, lineCount, pendingCount, invMoveHeaderCount, invMoveLineCount, invMovePendingCount)));

        } catch (Exception e) {
            log.error("資料庫連線測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "資料庫連線測試失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 資料庫連線測試結束 ===");
        }
    }

    /**
     * 插入測試資料
     */
    @GetMapping("/insert-test-data")
    public ResponseEntity<?> insertTestData() {
        log.info("=== 插入測試資料開始 ===");
        try {
            // 檢查是否已有測試資料
            String checkSql = "SELECT COUNT(*) FROM B2B.JIT_ASN_HEADER WHERE EXTERNAL_ID = '1'";
            Integer existingCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

            if (existingCount > 0) {
                return ResponseEntity.ok()
                    .body(new TestResponse(true, "測試資料已存在，無需重複插入",
                        "現有測試資料數量: " + existingCount));
            }

            // 插入 Header 資料
            String headerSql = """
                INSERT INTO B2B.JIT_ASN_HEADER (
                    EXTERNAL_ID, EXTERNAL_NO, WH_NAME, STORER_ABBR_NAME, PRIORITY,
                    DOC_TYPE, BIZ_TYPE, VOYAGE, BL_NO, CASE_CNT, PALLET_CNT, CONTAINER_CNT,
                    DESCRIPTIONS, USER_DEF1, USER_DEF2, USER_DEF3, USER_DEF4, USER_DEF5, STATUS
                ) VALUES (
                    '1', 'ASN20250611001', 'GT', 'TechGlobal', 1,
                    '一般方式', '原材料入库', 'VY2023-06B', 'BL20230612SZ01', 20, 8, 3,
                    '6月电子元器件原材料入库', '高优先级订单', '供应商：ABC电子', '采购单号：PO20230612001', '运输方式：海运', '质检等级：A', 'PENDING'
                )
                """;

            jdbcTemplate.update(headerSql);
            log.info("Header 資料插入成功");

            // 取得剛插入的 HEADER_ID
            String getHeaderIdSql = "SELECT HEADER_ID FROM B2B.JIT_ASN_HEADER WHERE EXTERNAL_ID = '1'";
            Long headerId = jdbcTemplate.queryForObject(getHeaderIdSql, Long.class);
            log.info("取得 HEADER_ID: {}", headerId);

            // 插入第一筆 Line 資料
            String line1Sql = """
                INSERT INTO B2B.JIT_ASN_LINE (
                    HEADER_ID, STORER_LINE_NO, SKU, SKU_STORER_ABBR_NAME, SKU_NAME, SKU_NAME_E, SPEC,
                    CATEGORY, SUB_CATEGORY, PRODUCT_LINE, UNIT_COO, UNIT_GW, UNIT_NW, UNIT_UOM,
                    ABC, MOQ, SAFETY_STOCK, LENGTH, WIDTH, HEIGHT, UNIT_CUBE, AREA,
                    UNIT_DESCRIPTIONS, UNIT_USER_DEF1, UNIT_USER_DEF2, UNIT_USER_DEF3, UNIT_USER_DEF4, UNIT_USER_DEF5,
                    QTY_EXPECTED, ZONE_NAME, LINE_NW, LINE_GW, LINE_CUBE, LINE_UOM,
                    LINE_USER_DEF1, LINE_USER_DEF2, LINE_USER_DEF3, LINE_USER_DEF4, LINE_USER_DEF5, LINE_DESCRIPTIONS,
                    LOC, LPN, DATE_CODE, EXPIRED_DT, ATTR_COO, PACKAGE_TYPE, VLOT,
                    LOT_ATTR01, LOT_ATTR02, LOT_ATTR03, LOT_ATTR04, LOT_ATTR05
                ) VALUES (
                    ?, 'LINE20230611001', 'IC-1001', 'TechGlobal', '微控制器芯片', 'Microcontroller IC', 'ARM Cortex-M4 32-bit',
                    '电子元器件', '集成电路', '嵌入式系统', 'TW', 0.01, 0.008, '个',
                    'A', 1000, 5000, 10, 10, 2, 0.0000002, 0.0001,
                    '工业级温度范围', 'ESD敏感', '无铅', '符合RoHS', '包装形式：卷带', '最小包装量：2500',
                    10000, 'ZSH基通.上海倉', 80.0, 100.0, 0.25, '个',
                    '批次A', '生产日期：2023W24', '供应商批次：ABC2306', '湿敏等级：3', '存储条件：干燥', '主控芯片',
                    'IC-A-01', 'LPN20230612001', '20231001', TO_TIMESTAMP('2026-06-11 00:00:00', 'YYYY-MM-DD HH24:MI:SS'), '台湾', '卷带', 'VL230612001',
                    '无铅', '符合RoHS', '湿敏等级3', '温度范围：-40°C~85°C', '封装：LQFP64'
                )
                """;

            jdbcTemplate.update(line1Sql, headerId);
            log.info("第一筆 Line 資料插入成功");

            return ResponseEntity.ok()
                .body(new TestResponse(true, "測試資料插入成功",
                    "Header ID: " + headerId + ", 已插入 1 筆 Header 和 1 筆 Line 資料"));

        } catch (Exception e) {
            log.error("插入測試資料失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "插入測試資料失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 插入測試資料結束 ===");
        }
    }

    /**
     * 簡化版本的資料映射測試 - 直接在 Controller 中執行，避免依賴問題
     */
    @GetMapping("/test-mapping-simple")
    public ResponseEntity<?> testJitAsnMappingSimple() {
        log.info("=== 簡化版 JIT ASN 資料映射測試開始 ===");
        try {
            // 1. 讀取 SQL 檔案
            String sql;
            try (Reader reader = new InputStreamReader(
                    this.getClass().getResourceAsStream("/sql/select_asn_for_jit.sql"),
                    StandardCharsets.UTF_8)) {
                sql = FileCopyUtils.copyToString(reader);
                log.info("成功讀取 SQL 檔案");
            } catch (Exception e) {
                log.error("讀取 SQL 檔案失敗", e);
                return ResponseEntity.internalServerError()
                    .body(new TestResponse(false, "讀取 SQL 檔案失敗: " + e.getMessage(), null));
            }

            // 2. 執行查詢
            log.info("開始執行資料庫查詢...");
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            log.info("查詢完成，共找到 {} 筆資料", rows.size());

            if (rows.isEmpty()) {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "沒有找到 STATUS='PENDING' 的 ASN 資料", null));
            }

            // 3. 顯示查詢結果的基本資訊
            Map<String, Object> firstRow = rows.get(0);
            String externalNo = firstRow.get("EXTERNAL_NO") != null ? firstRow.get("EXTERNAL_NO").toString() : "N/A";
            String whName = firstRow.get("WH_NAME") != null ? firstRow.get("WH_NAME").toString() : "N/A";

            log.info("第一筆資料 - ExternalNo: {}, WhName: {}", externalNo, whName);

            // 4. 嘗試使用反射調用映射方法
            try {
                java.lang.reflect.Method method = JitAsnMappingService.class
                    .getDeclaredMethod("mapDataToJitAsnRequest", List.class);
                method.setAccessible(true);

                JitAsnRequest result = (JitAsnRequest) method.invoke(jitAsnMappingService, rows);

                if (result != null) {
                    log.info("資料映射成功！ExternalNo: {}, Lines: {}",
                            result.getExternalNo(), result.getLines().size());
                    return ResponseEntity.ok()
                        .body(new TestResponse(true, "資料映射測試成功", result));
                } else {
                    return ResponseEntity.ok()
                        .body(new TestResponse(false, "資料映射返回 null", null));
                }

            } catch (Exception e) {
                log.error("調用映射方法失敗", e);
                return ResponseEntity.internalServerError()
                    .body(new TestResponse(false, "調用映射方法失敗: " + e.getMessage(), null));
            }

        } catch (Exception e) {
            log.error("簡化版資料映射測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "測試過程中發生錯誤: " + e.getMessage(), null));
        } finally {
            log.info("=== 簡化版 JIT ASN 資料映射測試結束 ===");
        }
    }

    /**
     * 測試 JIT ASN 資料映射功能
     * 僅執行資料查詢和映射，不會發送到外部 API
     */
    @GetMapping("/test-mapping")
    public ResponseEntity<?> testJitAsnMapping() {
        log.info("=== JIT ASN 資料映射測試開始 ===");
        try {
            log.info("收到 JIT ASN 資料映射測試請求");

            JitAsnRequest result = jitAsnMappingService.testDataMapping();

            if (result != null) {
                log.info("資料映射測試成功，準備回傳結果");
                log.info("映射結果 - ExternalNo: {}, StorerAbbrName: {}, WhName: {}",
                        result.getExternalNo(), result.getStorerAbbrName(), result.getWhName());
                return ResponseEntity.ok()
                    .body(new TestResponse(true, "資料映射測試成功", result));
            } else {
                log.warn("沒有找到待處理的資料或映射失敗");
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "沒有找到待處理的資料或映射失敗", null));
            }

        } catch (Exception e) {
            log.error("JIT ASN 資料映射測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "測試過程中發生錯誤: " + e.getMessage(), null));
        } finally {
            log.info("=== JIT ASN 資料映射測試結束 ===");
        }
    }

    /**
     * 查看將要發送到JIT的JSON內容（不實際發送）
     */
    @GetMapping("/preview-json")
    public ResponseEntity<?> previewJitJson() {
        log.info("=== 預覽 JIT JSON 內容開始 ===");
        try {
            JitAsnRequest result = jitAsnMappingService.testDataMapping();

            if (result != null) {
                // 將物件轉換為JSON字串以便查看
                com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                String jsonString = objectMapper.writeValueAsString(result);

                log.info("將要發送到JIT的JSON內容:");
                log.info(jsonString);

                return ResponseEntity.ok()
                    .body(new TestResponse(true, "JSON預覽成功",
                        "JSON內容: " + jsonString));
            } else {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "沒有找到待處理的資料", null));
            }

        } catch (Exception e) {
            log.error("預覽JSON失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "預覽過程中發生錯誤: " + e.getMessage(), null));
        } finally {
            log.info("=== 預覽 JIT JSON 內容結束 ===");
        }
    }

    /**
     * 執行完整的 JIT ASN 處理流程（包含發送到外部 API）
     * 請謹慎使用，確保在測試環境中執行
     */
    @GetMapping("/process-and-send")
    public ResponseEntity<?> processAndSendJitAsn() {
        try {
            log.info("收到 JIT ASN 完整處理請求");

            jitAsnMappingService.processAndSendJitAsn();

            return ResponseEntity.ok()
                .body(new TestResponse(true, "JIT ASN 處理完成，請查看日誌了解詳細結果", null));

        } catch (Exception e) {
            log.error("JIT ASN 處理失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "處理過程中發生錯誤: " + e.getMessage(), null));
        }
    }

    /**
     * 測試排程任務功能 - 手動觸發一次排程任務執行
     * 用於驗證排程任務是否正常運作
     */
    @GetMapping("/test-scheduled-task")
    public ResponseEntity<?> testScheduledTask() {
        log.info("=== JIT ASN 排程任務測試開始 ===");
        try {
            long startTime = System.currentTimeMillis();

            // 手動調用排程任務方法
            jitAsnScheduledTask.executeJitAsnProcessing();

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("排程任務測試完成，執行時間: {}ms", executionTime);

            return ResponseEntity.ok()
                .body(new TestResponse(true,
                    "排程任務測試完成",
                    String.format("執行時間: %dms，請查看日誌了解詳細執行結果", executionTime)));

        } catch (Exception e) {
            log.error("排程任務測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false,
                    "排程任務測試失敗: " + e.getMessage(),
                    "請檢查系統配置和資料庫連線"));
        } finally {
            log.info("=== JIT ASN 排程任務測試結束 ===");
        }
    }

    /**
     * 檢查排程任務狀態和配置資訊
     */
    @GetMapping("/scheduled-task-info")
    public ResponseEntity<?> getScheduledTaskInfo() {
        log.info("=== 查詢排程任務資訊 ===");
        try {
            // 收集排程任務相關資訊
            java.util.Map<String, Object> taskInfo = new java.util.HashMap<>();

            // 基本資訊
            taskInfo.put("taskClass", jitAsnScheduledTask.getClass().getSimpleName());
            taskInfo.put("cronExpression", "0 */5 * * * ?");
            taskInfo.put("timezone", "Asia/Taipei");
            taskInfo.put("description", "每5分鐘執行一次JIT ASN處理");

            // 系統資訊
            taskInfo.put("currentTime", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            taskInfo.put("jvmMemoryUsed", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
            taskInfo.put("activeThreads", Thread.activeCount());

            // 下次執行時間計算（簡化版本）
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime nextExecution = now.withSecond(0).withNano(0);
            int currentMinute = now.getMinute();
            int nextMinute = ((currentMinute / 5) + 1) * 5;
            if (nextMinute >= 60) {
                nextExecution = nextExecution.plusHours(1).withMinute(0);
            } else {
                nextExecution = nextExecution.withMinute(nextMinute);
            }
            taskInfo.put("estimatedNextExecution", nextExecution.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return ResponseEntity.ok()
                .body(new TestResponse(true, "排程任務資訊查詢成功", taskInfo));

        } catch (Exception e) {
            log.error("查詢排程任務資訊失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "查詢排程任務資訊失敗: " + e.getMessage(), null));
        }
    }

    // ========== 庫內移倉/交易測試端點 ==========

    /**
     * 測試庫內移倉/交易資料映射功能
     * 僅執行資料查詢和映射，不會發送到外部 API
     */
    @GetMapping("/test-inv-move-mapping")
    public ResponseEntity<?> testJitInvMoveOrTradeMapping() {
        log.info("=== JIT 庫內移倉/交易資料映射測試開始 ===");
        try {
            log.info("收到 JIT 庫內移倉/交易資料映射測試請求");

            JitInvMoveOrTradeRequest result = jitInvMoveOrTradeMappingService.testDataMapping();

            if (result != null) {
                log.info("資料映射測試成功，準備回傳結果");
                log.info("映射結果 - ExternalNo: {}, TradeType: {}, WhName: {}",
                        result.getExternalNo(), result.getTradeType(), result.getWhName());
                return ResponseEntity.ok()
                    .body(new TestResponse(true, "庫內移倉/交易資料映射測試成功", result));
            } else {
                log.warn("沒有找到待處理的庫內移倉/交易資料或映射失敗");
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "沒有找到待處理的庫內移倉/交易資料或映射失敗", null));
            }

        } catch (Exception e) {
            log.error("JIT 庫內移倉/交易資料映射測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "測試過程中發生錯誤: " + e.getMessage(), null));
        } finally {
            log.info("=== JIT 庫內移倉/交易資料映射測試結束 ===");
        }
    }

    /**
     * 查看將要發送到JIT的庫內移倉/交易JSON內容（不實際發送）
     */
    @GetMapping("/preview-inv-move-json")
    public ResponseEntity<?> previewJitInvMoveOrTradeJson() {
        log.info("=== 預覽庫內移倉/交易 JIT JSON 內容開始 ===");
        try {
            JitInvMoveOrTradeRequest result = jitInvMoveOrTradeMappingService.testDataMapping();

            if (result != null) {
                // 將物件轉換為JSON字串以便查看
                com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                    new com.fasterxml.jackson.databind.ObjectMapper();
                objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                String jsonString = objectMapper.writeValueAsString(result);

                log.info("將要發送到JIT的庫內移倉/交易JSON內容:");
                log.info(jsonString);

                return ResponseEntity.ok()
                    .body(new TestResponse(true, "庫內移倉/交易JSON預覽成功",
                        "JSON內容: " + jsonString));
            } else {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "沒有找到待處理的庫內移倉/交易資料", null));
            }

        } catch (Exception e) {
            log.error("預覽庫內移倉/交易JSON失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "預覽過程中發生錯誤: " + e.getMessage(), null));
        } finally {
            log.info("=== 預覽庫內移倉/交易 JIT JSON 內容結束 ===");
        }
    }

    /**
     * 執行完整的 JIT 庫內移倉/交易處理流程（包含發送到外部 API）
     * 請謹慎使用，確保在測試環境中執行
     */
    @GetMapping("/process-and-send-inv-move")
    public ResponseEntity<?> processAndSendJitInvMoveOrTrade() {
        try {
            log.info("收到 JIT 庫內移倉/交易完整處理請求");

            jitInvMoveOrTradeMappingService.processAndSendJitInvMoveOrTrade();

            return ResponseEntity.ok()
                .body(new TestResponse(true, "JIT 庫內移倉/交易處理完成，請查看日誌了解詳細結果", null));

        } catch (Exception e) {
            log.error("JIT 庫內移倉/交易處理失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "處理過程中發生錯誤: " + e.getMessage(), null));
        }
    }

    /**
     * 檢查庫內移倉/交易健康狀態
     */
    @GetMapping("/health-inv-move")
    public ResponseEntity<?> healthInvMoveOrTrade() {
        log.info("收到庫內移倉/交易健康檢查請求");
        try {
            // 檢查是否有待處理的庫內移倉/交易資料
            String pendingSql = "SELECT COUNT(*) FROM B2B.JIT_MOVE_TRADE_HEADER WHERE STATUS = 'PENDING'";
            Integer pendingCount = jdbcTemplate.queryForObject(pendingSql, Integer.class);

            // 檢查是否有失敗的庫內移倉/交易資料
            String failedSql = "SELECT COUNT(*) FROM B2B.JIT_MOVE_TRADE_HEADER WHERE STATUS = 'FAILED'";
            Integer failedCount = jdbcTemplate.queryForObject(failedSql, Integer.class);

            // 檢查是否有成功的庫內移倉/交易資料
            String completedSql = "SELECT COUNT(*) FROM B2B.JIT_MOVE_TRADE_HEADER WHERE STATUS = 'COMPLETED'";
            Integer completedCount = jdbcTemplate.queryForObject(completedSql, Integer.class);

            java.util.Map<String, Object> healthInfo = new java.util.HashMap<>();
            healthInfo.put("pendingCount", pendingCount);
            healthInfo.put("failedCount", failedCount);
            healthInfo.put("completedCount", completedCount);
            healthInfo.put("currentTime", java.time.LocalDateTime.now());
            healthInfo.put("serviceName", "JitInvMoveOrTradeMappingService");

            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫內移倉/交易健康檢查完成", healthInfo));

        } catch (Exception e) {
            log.error("庫內移倉/交易健康檢查失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "健康檢查失敗: " + e.getMessage(), null));
        }
    }

    /**
     * 測試庫內移倉/交易排程任務功能 - 手動觸發一次排程任務執行
     * 用於驗證排程任務是否正常運作
     */
    @GetMapping("/test-inv-move-scheduled-task")
    public ResponseEntity<?> testInvMoveOrTradeScheduledTask() {
        log.info("=== JIT 庫內移倉/交易排程任務測試開始 ===");
        try {
            long startTime = System.currentTimeMillis();

            // 手動調用排程任務方法
            jitInvMoveOrTradeScheduledTask.executeJitInvMoveOrTradeProcessing();

            long executionTime = System.currentTimeMillis() - startTime;

            log.info("庫內移倉/交易排程任務測試完成，執行時間: {}ms", executionTime);

            return ResponseEntity.ok()
                .body(new TestResponse(true,
                    "庫內移倉/交易排程任務測試完成",
                    String.format("執行時間: %dms，請查看日誌了解詳細執行結果", executionTime)));

        } catch (Exception e) {
            log.error("庫內移倉/交易排程任務測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false,
                    "庫內移倉/交易排程任務測試失敗: " + e.getMessage(),
                    "請檢查系統配置和資料庫連線"));
        } finally {
            log.info("=== JIT 庫內移倉/交易排程任務測試結束 ===");
        }
    }

    /**
     * 檢查庫內移倉/交易排程任務狀態和配置資訊
     */
    @GetMapping("/inv-move-scheduled-task-info")
    public ResponseEntity<?> getInvMoveOrTradeScheduledTaskInfo() {
        log.info("=== 查詢庫內移倉/交易排程任務資訊 ===");
        try {
            // 收集排程任務相關資訊
            java.util.Map<String, Object> taskInfo = new java.util.HashMap<>();

            // 基本資訊
            taskInfo.put("taskClass", jitInvMoveOrTradeScheduledTask.getClass().getSimpleName());
            taskInfo.put("cronExpression", "0 */5 * * * ?");
            taskInfo.put("timezone", "Asia/Taipei");
            taskInfo.put("description", "每5分鐘執行一次JIT庫內移倉/交易處理");

            // 系統資訊
            taskInfo.put("currentTime", java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            taskInfo.put("jvmMemoryUsed", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024 + "MB");
            taskInfo.put("activeThreads", Thread.activeCount());

            // 下次執行時間計算（簡化版本）
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime nextExecution = now.withSecond(0).withNano(0);
            int currentMinute = now.getMinute();
            int nextMinute = ((currentMinute / 5) + 1) * 5;
            if (nextMinute >= 60) {
                nextExecution = nextExecution.plusHours(1).withMinute(0);
            } else {
                nextExecution = nextExecution.withMinute(nextMinute);
            }
            taskInfo.put("estimatedNextExecution", nextExecution.format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫內移倉/交易排程任務資訊查詢成功", taskInfo));

        } catch (Exception e) {
            log.error("查詢庫內移倉/交易排程任務資訊失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "查詢庫內移倉/交易排程任務資訊失敗: " + e.getMessage(), null));
        }
    }

    // ========== 庫存查詢測試端點 ==========

    /**
     * 測試庫存查詢 JSON 格式映射
     * 僅測試 JSON 序列化/反序列化和資料庫操作，不實際調用 JIT API
     */
    @GetMapping("/test-inventory-mapping")
    public ResponseEntity<?> testInventoryMapping() {
        log.info("=== JIT 庫存查詢 JSON 格式測試開始 ===");
        try {
            // 測試參數
            String whName = "GT";
            String zoneName = "ZSH基通.上海倉";
            String storerAbbrName = "ZCSH";
            String sku = "SKU001234";

            // 1. 測試請求 JSON 格式
            com.znt.outbound.model.jit.JitInvLocApiRequest apiRequest = 
                com.znt.outbound.model.jit.JitInvLocApiRequest.builder()
                    .whName(whName)
                    .zoneName(zoneName)
                    .storerAbbrName(storerAbbrName)
                    .sku(sku)
                    .build();

            // 序列化請求物件為 JSON
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String requestJson = objectMapper.writeValueAsString(apiRequest);
            log.info("請求 JSON 格式: {}", requestJson);

            // 2. 模擬 JIT API 回應 JSON
            String mockResponseJson = """
                {
                  "InvLocs": [
                    {
                      "InvLocId": 12345,
                      "WhName": "GT",
                      "StorerAbbrName": "ZCSH",
                      "ZoneName": "ZSH基通.上海倉",
                      "Loc": "A01-01-01",
                      "Lpn": "LPN20250716001",
                      "Lot": "LOT20250716",
                      "Sku": "SKU001234",
                      "SkuName": "產品名稱",
                      "SkuNameE": "Product Name",
                      "Spec": "規格A",
                      "Qty": 1000,
                      "QtyAllocated": 200,
                      "QtyPicked": 150,
                      "QtyHold": 50,
                      "QtyAvailable": 600,
                      "Descriptions": "庫存描述",
                      "CreateDt": "2025-07-16T08:00:00",
                      "CreateP": "系統",
                      "CreatePn": "創建人姓名",
                      "UpdateDt": "2025-07-16T12:00:00",
                      "UpdateP": "系統",
                      "UpdatePn": "更新人姓名",
                      "ReceiveDt": "2025-07-15T10:00:00",
                      "InvAgeDays": 1,
                      "DateCode": 20250716,
                      "ExpiredDt": "2025-12-31T23:59:59",
                      "Coo": "TW",
                      "PackageType": "箱",
                      "InboundType": "入庫",
                      "VLot": "VL001",
                      "QtyPEa": 10.5,
                      "QtyPCase": 100,
                      "QtyPip": 200,
                      "QtyPPip": 300,
                      "LotAttr01": "批次屬性1",
                      "LotAttr02": "批次屬性2",
                      "LotAttr03": "批次屬性3",
                      "LotAttr04": "批次屬性4",
                      "LotAttr05": "批次屬性5",
                      "LotAttr06": "批次屬性6",
                      "LotAttr07": "批次屬性7",
                      "LotAttr08": "批次屬性8",
                      "LotAttr09": "批次屬性9",
                      "LotAttr10": "批次屬性10",
                      "LotAttr11": "批次屬性11",
                      "LotAttr12": "批次屬性12",
                      "LotAttr13": "批次屬性13",
                      "LotAttr14": "批次屬性14",
                      "LotAttr15": "批次屬性15",
                      "LotAttr16": "批次屬性16",
                      "LotAttr17": "批次屬性17",
                      "LotAttr18": "批次屬性18",
                      "LotAttr19": "批次屬性19",
                      "LotAttr20": "批次屬性20"
                    }
                  ]
                }
                """;

            // 3. 測試回應 JSON 解析
            objectMapper.findAndRegisterModules(); // 註冊 JavaTimeModule
            com.znt.outbound.model.jit.JitInvLocApiResponse apiResponse = 
                objectMapper.readValue(mockResponseJson, com.znt.outbound.model.jit.JitInvLocApiResponse.class);

            log.info("成功解析回應 JSON，庫存記錄數量: {}", 
                    apiResponse.getInvLocs() != null ? apiResponse.getInvLocs().size() : 0);

            // 4. 測試資料庫操作（寫入請求記錄）
            String queryIdSql = "SELECT B2B.JIT_INV_LOC_REQUEST_REQUEST_ID_SEQ.NEXTVAL FROM DUAL";
            Long requestId = jdbcTemplate.queryForObject(queryIdSql, Long.class);
            
            String insertRequestSql = """
                INSERT INTO B2B.JIT_INV_LOC_REQUEST (REQUEST_ID, WH_NAME, ZONE_NAME, STORER_ABBR_NAME, SKU, STATUS, CREATED_AT) 
                VALUES (?, ?, ?, ?, ?, 'PENDING', ?)
                """;
            
            jdbcTemplate.update(insertRequestSql, requestId, whName, zoneName, storerAbbrName, sku, 
                               java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

            // 5. 測試將 mock response 寫入 JIT_INV_LOC_LIST 表
            int insertedCount = 0;
            int failedCount = 0;
            String insertInvLocSql = """
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

            for (com.znt.outbound.model.jit.JitInvLoc invLoc : apiResponse.getInvLocs()) {
                try {
                    jdbcTemplate.update(insertInvLocSql,
                        invLoc.getInvLocId(), requestId, invLoc.getWhName(), invLoc.getStorerAbbrName(), 
                        invLoc.getZoneName(), invLoc.getLoc(), invLoc.getLpn(), invLoc.getLot(), invLoc.getSku(),
                        invLoc.getSkuName(), invLoc.getSkuNameE(), invLoc.getSpec(), invLoc.getQty(), 
                        invLoc.getQtyAllocated(), invLoc.getQtyPicked(), invLoc.getQtyHold(), invLoc.getQtyAvailable(),
                        invLoc.getDescriptions(), 
                        invLoc.getCreateDt() != null ? java.sql.Timestamp.valueOf(invLoc.getCreateDt()) : null,
                        invLoc.getCreateP(), invLoc.getCreatePn(), 
                        invLoc.getUpdateDt() != null ? java.sql.Timestamp.valueOf(invLoc.getUpdateDt()) : null,
                        invLoc.getUpdateP(), invLoc.getUpdatePn(), 
                        invLoc.getReceiveDt() != null ? java.sql.Timestamp.valueOf(invLoc.getReceiveDt()) : null,
                        invLoc.getInvAgeDays(), invLoc.getDateCode(), 
                        invLoc.getExpiredDt() != null ? java.sql.Timestamp.valueOf(invLoc.getExpiredDt()) : null,
                        invLoc.getCoo(), invLoc.getPackageType(), invLoc.getInboundType(), invLoc.getVLot(), 
                        invLoc.getQtyPEa(), invLoc.getQtyPCase(), invLoc.getQtyPip(), invLoc.getQtyPPip(),
                        invLoc.getLotAttr01(), invLoc.getLotAttr02(), invLoc.getLotAttr03(), invLoc.getLotAttr04(),
                        invLoc.getLotAttr05(), invLoc.getLotAttr06(), invLoc.getLotAttr07(), invLoc.getLotAttr08(),
                        invLoc.getLotAttr09(), invLoc.getLotAttr10(), invLoc.getLotAttr11(), invLoc.getLotAttr12(),
                        invLoc.getLotAttr13(), invLoc.getLotAttr14(), invLoc.getLotAttr15(), invLoc.getLotAttr16(),
                        invLoc.getLotAttr17(), invLoc.getLotAttr18(), invLoc.getLotAttr19(), invLoc.getLotAttr20()
                    );
                    insertedCount++;
                    log.info("成功寫入庫存資料。InvLocId: {}", invLoc.getInvLocId());
                } catch (Exception e) {
                    failedCount++;
                    log.error("寫入庫存資料失敗。InvLocId: {}", invLoc.getInvLocId(), e);
                }
            }

            // 6. 根據插入結果決定請求狀態
            String finalStatus;
            if (failedCount > 0) {
                finalStatus = "FAILED";
                log.warn("庫存資料插入部分失敗。成功: {}, 失敗: {}", insertedCount, failedCount);
            } else {
                finalStatus = "COMPLETED";
                log.info("庫存資料插入全部成功。成功: {}", insertedCount);
            }
            
            String updateStatusSql = "UPDATE B2B.JIT_INV_LOC_REQUEST SET STATUS = ?, UPDATED_AT = ? WHERE REQUEST_ID = ?";
            jdbcTemplate.update(updateStatusSql, finalStatus, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), requestId);

            // 7. 準備測試結果
            java.util.Map<String, Object> testResult = new java.util.HashMap<>();
            testResult.put("requestId", requestId);
            testResult.put("requestJson", requestJson);
            testResult.put("responseJsonParsed", true);
            testResult.put("inventoryRecordCount", apiResponse.getInvLocs().size());
            testResult.put("insertedInventoryRecords", insertedCount);
            testResult.put("failedInventoryRecords", failedCount);
            testResult.put("finalStatus", finalStatus);
            testResult.put("firstInventoryRecord", apiResponse.getInvLocs().get(0));
            testResult.put("databaseInsert", failedCount > 0 ? "部分失敗" : "成功");

            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢 JSON 格式測試成功", testResult));

        } catch (Exception e) {
            log.error("庫存查詢 JSON 格式測試失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "JSON 格式測試失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== JIT 庫存查詢 JSON 格式測試結束 ===");
        }
    }

    /**
     * 預覽庫存查詢的 JSON 內容（不實際發送）
     */
    @GetMapping("/preview-inventory-json")
    public ResponseEntity<?> previewInventoryJson() {
        log.info("=== 預覽庫存查詢 JSON 內容開始 ===");
        try {
            // 使用測試參數
            String whName = "GT";
            String zoneName = "ZSH基通.上海倉";
            String storerAbbrName = "ZCSH";
            String sku = "SKU001234";

            // 建立請求物件
            com.znt.outbound.model.jit.JitInvLocApiRequest apiRequest = 
                com.znt.outbound.model.jit.JitInvLocApiRequest.builder()
                    .whName(whName)
                    .zoneName(zoneName)
                    .storerAbbrName(storerAbbrName)
                    .sku(sku)
                    .build();

            // 轉換為 JSON 字串
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiRequest);

            log.info("將要發送到 JIT 的庫存查詢 JSON 內容:");
            log.info(jsonString);

            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢 JSON 預覽成功",
                    "JSON內容: " + jsonString));

        } catch (Exception e) {
            log.error("預覽庫存查詢 JSON 失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "預覽過程中發生錯誤: " + e.getMessage(), null));
        } finally {
            log.info("=== 預覽庫存查詢 JSON 內容結束 ===");
        }
    }

    /**
     * 檢查庫存查詢健康狀態
     */
    @GetMapping("/health-inventory")
    public ResponseEntity<?> healthInventory() {
        log.info("收到庫存查詢健康檢查請求");
        try {
            // 檢查是否有待處理的庫存查詢請求
            String pendingSql = "SELECT COUNT(*) FROM B2B.JIT_INV_LOC_REQUEST WHERE STATUS = 'PENDING'";
            Integer pendingCount = jdbcTemplate.queryForObject(pendingSql, Integer.class);

            // 檢查是否有失敗的庫存查詢請求
            String failedSql = "SELECT COUNT(*) FROM B2B.JIT_INV_LOC_REQUEST WHERE STATUS = 'FAILED'";
            Integer failedCount = jdbcTemplate.queryForObject(failedSql, Integer.class);

            // 檢查是否有成功的庫存查詢請求
            String completedSql = "SELECT COUNT(*) FROM B2B.JIT_INV_LOC_REQUEST WHERE STATUS = 'COMPLETED'";
            Integer completedCount = jdbcTemplate.queryForObject(completedSql, Integer.class);

            // 檢查庫存結果數量
            String invLocCountSql = "SELECT COUNT(*) FROM B2B.JIT_INV_LOC_LIST";
            Integer invLocCount = jdbcTemplate.queryForObject(invLocCountSql, Integer.class);

            // 檢查 API 配置
            String invLocApiUrl = apiConfigService.getInvLocApiUrl();

            java.util.Map<String, Object> healthInfo = new java.util.HashMap<>();
            healthInfo.put("pendingRequests", pendingCount);
            healthInfo.put("failedRequests", failedCount);
            healthInfo.put("completedRequests", completedCount);
            healthInfo.put("totalInventoryRecords", invLocCount);
            healthInfo.put("invLocApiUrl", invLocApiUrl != null ? "已設定" : "未設定");
            healthInfo.put("currentTime", java.time.LocalDateTime.now());
            healthInfo.put("serviceName", "JitInvLocService");

            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢健康檢查完成", healthInfo));

        } catch (Exception e) {
            log.error("庫存查詢健康檢查失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "健康檢查失敗: " + e.getMessage(), null));
        }
    }

    /**
     * 執行庫存查詢作業 (呼叫 JitInvLocService.queryInventoryLocation)
     */
    @GetMapping("/process-inventory-query")
    public ResponseEntity<?> processInventoryQuery() {
        log.info("=== 開始執行庫存查詢作業 ===");
        try {
            // 使用預設查詢條件 (所有參數為 null，查詢所有庫存)
            String result = jitInvLocService.queryInventoryLocation(null, null, null, null);
            
            log.info("庫存查詢作業完成，結果: {}", result);
            
            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢作業完成", result));
                
        } catch (Exception e) {
            log.error("執行庫存查詢作業時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "庫存查詢作業失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 庫存查詢作業結束 ===");
        }
    }

    /**
     * 執行庫存查詢作業 (指定查詢條件)
     */
    @GetMapping("/process-inventory-query-with-params")
    public ResponseEntity<?> processInventoryQueryWithParams() {
        log.info("=== 開始執行庫存查詢作業 (指定條件) ===");
        try {
            // 使用指定的查詢條件進行測試
            String whName = "GT";
            String zoneName = "ZSH基通.上海倉";  // 可以設定為特定儲區，如 "基通.上海倉"
            String storerAbbrName = "ZCSH";  // 查詢特定貨主
            String sku = null;  // 可以設定為特定料號
            
            log.info("查詢條件: WhName={}, ZoneName={}, StorerAbbrName={}, Sku={}", 
                    whName, zoneName, storerAbbrName, sku);
            
            String result = jitInvLocService.queryInventoryLocation(whName, zoneName, storerAbbrName, sku);
            
            log.info("庫存查詢作業完成，結果: {}", result);
            
            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢作業完成", result));
                
        } catch (Exception e) {
            log.error("執行庫存查詢作業時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "庫存查詢作業失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 庫存查詢作業結束 ===");
        }
    }

    /**
     * 測試庫存查詢排程任務
     */
    @GetMapping("/test-inventory-scheduled-task")
    public ResponseEntity<?> testInventoryScheduledTask() {
        log.info("=== 開始測試庫存查詢排程任務 ===");
        try {
            // 手動執行排程任務
            jitInvLocScheduledTask.executeJitInventoryLocationQuery();
            
            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢排程任務執行完成", 
                    "排程任務已手動執行，請查看日誌獲取詳細結果"));
                
        } catch (Exception e) {
            log.error("測試庫存查詢排程任務時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "排程任務執行失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 庫存查詢排程任務測試結束 ===");
        }
    }

    /**
     * 獲取庫存查詢排程任務資訊
     */
    @GetMapping("/inventory-scheduled-task-info")
    public ResponseEntity<?> getInventoryScheduledTaskInfo() {
        log.info("收到庫存查詢排程任務資訊查詢請求");
        try {
            java.util.Map<String, Object> taskInfo = new java.util.HashMap<>();
            taskInfo.put("taskName", "JitInvLocScheduledTask");
            taskInfo.put("description", "JIT 庫存查詢排程任務");
            taskInfo.put("schedule", "每日凌晨 2:00 執行 (cron: 0 0 2 * * ?)");
            taskInfo.put("timezone", "Asia/Taipei");
            taskInfo.put("enabled", true);
            taskInfo.put("queryCondition", "查詢所有庫存資料 (所有參數為 null)");
            taskInfo.put("currentTime", java.time.LocalDateTime.now());
            
            // 檢查相關資料表狀態
            String requestCountSql = "SELECT COUNT(*) FROM B2B.JIT_INV_LOC_REQUEST";
            Integer requestCount = jdbcTemplate.queryForObject(requestCountSql, Integer.class);
            
            String invLocCountSql = "SELECT COUNT(*) FROM B2B.JIT_INV_LOC_LIST";
            Integer invLocCount = jdbcTemplate.queryForObject(invLocCountSql, Integer.class);
            
            taskInfo.put("totalRequests", requestCount);
            taskInfo.put("totalInventoryRecords", invLocCount);
            
            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫存查詢排程任務資訊", taskInfo));
                
        } catch (Exception e) {
            log.error("獲取庫存查詢排程任務資訊時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "獲取任務資訊失敗: " + e.getMessage(), null));
        }
    }

    /**
     * 除錯端點：取得 JIT 庫存查詢的原始 JSON 回應
     * 用於檢查 JIT API 實際返回的 JSON 格式
     */
    @GetMapping(value = "/debug-inv-loc", produces = "application/json")
    public ResponseEntity<String> debugInventoryLocation(
            @RequestParam(required = false) String whName,
            @RequestParam(required = false) String zoneName,
            @RequestParam(required = false) String storerAbbrName,
            @RequestParam(required = false) String sku) {
        
        log.info("=== 開始除錯 JIT 庫存查詢 API ===");
        log.info("查詢參數: whName={}, zoneName={}, storerAbbrName={}, sku={}", 
                whName, zoneName, storerAbbrName, sku);
        
        try {
            // 取得 API URL
            String url = apiConfigService.getInvLocApiUrl();
            if (url == null || url.isEmpty()) {
                return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"JIT 庫存查詢 API URL 未設定\"}");
            }
            
            // 取得認證 Token
            String authToken = jitAuthService.getAuthToken();
            if (authToken == null) {
                return ResponseEntity.badRequest()
                    .header("Content-Type", "application/json")
                    .body("{\"error\": \"無法獲取 JIT auth-token\"}");
            }
            
            // 建立 URL 參數
            StringBuilder urlWithParams = new StringBuilder(url);
            urlWithParams.append("?");
            
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
            
            // 設定 HTTP Headers
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("username", apiConfigService.getLoginUsername());
            headers.set("auth-token", authToken);
            
            org.springframework.http.HttpEntity<Void> requestEntity = new org.springframework.http.HttpEntity<>(headers);
            
            log.info("準備發送請求到: {}", urlWithParams.toString());
            
            // 發送請求
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(
                urlWithParams.toString(),
                org.springframework.http.HttpMethod.GET,
                requestEntity,
                String.class
            );
            
            String responseBody = response.getBody();
            log.info("收到回應，狀態碼: {}", response.getStatusCode());
            
            // 直接返回原始的 JSON response body
            // 設定 Content-Type 為 application/json
            return ResponseEntity.ok()
                .header("Content-Type", "application/json")
                .body(responseBody);
            
        } catch (Exception e) {
            log.error("除錯 JIT 庫存查詢時發生錯誤", e);
            return ResponseEntity.badRequest()
                .header("Content-Type", "application/json")
                .body("{\"error\": \"" + e.getMessage() + "\"}");
        } finally {
            log.info("=== 除錯 JIT 庫存查詢結束 ===");
        }
    }
    
    /**
     * 測試 JIT 庫內換料處理與發送
     */
    @GetMapping("/process-and-send-inv-exchange")
    public ResponseEntity<?> processAndSendInvExchange() {
        log.info("=== 手動觸發 JIT 庫內換料處理開始 ===");
        try {
            // 檢查配置
            String apiUrl = apiConfigService.getInvExchangeApiUrl();
            if (apiUrl == null || apiUrl.isEmpty()) {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "JIT 庫內換料 API URL 未設定",
                        "請檢查資料庫中的 JIT-INV_EXCHANGE_URLT 設定"));
            }

            // 執行庫內換料處理
            jitInvExchangeService.processAndSendJitInvExchange();

            return ResponseEntity.ok()
                .body(new TestResponse(true, "JIT 庫內換料處理已觸發",
                    "請檢查日誌以了解處理結果"));

        } catch (Exception e) {
            log.error("處理 JIT 庫內換料時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "JIT 庫內換料處理失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 手動觸發 JIT 庫內換料處理結束 ===");
        }
    }

    /**
     * 測試 JIT 庫內換料 JSON Mapping
     */
    @GetMapping("/test-inv-exchange-mapping")
    public ResponseEntity<?> testInvExchangeMapping() {
        log.info("=== 測試 JIT 庫內換料 JSON Mapping 開始 ===");
        try {
            // 查詢待處理的第一筆換料單
            String headerSql = "SELECT * FROM JIT_INV_EXCHANGE_HEADER WHERE STATUS IN ('PENDING', 'FAILED') AND ROWNUM = 1 ORDER BY HEADER_ID";
            List<Map<String, Object>> headers = jdbcTemplate.queryForList(headerSql);
            
            if (headers.isEmpty()) {
                return ResponseEntity.ok()
                    .body(new TestResponse(false, "沒有待處理的庫內換料資料", 
                        "請先在資料庫中插入測試資料"));
            }

            Map<String, Object> header = headers.get(0);
            Long headerId = ((Number) header.get("HEADER_ID")).longValue();
            
            // 查詢成品明細
            String finalSql = "SELECT * FROM JIT_INV_EXCHANGE_FINAL WHERE HEADER_ID = ? ORDER BY FINAL_ID";
            List<Map<String, Object>> finalLines = jdbcTemplate.queryForList(finalSql, headerId);
            
            // 建立測試用的 JitInvExchangeRequest
            com.znt.outbound.model.jit.JitInvExchangeRequest request = new com.znt.outbound.model.jit.JitInvExchangeRequest();
            
            // 設置主要欄位
            request.setExternalId(header.get("EXTERNAL_ID") != null ? header.get("EXTERNAL_ID").toString() : null);
            request.setExternalNo(header.get("EXTERNAL_NO") != null ? header.get("EXTERNAL_NO").toString() : null);
            request.setWhName(header.get("WH_NAME") != null ? header.get("WH_NAME").toString() : null);
            request.setStorer(header.get("STORER") != null ? header.get("STORER").toString() : null);
            request.setExchangeType(header.get("EXCHANGE_TYPE") != null ? header.get("EXCHANGE_TYPE").toString() : null);
            
            // 處理日期
            if (header.get("APPLY_DATE") != null) {
                if (header.get("APPLY_DATE") instanceof java.sql.Timestamp) {
                    request.setApplyDate(((java.sql.Timestamp) header.get("APPLY_DATE")).toLocalDateTime());
                }
            }
            
            request.setRefNo(header.get("REF_NO") != null ? header.get("REF_NO").toString() : null);
            request.setRemark(header.get("REMARK") != null ? header.get("REMARK").toString() : null);
            
            // 建立成品明細
            List<com.znt.outbound.model.jit.JitInvExchangeSkuFinalLineByApi> jitFinalLines = new ArrayList<>();
            
            for (Map<String, Object> finalLine : finalLines) {
                com.znt.outbound.model.jit.JitInvExchangeSkuFinalLineByApi jitFinalLine = 
                    new com.znt.outbound.model.jit.JitInvExchangeSkuFinalLineByApi();
                
                jitFinalLine.setProduct(finalLine.get("PRODUCT") != null ? finalLine.get("PRODUCT").toString() : null);
                jitFinalLine.setMfSku(finalLine.get("MF_SKU") != null ? finalLine.get("MF_SKU").toString() : null);  // 設置原廠料號
                jitFinalLine.setQty(finalLine.get("QTY") != null ? ((Number) finalLine.get("QTY")).intValue() : null);
                jitFinalLine.setZoneName(finalLine.get("ZONE_NAME") != null ? finalLine.get("ZONE_NAME").toString() : null);
                jitFinalLine.setLot(finalLine.get("LOT") != null ? finalLine.get("LOT").toString() : null);
                jitFinalLine.setBatch(finalLine.get("BATCH") != null ? finalLine.get("BATCH").toString() : null);
                jitFinalLine.setDateCode(finalLine.get("DATE_CODE") != null ? finalLine.get("DATE_CODE").toString() : null);
                jitFinalLine.setCoo(finalLine.get("COO") != null ? finalLine.get("COO").toString() : null);
                
                // 查詢原材料明細
                Long finalId = ((Number) finalLine.get("FINAL_ID")).longValue();
                String materialSql = "SELECT * FROM JIT_INV_EXCHANGE_MATERIAL WHERE FINAL_ID = ? ORDER BY MATERIAL_ID";
                List<Map<String, Object>> materialLines = jdbcTemplate.queryForList(materialSql, finalId);
                
                List<com.znt.outbound.model.jit.JitInvExchangeMaterialLineByApi> jitMaterialLines = new ArrayList<>();
                
                for (Map<String, Object> materialLine : materialLines) {
                    com.znt.outbound.model.jit.JitInvExchangeMaterialLineByApi jitMaterialLine = 
                        new com.znt.outbound.model.jit.JitInvExchangeMaterialLineByApi();
                    
                    jitMaterialLine.setMaterial(materialLine.get("MATERIAL") != null ? materialLine.get("MATERIAL").toString() : null);
                    jitMaterialLine.setMfSku(materialLine.get("MF_SKU") != null ? materialLine.get("MF_SKU").toString() : null);  // 設置原廠料號
                    jitMaterialLine.setQty(materialLine.get("QTY") != null ? ((Number) materialLine.get("QTY")).intValue() : null);
                    jitMaterialLine.setZoneName(materialLine.get("ZONE_NAME") != null ? materialLine.get("ZONE_NAME").toString() : null);
                    jitMaterialLine.setLot(materialLine.get("LOT") != null ? materialLine.get("LOT").toString() : null);
                    jitMaterialLine.setBatch(materialLine.get("BATCH") != null ? materialLine.get("BATCH").toString() : null);
                    jitMaterialLine.setDateCode(materialLine.get("DATE_CODE") != null ? materialLine.get("DATE_CODE").toString() : null);
                    jitMaterialLine.setCoo(materialLine.get("COO") != null ? materialLine.get("COO").toString() : null);
                    
                    jitMaterialLines.add(jitMaterialLine);
                }
                
                jitFinalLine.setMaterialLines(jitMaterialLines);
                jitFinalLines.add(jitFinalLine);
            }
            
            request.setFinalLines(jitFinalLines);
            
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("externalId", request.getExternalId());
            result.put("exchangeType", request.getExchangeType());
            result.put("finalLineCount", request.getFinalLines() != null ? request.getFinalLines().size() : 0);
            result.put("jsonMapping", request);  // 直接返回物件，Spring 會自動轉換為 JSON
            
            return ResponseEntity.ok()
                .body(new TestResponse(true, "JIT 庫內換料 JSON Mapping 測試成功", result));
            
        } catch (Exception e) {
            log.error("測試 JIT 庫內換料 JSON Mapping 失敗", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "測試失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 測試 JIT 庫內換料 JSON Mapping 結束 ===");
        }
    }

    /**
     * 手動執行庫內換料排程任務
     */
    @GetMapping("/run-inv-exchange-schedule")
    public ResponseEntity<?> runInvExchangeSchedule() {
        log.info("=== 手動執行庫內換料排程任務開始 ===");
        try {
            jitInvExchangeScheduledTask.executeManually();
            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫內換料排程任務已手動執行",
                    "請檢查日誌以了解執行結果"));
        } catch (Exception e) {
            log.error("手動執行庫內換料排程任務時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "執行失敗: " + e.getMessage(), null));
        } finally {
            log.info("=== 手動執行庫內換料排程任務結束 ===");
        }
    }

    /**
     * 取得庫內換料排程任務資訊
     */
    @GetMapping("/inv-exchange-schedule-info")
    public ResponseEntity<?> getInvExchangeScheduleInfo() {
        log.info("=== 取得庫內換料排程任務資訊 ===");
        try {
            String taskInfo = jitInvExchangeScheduledTask.getTaskInfo();
            boolean isEnabled = jitInvExchangeScheduledTask.isSchedulingEnabled();
            
            Map<String, Object> info = new java.util.HashMap<>();
            info.put("taskInfo", taskInfo);
            info.put("enabled", isEnabled);
            info.put("cronExpression", "0 */5 * * * ?");
            info.put("timezone", "Asia/Taipei");
            info.put("frequency", "每5分鐘執行一次");
            info.put("description", "自動處理待處理的JIT庫內換料請求");
            
            return ResponseEntity.ok()
                .body(new TestResponse(true, "庫內換料排程任務資訊", info));
                
        } catch (Exception e) {
            log.error("獲取庫內換料排程任務資訊時發生錯誤", e);
            return ResponseEntity.internalServerError()
                .body(new TestResponse(false, "獲取任務資訊失敗: " + e.getMessage(), null));
        }
    }

    /**
     * 測試回應物件
     */
    public static class TestResponse {
        private boolean success;
        private String message;
        private Object data;

        public TestResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }

        // Getters
        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public Object getData() {
            return data;
        }

        // Setters
        public void setSuccess(boolean success) {
            this.success = success;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public void setData(Object data) {
            this.data = data;
        }
    }
}
