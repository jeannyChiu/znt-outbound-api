package com.znt.outbound.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbcTemplate;
    private final ApiConfigService apiConfigService;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    // 定義一個內部記錄類來保存查詢結果
    private record ErrorNotificationData(String id, String receiverCode) {}

    private static class ErrorNotificationDataMapper implements RowMapper<ErrorNotificationData> {
        @Override
        public ErrorNotificationData mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new ErrorNotificationData(
                rs.getString("ID"),
                rs.getString("RECEIVER_CODE")
            );
        }
    }

    private String getSubjectPrefix() {
        if (databaseUrl != null && databaseUrl.contains("10.1.1.15")) {
            return "{Testing, Please Ignore} ";
        } else if (databaseUrl != null && databaseUrl.contains("10.1.1.144")) {
            return "{Production, Please Check} ";
        }
        return "";
    }

    public void sendErrorNotifications() {
        String providerName = apiConfigService.getProviderName();
        log.info("開始檢查並寄送錯誤通知郵件 (RECEIVER_CODE: {})...", providerName);

        String findErrorsSql = "SELECT ID, RECEIVER_CODE FROM ZEN_B2B_JSON_SO WHERE STATUS = 'N' AND RECEIVER_CODE = ?";
        List<ErrorNotificationData> errors = jdbcTemplate.query(findErrorsSql, new ErrorNotificationDataMapper(), providerName);

        if (errors.isEmpty()) {
            log.info("沒有狀態為 'N' 的錯誤資料 (RECEIVER_CODE: {})，無需寄送通知。", providerName);
            return;
        }

        log.info("發現 {} 筆狀態為 'N' 的錯誤資料 (RECEIVER_CODE: {})，開始處理...", errors.size(), providerName);

        String subjectPrefix = getSubjectPrefix();

        for (ErrorNotificationData error : errors) {
            try {
                // 1. 查找收件人
                String findRecipientsSql = "SELECT TD_NO FROM B2B.ZEN_B2B_TAB_D WHERE T_NO = 'JSON_RECEIV_MAIL' AND TD_NAME1 = ? ORDER BY T_NO, TD_SEQ, TD_NO";
                List<String> recipients = jdbcTemplate.queryForList(findRecipientsSql, String.class, error.receiverCode());

                if (recipients.isEmpty()) {
                    log.warn("找不到訂單 ID '{}' (RECEIVER_CODE: '{}') 的郵件收件人，跳過此筆通知。", error.id(), error.receiverCode());
                    continue;
                }

                // 2. 組合並發送郵件
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("oa@zenitron.com.tw"); // 設定寄件人
                message.setTo(recipients.toArray(new String[0]));
                String originalSubject = "Send Order ID#[" + error.id() + "] To [" + error.receiverCode() + "] Error !";
                message.setSubject(subjectPrefix + originalSubject);
                message.setText(
                    // "請至EC Info Cloud → B2B管理 → EDI管理 → E.16.FEILIKS EDI → E.16.1.Send Data Check \n" +
                    "確認錯誤訂單並至相關系統修改有空值的訂單資料\n" +
                    "***EDI系統自動通知請勿回覆!***"
                );

                mailSender.send(message);
                log.info("成功寄送訂單 ID '{}' 的錯誤通知至: {}", error.id(), String.join(", ", recipients));

            } catch (Exception e) {
                log.error("處理訂單 ID '{}' 的錯誤通知時發生例外狀況，此筆郵件可能未寄出: {}", error.id(), e.getMessage(), e);
            }
        }
        log.info("錯誤通知郵件寄送處理完成 (RECEIVER_CODE: {})。", providerName);
    }
} 