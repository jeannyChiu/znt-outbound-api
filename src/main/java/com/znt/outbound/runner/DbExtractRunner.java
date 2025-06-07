package com.znt.outbound.runner;

import com.znt.outbound.service.GenericExtractService;
import com.znt.outbound.service.LoadService;
import com.znt.outbound.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component      
@RequiredArgsConstructor
@Slf4j
public class DbExtractRunner implements CommandLineRunner {

    private final GenericExtractService extractService;
    private final LoadService loadService;
    private final NotificationService notificationService;

    @Override
    public void run(String... args) {
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

        log.info(">>> 所有任務執行完畢。");
    }
}
