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
    private final ApiConfigService apiConfigService;

    /**
     * 檢查 ZEN_B2B_JSON_SO 表中是否有狀態為 'W' 的紀錄。
     * @return 如果有待處理紀錄則返回 true，否則返回 false。
     */
    public boolean hasRecordsToProcess() {
        String providerName = apiConfigService.getProviderName();
        log.info(">>> 正在檢查是否有狀態為 'W' 的待處理資料 (RECEIVER_CODE: {})...", providerName);

        try {
            // 直接執行查詢檢查是否有待處理的資料
            String countSql = "SELECT COUNT(*) FROM ZEN_B2B_JSON_SO WHERE STATUS = 'W' AND RECEIVER_CODE = ?";
            Integer count = jdbcTemplate.queryForObject(countSql, Integer.class, providerName);
            
            if (count == null || count == 0) {
                log.info(">>> 沒有狀態為 'W' 的資料需要處理 (RECEIVER_CODE: {})。", providerName);
                return false;
            } else {
                log.info(">>> 發現 {} 筆待處理資料 (RECEIVER_CODE: {})，準備執行下一步。", count, providerName);
                return true;
            }
        } catch (DataAccessException e) {
            log.error(">>> 檢查待處理資料時發生資料庫錯誤 (RECEIVER_CODE: {})。", providerName, e);
            throw e;
        }
    }
} 