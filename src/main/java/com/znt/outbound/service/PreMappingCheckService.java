package com.znt.outbound.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;

@Service
@RequiredArgsConstructor
@Slf4j
public class PreMappingCheckService {

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    /**
     * 檢查 ZEN_B2B_JSON_SO 表中是否有狀態為 'W' 的紀錄。
     * @return 如果有待處理紀錄則返回 true，否則返回 false。
     */
    public boolean hasRecordsToProcess() {
        log.info(">>> 正在檢查是否有狀態為 'W' 的待處理資料...");

        String plsql;
        try (InputStream is = resourceLoader.getResource("classpath:sql/pre_mapping_check.sql").getInputStream()) {
            plsql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(">>> 無法讀取 pre_mapping_check.sql 檔案。", e);
            throw new IllegalStateException("無法讀取 pre_mapping_check.sql", e);
        }

        try {
            jdbcTemplate.execute(plsql);
            log.info(">>> 發現待處理資料，準備執行下一步。");
            return true;
        } catch (DataAccessException e) {
            Throwable rootCause = e.getRootCause();
            if (rootCause instanceof SQLException sqlEx) {
                if (sqlEx.getErrorCode() == 20001) {
                    log.info(">>> {}", sqlEx.getMessage());
                    return false;
                }
            }
            log.error(">>> 執行 pre_mapping_check.sql 時發生非預期的資料庫錯誤。", e);
            throw e;
        }
    }
} 