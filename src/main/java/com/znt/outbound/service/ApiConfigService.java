package com.znt.outbound.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${logistics.provider.name:FEILIKS}")
    private String providerName;

    private String partCode;
    private String secretKey;
    private String apiUrl;
    private String asnApiUrl;
    private String moveTradeApiUrl;
    private String invLocApiUrl;
    private String loginApiUrl;
    private String loginUsername;
    private String loginPassword;

    @PostConstruct
    public void init() {
        log.info("正在為物流商 '{}' 從資料庫載入 API 設定...", providerName);
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

            this.partCode = configMap.get(providerName + "-APPID");
            this.secretKey = configMap.get(providerName + "-APPKEY");
            this.apiUrl = configMap.get(providerName + "-SO_URLT");
            this.asnApiUrl = configMap.get(providerName + "-ASN_URLT");
            this.moveTradeApiUrl = configMap.get(providerName + "-MOVE_TRADE_URLT");
            this.invLocApiUrl = configMap.get(providerName + "-INV_LOC_URLT");
            this.loginApiUrl = configMap.get(providerName + "-LOGIN_URLT");
            this.loginUsername = configMap.get(providerName + "-LOGIN_USERNAME");
            this.loginPassword = configMap.get(providerName + "-LOGIN_PASSWORD");

            if (this.partCode == null || this.secretKey == null) {
                log.warn("物流商 '{}' 的部分 API 設定 (APPID 或 APPKEY) 未找到，部分功能可能受限。", providerName);
                // 這不再是一個會中斷啟動的錯誤，因為某些 provider 可能不需要它
            }
            log.info("API 設定載入成功。Provider: {}, Part Code: {}",
                    providerName, this.partCode);
            log.info("SO_URL: {}, ASN_URL: {}, MOVE_TRADE_URL: {}, INV_LOC_URL: {}, LOGIN_URL: {}", 
                    this.apiUrl, this.asnApiUrl, this.moveTradeApiUrl, this.invLocApiUrl, this.loginApiUrl);
            log.info("Login Username: {}", this.loginUsername);

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

    public String getAsnApiUrl() {
        if (asnApiUrl == null) {
            log.warn("ASN API URL 尚未從資料庫初始化或未設定。");
        }
        return asnApiUrl;
    }

    public String getLoginApiUrl() {
        if (loginApiUrl == null) {
            log.warn("Login API URL 尚未從資料庫初始化或未設定。");
        }
        return loginApiUrl;
    }

    public String getLoginUsername() {
        if (loginUsername == null) {
            log.error("Login Username 尚未從資料庫初始化。");
        }
        return loginUsername;
    }

    public String getLoginPassword() {
        if (loginPassword == null) {
            log.error("Login Password 尚未從資料庫初始化。");
        }
        return loginPassword;
    }

    public String getMoveTradeApiUrl() {
        if (moveTradeApiUrl == null) {
            log.warn("Move Trade API URL 尚未從資料庫初始化或未設定。");
        }
        return moveTradeApiUrl;
    }

    public String getInvLocApiUrl() {
        if (invLocApiUrl == null) {
            log.warn("Inventory Location API URL 尚未從資料庫初始化或未設定。");
        }
        return invLocApiUrl;
    }

    public String getProviderName() {
        return providerName;
    }
} 