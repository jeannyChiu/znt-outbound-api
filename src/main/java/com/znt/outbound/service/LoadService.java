package com.znt.outbound.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadService {

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public void loadFromTmpToMain() {
        log.info("開始將資料從 ZEN_B2B_JSON_SO_TMP 寫入 ZEN_B2B_JSON_SO。");
        transactionTemplate.execute(status -> {
            try {
                String insertSql = """
                    INSERT INTO ZEN_B2B_JSON_SO
                    SELECT * FROM ZEN_B2B_JSON_SO_TMP
                    """;
                int insertedToMain = jdbcTemplate.update(insertSql);
                log.info("成功將 {} 筆資料從 ZEN_B2B_JSON_SO_TMP 寫入 ZEN_B2B_JSON_SO。", insertedToMain);

                log.info("開始清理 ZEN_B2B_JSON_SO 中的過時 'N' 狀態紀錄...");
                String cleanupSql = """
                    DELETE FROM ZEN_B2B_JSON_SO
                    WHERE ID IN (
                        SELECT DISTINCT ID
                        FROM (
                            SELECT ID,
                                   COUNT(*) OVER (PARTITION BY ID) AS id_count,
                                   FIRST_VALUE(STATUS) OVER (PARTITION BY ID ORDER BY SEQ_ID DESC) AS max_status
                            FROM ZEN_B2B_JSON_SO
                        )
                        WHERE id_count > 1
                        AND max_status = 'W'
                    )
                    AND STATUS = 'N'
                    """;
                int deletedRows = jdbcTemplate.update(cleanupSql);
                log.info("成功清理了 {} 筆過時的 'N' 狀態紀錄。", deletedRows);

            } catch (DataAccessException e) {
                log.error("從 ZEN_B2B_JSON_SO_TMP 寫入或清理 ZEN_B2B_JSON_SO 時發生錯誤", e);
                status.setRollbackOnly();
            }
            return null;
        });
    }
} 