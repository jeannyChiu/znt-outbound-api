package com.znt.outbound.scheduler;

import com.znt.outbound.runner.DbExtractRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 主排程器 - 統一管理所有排程任務的執行
 * 
 * 執行順序：
 * 1. ASN處理
 * 2. 庫存移倉/交易
 * 3. 庫內換料
 * 4. 資料庫抽取與載入
 * 
 * 每個任務按順序執行，前一個完成才執行下一個
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MasterScheduledTask {

    private final JitAsnScheduledTask jitAsnScheduledTask;
    private final JitInvMoveOrTradeScheduledTask jitInvMoveOrTradeScheduledTask;
    private final JitInvExchangeScheduledTask jitInvExchangeScheduledTask;
    private final DbExtractRunner dbExtractRunner;
    
    // 配置開關 - 可透過 application.properties 設定
    @Value("${master.scheduler.enabled:true}")
    private boolean masterSchedulerEnabled;
    
    @Value("${master.scheduler.asn.enabled:true}")
    private boolean asnTaskEnabled;
    
    @Value("${master.scheduler.invMoveOrTrade.enabled:true}")
    private boolean invMoveOrTradeTaskEnabled;
    
    @Value("${master.scheduler.invExchange.enabled:true}")
    private boolean invExchangeTaskEnabled;
    
    @Value("${master.scheduler.dbExtract.enabled:true}")
    private boolean dbExtractTaskEnabled;
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 主排程任務 - 每5分鐘執行一次
     */
    @Scheduled(cron = "0 */5 * * * ?", zone = "Asia/Taipei")
    public void executeMasterSchedule() {
        if (!masterSchedulerEnabled) {
            log.debug("主排程器已停用，跳過此次執行");
            return;
        }
        
        LocalDateTime startTime = LocalDateTime.now();
        log.info("========================================");
        log.info("主排程器開始執行");
        log.info("開始時間: {}", startTime.format(DATE_TIME_FORMATTER));
        log.info("========================================");
        
        // 用於記錄執行結果
        Map<String, TaskExecutionResult> executionResults = new LinkedHashMap<>();
        
        // 1. 執行 ASN 處理
        if (asnTaskEnabled) {
            executeTask("ASN處理", 
                       () -> jitAsnScheduledTask.executeTask(),
                       executionResults);
        } else {
            log.info("ASN處理任務已停用，跳過");
        }
        
        // 2. 執行庫存移倉/交易
        if (invMoveOrTradeTaskEnabled) {
            executeTask("庫存移倉/交易", 
                       () -> jitInvMoveOrTradeScheduledTask.executeTask(),
                       executionResults);
        } else {
            log.info("庫存移倉/交易任務已停用，跳過");
        }
        
        // 3. 執行庫內換料
        if (invExchangeTaskEnabled) {
            executeTask("庫內換料", 
                       () -> jitInvExchangeScheduledTask.executeTask(),
                       executionResults);
        } else {
            log.info("庫內換料任務已停用，跳過");
        }
        
        // 4. 執行資料庫抽取與載入
        if (dbExtractTaskEnabled) {
            executeTask("資料庫抽取與載入", 
                       () -> dbExtractRunner.executeTask(),
                       executionResults);
        } else {
            log.info("資料庫抽取與載入任務已停用，跳過");
        }
        
        // 顯示執行摘要
        LocalDateTime endTime = LocalDateTime.now();
        Duration totalDuration = Duration.between(startTime, endTime);
        
        log.info("========================================");
        log.info("主排程器執行完成");
        log.info("結束時間: {}", endTime.format(DATE_TIME_FORMATTER));
        log.info("總耗時: {} 秒", totalDuration.getSeconds());
        log.info("----------------------------------------");
        log.info("執行摘要:");
        
        int successCount = 0;
        int failureCount = 0;
        for (Map.Entry<String, TaskExecutionResult> entry : executionResults.entrySet()) {
            TaskExecutionResult result = entry.getValue();
            String status = result.isSuccess() ? "成功" : "失敗";
            log.info("{}: {} (耗時: {} 秒)", 
                    entry.getKey(), status, result.getDuration().getSeconds());
            
            if (result.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                if (result.getErrorMessage() != null) {
                    log.info("  錯誤訊息: {}", result.getErrorMessage());
                }
            }
        }
        
        log.info("----------------------------------------");
        log.info("總計: 成功 {} 個, 失敗 {} 個", successCount, failureCount);
        log.info("========================================");
    }
    
    /**
     * 執行單個任務並記錄結果
     */
    private void executeTask(String taskName, Runnable task, Map<String, TaskExecutionResult> results) {
        log.info(">>> 開始執行: {}", taskName);
        
        LocalDateTime taskStartTime = LocalDateTime.now();
        boolean success = false;
        String errorMessage = null;
        
        try {
            task.run();
            success = true;
            log.info(">>> {} 執行成功", taskName);
        } catch (Exception e) {
            success = false;
            errorMessage = e.getMessage();
            log.error(">>> {} 執行失敗: {}", taskName, errorMessage, e);
            // 錯誤不中斷後續任務執行
        }
        
        LocalDateTime taskEndTime = LocalDateTime.now();
        Duration taskDuration = Duration.between(taskStartTime, taskEndTime);
        
        TaskExecutionResult result = new TaskExecutionResult(
            success, 
            taskDuration, 
            taskStartTime, 
            taskEndTime,
            errorMessage
        );
        
        results.put(taskName, result);
        
        log.info(">>> {} 耗時: {} 秒", taskName, taskDuration.getSeconds());
    }
    
    /**
     * 手動觸發主排程器執行（用於測試）
     */
    public void executeManually() {
        log.info(">>> 手動觸發主排程器執行");
        // 臨時啟用排程器
        boolean originalEnabled = masterSchedulerEnabled;
        masterSchedulerEnabled = true;
        
        try {
            executeMasterSchedule();
        } finally {
            // 恢復原始設定
            masterSchedulerEnabled = originalEnabled;
        }
    }
    
    /**
     * 任務執行結果記錄
     */
    private static class TaskExecutionResult {
        private final boolean success;
        private final Duration duration;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final String errorMessage;
        
        public TaskExecutionResult(boolean success, Duration duration, 
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  String errorMessage) {
            this.success = success;
            this.duration = duration;
            this.startTime = startTime;
            this.endTime = endTime;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public Duration getDuration() {
            return duration;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}