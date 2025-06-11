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
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component      
@RequiredArgsConstructor
@Slf4j
public class DbExtractRunner implements CommandLineRunner {

    private final GenericExtractService extractService;
    private final LoadService loadService;
    private final NotificationService notificationService;
    private final PreMappingCheckService preMappingCheckService;
    private final JsonMappingService jsonMappingService;
    private final ApiService apiService;
    private final ObjectMapper objectMapper;
    private final EnvelopeService envelopeService;
    private final StatusUpdateService statusUpdateService;

    @Override
    public void run(String... args) throws Exception {
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

        // 新增：檢查是否有狀態為 'W' 的待處理資料
        if (!preMappingCheckService.hasRecordsToProcess()) {
            log.info(">>> 所有任務執行完畢 (沒有需要 Mapping 的資料)。");
            return; // 結束執行
        }

        log.info(">>> 準備開始執行 JSON Mapping 與 API 呼叫任務...");
        try {
            var request = jsonMappingService.buildRequest();
            if (request != null) {
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

                // 新增：呼叫 API Service 發送請求
                apiService.sendRequest(request);

                // TODO: 根據 API 回應更新本地資料庫中這些訂單的狀態 (例如從 'W' 更新為 'S' 表示已發送)
            }
        } catch (Exception e) {
            log.error(">>> 執行 JSON Mapping 或 API 呼叫任務時發生錯誤。", e);
        }

        log.info(">>> 所有任務執行完畢。");
    }
}
