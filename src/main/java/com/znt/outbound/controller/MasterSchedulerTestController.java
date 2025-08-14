package com.znt.outbound.controller;

import com.znt.outbound.scheduler.MasterScheduledTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 主排程器測試控制器
 * 提供手動觸發主排程器的端點
 */
@RestController
@RequestMapping("/api/test/master-scheduler")
@RequiredArgsConstructor
@Slf4j
public class MasterSchedulerTestController {
    
    private final MasterScheduledTask masterScheduledTask;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 手動觸發主排程器執行
     * @return 執行狀態訊息
     */
    @PostMapping("/execute")
    public ResponseEntity<Map<String, Object>> executeMasterScheduler() {
        Map<String, Object> response = new HashMap<>();
        LocalDateTime startTime = LocalDateTime.now();
        
        try {
            log.info(">>> 收到手動觸發主排程器的請求");
            response.put("status", "STARTED");
            response.put("message", "主排程器開始執行");
            response.put("startTime", startTime.format(DATE_TIME_FORMATTER));
            
            // 執行主排程器
            masterScheduledTask.executeManually();
            
            LocalDateTime endTime = LocalDateTime.now();
            response.put("status", "SUCCESS");
            response.put("message", "主排程器執行完成");
            response.put("endTime", endTime.format(DATE_TIME_FORMATTER));
            response.put("executionTimeMs", java.time.Duration.between(startTime, endTime).toMillis());
            
            log.info(">>> 主排程器手動執行完成");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">>> 手動觸發主排程器執行失敗", e);
            
            response.put("status", "FAILED");
            response.put("message", "主排程器執行失敗: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("errorDetail", e.getMessage());
            response.put("timestamp", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 檢查主排程器狀態
     * @return 配置狀態訊息
     */
    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> checkSchedulerStatus() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            response.put("status", "OK");
            response.put("message", "主排程器已就緒");
            response.put("timestamp", LocalDateTime.now().format(DATE_TIME_FORMATTER));
            
            // 可以在這裡加入更多狀態檢查
            response.put("configuration", Map.of(
                "enabled", "根據application.properties設定",
                "cron", "0 */5 * * * ? (每5分鐘)",
                "timezone", "Asia/Taipei"
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error(">>> 檢查主排程器狀態失敗", e);
            
            response.put("status", "ERROR");
            response.put("message", "無法檢查主排程器狀態");
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}