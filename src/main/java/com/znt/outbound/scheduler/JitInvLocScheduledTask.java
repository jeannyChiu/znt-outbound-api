package com.znt.outbound.scheduler;

import com.znt.outbound.service.JitInvLocService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JIT 庫存查詢排程任務
 * 每日凌晨自動執行庫存資料撈取
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JitInvLocScheduledTask {

    private final JitInvLocService jitInvLocService;

    /**
     * 排程任務：每日凌晨2:00執行庫存查詢
     * Cron: 秒 分 時 日 月 週
     * 表示每日凌晨2:00執行
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "Asia/Taipei")
    public void executeJitInventoryLocationQuery() {
        // 暫時停用排程 - JIT 庫存查詢 API 尚未開放測試
        boolean schedulingEnabled = false;
        if (!schedulingEnabled) {
            log.debug("JIT 庫存查詢排程已暫停，跳過此次執行");
            return;
        }
        
        String taskId = generateTaskId();
        long startTime = System.currentTimeMillis();

        // 記錄排程任務開始資訊
        logScheduleStart(taskId);

        try {
            // 執行庫存查詢 (查詢所有庫存資料)
            log.info("[{}] 開始執行每日庫存查詢作業...", taskId);
            String result = jitInvLocService.queryInventoryLocation(null, null, null, null);
            
            log.info("[{}] 每日庫存查詢作業完成: {}", taskId, result);

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
            log.info("[{}] 開始處理庫存查詢排程任務錯誤...", taskId);

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
            log.error("[{}] 處理庫存查詢排程任務錯誤時發生異常", taskId, handlingException);
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
                suggestion = "資料庫連線異常，請檢查庫存查詢相關表格狀態和連線設定";
                break;
            case "ConnectException":
            case "SocketTimeoutException":
                suggestion = "網路連線異常，請檢查JIT庫存查詢API服務狀態";
                break;
            case "NullPointerException":
                suggestion = "空指標異常，請檢查庫存查詢資料完整性";
                break;
            case "OutOfMemoryError":
                suggestion = "記憶體不足，請檢查系統資源使用情況";
                break;
            case "InterruptedException":
                suggestion = "執行緒被中斷，可能是系統關機或重啟導致";
                break;
            default:
                suggestion = "未知異常類型，建議聯繫系統管理員";
        }

        log.warn("[{}] 異常分析 - 類型: {}, 建議: {}", taskId, exceptionType, suggestion);
    }

    /**
     * 記錄錯誤統計資訊
     * @param taskId 任務ID
     * @param exception 異常物件
     */
    private void logErrorStatistics(String taskId, Exception exception) {
        log.error("[{}] 錯誤統計 - 異常類型: {}, 錯誤訊息: {}", 
                taskId, 
                exception.getClass().getSimpleName(), 
                exception.getMessage());
        
        // 未來可以擴展為持久化統計
        // 例如：統計各種異常類型的發生次數、頻率等
    }

    /**
     * 產生任務ID
     * @return 任務ID字串
     */
    private String generateTaskId() {
        return "INV-SCHED-" + System.currentTimeMillis() + "-" + Thread.currentThread().getId();
    }

    /**
     * 記錄排程任務開始資訊
     * @param taskId 任務ID
     */
    private void logScheduleStart(String taskId) {
        log.info("=".repeat(80));
        log.info("[{}] JIT 庫存查詢排程任務開始", taskId);
        log.info("[{}] 執行時間: {}", taskId, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("=".repeat(80));
    }

    /**
     * 記錄排程任務成功完成資訊
     * @param taskId 任務ID
     * @param executionTime 執行時間（毫秒）
     */
    private void logScheduleSuccess(String taskId, long executionTime) {
        log.info("=".repeat(80));
        log.info("[{}] JIT 庫存查詢排程任務完成", taskId);
        log.info("[{}] 執行時間: {} 毫秒 ({} 秒)", taskId, executionTime, executionTime / 1000.0);
        log.info("[{}] 狀態: 成功", taskId);
        log.info("=".repeat(80));
    }

    /**
     * 記錄排程任務失敗資訊
     * @param taskId 任務ID
     * @param executionTime 執行時間（毫秒）
     * @param exception 異常物件
     */
    private void logScheduleFailure(String taskId, long executionTime, Exception exception) {
        log.error("=".repeat(80));
        log.error("[{}] JIT 庫存查詢排程任務失敗", taskId);
        log.error("[{}] 執行時間: {} 毫秒 ({} 秒)", taskId, executionTime, executionTime / 1000.0);
        log.error("[{}] 狀態: 失敗", taskId);
        log.error("[{}] 錯誤訊息: {}", taskId, exception.getMessage());
        log.error("=".repeat(80));
    }
}