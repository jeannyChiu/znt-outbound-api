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
    private final ApiConfigService apiConfigService;

    public void loadFromTmpToMain() {
        String providerName = apiConfigService.getProviderName();
        log.info("開始將資料從 ZEN_B2B_JSON_SO_TMP 寫入 ZEN_B2B_JSON_SO (RECEIVER_CODE: {})。", providerName);
        transactionTemplate.execute(status -> {
            try {
                String insertSql = """
                    INSERT INTO ZEN_B2B_JSON_SO
                    SELECT * FROM ZEN_B2B_JSON_SO_TMP
                    WHERE RECEIVER_CODE = ?
                    """;
                int insertedToMain = jdbcTemplate.update(insertSql, providerName);
                log.info("成功將 {} 筆資料從 ZEN_B2B_JSON_SO_TMP 寫入 ZEN_B2B_JSON_SO (RECEIVER_CODE: {})。", insertedToMain, providerName);

                log.info("開始清理 ZEN_B2B_JSON_SO 中的過時 'N' 狀態紀錄 (RECEIVER_CODE: {})...", providerName);
                String cleanupSql = """
                    DELETE FROM ZEN_B2B_JSON_SO
                    WHERE RECEIVER_CODE = ?
                    AND ID IN (
                        SELECT DISTINCT ID
                        FROM (
                            SELECT ID,
                                   COUNT(*) OVER (PARTITION BY ID) AS id_count,
                                   FIRST_VALUE(STATUS) OVER (PARTITION BY ID ORDER BY SEQ_ID DESC) AS max_status
                            FROM ZEN_B2B_JSON_SO
                            WHERE RECEIVER_CODE = ?
                        )
                        WHERE id_count > 1
                        AND max_status = 'W'
                    )
                    AND STATUS = 'N'
                    """;
                int deletedRows = jdbcTemplate.update(cleanupSql, providerName, providerName);
                log.info("成功清理了 {} 筆過時的 'N' 狀態紀錄 (RECEIVER_CODE: {})。", deletedRows, providerName);

                log.info("開始清理已有成功(S)紀錄的待處理(W)訂單 (RECEIVER_CODE: {})...", providerName);
                String deleteWaitingIfSuccessfulSql = """
                    DELETE FROM ZEN_B2B_JSON_SO
                    WHERE RECEIVER_CODE = ?
                    AND STATUS = 'W' AND INVOICE_NO IN (
                        SELECT INVOICE_NO FROM ZEN_B2B_JSON_SO WHERE RECEIVER_CODE = ? AND STATUS = 'S'
                    )
                    """;
                int deletedWaitingRows = jdbcTemplate.update(deleteWaitingIfSuccessfulSql, providerName, providerName);
                log.info("成功清理了 {} 筆已有成功紀錄的待處理訂單 (RECEIVER_CODE: {})。", deletedWaitingRows, providerName);

                return null;
            } catch (DataAccessException e) {
                log.error("從 ZEN_B2B_JSON_SO_TMP 寫入或清理 ZEN_B2B_JSON_SO 時發生錯誤", e);
                status.setRollbackOnly();
            }
            return null;
        });
    }
} 