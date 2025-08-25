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
        String requestJson = null;
        String responseJson = null;

        try {
            // 在發送請求前，先建立 B2B 封套紀錄並取得 SEQ_ID
            seqId = envelopeService.createOutboundEnvelope(request.getPartCode(), request.getSign());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            requestJson = objectMapper.writeValueAsString(request);
            log.debug("發送的請求 Body: {}", requestJson);

            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            // 直接獲取原始位元組以確保編碼正確
            ResponseEntity<byte[]> responseEntity = restTemplate.postForEntity(url, entity, byte[].class);
            
            log.info("API 請求完成。狀態碼: {}", responseEntity.getStatusCode());
            
            // 檢查狀態碼
            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                // 檢查是否有回應內容
                byte[] responseBytes = responseEntity.getBody();
                if (responseBytes != null && responseBytes.length > 0) {
                    responseJson = new String(responseBytes, StandardCharsets.UTF_8);
                    log.info("API 回應 Body: {}", responseJson);
                    
                    try {
                        // 嘗試解析回應內容
                        FeiliksResponseDto responseDto = objectMapper.readValue(responseJson, FeiliksResponseDto.class);
                        if (responseDto.isSuccess()) {
                            transFlag = "S";
                            log.info("API 處理成功（根據回應內容）");
                        } else {
                            transFlag = "F";
                            log.warn("API 處理失敗（根據回應內容）: {}", responseDto.getMsg());
                        }
                    } catch (JsonProcessingException e) {
                        log.error("無法解析 API 回應內容: {}", responseJson, e);
                        transFlag = "F";
                    }
                } else {
                    // 沒有回應內容，但狀態碼是 200，視為成功
                    log.info("API 回應狀態碼 200 但沒有回應內容，視為成功");
                    responseJson = "HTTP 200 OK - 無回應內容";
                    transFlag = "S";
                }
            } else {
                transFlag = "F";
                responseJson = "HTTP " + responseEntity.getStatusCode() + " - 請求失敗";
                log.error("API 請求失敗。狀態碼: {}", responseEntity.getStatusCode());
            }

        } catch (Exception e) {
            log.error("在處理 API 請求過程中發生錯誤。", e);
            transFlag = "F";
            responseJson = "異常錯誤: " + e.getMessage();
        } finally {
            if (seqId != null) {
                // 無論成功或失敗，都更新封套狀態並包含 JSON 內容
                statusUpdateService.updateStatus(seqId, transFlag);
                // 使用 EnvelopeService 更新狀態並發送包含 JSON 的通知
                envelopeService.updateEnvelopeStatusWithJson(seqId, transFlag, requestJson, responseJson);
            } else {
                log.error("SEQ_ID 為 null，無法更新封套狀態。可能是建立封套時就已失敗。");
            }
        }
    }
} 