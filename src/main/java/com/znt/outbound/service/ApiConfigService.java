package com.znt.outbound.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiConfigService {

    private final JdbcTemplate jdbcTemplate;
    private String partCode;
    private String secretKey;
    private String apiUrl;

    @PostConstruct
    public void init() {
        log.info("正在從資料庫載入 API 設定...");
        // 修正：從 TD_NAME 欄位讀取資料
        String sql = "SELECT TD_NO, TD_NAME FROM ZEN_B2B_TAB_D WHERE T_NO = 'JSON_SYS_INFO'";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

            // 將查詢結果轉換為 Map 以方便取值
            Map<String, String> configMap = rows.stream()
                .filter(row -> row.get("TD_NO") != null && row.get("TD_NAME") != null)
                .collect(Collectors.toMap(
                    row -> row.get("TD_NO").toString(),
                    row -> row.get("TD_NAME").toString()
                ));

            this.partCode = configMap.get("FEILIKS-APPID");
            this.secretKey = configMap.get("FEILIKS-APPKEY");
            this.apiUrl = configMap.get("FEILIKS-SO_URLT");

            if (this.partCode == null || this.secretKey == null || this.apiUrl == null) {
                log.error("在資料庫中找不到必要的 API 設定 (FEILIKS-APPID, FEILIKS-APPKEY, 或 FEILIKS-SO_URLT)。");
                throw new IllegalStateException("無法初始化 API 設定，請檢查 ZEN_B2B_TAB_D 資料表中的 TD_NAME 欄位。");
            }
            log.info("API 設定載入成功。URL: {}, Part Code: {}, Secret Key: (已隱藏)", this.apiUrl, this.partCode);

        } catch (Exception e) {
            log.error("從資料庫載入 API 設定失敗。", e);
            throw new RuntimeException("無法從資料庫載入 API 設定。", e);
        }
    }

    public String getPartCode() {
        if (partCode == null) {
            throw new IllegalStateException("Part Code 尚未從資料庫初始化。");
        }
        return partCode;
    }

    public String getSecretKey() {
        if (secretKey == null) {
            throw new IllegalStateException("Secret Key 尚未從資料庫初始化。");
        }
        return secretKey;
    }

    public String getApiUrl() {
        if (apiUrl == null) {
            throw new IllegalStateException("API URL 尚未從資料庫初始化。");
        }
        return apiUrl;
    }
} 