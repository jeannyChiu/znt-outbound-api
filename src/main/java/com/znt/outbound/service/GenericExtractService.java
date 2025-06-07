package com.znt.outbound.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.dao.DataAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenericExtractService {

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final PlatformTransactionManager transactionManager;

    /**
     * 執行 SQL scripts，將當日單據資料寫入 ZEN_B2B_JSON_SO_TMP
     * 每個 script 會在獨立的 transaction 中執行，互不影響。
     *
     * @return 實際寫入筆數
     */
    public int extract() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        // 步驟 1: 在一個獨立的專用交易中，優先清空暫存表
        log.info("Cleaning ZEN_B2B_JSON_SO_TMP table in a dedicated transaction.");
        transactionTemplate.execute(status -> {
            jdbcTemplate.update("DELETE FROM ZEN_B2B_JSON_SO_TMP");
            return null;
        });
        log.info("Temp table cleaned successfully.");

        // 步驟 2: 執行各個腳本的 INSERT 操作，每個腳本都在自己的交易中
        List<String> sqlScripts = List.of(
                "mi_extract.sql",
                "mv_extract.sql",
                "om_extract.sql",
                "rt_extract.sql",
                "st_extract.sql"
        );

        for (String scriptName : sqlScripts) {
            log.info("Executing script: {} in a new transaction.", scriptName);
            try {
                transactionTemplate.execute(status -> {
                    String plsql;
                    try (var is = resourceLoader
                            .getResource("classpath:sql/" + scriptName)
                            .getInputStream()) {

                        plsql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    } catch (IOException e) {
                        // 如果腳本讀取失敗，將其包裝為 RuntimeException 以觸發回滾
                        throw new IllegalStateException("無法讀取 " + scriptName, e);
                    }

                    // 執行匿名 PL/SQL 區塊
                    jdbcTemplate.execute(plsql);
                    return null; // 返回 null 因為我們沒有回傳值
                });
                log.info("Finished and committed script: {}", scriptName);
            } catch (Exception e) {
                // 如果交易執行中發生任何錯誤，TransactionTemplate 會自動回滾。
                // 我們在此處捕獲異常，記錄錯誤，然後繼續執行下一個腳本。
                log.error("Transaction for script {} was rolled back due to an error: {}", scriptName, e.getMessage(), e);
            }
        }

        // 步驟 3: 取得本次寫入筆數（以當天日期計算，可依實際欄位調整）
        Integer cnt = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM   ZEN_B2B_JSON_SO_TMP
                WHERE  TO_DATE(CREATION_DATE, 'YYYY-MM-DD HH24:MI:SS')
                          >= TRUNC(SYSDATE)
                  AND  TO_DATE(CREATION_DATE, 'YYYY-MM-DD HH24:MI:SS')
                          <  TRUNC(SYSDATE) + 1
                """,
                Integer.class);

        int inserted = cnt != null ? cnt : 0;
        log.info("[{}] 抽取單據完成，寫入 ZEN_B2B_JSON_SO_TMP 共 {} 筆",
                LocalDateTime.now(), inserted);

        return inserted;
    }
}
