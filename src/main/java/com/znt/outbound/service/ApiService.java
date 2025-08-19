package com.znt.outbound.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znt.outbound.model.dto.FeiliksResponseDto;
import com.znt.outbound.model.json.FeiliksRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {

    private final RestTemplate restTemplate;
    private final ApiConfigService apiConfigService;
    private final ObjectMapper objectMapper;
    private final EnvelopeService envelopeService;
    private final StatusUpdateService statusUpdateService;
    private final StatusNotificationService statusNotificationService;

    public void sendRequest(FeiliksRequest request) {
        String url = apiConfigService.getApiUrl();
        String seqId = null;
        String transFlag = "F"; // 預設為失敗

        try {
            // 在發送請求前，先建立 B2B 封套紀錄並取得 SEQ_ID
            seqId = envelopeService.createOutboundEnvelope(request.getPartCode(), request.getSign());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("發送的請求 Body: {}", requestBody);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // 直接獲取原始位元組以確保編碼正確
            ResponseEntity<byte[]> responseEntity = restTemplate.postForEntity(url, entity, byte[].class);
            
            log.info("API 請求完成。狀態碼: {}", responseEntity.getStatusCode());
            
            // 檢查狀態碼
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // 檢查是否有回應內容
                byte[] responseBytes = responseEntity.getBody();
                if (responseBytes != null && responseBytes.length > 0) {
                    String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
                    log.info("API 回應 Body: {}", responseBody);
                    
                    try {
                        // 嘗試解析回應內容
                        FeiliksResponseDto responseDto = objectMapper.readValue(responseBody, FeiliksResponseDto.class);
                        if (responseDto.isSuccess()) {
                            transFlag = "S";
                            log.info("API 處理成功（根據回應內容）");
                        } else {
                            transFlag = "F";
                            log.warn("API 處理失敗（根據回應內容）: {}", responseDto.getMsg());
                        }
                    } catch (JsonProcessingException e) {
                        log.error("無法解析 API 回應內容: {}", responseBody, e);
                        transFlag = "F";
                    }
                } else {
                    // 沒有回應內容，但狀態碼是 200，視為成功
                    log.info("API 回應狀態碼 200 但沒有回應內容，視為成功");
                    transFlag = "S";
                }
            } else {
                transFlag = "F";
                log.error("API 請求失敗。狀態碼: {}", responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("在處理 API 請求過程中發生錯誤。", e);
            transFlag = "F";
        } finally {
            if (seqId != null) {
                // 無論成功或失敗，都呼叫服務更新狀態
                statusUpdateService.updateStatus(seqId, transFlag);
                // 新增：發送狀態通知郵件
                statusNotificationService.sendNotification(seqId);
            } else {
                log.error("SEQ_ID 為 null，無法更新封套狀態。可能是建立封套時就已失敗。");
            }
        }
    }
} 