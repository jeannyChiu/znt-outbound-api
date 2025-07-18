package com.znt.outbound.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnvelopeService {

    private final JdbcTemplate jdbcTemplate;
    private final ApiConfigService apiConfigService;

    @Transactional
    public String createOutboundEnvelope(String partCode, String sign) {
        log.info("正在為 conversation_id: {} 建立 B2B 封套...", sign);

        String providerName = apiConfigService.getProviderName();

        // 1. 從序列獲取 SEQ_ID
        String seqId = jdbcTemplate.queryForObject("SELECT TO_CHAR(B2B.ZEN_B2B_SEQ.NEXTVAL) FROM DUAL", String.class);
        if (seqId == null) {
            log.error("無法從序列 B2B.ZEN_B2B_SEQ 獲取新的 SEQ_ID。");
            throw new IllegalStateException("無法獲取序列號。");
        }

        // 2. 準備時間相關的資料
        LocalDateTime now = LocalDateTime.now();
        String docNo = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String formattedTimestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 3. 組合並執行 INSERT SQL
        String sql = "INSERT INTO ZEN_B2B_ENVELOPE (" +
                     "SEQ_ID, SENDER_CODE, RECEIVER_CODE, SENDER_AP_ID, RECEIVER_AP_ID, " +
                     "SENDER_GS_ID, RECEIVER_GS_ID, DOC_NO, DOC_ID, DIRECTION, " +
                     "B2B_MSG_TYPE, DATASOURCE, DOC_DATETIME, TRANS_FLAG, CONVERSATION_ID, " +
                     "CREATION_DATE, LAST_UPDATE_DATE" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'))";

        try {
            jdbcTemplate.update(sql,
                seqId,                  // SEQ_ID
                "ZEN",                  // SENDER_CODE
                providerName,           // RECEIVER_CODE
                "ZEN",                  // SENDER_AP_ID
                partCode,               // RECEIVER_AP_ID
                "ZEN",                  // SENDER_GS_ID
                providerName,           // RECEIVER_GS_ID
                docNo,                  // DOC_NO
                seqId,                  // DOC_ID
                "OUT",                  // DIRECTION
                "ORDERS",               // B2B_MSG_TYPE
                "EDI",                  // DATASOURCE
                formattedTimestamp,       // DOC_DATETIME
                "W",                    // TRANS_FLAG
                sign,                   // CONVERSATION_ID
                formattedTimestamp,       // CREATION_DATE
                formattedTimestamp        // LAST_UPDATE_DATE
            );
            log.info("B2B 封套建立成功。 SEQ_ID: {}", seqId);
            return seqId; // 回傳產生的 SEQ_ID
        } catch (Exception e) {
            log.error("寫入 ZEN_B2B_ENVELOPE 表時發生錯誤。", e);
            // 重新拋出異常，以中斷後續的 API 請求
            throw new RuntimeException("無法建立 B2B 封套記錄。", e);
        }
    }

    @Transactional
    public String createJitAsnEnvelope(String externalNo, String providerName) {
        log.info("正在為 JIT ASN (ExternalNo: {}) 建立 B2B 封套...", externalNo);

        String seqId = jdbcTemplate.queryForObject("SELECT TO_CHAR(B2B.ZEN_B2B_SEQ.NEXTVAL) FROM DUAL", String.class);
        if (seqId == null) {
            log.error("無法從序列 B2B.ZEN_B2B_SEQ 獲取新的 SEQ_ID。");
            throw new IllegalStateException("無法獲取序列號。");
        }

        LocalDateTime now = LocalDateTime.now();
        String docNo = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String formattedTimestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String sql = "INSERT INTO ZEN_B2B_ENVELOPE (" +
                     "SEQ_ID, SENDER_CODE, RECEIVER_CODE, SENDER_AP_ID, RECEIVER_AP_ID, " +
                     "SENDER_GS_ID, RECEIVER_GS_ID, DOC_NO, DOC_ID, DIRECTION, " +
                     "B2B_MSG_TYPE, DATASOURCE, DOC_DATETIME, TRANS_FLAG, CONVERSATION_ID, " +
                     "CREATION_DATE, LAST_UPDATE_DATE" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'))";
        try {
            jdbcTemplate.update(sql,
                seqId,                  // SEQ_ID
                "ZEN",                  // SENDER_CODE
                providerName,           // RECEIVER_CODE (e.g., "JIT")
                "ZEN",                  // SENDER_AP_ID
                providerName,           // RECEIVER_AP_ID
                "ZEN",                  // SENDER_GS_ID
                providerName,           // RECEIVER_GS_ID (e.g., "JIT")
                docNo,                  // DOC_NO
                seqId,                  // DOC_ID
                "OUT",                  // DIRECTION
                "ASN",                  // B2B_MSG_TYPE (入庫單)
                "API",                  // DATASOURCE
                formattedTimestamp,     // DOC_DATETIME
                "W",                    // TRANS_FLAG (Waiting)
                externalNo,             // CONVERSATION_ID (使用外部單號)
                formattedTimestamp,     // CREATION_DATE
                formattedTimestamp      // LAST_UPDATE_DATE
            );
            log.info("JIT ASN B2B 封套建立成功。SEQ_ID: {}, ExternalNo: {}", seqId, externalNo);
            return seqId;
        } catch (Exception e) {
            log.error("寫入 JIT ASN 封套到 ZEN_B2B_ENVELOPE 表時發生錯誤。", e);
            throw new RuntimeException("無法建立 JIT ASN B2B 封套記錄。", e);
        }
    }

    @Transactional
    public String createJitInvMoveOrTradeEnvelope(String externalNo, String providerName) {
        log.info("正在為 JIT 庫內移倉/交易 (ExternalNo: {}) 建立 B2B 封套...", externalNo);

        String seqId = jdbcTemplate.queryForObject("SELECT TO_CHAR(B2B.ZEN_B2B_SEQ.NEXTVAL) FROM DUAL", String.class);
        if (seqId == null) {
            log.error("無法從序列 B2B.ZEN_B2B_SEQ 獲取新的 SEQ_ID。");
            throw new IllegalStateException("無法獲取序列號。");
        }

        LocalDateTime now = LocalDateTime.now();
        String docNo = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String formattedTimestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String sql = "INSERT INTO ZEN_B2B_ENVELOPE (" +
                     "SEQ_ID, SENDER_CODE, RECEIVER_CODE, SENDER_AP_ID, RECEIVER_AP_ID, " +
                     "SENDER_GS_ID, RECEIVER_GS_ID, DOC_NO, DOC_ID, DIRECTION, " +
                     "B2B_MSG_TYPE, DATASOURCE, DOC_DATETIME, TRANS_FLAG, CONVERSATION_ID, " +
                     "CREATION_DATE, LAST_UPDATE_DATE" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'))";
        try {
            jdbcTemplate.update(sql,
                seqId,                  // SEQ_ID
                "ZEN",                  // SENDER_CODE
                providerName,           // RECEIVER_CODE (e.g., "JIT")
                "ZEN",                  // SENDER_AP_ID
                providerName,           // RECEIVER_AP_ID
                "ZEN",                  // SENDER_GS_ID
                providerName,           // RECEIVER_GS_ID (e.g., "JIT")
                docNo,                  // DOC_NO
                seqId,                  // DOC_ID
                "OUT",                  // DIRECTION
                "MOVE_TRADE",           // B2B_MSG_TYPE (庫內移倉/交易)
                "API",                  // DATASOURCE
                formattedTimestamp,     // DOC_DATETIME
                "W",                    // TRANS_FLAG (Waiting)
                externalNo,             // CONVERSATION_ID (使用外部單號)
                formattedTimestamp,     // CREATION_DATE
                formattedTimestamp      // LAST_UPDATE_DATE
            );
            log.info("JIT 庫內移倉/交易 B2B 封套建立成功。SEQ_ID: {}, ExternalNo: {}", seqId, externalNo);
            return seqId;
        } catch (Exception e) {
            log.error("寫入 JIT 庫內移倉/交易封套到 ZEN_B2B_ENVELOPE 表時發生錯誤。", e);
            throw new RuntimeException("無法建立 JIT 庫內移倉/交易 B2B 封套記錄。", e);
        }
    }

    @Transactional
    public String createJitInvLocEnvelope(String requestId, String providerName) {
        log.info("正在為 JIT 庫存查詢 (RequestId: {}) 建立 B2B 封套...", requestId);

        String seqId = jdbcTemplate.queryForObject("SELECT TO_CHAR(B2B.ZEN_B2B_SEQ.NEXTVAL) FROM DUAL", String.class);
        if (seqId == null) {
            log.error("無法從序列 B2B.ZEN_B2B_SEQ 獲取新的 SEQ_ID。");
            throw new IllegalStateException("無法獲取序列號。");
        }

        LocalDateTime now = LocalDateTime.now();
        String docNo = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String formattedTimestamp = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String sql = "INSERT INTO ZEN_B2B_ENVELOPE (" +
                     "SEQ_ID, SENDER_CODE, RECEIVER_CODE, SENDER_AP_ID, RECEIVER_AP_ID, " +
                     "SENDER_GS_ID, RECEIVER_GS_ID, DOC_NO, DOC_ID, DIRECTION, " +
                     "B2B_MSG_TYPE, DATASOURCE, DOC_DATETIME, TRANS_FLAG, CONVERSATION_ID, " +
                     "CREATION_DATE, LAST_UPDATE_DATE" +
                     ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), ?, ?, TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'), TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS'))";
        try {
            jdbcTemplate.update(sql,
                seqId,                  // SEQ_ID
                "ZEN",                  // SENDER_CODE
                providerName,           // RECEIVER_CODE (e.g., "JIT")
                "ZEN",                  // SENDER_AP_ID
                providerName,           // RECEIVER_AP_ID
                "ZEN",                  // SENDER_GS_ID
                providerName,           // RECEIVER_GS_ID (e.g., "JIT")
                docNo,                  // DOC_NO
                seqId,                  // DOC_ID
                "OUT",                  // DIRECTION
                "INV_LOC",              // B2B_MSG_TYPE (庫存查詢)
                "API",                  // DATASOURCE
                formattedTimestamp,     // DOC_DATETIME
                "W",                    // TRANS_FLAG (Waiting)
                requestId,              // CONVERSATION_ID (使用請求ID)
                formattedTimestamp,     // CREATION_DATE
                formattedTimestamp      // LAST_UPDATE_DATE
            );
            log.info("JIT 庫存查詢 B2B 封套建立成功。SEQ_ID: {}, RequestId: {}", seqId, requestId);
            return seqId;
        } catch (Exception e) {
            log.error("寫入 JIT 庫存查詢封套到 ZEN_B2B_ENVELOPE 表時發生錯誤。", e);
            throw new RuntimeException("無法建立 JIT 庫存查詢 B2B 封套記錄。", e);
        }
    }

    @Transactional
    public void updateEnvelopeStatus(String seqId, String status) {
        if (seqId == null || status == null) {
            log.warn("SEQ_ID 或狀態為空，無法更新 B2B 封套狀態。");
            return;
        }
        log.info("正在更新 B2B 封套 (SEQ_ID: {}) 的狀態為 '{}'...", seqId, status);
        String sql = "UPDATE ZEN_B2B_ENVELOPE SET TRANS_FLAG = ?, LAST_UPDATE_DATE = CURRENT_TIMESTAMP WHERE SEQ_ID = ?";
        try {
            int updatedRows = jdbcTemplate.update(sql, status, seqId);
            if (updatedRows > 0) {
                log.info("B2B 封套 (SEQ_ID: {}) 狀態更新成功。", seqId);
            } else {
                log.warn("找不到要更新的 B2B 封套，SEQ_ID: {}", seqId);
            }
        } catch (Exception e) {
            log.error("更新 ZEN_B2B_ENVELOPE (SEQ_ID: {}) 時發生錯誤。", seqId, e);
            // 不重新拋出異常，以避免影響主流程的狀態判斷
        }
    }
} 