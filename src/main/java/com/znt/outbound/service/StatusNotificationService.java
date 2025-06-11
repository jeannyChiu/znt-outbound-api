package com.znt.outbound.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusNotificationService {

    private final JdbcTemplate jdbcTemplate;
    private final JavaMailSender mailSender;

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    private record EnvelopeInfo(String receiverCode, String transFlag, String docId, String docNo) {}

    private String getEnglishStatus(String transFlag) {
        return switch (transFlag) {
            case "S" -> "Success";
            case "F" -> "Failed";
            case "W" -> "Processing";
            default -> transFlag;
        };
    }

    private String getSubjectPrefix() {
        if (databaseUrl != null && databaseUrl.contains("10.1.1.15")) {
            return "{Testing, Please Ignore} ";
        } else if (databaseUrl != null && databaseUrl.contains("10.1.1.144")) {
            return "{Production, Please Check} ";
        }
        return "";
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification(String seqId) {
        log.info("準備為 SEQ_ID: {} 發送狀態通知郵件。", seqId);

        EnvelopeInfo envelopeInfo;
        try {
            String envelopeSql = "SELECT RECEIVER_CODE, TRANS_FLAG, DOC_ID, DOC_NO FROM ZEN_B2B_ENVELOPE WHERE SEQ_ID = ?";
            envelopeInfo = jdbcTemplate.queryForObject(envelopeSql, (rs, rowNum) -> new EnvelopeInfo(
                    rs.getString("RECEIVER_CODE"),
                    rs.getString("TRANS_FLAG"),
                    rs.getString("DOC_ID"),
                    rs.getString("DOC_NO")
            ), seqId);
        } catch (EmptyResultDataAccessException e) {
            log.warn("在 ZEN_B2B_ENVELOPE 中找不到 SEQ_ID: {} 的紀錄，無法發送郵件。", seqId);
            return;
        }

        // 根據您的需求，我們假設 `TD_NAME1` 的值就是 `RECEIVER_CODE`。
        String recipientsSql = "SELECT TD_NO FROM B2B.ZEN_B2B_TAB_D WHERE T_NO = 'JSON_RECEIV_MAIL' AND TD_NAME1 = ? ORDER BY TD_SEQ, TD_NO";
        List<String> toEmails = jdbcTemplate.queryForList(recipientsSql, String.class, envelopeInfo.receiverCode());

        if (toEmails.isEmpty()) {
            log.warn("在 B2B.ZEN_B2B_TAB_D 中找不到 T_NO='JSON_RECEIV_MAIL' 且 TD_NAME1='{}' 的收件人設定，無法發送郵件。", envelopeInfo.receiverCode());
            return;
        }

        // 組合郵件主旨和內文
        String subjectPrefix = getSubjectPrefix();
        String transFlagEnglish = getEnglishStatus(envelopeInfo.transFlag());
        String originalSubject = String.format("Send Orders To [%s] %s ! (DOC_ID: %s)",
                envelopeInfo.receiverCode(),
                transFlagEnglish,
                envelopeInfo.docNo());
        String subject = subjectPrefix + originalSubject;
        
        String textBody = "***EDI系統自動通知請勿回覆!***";

        // 發送郵件
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("oa@zenitron.com.tw");
            message.setTo(toEmails.toArray(new String[0]));
            message.setSubject(subject);
            message.setText(textBody);
            mailSender.send(message);
            log.info("已成功為 SEQ_ID: {} 發送狀態通知郵件至: {}", seqId, String.join(", ", toEmails));
        } catch (Exception e) {
            log.error("為 SEQ_ID: {} 發送郵件時發生錯誤", seqId, e);
        }
    }
} 