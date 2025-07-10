package com.znt.outbound.controller;

import com.znt.outbound.model.jit.JitAsnRequest;
import com.znt.outbound.scheduler.JitAsnScheduledTask;
import com.znt.outbound.service.JitAsnMappingService;
import com.znt.outbound.service.JitAuthService;
import com.znt.outbound.service.ApiConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test/jit")
@RequiredArgsConstructor
@Slf4j
public class JitTestController {

    private final JitAsnMappingService jitAsnMappingService;
    private final JdbcTemplate jdbcTemplate;
    private final JitAuthService jitAuthService;
    private final ApiConfigService apiConfigService;
    private final JitAsnScheduledTask jitAsnScheduledTask;

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

            return ResponseEntity.ok()
                .body(new TestResponse(true, "資料庫連線測試成功",
                    String.format("Header: %d 筆, Line: %d 筆, Pending: %d 筆",
                        headerCount, lineCount, pendingCount)));

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
                    QTY_EXPECTED, LINE_NW, LINE_GW, LINE_CUBE, LINE_UOM,
                    LINE_USER_DEF1, LINE_USER_DEF2, LINE_USER_DEF3, LINE_USER_DEF4, LINE_USER_DEF5, LINE_DESCRIPTIONS,
                    LOC, LPN, DATE_CODE, EXPIRED_DT, ATTR_COO, PACKAGE_TYPE, VLOT,
                    LOT_ATTR01, LOT_ATTR02, LOT_ATTR03, LOT_ATTR04, LOT_ATTR05
                ) VALUES (
                    ?, 'LINE20230611001', 'IC-1001', 'TechGlobal', '微控制器芯片', 'Microcontroller IC', 'ARM Cortex-M4 32-bit',
                    '电子元器件', '集成电路', '嵌入式系统', 'TW', 0.01, 0.008, '个',
                    'A', 1000, 5000, 10, 10, 2, 0.0000002, 0.0001,
                    '工业级温度范围', 'ESD敏感', '无铅', '符合RoHS', '包装形式：卷带', '最小包装量：2500',
                    10000, 80.0, 100.0, 0.25, '个',
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
