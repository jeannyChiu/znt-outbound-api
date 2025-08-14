package com.znt.outbound.scheduler;

import com.znt.outbound.service.JitInvMoveOrTradeMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JIT 庫內移倉/交易排程任務
 * 每5分鐘自動執行JIT庫內移倉/交易資料撈取並調用API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JitInvMoveOrTradeScheduledTask {

    private final JitInvMoveOrTradeMappingService jitInvMoveOrTradeMappingService;

    /**
     * 排程任務：每5分鐘執行一次JIT庫內移倉/交易處理
     * Cron: 秒 分 時 日 月 週
     * 表示每小時的第0、5、10、15、20、25、30、35、40、45、50、55分鐘執行
     * 
     * 注意：此方法已被主排程器取代，保留但停用
     */
    // @Scheduled(cron = "0 */5 * * * ?", zone = "Asia/Taipei")
    public void executeJitInvMoveOrTradeProcessing() {
        // 暫時停用排程 - 測試期間使用
        boolean schedulingEnabled = false;
        if (!schedulingEnabled) {
            log.debug("JIT 庫內移倉/交易排程已暫停，跳過此次執行");
            return;
        }
        
        executeTask();
    }
    
    /**
     * 執行JIT庫內移倉/交易處理任務
     * 供主排程器或手動觸發使用
     */
    public void executeTask() {
        String taskId = generateTaskId();
        long startTime = System.currentTimeMillis();

        // 記錄排程任務開始資訊
        logScheduleStart(taskId);

        try {
            // 調用JIT庫內移倉/交易處理服務
            log.info("[{}] 開始調用 JIT 庫內移倉/交易處理服務...", taskId);
            jitInvMoveOrTradeMappingService.processAndSendJitInvMoveOrTrade();

            // 記錄成功完成資訊
            long executionTime = System.currentTimeMillis() - startTime;
            logScheduleSuccess(taskId, executionTime);

        } catch (Exception e) {
            // 記錄失敗資訊
            long executionTime = System.currentTimeMillis() - startTime;
            logScheduleFailure(taskId, executionTime, e);

            // 處理排程任務錯誤
            handleScheduledTaskError(taskId, e);
        }
    }

    /**
     * 處理排程任務執行時的錯誤
     * @param taskId 任務ID
     * @param exception 異常物件
     */
    private void handleScheduledTaskError(String taskId, Exception exception) {
        try {
            log.info("[{}] 開始處理庫內移倉/交易排程任務錯誤...", taskId);

            // 記錄系統環境資訊
            logSystemInfo(taskId);

            // 分析異常類型並給出建議
            analyzeExceptionAndSuggest(taskId, exception);

            // 記錄錯誤統計資訊（未來可擴展為持久化統計）
            logErrorStatistics(taskId, exception);

            // 未來可以在這裡加入告警通知機制
            // 例如：發送郵件、推送訊息等
            log.info("[{}] 錯誤處理完成，系統將繼續下次排程執行", taskId);

        } catch (Exception handlingException) {
            // 確保錯誤處理本身不會拋出異常
            log.error("[{}] 處理庫內移倉/交易排程任務錯誤時發生異常", taskId, handlingException);
        }
    }

    /**
     * 記錄系統環境資訊，用於錯誤診斷
     * @param taskId 任務ID
     */
    private void logSystemInfo(String taskId) {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;

        log.info("[{}] 系統資訊 - 總記憶體: {}MB, 已使用: {}MB, 可用: {}MB",
                taskId,
                totalMemory / 1024 / 1024,
                usedMemory / 1024 / 1024,
                freeMemory / 1024 / 1024);
        log.info("[{}] 系統資訊 - 活動執行緒數: {}", taskId, Thread.activeCount());
    }

    /**
     * 分析異常類型並給出處理建議
     * @param taskId 任務ID
     * @param exception 異常物件
     */
    private void analyzeExceptionAndSuggest(String taskId, Exception exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String suggestion = "請檢查系統日誌以獲取更多資訊";

        // 根據異常類型給出具體建議
        switch (exceptionType) {
            case "SQLException":
            case "DataAccessException":
                suggestion = "資料庫連線異常，請檢查庫內移倉/交易表格狀態和連線設定";
                break;
            case "ConnectException":
            case "SocketTimeoutException":
                suggestion = "網路連線異常，請檢查JIT庫內移倉/交易API服務狀態";
                break;
            case "NullPointerException":
                suggestion = "空指標異常，請檢查庫內移倉/交易資料完整性";
                break;
            case "OutOfMemoryError":
                suggestion = "記憶體不足，請檢查系統資源使用情況";
                break;
            default:
                if (exception.getMessage() != null && exception.getMessage().contains("timeout")) {
                    suggestion = "操作逾時，請檢查系統效能和網路狀況";
                } else if (exception.getMessage() != null && exception.getMessage().contains("inv_move_or_trade")) {
                    suggestion = "庫內移倉/交易API調用異常，請檢查API端點設定";
                }
                break;
        }

        log.warn("[{}] 異常分析 - 類型: {}, 建議: {}", taskId, exceptionType, suggestion);
    }

    /**
     * 記錄錯誤統計資訊
     * @param taskId 任務ID
     * @param exception 異常物件
     */
    private void logErrorStatistics(String taskId, Exception exception) {
        log.info("[{}] 錯誤統計 - 異常類型: {}, 發生時間: {}",
                taskId,
                exception.getClass().getSimpleName(),
                java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 未來可以擴展為持久化統計，例如：
        // - 錯誤次數統計
        // - 錯誤類型分布
        // - 錯誤趨勢分析
        log.debug("[{}] 提示：可考慮實作庫內移倉/交易錯誤統計持久化功能", taskId);
    }

    /**
     * 記錄排程任務開始執行的詳細資訊
     * @param taskId 任務ID
     */
    private void logScheduleStart(String taskId) {
        log.info("=".repeat(80));
        log.info("[{}] JIT 庫內移倉/交易排程任務開始執行", taskId);
        log.info("[{}] 執行時間: {}", taskId, java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("[{}] 執行執行緒: {}", taskId, Thread.currentThread().getName());
        log.info("=".repeat(80));
    }

    /**
     * 記錄排程任務成功完成的詳細資訊
     * @param taskId 任務ID
     * @param executionTime 執行時間（毫秒）
     */
    private void logScheduleSuccess(String taskId, long executionTime) {
        log.info("=".repeat(80));
        log.info("[{}] JIT 庫內移倉/交易排程任務執行成功", taskId);
        log.info("[{}] 總執行時間: {}ms ({}秒)", taskId, executionTime, String.format("%.2f", executionTime / 1000.0));
        log.info("[{}] 完成時間: {}", taskId, java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 根據執行時間給出效能提示
        if (executionTime > 60000) { // 超過1分鐘
            log.warn("[{}] 注意：執行時間較長，建議檢查庫內移倉/交易系統效能", taskId);
        } else if (executionTime > 30000) { // 超過30秒
            log.info("[{}] 執行時間正常，但稍長", taskId);
        } else {
            log.info("[{}] 執行時間良好", taskId);
        }
        log.info("=".repeat(80));
    }

    /**
     * 記錄排程任務執行失敗的詳細資訊
     * @param taskId 任務ID
     * @param executionTime 執行時間（毫秒）
     * @param exception 異常物件
     */
    private void logScheduleFailure(String taskId, long executionTime, Exception exception) {
        log.error("=".repeat(80));
        log.error("[{}] JIT 庫內移倉/交易排程任務執行失敗", taskId);
        log.error("[{}] 失敗時間: {}", taskId, java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.error("[{}] 執行時間: {}ms ({}秒)", taskId, executionTime, String.format("%.2f", executionTime / 1000.0));
        log.error("[{}] 異常類型: {}", taskId, exception.getClass().getSimpleName());
        log.error("[{}] 錯誤訊息: {}", taskId, exception.getMessage());

        // 記錄堆疊追蹤的前幾行，避免日誌過長
        StackTraceElement[] stackTrace = exception.getStackTrace();
        if (stackTrace.length > 0) {
            log.error("[{}] 錯誤位置: {}:{}", taskId,
                    stackTrace[0].getClassName(), stackTrace[0].getLineNumber());
        }
        log.error("=".repeat(80));
    }

    /**
     * 產生任務ID，用於追蹤單次排程執行
     * @return 任務ID
     */
    private String generateTaskId() {
        return "JIT-INV-MOVE-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }
}