package com.znt.outbound.service;

import com.znt.outbound.model.jit.auth.JitLoginRequest;
import com.znt.outbound.model.jit.auth.JitLoginResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class JitAuthService {

    private final RestTemplate restTemplate;
    private final ApiConfigService apiConfigService;
    private final AtomicReference<String> cachedToken = new AtomicReference<>(null);

    /**
     * 獲取 JIT API 的認證 Token。
     * 會先嘗試返回快取的 Token，若無則呼叫登入 API 取得新 Token。
     * @return auth-token 字串，若獲取失敗則返回 null。
     */
    public String getAuthToken() {
        if (cachedToken.get() == null) {
            synchronized (this) {
                // Double-checked locking
                if (cachedToken.get() == null) {
                    log.info("快取中無有效的 auth-token，正在嘗試登入以獲取新的 token...");
                    String newToken = loginAndGetToken();
                    if (newToken != null) {
                        log.info("成功獲取新的 auth-token 並存入快取。");
                        cachedToken.set(newToken);
                    }
                }
            }
        }
        return cachedToken.get();
    }

    /**
     * 清除快取的 Token，以便下次能強制重新登入。
     */
    public void clearAuthToken() {
        log.warn("正在清除已快取的 auth-token。");
        cachedToken.set(null);
    }

    private String loginAndGetToken() {
        String loginUrl = apiConfigService.getLoginApiUrl();
        String username = apiConfigService.getLoginUsername();
        String password = apiConfigService.getLoginPassword();

        if (loginUrl == null || username == null || password == null) {
            log.error("JIT 登入資訊不完整 (URL, username, 或 password)，無法執行登入。請檢查資料庫設定。");
            return null;
        }

        JitLoginRequest loginRequest = new JitLoginRequest(username, password);
        HttpEntity<JitLoginRequest> requestEntity = new HttpEntity<>(loginRequest);

        try {
            log.info("正在向 JIT 登入 API 發送請求。URL: {}", loginUrl);
            ResponseEntity<JitLoginResponse> response = restTemplate.postForEntity(loginUrl, requestEntity, JitLoginResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().getAppToken() != null) {
                return response.getBody().getAppToken();
            } else {
                log.error("JIT 登入 API 回應異常。Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                return null;
            }
        } catch (RestClientException e) {
            log.error("呼叫 JIT 登入 API 失敗。", e);
            return null;
        }
    }
} 