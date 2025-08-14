package com.znt.outbound.service;

import com.znt.outbound.model.jit.JitInvMoveOrTradeRequest;
import com.znt.outbound.model.jit.JitInvMoveOrTradeLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JitInvMoveOrTradeMappingService {

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
     * 從生產系統預處理庫內移倉資料到 JIT 中介表格
     * 此方法執行同事提供的 INSERT SQL，將最新的庫內移倉資料準備到中介表格中
     */
    public void prepareInvMoveDataFromSource() {
        String operationId = generateOperationId();
        log.info("[{}] 開始執行庫內移倉資料預處理作業...", operationId);

        long startTime = System.currentTimeMillis();

        try {
            // 讀取預處理 SQL
            String sql = loadPrepareSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取預處理 SQL 查詢語句，終止作業。", operationId);
                return;
            }

            log.info("[{}] 開始執行庫內移倉資料預處理 SQL...", operationId);
            
            // 執行預處理 SQL（包含 BEGIN...END 區塊）
            jdbcTemplate.execute(sql);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("[{}] 庫內移倉資料預處理完成，執行時間: {}ms", operationId, duration);

        } catch (DataAccessException e) {
            log.error("[{}] 庫內移倉資料預處理資料庫操作失敗", operationId, e);
            throw new RuntimeException("庫內移倉資料預處理失敗", e);
        } catch (Exception e) {
            log.error("[{}] 庫內移倉資料預處理發生未預期錯誤", operationId, e);
            throw new RuntimeException("庫內移倉資料預處理失敗", e);
        }
    }

    /**
     * 讀取預處理 SQL 查詢語句
     */
    private String loadPrepareSqlQuery() {
        try {
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("sql/prepare_inv_move_data.sql"),
                    StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("無法讀取預處理 SQL 檔案: prepare_inv_move_data.sql", e);
            return null;
        }
    }

    /**
     * 從生產系統預處理關係人交易資料到 JIT 中介表格
     * 此方法執行關係人交易的 INSERT SQL，將最新的交易資料準備到中介表格中
     */
    public void prepareInvTradeDataFromSource() {
        String operationId = generateOperationId();
        log.info("[{}] 開始執行關係人交易資料預處理作業...", operationId);

        long startTime = System.currentTimeMillis();

        try {
            // 讀取預處理 SQL
            String sql = loadPrepareTradeDataSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取關係人交易預處理 SQL 查詢語句，終止作業。", operationId);
                return;
            }

            log.info("[{}] 開始執行關係人交易資料預處理 SQL...", operationId);
            
            // 執行預處理 SQL（包含 BEGIN...END 區塊）
            jdbcTemplate.execute(sql);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("[{}] 關係人交易資料預處理完成，執行時間: {}ms", operationId, duration);

        } catch (DataAccessException e) {
            log.error("[{}] 關係人交易資料預處理資料庫操作失敗", operationId, e);
            throw new RuntimeException("關係人交易資料預處理失敗", e);
        } catch (Exception e) {
            log.error("[{}] 關係人交易資料預處理發生未預期錯誤", operationId, e);
            throw new RuntimeException("關係人交易資料預處理失敗", e);
        }
    }

    /**
     * 讀取關係人交易預處理 SQL 查詢語句
     */
    private String loadPrepareTradeDataSqlQuery() {
        try {
            Reader reader = new InputStreamReader(
                    getClass().getClassLoader().getResourceAsStream("sql/prepare_inv_trade_data.sql"),
                    StandardCharsets.UTF_8
            );
            return FileCopyUtils.copyToString(reader);
        } catch (Exception e) {
            log.error("無法讀取關係人交易預處理 SQL 檔案: prepare_inv_trade_data.sql", e);
            return null;
        }
    }

    /**
     * 執行所有庫內移倉/交易資料預處理
     * 包含庫內移倉和關係人交易兩種資料類型
     */
    public void prepareAllInvData() {
        String operationId = generateOperationId();
        log.info("[{}] 開始執行所有庫內移倉/交易資料預處理作業...", operationId);

        long startTime = System.currentTimeMillis();

        try {
            // 執行庫內移倉資料預處理
            log.info("[{}] 執行庫內移倉資料預處理...", operationId);
            prepareInvMoveDataFromSource();
            
            // 執行關係人交易資料預處理
            log.info("[{}] 執行關係人交易資料預處理...", operationId);
            prepareInvTradeDataFromSource();

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            log.info("[{}] 所有庫內移倉/交易資料預處理完成，總執行時間: {}ms", operationId, duration);

        } catch (Exception e) {
            log.error("[{}] 庫內移倉/交易資料預處理發生錯誤", operationId, e);
            throw e;
        }
    }

    /**
     * 處理並發送 JIT 庫內移倉/交易的主要方法。
     * 該方法將持續查詢並處理待處理的庫內移倉/交易資料，每次只處理一筆 ExternalId，
     * 符合 JIT 系統「每次 API 調用只能傳送一筆 ExternalId 資料」的限制。
     */
    public void processAndSendJitInvMoveOrTrade() {
        String operationId = generateOperationId();
        log.info("[{}] 開始執行 JIT 庫內移倉/交易處理與發送作業...", operationId);

        long startTime = System.currentTimeMillis();
        ProcessingStatistics stats = new ProcessingStatistics();

        try {
            // 步驟 1: 預處理所有庫內移倉/交易資料（從生產系統插入到中介表格）
            log.info("[{}] 步驟 1: 執行所有庫內移倉/交易資料預處理...", operationId);
            prepareAllInvData();

            // 步驟 2: 讀取 SQL 查詢語句
            String sql = loadSqlQuery();
            if (sql == null) {
                log.error("[{}] 無法讀取 SQL 查詢語句，終止處理作業。", operationId);
                return;
            }

            // 步驟 3: 迴圈處理所有待處理的庫內移倉/交易資料
            processInvMoveOrTradeDataLoop(operationId, sql, stats);

        } catch (Exception e) {
            log.error("[{}] JIT 庫內移倉/交易處理作業發生嚴重錯誤", operationId, e);
            stats.criticalErrors++;
        } finally {
            // 記錄最終統計和性能指標
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            logFinalStatistics(operationId, stats, duration);

            // 如果有失敗的記錄，記錄詳細資訊以便後續分析
            if (stats.failedCount > 0) {
                log.warn("[{}] 本次處理有 {} 筆庫內移倉/交易失敗，建議檢查失敗原因並考慮重新處理",
                        operationId, stats.failedCount);
                logFailedInvMoveOrTradeSummary();
            }
        }
    }

    /**
     * 處理庫內移倉/交易資料的主要迴圈
     */
    private void processInvMoveOrTradeDataLoop(String operationId, String sql, ProcessingStatistics stats) {
        int consecutiveErrors = 0;
        final int maxConsecutiveErrors = 5;
        
        // SQL 現在只查詢 PENDING 狀態，不需要防止無限循環的機制

        while (true) {
            try {
                log.debug("[{}] 開始查詢下一筆待處理的庫內移倉/交易資料...", operationId);

                // 查詢單筆待處理資料
                List<Map<String, Object>> rows;
                try {
                    rows = jdbcTemplate.queryForList(sql);
                } catch (Exception e) {
                    log.error("[{}] 查詢待處理庫內移倉/交易資料時發生錯誤", operationId, e);
                    stats.databaseErrors++;
                    consecutiveErrors++;

                    if (consecutiveErrors >= maxConsecutiveErrors) {
                        log.error("[{}] 連續 {} 次查詢失敗，終止處理作業", operationId, maxConsecutiveErrors);
                        break;
                    }

                    // 等待後重試
                    sleepSafely(2000, operationId);
                    continue;
                }

                if (rows.isEmpty()) {
                    log.info("[{}] 沒有更多待處理的庫內移倉/交易資料，處理作業完成。", operationId);
                    break;
                }

                // 重置連續錯誤計數
                consecutiveErrors = 0;
                stats.processedCount++;

                String externalId = getStringValue(rows.get(0), "EXTERNAL_ID");
                
                log.info("[{}] 開始處理第 {} 筆庫內移倉/交易資料，ExternalId: {}",
                        operationId, stats.processedCount, externalId);

                // 步驟 3: 處理單筆庫內移倉/交易資料
                long singleStartTime = System.currentTimeMillis();
                boolean success = processSingleInvMoveOrTradeWithErrorHandling(operationId, rows, externalId, stats);
                long singleDuration = System.currentTimeMillis() - singleStartTime;

                if (success) {
                    stats.successCount++;
                    log.info("[{}] 庫內移倉/交易 (ExternalId: {}) 處理成功，耗時: {}ms",
                            operationId, externalId, singleDuration);
                } else {
                    stats.failedCount++;
                    log.error("[{}] 庫內移倉/交易 (ExternalId: {}) 處理失敗，耗時: {}ms，狀態已更新為 FAILED",
                            operationId, externalId, singleDuration);
                }

                // 為避免過度頻繁的資料庫查詢，加入短暫延遲
                sleepSafely(100, operationId);

            } catch (Exception e) {
                log.error("[{}] 處理迴圈中發生未預期的錯誤", operationId, e);
                stats.criticalErrors++;
                consecutiveErrors++;

                if (consecutiveErrors >= maxConsecutiveErrors) {
                    log.error("[{}] 連續 {} 次嚴重錯誤，終止處理作業", operationId, maxConsecutiveErrors);
                    break;
                }

                sleepSafely(5000, operationId); // 發生嚴重錯誤時等待更長時間
            }
        }
    }

    /**
     * 生成操作ID，用於追蹤單次處理作業
     */
    private String generateOperationId() {
        return "JIT-INV-MOVE-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 安全的睡眠方法，支援中斷處理
     */
    private void sleepSafely(long millis, String operationId) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[{}] 處理過程被中斷", operationId);
        }
    }

    /**
     * 記錄最終統計資訊
     */
    private void logFinalStatistics(String operationId, ProcessingStatistics stats, long duration) {
        log.info("[{}] JIT 庫內移倉/交易處理作業結束。耗時: {}ms", operationId, duration);
        log.info("[{}] 處理統計: 總計: {} 筆, 成功: {} 筆, 失敗: {} 筆",
                operationId, stats.processedCount, stats.successCount, stats.failedCount);
        log.info("[{}] 錯誤統計: 資料庫錯誤: {} 次, API錯誤: {} 次, 映射錯誤: {} 次, 嚴重錯誤: {} 次",
                operationId, stats.databaseErrors, stats.apiErrors, stats.mappingErrors, stats.criticalErrors);

        if (stats.processedCount > 0) {
            double successRate = (double) stats.successCount / stats.processedCount * 100;
            log.info("[{}] 成功率: {:.2f}%, 平均處理時間: {:.2f}ms/筆",
                    operationId, successRate, (double) duration / stats.processedCount);
        }
    }

    /**
     * 帶錯誤處理的單筆庫內移倉/交易處理方法
     */
    private boolean processSingleInvMoveOrTradeWithErrorHandling(String operationId, List<Map<String, Object>> rows,
                                                               String externalId, ProcessingStatistics stats) {
        try {
            return processSingleInvMoveOrTrade(rows, externalId);
        } catch (Exception e) {
            log.error("[{}] 處理庫內移倉/交易 (ExternalId: {}) 時發生未預期的錯誤", operationId, externalId, e);

            // 根據異常類型更新統計
            if (e.getMessage() != null) {
                if (e.getMessage().contains("資料映射")) {
                    stats.mappingErrors++;
                } else if (e.getMessage().contains("API") || e.getMessage().contains("HTTP")) {
                    stats.apiErrors++;
                } else if (e.getMessage().contains("資料庫") || e.getMessage().contains("SQL")) {
                    stats.databaseErrors++;
                } else {
                    stats.criticalErrors++;
                }
            } else {
                stats.criticalErrors++;
            }

            // 嘗試更新狀態為失敗
            try {
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED,
                        "處理過程發生未預期錯誤: " + e.getClass().getSimpleName());
            } catch (Exception statusUpdateError) {
                log.error("[{}] 更新失敗狀態時也發生錯誤，ExternalId: {}", operationId, externalId, statusUpdateError);
            }

            return false;
        }
    }

    /**
     * 記錄失敗庫內移倉/交易的摘要資訊，便於問題分析
     */
    private void logFailedInvMoveOrTradeSummary() {
        try {
            String failedSql = "SELECT EXTERNAL_ID, UPDATED_AT FROM B2B.JIT_MOVE_TRADE_HEADER WHERE STATUS = ? ORDER BY UPDATED_AT DESC";
            List<Map<String, Object>> failedData = jdbcTemplate.queryForList(failedSql, STATUS_FAILED);

            if (!failedData.isEmpty()) {
                log.info("=== 失敗庫內移倉/交易摘要 ===");
                for (Map<String, Object> data : failedData) {
                    String externalId = getStringValue(data, "EXTERNAL_ID");
                    Object updatedAt = data.get("UPDATED_AT");
                    log.info("失敗庫內移倉/交易: {}, 最後更新時間: {}", externalId, updatedAt);
                }
                log.info("=== 失敗庫內移倉/交易摘要結束 ===");
            }
        } catch (Exception e) {
            log.error("記錄失敗庫內移倉/交易摘要時發生錯誤", e);
        }
    }

    /**
     * 重新處理失敗的庫內移倉/交易資料
     * 此方法可以被排程任務調用，定期重試失敗的庫內移倉/交易
     */
    public void retryFailedInvMoveOrTrade() {
        log.info("開始重新處理失敗的庫內移倉/交易資料...");

        try {
            // 查詢失敗狀態的庫內移倉/交易，按更新時間排序
            String failedSql = "SELECT EXTERNAL_ID FROM B2B.JIT_MOVE_TRADE_HEADER WHERE STATUS = ? ORDER BY UPDATED_AT ASC";
            List<Map<String, Object>> failedData = jdbcTemplate.queryForList(failedSql, STATUS_FAILED);

            if (failedData.isEmpty()) {
                log.info("沒有找到需要重試的失敗庫內移倉/交易");
                return;
            }

            log.info("找到 {} 筆失敗的庫內移倉/交易，開始重新處理", failedData.size());

            int retryCount = 0;
            int retrySuccessCount = 0;

            for (Map<String, Object> dataRow : failedData) {
                String externalId = getStringValue(dataRow, "EXTERNAL_ID");
                retryCount++;

                log.info("重試第 {} 筆失敗庫內移倉/交易，ExternalId: {}", retryCount, externalId);

                // 將狀態重置為 PENDING，讓主處理流程重新處理
                boolean resetSuccess = updateInvMoveOrTradeStatus(externalId, STATUS_PENDING, null);
                if (resetSuccess) {
                    retrySuccessCount++;
                    log.info("成功重置庫內移倉/交易狀態為 PENDING，ExternalId: {}", externalId);
                } else {
                    log.error("重置庫內移倉/交易狀態失敗，ExternalId: {}", externalId);
                }

                // 避免過度頻繁的資料庫操作
                try {
                    Thread.sleep(500); // 500ms 延遲
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("重試過程被中斷");
                    break;
                }
            }

            log.info("失敗庫內移倉/交易重試作業完成。總計: {} 筆，成功重置: {} 筆", retryCount, retrySuccessCount);

        } catch (Exception e) {
            log.error("重新處理失敗庫內移倉/交易時發生錯誤", e);
        }
    }

    /**
     * 讀取 SQL 查詢語句
     * @return SQL 查詢語句，如果讀取失敗則返回 null
     */
    private String loadSqlQuery() {
        try {
            try (Reader reader = new InputStreamReader(
                    this.getClass().getResourceAsStream("/sql/select_inv_move_for_jit.sql"),
                    StandardCharsets.UTF_8)) {
                return FileCopyUtils.copyToString(reader);
            }
        } catch (Exception e) {
            log.error("讀取 select_inv_move_for_jit.sql 檔案失敗。", e);
            return null;
        }
    }

    /**
     * 處理單筆庫內移倉/交易資料
     * @param rows 查詢結果（單一 ExternalId 的所有明細行）
     * @param externalId 外部單號
     * @return 處理是否成功
     */
    private boolean processSingleInvMoveOrTrade(List<Map<String, Object>> rows, String externalId) {
        String seqId = null;
        String currentStep = "初始化";

        try {
            // 步驟 1: 將查詢結果映射到 JitInvMoveOrTradeRequest 物件
            currentStep = "資料映射";
            log.debug("開始映射庫內移倉/交易資料，ExternalId: {}", externalId);

            JitInvMoveOrTradeRequest requestToSend;
            try {
                requestToSend = mapDataToJitInvMoveOrTradeRequest(rows);
            } catch (Exception e) {
                log.error("庫內移倉/交易資料映射過程發生異常，ExternalId: {}", externalId, e);
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED, "資料映射異常: " + e.getMessage());
                return false;
            }

            if (requestToSend == null) {
                log.error("資料映射失敗，無法產生有效的 JitInvMoveOrTradeRequest 物件，ExternalId: {}", externalId);
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED, "資料映射失敗");
                return false;
            }

            // 步驟 2: 更新狀態為 PROCESSING
            currentStep = "狀態更新";
            try {
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_PROCESSING, null);
            } catch (Exception e) {
                log.error("更新庫內移倉/交易狀態為 PROCESSING 時發生異常，ExternalId: {}", externalId, e);
                // 狀態更新失敗不應該阻止後續處理，記錄警告即可
                log.warn("狀態更新失敗，但繼續處理庫內移倉/交易，ExternalId: {}", externalId);
            }

            // 步驟 3: 寫入封套 (Envelope)
            currentStep = "封套建立";
            log.info("準備為庫內移倉/交易 (ExternalId: {}) 寫入 B2B 封套...", externalId);

            try {
                String providerName = apiConfigService.getProviderName();
                seqId = envelopeService.createJitInvMoveOrTradeEnvelope(externalId, providerName);
                log.debug("成功建立封套，ExternalId: {}, SEQ_ID: {}", externalId, seqId);
            } catch (Exception e) {
                log.error("建立 B2B 封套時發生異常，ExternalId: {}", externalId, e);
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED, "封套建立失敗: " + e.getMessage());
                return false;
            }

            // 步驟 4: 發送到 JIT API
            currentStep = "API 發送";
            log.info("B2B 封套寫入成功 (SEQ_ID: {}), 準備發送庫內移倉/交易 (ExternalId: {}) 到 JIT。", seqId, externalId);

            ResponseEntity<String> response;
            try {
                response = jitApiClient.sendInvMoveOrTrade(requestToSend);
                log.debug("JIT API 調用完成，ExternalId: {}, Response Status: {}",
                        externalId, response != null ? response.getStatusCode() : "null");
            } catch (Exception e) {
                log.error("調用 JIT API 時發生異常，ExternalId: {}", externalId, e);

                // 更新封套狀態為失敗
                try {
                    envelopeService.updateEnvelopeStatus(seqId, "F");
                } catch (Exception envelopeError) {
                    log.error("更新封套狀態為失敗時也發生錯誤，SEQ_ID: {}", seqId, envelopeError);
                }

                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED, "API 調用異常: " + e.getMessage());
                return false;
            }

            // 步驟 5: 根據 API 回應更新狀態
            currentStep = "結果處理";
            return handleApiResponse(externalId, seqId, response);

        } catch (Exception e) {
            // 處理過程中發生未預期的異常
            log.error("在處理庫內移倉/交易 (ExternalId: {}) 的 {} 階段發生嚴重錯誤", externalId, currentStep, e);

            // 嘗試清理資源
            cleanupOnError(externalId, seqId, currentStep, e);

            return false;
        }
    }

    /**
     * 處理 API 回應並更新相應狀態
     */
    private boolean handleApiResponse(String externalId, String seqId, ResponseEntity<String> response) {
        try {
            if (response != null && response.getStatusCode().is2xxSuccessful()) {
                // 成功情況
                envelopeService.updateEnvelopeStatus(seqId, "S"); // Success
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_COMPLETED, "成功發送到 JIT");
                log.info("庫內移倉/交易 (ExternalId: {}) 成功發送到 JIT，HTTP Status: {}", externalId, response.getStatusCode());

                // 發送成功通知郵件
                sendNotificationSafely(seqId, "成功");

                return true;
            } else {
                // 失敗情況
                String errorMsg = response != null ?
                    String.format("HTTP Status: %s, Response: %s", response.getStatusCode(), response.getBody()) :
                    "API 回應為 null";

                envelopeService.updateEnvelopeStatus(seqId, "F"); // Failed
                updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED, errorMsg);
                log.error("庫內移倉/交易 (ExternalId: {}) 發送失敗：{}", externalId, errorMsg);

                // 發送失敗通知郵件
                sendNotificationSafely(seqId, "失敗");

                return false;
            }
        } catch (Exception e) {
            log.error("處理 API 回應時發生異常，ExternalId: {}", externalId, e);
            cleanupOnError(externalId, seqId, "結果處理", e);
            return false;
        }
    }

    /**
     * 安全地發送郵件通知，確保郵件發送失敗不會影響主要業務流程
     */
    private void sendNotificationSafely(String seqId, String statusDescription) {
        try {
            log.info("準備為 SEQ_ID: {} 發送 {} 通知郵件", seqId, statusDescription);
            statusNotificationService.sendNotification(seqId);
            log.info("已成功為 SEQ_ID: {} 發送 {} 通知郵件", seqId, statusDescription);
        } catch (Exception e) {
            // 郵件發送失敗不應該影響主要業務流程，只記錄錯誤
            log.error("為 SEQ_ID: {} 發送 {} 通知郵件時發生錯誤，但不影響主要業務流程", seqId, statusDescription, e);
        }
    }

    /**
     * 錯誤發生時的清理工作
     */
    private void cleanupOnError(String externalId, String seqId, String currentStep, Exception originalError) {
        try {
            // 更新封套狀態
            if (seqId != null) {
                try {
                    envelopeService.updateEnvelopeStatus(seqId, "F");
                    // 發送失敗通知郵件
                    sendNotificationSafely(seqId, "失敗");
                } catch (Exception e) {
                    log.error("清理封套狀態時發生錯誤，SEQ_ID: {}", seqId, e);
                }
            }

            // 更新庫內移倉/交易狀態
            String errorMessage = String.format("%s階段發生異常: %s", currentStep, originalError.getMessage());
            updateInvMoveOrTradeStatusWithRetry(externalId, STATUS_FAILED, errorMessage);

        } catch (Exception e) {
            log.error("執行錯誤清理工作時發生異常，ExternalId: {}", externalId, e);
        }
    }

    /**
     * 帶重試機制的庫內移倉/交易狀態更新方法
     * @param externalId 外部單號
     * @param status 新狀態 (PENDING, PROCESSING, COMPLETED, FAILED)
     * @param errorMessage 錯誤訊息（目前不儲存到資料庫，僅用於日誌記錄）
     */
    private void updateInvMoveOrTradeStatusWithRetry(String externalId, String status, String errorMessage) {
        long startTime = System.currentTimeMillis();

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                boolean success = updateInvMoveOrTradeStatus(externalId, status, errorMessage);
                if (success) {
                    long duration = System.currentTimeMillis() - startTime;
                    if (attempt > 1) {
                        log.info("庫內移倉/交易狀態更新成功（第 {} 次嘗試），ExternalId: {}, Status: {}, 耗時: {}ms",
                                attempt, externalId, status, duration);
                    } else {
                        log.debug("庫內移倉/交易狀態更新成功，ExternalId: {}, Status: {}, 耗時: {}ms",
                                externalId, status, duration);
                    }
                    return; // 成功則直接返回
                }

                // 如果更新失敗但沒有異常，記錄警告並重試
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("庫內移倉/交易狀態更新失敗（第 {} 次嘗試），可能是記錄不存在或已被其他程序處理，將在 {}ms 後重試，ExternalId: {}, Status: {}",
                            attempt, RETRY_DELAY_MS, externalId, status);
                    Thread.sleep(RETRY_DELAY_MS);
                } else {
                    long totalDuration = System.currentTimeMillis() - startTime;
                    log.error("庫內移倉/交易狀態更新失敗，已達最大重試次數 {}，ExternalId: {}, Status: {}, 總耗時: {}ms",
                            MAX_RETRY_ATTEMPTS, externalId, status, totalDuration);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("庫內移倉/交易狀態更新重試過程被中斷，ExternalId: {}, Status: {}", externalId, status);
                return;
            } catch (Exception e) {
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    log.warn("庫內移倉/交易狀態更新發生異常（第 {} 次嘗試），異常類型: {}，將在 {}ms 後重試，ExternalId: {}, Status: {}",
                            attempt, e.getClass().getSimpleName(), RETRY_DELAY_MS, externalId, status, e);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("庫內移倉/交易狀態更新重試過程被中斷，ExternalId: {}, Status: {}", externalId, status);
                        return;
                    }
                } else {
                    long totalDuration = System.currentTimeMillis() - startTime;
                    log.error("庫內移倉/交易狀態更新失敗，已達最大重試次數 {}，ExternalId: {}, Status: {}, 總耗時: {}ms",
                            MAX_RETRY_ATTEMPTS, externalId, status, totalDuration, e);
                }
            }
        }
    }

    /**
     * 更新庫內移倉/交易的處理狀態（單次嘗試）
     * @param externalId 外部單號
     * @param status 新狀態 (PENDING, PROCESSING, COMPLETED, FAILED)
     * @param errorMessage 錯誤訊息（目前不儲存到資料庫，僅用於日誌記錄）
     * @return 更新是否成功
     */
    private boolean updateInvMoveOrTradeStatus(String externalId, String status, String errorMessage) {
        try {
            String updateSql = "UPDATE B2B.JIT_MOVE_TRADE_HEADER SET STATUS = ?, UPDATED_AT = SYSDATE WHERE EXTERNAL_ID = ?";
            int updatedRows = jdbcTemplate.update(updateSql, status, externalId);

            if (updatedRows > 0) {
                log.debug("成功更新庫內移倉/交易狀態，ExternalId: {}, Status: {}", externalId, status);
                if (errorMessage != null && STATUS_FAILED.equals(status)) {
                    log.error("庫內移倉/交易處理錯誤詳情，ExternalId: {}, Error: {}", externalId, errorMessage);
                } else if (errorMessage != null && !STATUS_FAILED.equals(status)) {
                    log.info("庫內移倉/交易處理狀態詳情，ExternalId: {}, Status: {}, Message: {}", externalId, status, errorMessage);
                }
                return true;
            } else {
                log.warn("更新庫內移倉/交易狀態時沒有找到對應的記錄，ExternalId: {}", externalId);
                return false;
            }
        } catch (Exception e) {
            log.error("更新庫內移倉/交易狀態時發生錯誤，ExternalId: {}, Status: {}", externalId, status, e);
            throw e; // 重新拋出異常，讓重試機制處理
        }
    }

    /**
     * 將從資料庫查詢出的扁平結果映射到 JitInvMoveOrTradeRequest 物件。
     * 此方法已針對單一 EXTERNAL_ID 處理進行優化，包含資料完整性驗證。
     * @param rows 從 jdbcTemplate 查詢出的結果列表（應該只包含單一 EXTERNAL_ID 的資料）
     * @return 組裝好的 JitInvMoveOrTradeRequest 物件，如果無法組裝則返回 null
     */
    private JitInvMoveOrTradeRequest mapDataToJitInvMoveOrTradeRequest(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            log.warn("查詢結果為空，無法建立 JitInvMoveOrTradeRequest");
            return null;
        }

        try {
            // 步驟 1: 驗證資料完整性 - 確保所有資料行都屬於同一個 EXTERNAL_ID
            if (!validateDataConsistency(rows)) {
                log.error("資料完整性驗證失敗，查詢結果包含多個不同的 EXTERNAL_ID");
                return null;
            }

            // 步驟 2: 取得第一筆資料作為 Header 資訊的來源
            Map<String, Object> firstRow = rows.get(0);
            String externalId = getStringValue(firstRow, "EXTERNAL_ID");

            log.debug("開始映射庫內移倉/交易資料，ExternalId: {}, 明細行數: {}", externalId, rows.size());

            // 步驟 3: 建立 JitInvMoveOrTradeRequest 物件並設定 Header 資訊
            JitInvMoveOrTradeRequest request = new JitInvMoveOrTradeRequest();

            // 映射 Header 欄位 (來自 JIT_INV_MOVE_TRADE_HEADER 表格)
            request.setExternalId(externalId);
            request.setExternalNo(getStringValue(firstRow, "EXTERNAL_NO"));
            request.setWhName(getStringValue(firstRow, "WH_NAME"));
            request.setTradeType(getStringValue(firstRow, "TRADE_TYPE"));
            request.setFromStorer(getStringValue(firstRow, "FROM_STORER"));
            request.setToStorer(getStringValue(firstRow, "TO_STORER"));
            request.setApplyDate(getTimestampAsLocalDateTime(firstRow, "APPLY_DATE"));
            request.setRefNo(getStringValue(firstRow, "REF_NO"));
            request.setRemark(getStringValue(firstRow, "HEADER_REMARK"));

            // 步驟 4: 建立 Lines 列表並映射每一行資料
            List<JitInvMoveOrTradeLine> lines = new ArrayList<>();
            int successfulLines = 0;
            int failedLines = 0;

            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                String sku = getStringValue(row, "SKU");

                try {
                    JitInvMoveOrTradeLine line = createJitInvMoveOrTradeLine(row);
                    if (line != null) {
                        lines.add(line);
                        successfulLines++;
                        log.debug("成功映射明細行 {}/{}: SKU={}", i + 1, rows.size(), sku);
                    } else {
                        failedLines++;
                        log.warn("映射明細行失敗 {}/{}: SKU={}", i + 1, rows.size(), sku);
                    }
                } catch (Exception e) {
                    failedLines++;
                    log.error("映射明細行時發生異常 {}/{}: SKU={}", i + 1, rows.size(), sku, e);
                }
            }

            // 步驟 5: 驗證映射結果
            if (lines.isEmpty()) {
                log.error("所有明細行映射都失敗，無法建立有效的 JitInvMoveOrTradeRequest，ExternalId: {}", externalId);
                return null;
            }

            if (failedLines > 0) {
                log.warn("部分明細行映射失敗，ExternalId: {}, 成功: {} 行, 失敗: {} 行",
                        externalId, successfulLines, failedLines);
            }

            request.setLines(lines);

            log.info("成功映射 JitInvMoveOrTradeRequest，ExternalId: {}, 總明細行: {}, 成功映射: {} 行, 失敗: {} 行",
                    externalId, rows.size(), successfulLines, failedLines);

            return request;

        } catch (Exception e) {
            log.error("映射 JitInvMoveOrTradeRequest 時發生嚴重錯誤，ExternalNo: {}",
                    rows != null && !rows.isEmpty() ? getStringValue(rows.get(0), "EXTERNAL_NO") : "unknown", e);

            // 記錄詳細的錯誤資訊以便除錯
            if (rows != null) {
                log.error("錯誤發生時的資料行數: {}", rows.size());
                if (!rows.isEmpty()) {
                    Map<String, Object> firstRow = rows.get(0);
                    log.error("第一行資料的關鍵欄位: EXTERNAL_NO={}, SKU={}, QTY={}",
                            getStringValue(firstRow, "EXTERNAL_NO"),
                            getStringValue(firstRow, "SKU"),
                            getNullableIntValue(firstRow, "QTY"));
                }
            }

            return null;
        }
    }

    /**
     * 驗證查詢結果的資料完整性，確保所有資料行都屬於同一個 EXTERNAL_ID
     * @param rows 查詢結果列表
     * @return 如果資料一致則返回 true，否則返回 false
     */
    private boolean validateDataConsistency(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }

        String expectedExternalId = getStringValue(rows.get(0), "EXTERNAL_ID");
        if (expectedExternalId == null) {
            log.error("第一筆資料的 EXTERNAL_ID 為 null，資料完整性驗證失敗");
            return false;
        }

        // 檢查所有資料行是否都有相同的 EXTERNAL_ID
        for (int i = 1; i < rows.size(); i++) {
            String currentExternalId = getStringValue(rows.get(i), "EXTERNAL_ID");
            if (!expectedExternalId.equals(currentExternalId)) {
                log.error("資料完整性驗證失敗：第 {} 行的 EXTERNAL_ID '{}' 與預期的 '{}' 不符",
                        i + 1, currentExternalId, expectedExternalId);
                return false;
            }
        }

        log.debug("資料完整性驗證通過，所有 {} 行資料都屬於 EXTERNAL_ID: {}", rows.size(), expectedExternalId);
        return true;
    }

    /**
     * 建立 JitInvMoveOrTradeLine 物件
     * 已優化錯誤處理和資料驗證機制
     * @param row 資料庫查詢結果的一行資料
     * @return 建立好的 JitInvMoveOrTradeLine 物件，如果建立失敗則返回 null
     */
    private JitInvMoveOrTradeLine createJitInvMoveOrTradeLine(Map<String, Object> row) {
        String sku = getStringValue(row, "SKU");

        try {
            // 步驟 1: 驗證必要欄位
            String mfSku = getStringValue(row, "MF_SKU");
            if (!validateRequiredLineFields(row, sku, mfSku)) {
                return null;
            }

            // 步驟 2: 建立 JitInvMoveOrTradeLine 物件並設定資訊
            JitInvMoveOrTradeLine line = new JitInvMoveOrTradeLine();

            line.setSku(sku);
            line.setMfSku(mfSku);
            line.setQty(getNullableIntValue(row, "QTY"));
            line.setFromStorerCate(getStringValue(row, "FROM_STORER_CATE"));
            line.setToStorerCate(getStringValue(row, "TO_STORER_CATE"));
            line.setLot(getStringValue(row, "LOT"));
            line.setBatch(getStringValue(row, "BATCH"));
            line.setDateCode(getStringValue(row, "DATE_CODE"));
            line.setCoo(getStringValue(row, "COO"));
            line.setRemark(getStringValue(row, "LINE_REMARK"));

            log.debug("成功建立 JitInvMoveOrTradeLine，SKU: {}, MfSku: {}, QTY: {}, FromStorerCate: {}, ToStorerCate: {}",
                    sku, mfSku, line.getQty(), line.getFromStorerCate(), line.getToStorerCate());

            return line;

        } catch (Exception e) {
            log.error("建立 JitInvMoveOrTradeLine 時發生嚴重錯誤，SKU: {}", sku, e);
            return null;
        }
    }

    /**
     * 驗證明細行的必要欄位
     * @param row 資料行
     * @param sku 料號
     * @return 驗證是否通過
     */
    private boolean validateRequiredLineFields(Map<String, Object> row, String sku, String mfSku) {
        // 檢查必要的字串欄位 - SKU
        if (sku == null || sku.trim().isEmpty()) {
            log.error("SKU 為空或 null，無法建立明細行");
            return false;
        }
        
        // 檢查必要的字串欄位 - MfSku
        if (mfSku == null || mfSku.trim().isEmpty()) {
            log.error("MF_SKU 為空或 null，無法建立明細行，SKU: {}", sku);
            return false;
        }

        // 檢查數量欄位
        Integer qty = getNullableIntValue(row, "QTY");
        if (qty == null || qty <= 0) {
            log.error("QTY 無效 ({}), 無法建立明細行，SKU: {}",
                    qty, sku);
            return false;
        }

        return true;
    }

    // ========== 工具方法 ==========

    /**
     * 安全地從 Map 中取得字串值
     */
    private String getStringValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        return value != null ? value.toString() : null;
    }

    /**
     * 安全地從 Map 中取得可為空的整數值
     */
    private Integer getNullableIntValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) {
            return null; // 允許返回 null
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            log.warn("無法將 {} 的值 '{}' 轉換為整數，返回 null", columnName, value);
            return null;
        }
    }

    /**
     * 將 Timestamp 轉換為 LocalDateTime
     */
    private java.time.LocalDateTime getTimestampAsLocalDateTime(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp) {
            Timestamp timestamp = (Timestamp) value;
            return timestamp.toLocalDateTime();
        }
        return null;
    }

    /**
     * 測試用方法：僅執行資料映射，不發送到外部 API
     * 用於測試 mapDataToJitInvMoveOrTradeRequest 方法是否正常運作
     */
    public JitInvMoveOrTradeRequest testDataMapping() {
        log.info("開始測試 JIT 庫內移倉/交易資料映射...");

        // 讀取 SQL 查詢語句
        String sql;
        try {
            try (Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("/sql/select_inv_move_for_jit.sql"), StandardCharsets.UTF_8)) {
                sql = FileCopyUtils.copyToString(reader);
            }
        } catch (Exception e) {
            log.error("讀取 select_inv_move_for_jit.sql 檔案失敗。", e);
            return null;
        }

        // 執行 SQL 查詢
        log.info("正在從內部資料庫查詢待處理的庫內移倉/交易資料...");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) {
            log.info("沒有找到需要處理的庫內移倉/交易資料。");
            return null;
        }

        log.info("查詢到 {} 筆資料，開始進行映射...", rows.size());

        // 執行資料映射
        JitInvMoveOrTradeRequest result = mapDataToJitInvMoveOrTradeRequest(rows);

        if (result != null) {
            log.info("=== 資料映射測試結果 ===");
            log.info("ExternalId: {}", result.getExternalId());
            log.info("ExternalNo: {}", result.getExternalNo());
            log.info("WhName: {}", result.getWhName());
            log.info("TradeType: {}", result.getTradeType());
            log.info("FromStorer: {}", result.getFromStorer());
            log.info("ToStorer: {}", result.getToStorer());
            log.info("ApplyDate: {}", result.getApplyDate());
            log.info("Lines 數量: {}", result.getLines().size());

            // 顯示每一行的基本資訊
            for (int i = 0; i < result.getLines().size(); i++) {
                var line = result.getLines().get(i);
                log.info("Line {}: SKU={}, QTY={}, FromStorerCate={}, ToStorerCate={}",
                        i + 1,
                        line.getSku(),
                        line.getQty(),
                        line.getFromStorerCate(),
                        line.getToStorerCate());
            }
            log.info("=== 資料映射測試完成 ===");
        } else {
            log.error("資料映射失敗！");
        }

        return result;
    }
}