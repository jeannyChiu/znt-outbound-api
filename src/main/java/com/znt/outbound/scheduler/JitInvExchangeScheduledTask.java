package com.znt.outbound.scheduler;

import com.znt.outbound.service.JitInvExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * JIT 庫內換料排程任務
 * 每5分鐘自動執行JIT庫內換料資料撈取並調用API
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JitInvExchangeScheduledTask {

    private final JitInvExchangeService jitInvExchangeService;

    /**
     * 排程任務：每5分鐘執行一次JIT庫內換料處理
     * Cron: 秒 分 時 日 月 週
     * 表示每小時的第0、5、10、15、20、25、30、35、40、45、50、55分鐘執行
     */
    @Scheduled(cron = "0 */5 * * * ?", zone = "Asia/Taipei")
    public void executeJitInvExchangeProcessing() {
        // 暫時停用排程 - 測試期間使用
        boolean schedulingEnabled = false; // 預設停用，等JIT API開放後再啟用
        if (!schedulingEnabled) {
            log.debug("JIT 庫內換料排程已暫停，跳過此次執行");
            return;
        }
        
        String taskId = generateTaskId();
        long startTime = System.currentTimeMillis();

        // 記錄排程任務開始資訊
        logScheduleStart(taskId);

        try {
            // 調用JIT庫內換料處理服務
            log.info("[{}] 開始調用 JIT 庫內換料處理服務...", taskId);
            jitInvExchangeService.processAndSendJitInvExchange();

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
            log.info("[{}] 開始處理庫內換料排程任務錯誤...", taskId);

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
            log.error("[{}] 處理庫內換料排程任務錯誤時發生異常", taskId, handlingException);
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

        log.info("[{}] 系統資訊 - Java版本: {}, 作業系統: {}",
                taskId,
                System.getProperty("java.version"),
                System.getProperty("os.name"));
    }

    /**
     * 分析異常類型並給出建議
     * @param taskId 任務ID
     * @param exception 異常物件
     */
    private void analyzeExceptionAndSuggest(String taskId, Exception exception) {
        String exceptionClass = exception.getClass().getSimpleName();
        String suggestion = switch (exceptionClass) {
            case "DataAccessException" -> "建議檢查：1) Oracle資料庫連線是否正常 2) 相關資料表是否存在 3) SQL語法是否正確";
            case "RestClientException" -> "建議檢查：1) JIT API服務是否正常 2) 網路連線狀況 3) API URL設定是否正確";
            case "HttpClientErrorException" -> "建議檢查：1) JIT API認證是否有效 2) 請求資料格式是否正確 3) API權限設定";
            case "JsonProcessingException" -> "建議檢查：1) JSON資料格式是否正確 2) 資料映射是否有誤 3) 特殊字符處理";
            case "NullPointerException" -> "建議檢查：1) 必要的設定值是否為null 2) 資料庫查詢結果是否為空";
            default -> "建議檢查：1) 應用程式日誌詳細錯誤 2) 系統資源使用狀況 3) 相關服務依賴";
        };

        log.warn("[{}] 異常分析 - 類型: {}, 建議: {}", taskId, exceptionClass, suggestion);
        log.warn("[{}] 異常訊息: {}", taskId, exception.getMessage());
    }

    /**
     * 記錄錯誤統計資訊
     * @param taskId 任務ID
     * @param exception 異常物件
     */
    private void logErrorStatistics(String taskId, Exception exception) {
        // 記錄錯誤統計（未來可持久化到資料庫）
        log.info("[{}] 錯誤統計 - 異常類型: {}, 發生時間: {}, 任務類型: JIT庫內換料排程",
                taskId,
                exception.getClass().getSimpleName(),
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    /**
     * 記錄排程任務開始資訊
     * @param taskId 任務ID
     */
    private void logScheduleStart(String taskId) {
        log.info("[{}] =================================", taskId);
        log.info("[{}] JIT 庫內換料排程任務開始執行", taskId);
        log.info("[{}] 執行時間: {}", taskId,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        log.info("[{}] =================================", taskId);
    }

    /**
     * 記錄排程任務成功完成資訊
     * @param taskId 任務ID
     * @param executionTime 執行時間（毫秒）
     */
    private void logScheduleSuccess(String taskId, long executionTime) {
        log.info("[{}] =================================", taskId);
        log.info("[{}] JIT 庫內換料排程任務執行成功", taskId);
        log.info("[{}] 執行耗時: {}ms", taskId, executionTime);
        log.info("[{}] =================================", taskId);
    }

    /**
     * 記錄排程任務失敗資訊
     * @param taskId 任務ID
     * @param executionTime 執行時間（毫秒）
     * @param exception 異常物件
     */
    private void logScheduleFailure(String taskId, long executionTime, Exception exception) {
        log.error("[{}] =================================", taskId);
        log.error("[{}] JIT 庫內換料排程任務執行失敗", taskId);
        log.error("[{}] 執行耗時: {}ms", taskId, executionTime);
        log.error("[{}] 失敗原因: {}", taskId, exception.getMessage(), exception);
        log.error("[{}] =================================", taskId);
    }

    /**
     * 生成唯一任務ID
     * @return 任務ID
     */
    private String generateTaskId() {
        return "JIT-INV-EXCH-SCHED-" + System.currentTimeMillis();
    }

    /**
     * 檢查排程任務是否啟用
     * @return true 如果排程啟用
     */
    public boolean isSchedulingEnabled() {
        // 這個方法可以被測試端點調用來檢查狀態
        return false; // 預設停用
    }

    /**
     * 手動執行排程任務（用於測試）
     */
    public void executeManually() {
        log.info("手動執行 JIT 庫內換料排程任務...");
        executeJitInvExchangeProcessing();
    }

    /**
     * 取得排程任務資訊
     * @return 包含任務資訊的字符串
     */
    public String getTaskInfo() {
        return String.format(
            "JIT庫內換料排程任務 - 狀態: %s, Cron表達式: '0 */5 * * * ?', 時區: Asia/Taipei, 頻率: 每5分鐘執行一次",
            isSchedulingEnabled() ? "啟用" : "停用"
        );
    }
}