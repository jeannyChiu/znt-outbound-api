package com.znt.outbound.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.znt.outbound.model.json.FeiliksRequest;
import com.znt.outbound.service.JsonMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Slf4j
public class JsonExportController {

    private final JsonMappingService jsonMappingService;
    private final ObjectMapper objectMapper;

    /**
     * 取得從 ZEN_B2B_JSON_SO_TMP 傳送到 ZEN_B2B_JSON_SO 並完成 JSON Mapping 的資料
     * 
     * @return 映射後的 JSON 資料
     */
    @GetMapping("/mapped-json")
    public ResponseEntity<String> getMappedJson() {
        try {
            log.info("開始產生映射後的 JSON 資料...");
            
            // 呼叫 JsonMappingService 來建立請求物件
            FeiliksRequest request = jsonMappingService.buildRequest();
            
            if (request == null || request.getData() == null || 
                request.getData().getOrders() == null || 
                request.getData().getOrders().isEmpty()) {
                log.info("沒有找到待處理的訂單資料");
                return ResponseEntity.ok("{\n  \"message\": \"沒有找到待處理的訂單資料\"\n}");
            }
            
            // 使用 pretty print 格式化 JSON
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter()
                                          .writeValueAsString(request);
            
            log.info("成功產生 {} 筆訂單的 JSON 資料", request.getData().getOrders().size());
            
            // 設定回應標頭
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Content-Disposition", 
                       "inline; filename=\"mapped-orders-" + 
                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + 
                       ".json\"");
            
            return ResponseEntity.ok()
                                .headers(headers)
                                .body(jsonOutput);
                                
        } catch (Exception e) {
            log.error("產生 JSON 時發生錯誤", e);
            return ResponseEntity.internalServerError()
                                .body("{\n  \"error\": \"產生 JSON 時發生錯誤: " + e.getMessage() + "\"\n}");
        }
    }
    
    /**
     * 取得目前在 ZEN_B2B_JSON_SO 表中的訂單摘要資訊
     * 
     * @return 訂單摘要資訊
     */
    @GetMapping("/order-summary")
    public ResponseEntity<String> getOrderSummary() {
        try {
            log.info("查詢訂單摘要資訊...");
            
            // 這裡可以加入查詢 ZEN_B2B_JSON_SO 表的邏輯
            // 顯示訂單數量、狀態等摘要資訊
            
            String summary = "{\n" +
                           "  \"message\": \"請使用 /api/export/mapped-json 來取得完整的映射後 JSON 資料\",\n" +
                           "  \"endpoint\": \"GET http://localhost:8080/api/export/mapped-json\"\n" +
                           "}";
            
            return ResponseEntity.ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(summary);
                                
        } catch (Exception e) {
            log.error("查詢訂單摘要時發生錯誤", e);
            return ResponseEntity.internalServerError()
                                .body("{\n  \"error\": \"查詢時發生錯誤: " + e.getMessage() + "\"\n}");
        }
    }
}