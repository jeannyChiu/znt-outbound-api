package com.znt.outbound.service;

import com.znt.outbound.model.jit.JitAsnRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class JitApiClient {

    private final RestTemplate restTemplate;
    private final ApiConfigService apiConfigService;
    private final JitAuthService jitAuthService;

    /**
     * 發送 ASN (入庫單) 資料到 JIT API，包含自動驗證和重試機制。
     * @param asnRequest 包含入庫單資料的請求物件
     * @return JIT API 的回應實體，如果發送失敗則返回 null
     */
    public ResponseEntity<String> sendAsn(JitAsnRequest asnRequest) {
        // 嘗試第一次發送
        ResponseEntity<String> response = attemptToSendAsn(asnRequest, false);

        // 如果因為未授權而失敗，則清除 token 並重試一次
        if (response != null && response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            log.warn("接收到 401 Unauthorized 回應，Token 可能已過期。正在清除 token 並重試...");
            jitAuthService.clearAuthToken();
            response = attemptToSendAsn(asnRequest, true);
        }

        return response;
    }

    private ResponseEntity<String> attemptToSendAsn(JitAsnRequest asnRequest, boolean isRetry) {
        String url = apiConfigService.getAsnApiUrl();
        if (url == null || url.isEmpty()) {
            log.error("JIT ASN API URL 未設定，無法發送請求。");
            return null;
        }

        // 從驗證服務獲取 Token
        String authToken = jitAuthService.getAuthToken();
        if (authToken == null) {
            log.error("無法獲取 JIT auth-token，無法發送 ASN 請求。");
            return null;
        }

        // 準備 Headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("username", apiConfigService.getLoginUsername());
        headers.set("auth-token", authToken);

        HttpEntity<JitAsnRequest> requestEntity = new HttpEntity<>(asnRequest, headers);

        String attemptLog = isRetry ? "重試" : "首次嘗試";
        log.info("({}) 準備發送 ASN 到 JIT API。URL: {}, ExternalNo: {}", attemptLog, url, asnRequest.getExternalNo());

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, requestEntity, String.class);
            log.info("({}) 成功接收到 JIT API 的回應。ExternalNo: {}, Status: {}",
                    attemptLog, asnRequest.getExternalNo(), responseEntity.getStatusCode());
            return responseEntity;
        } catch (HttpClientErrorException e) {
            // 捕獲 HTTP 錯誤，例如 401, 404 等
            log.error("({}) 發送 ASN 到 JIT API 時發生 HTTP 錯誤。ExternalNo: {}, Status: {}, Response: {}",
                    attemptLog, asnRequest.getExternalNo(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            // 將 HttpClientErrorException 包裝成 ResponseEntity 回傳，以便上層判斷
            return new ResponseEntity<>(e.getResponseBodyAsString(), e.getStatusCode());
        } catch (RestClientException e) {
            log.error("({}) 發送 ASN 到 JIT API 時發生連接錯誤。ExternalNo: {}, URL: {}",
                    attemptLog, asnRequest.getExternalNo(), url, e);
            return null;
        }
    }
} 