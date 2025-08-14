package com.znt.outbound.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.znt.outbound.service.GenericExtractService;
import com.znt.outbound.service.LoadService;
import com.znt.outbound.service.NotificationService;
import com.znt.outbound.service.PreMappingCheckService;
import com.znt.outbound.service.JsonMappingService;
import com.znt.outbound.service.ApiService;
import com.znt.outbound.service.EnvelopeService;
import com.znt.outbound.service.StatusUpdateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
@RequiredArgsConstructor
@Slf4j
public class DbExtractRunner {

    private final GenericExtractService extractService;
    private final LoadService loadService;
    private final NotificationService notificationService;
    private final PreMappingCheckService preMappingCheckService;
    private final JsonMappingService jsonMappingService;
    private final ApiService apiService;
    private final ObjectMapper objectMapper;
    private final EnvelopeService envelopeService;
    private final StatusUpdateService statusUpdateService;

    /**
     * 排程任務：每週一至週六，從 08:15 到 20:45，每 30 分鐘執行一次。
     * Cron: 秒 分 時 日 月 週
     * 0 15/30 8-20 * * MON-SAT
     * 
     * 注意：此方法已被主排程器取代，保留但停用
     */
    // @Scheduled(cron = "0 15/30 8-20 * * MON-SAT", zone = "Asia/Taipei")
    public void executeScheduledTask() {
        // 暫時停用排程 - 測試期間使用
        boolean schedulingEnabled = false;
        if (!schedulingEnabled) {
            log.debug("DB 抽取排程已暫停，跳過此次執行");
            return;
        }
        
        executeTask();
    }
    
    /**
     * 執行資料庫抽取任務
     * 供主排程器或手動觸發使用
     */
    public void executeTask() {
        
        log.info("========== 開始執行排程任務 ==========");
        try {
            log.info(">>> 開始執行資料庫抽取與載入任務...");
            int extractedCount = extractService.extract();

            if (extractedCount > 0) {
                log.info(">>> 已抽取 {} 筆資料至暫存表，準備載入正式表...", extractedCount);
                loadService.loadFromTmpToMain();
                log.info(">>> 資料載入任務完成。");
            } else {
                log.info(">>> 暫存表中無新資料。");
            }

            // 無論是否有新資料，都檢查並寄送錯誤通知
            try {
                notificationService.sendErrorNotifications();
            } catch (Exception e) {
                log.error(">>> 執行郵件通知任務時發生嚴重錯誤。", e);
            }

            // 檢查是否有狀態為 'W' 的待處理資料
            if (!preMappingCheckService.hasRecordsToProcess()) {
                log.info(">>> 沒有需要 Mapping 的資料，本次排程提前結束。");
                return;
            }

            log.info(">>> 準備開始執行 JSON Mapping 與 API 呼叫任務...");
            var request = jsonMappingService.buildRequest();
            if (request != null && request.getData() != null && !request.getData().getOrders().isEmpty()) {
                int orderCount = request.getData().getOrders().size();
                log.info(">>> 成功映射 {} 個訂單標頭。", orderCount);

                // 將產生的 request 物件序列化為 JSON 字串
                String jsonForTest = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);

                // 將 JSON 寫入檔案以供除錯
                try {
                    Path outputPath = Paths.get("target/generated-json-test.json");
                    Files.createDirectories(outputPath.getParent());
                    Files.writeString(outputPath, jsonForTest, StandardCharsets.UTF_8);
                    log.info(">>> 測試用的 JSON 已寫入檔案: {}", outputPath.toAbsolutePath());
                } catch (IOException e) {
                    log.error(">>> 無法將測試 JSON 寫入檔案。", e);
                }

                apiService.sendRequest(request);
                log.info(">>> API 呼叫任務已提交。");

            } else {
                log.info(">>> 經過 Mapping 後，沒有可發送的訂單資料。");
            }

        } catch (Exception e) {
            log.error(">>> 排程任務執行期間發生未預期的嚴重錯誤。", e);
        } finally {
            log.info("========== 排程任務執行結束 ==========");
        }
    }
}
