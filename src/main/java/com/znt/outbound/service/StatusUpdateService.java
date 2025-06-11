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
public class StatusUpdateService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void updateStatus(String seqId, String transFlag) {
        String formattedTimestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // 1. 更新 ZEN_B2B_ENVELOPE
        String envelopeSql = "UPDATE ZEN_B2B_ENVELOPE SET TRANS_FLAG = ?, LAST_UPDATE_DATE = TO_TIMESTAMP(?, 'YYYY-MM-DD HH24:MI:SS') WHERE SEQ_ID = ?";
        jdbcTemplate.update(envelopeSql, transFlag, formattedTimestamp, seqId);
        log.info("ZEN_B2B_ENVELOPE 表狀態已更新。SEQ_ID: {}, TRANS_FLAG: {}", seqId, transFlag);

        // 2. 更新 ZEN_B2B_JSON_SO
        String soSql = "UPDATE ZEN_B2B_JSON_SO so SET so.STATUS = ?, so.LAST_UPDATE_DATE = ? " +
                     "WHERE EXISTS (SELECT 1 FROM ZEN_B2B_JSON_SO_TMP tmp WHERE tmp.ID = so.ID) " +
                     "AND so.STATUS = 'W'";
        int updatedSoRows = jdbcTemplate.update(soSql, transFlag, formattedTimestamp);
        log.info("ZEN_B2B_JSON_SO 表狀態已更新。共 {} 筆訂單狀態更新為 '{}'", updatedSoRows, transFlag);

        // 3. 新增：清理成功的訂單中，狀態為 'F' 的過時紀錄
        String cleanupSql = "DELETE FROM ZEN_B2B_JSON_SO " +
                            "WHERE ID IN ( " +
                            "    SELECT DISTINCT ID " +
                            "    FROM ( " +
                            "        SELECT ID, " +
                            "               COUNT(*) OVER (PARTITION BY ID) AS id_count, " +
                            "               FIRST_VALUE(STATUS) OVER (PARTITION BY ID ORDER BY SEQ_ID DESC) AS max_status " +
                            "        FROM ZEN_B2B_JSON_SO " +
                            "    ) " +
                            "    WHERE id_count > 1 " +
                            "    AND max_status = 'S' " +
                            ") " +
                            "AND STATUS = 'F'";
        int deletedRows = jdbcTemplate.update(cleanupSql);
        if (deletedRows > 0) {
            log.info("成功清理了 {} 筆狀態為 'F' 的過時訂單紀錄。", deletedRows);
        }
    }
} 