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
            String responseBody = new String(responseEntity.getBody(), StandardCharsets.UTF_8);

            log.info("API 請求成功。狀態碼: {}", responseEntity.getStatusCode());
            log.info("API 回應 Body: {}", responseBody);

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                FeiliksResponseDto responseDto = objectMapper.readValue(responseBody, FeiliksResponseDto.class);
                if (responseDto.isSuccess()) {
                    transFlag = "S";
                } else {
                    transFlag = "F";
                }
            } else {
                transFlag = "F";
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