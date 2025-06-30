package com.znt.outbound.service;

import com.znt.outbound.model.jit.JitAsnRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JitAsnMappingService {

    private final JdbcTemplate jdbcTemplate;
    private final JitApiClient jitApiClient;
    private final EnvelopeService envelopeService;
    private final ApiConfigService apiConfigService;

    /**
     * 處理並發送 JIT 入庫單 (ASN) 的主要方法。
     * 該方法將執行 SQL 查詢，將結果映射到 JIT API 所需的格式，然後發送它。
     */
    public void processAndSendJitAsn() {
        log.info("開始執行 JIT 入庫單處理與發送作業...");

        // 步驟 1: 讀取 SQL 查詢語句 (此處為示意，實際路徑需確認)
        String sql;
        try {
            // 假設 SQL 檔案位於 classpath:sql/select_asn_for_jit.sql
            // 您可能需要根據專案結構調整 Resource 的讀取方式
            try (Reader reader = new InputStreamReader(this.getClass().getResourceAsStream("/sql/select_asn_for_jit.sql"), StandardCharsets.UTF_8)) {
                sql = FileCopyUtils.copyToString(reader);
            }
        } catch (Exception e) {
            log.error("讀取 select_asn_for_jit.sql 檔案失敗。", e);
            return; // 無法讀取 SQL，終止執行
        }

        // 步驟 2: 執行 SQL 查詢
        log.info("正在從內部資料庫查詢待處理的 ASN 資料...");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (rows.isEmpty()) {
            log.info("沒有找到需要發送到 JIT 的入庫單資料。");
            return;
        }

        // 步驟 3: 將查詢結果分組並映射到 DTO
        // 這是最關鍵的業務邏輯部分。
        // 您需要根據 SQL 查詢返回的欄位，將扁平的資料結構 (List<Map>)
        // 轉換為具有層次結構的 JitAsnRequest 物件。
        // 通常這會需要對 `rows` 進行迴圈和分組 (例如，按入庫單號分組)。

        // *** 待辦事項: 在此處實現資料映射邏輯 ***
        // 以下為一個非常簡化的示意，您需要根據實際資料進行擴充
        log.warn("注意：JIT ASN 資料映射邏輯尚未完全實現，目前僅為框架。");

        // 假設一個查詢結果對應一個 ASN 請求
        JitAsnRequest requestToSend = mapDataToJitAsnRequest(rows);


        // 步驟 4: 寫入封套 (Envelope) 並呼叫 API Client 發送請求
        if (requestToSend != null) {
            String seqId = null; // 用於儲存封套的 SEQ_ID
            try {
                // 發送前，先寫入 ZEN_B2B_ENVELOPE，狀態為 'W' (Waiting)
                log.info("準備為 ASN (ExternalNo: {}) 寫入 B2B 封套...", requestToSend.getExternalNo());
                String providerName = apiConfigService.getProviderName();
                seqId = envelopeService.createJitAsnEnvelope(requestToSend.getExternalNo(), providerName);

                // 封套寫入成功後，才發送 API
                log.info("B2B 封套寫入成功 (SEQ_ID: {}), 準備發送 ASN (ExternalNo: {}) 到 JIT。", seqId, requestToSend.getExternalNo());
                ResponseEntity<String> response = jitApiClient.sendAsn(requestToSend);

                // 根據 API 回應更新封套狀態
                if (response != null && response.getStatusCode().is2xxSuccessful()) {
                    envelopeService.updateEnvelopeStatus(seqId, "S"); // Success
                } else {
                    envelopeService.updateEnvelopeStatus(seqId, "F"); // Failed
                }

            } catch (Exception e) {
                // 如果在寫入封套或發送過程中發生無法捕獲的異常
                log.error("在處理 ASN (ExternalNo: {}) 過程中發生嚴重錯誤。", requestToSend.getExternalNo(), e);
                // 如果在 API call 之前就出錯，seqId 可能為 null
                if (seqId != null) {
                    envelopeService.updateEnvelopeStatus(seqId, "F");
                }
            }
        } else {
            log.error("資料映射失敗，無法產生有效的 JitAsnRequest 物件。");
        }

        log.info("JIT 入庫單處理作業結束。");
    }

    /**
     * 將從資料庫查詢出的扁平結果映射到 JitAsnRequest 物件。
     * @param rows 從 jdbcTemplate 查詢出的結果列表
     * @return 組裝好的 JitAsnRequest 物件，如果無法組裝則返回 null
     */
    private JitAsnRequest mapDataToJitAsnRequest(List<Map<String, Object>> rows) {
        // *** 待辦事項: 根據您同事提供的 SQL 查詢結果，在此處實現完整的映射邏輯。***
        // 您需要從 `rows` 中取出每一個欄位的值，然後設定到 JitAsnRequest
        // 以及其內部的 JitAsnLine, JitSkuInfo, JitAsnLineAttr 物件中。

        // 以下是一個無法運作的偽代碼，僅用於示意結構
        /*
        if (rows.isEmpty()) {
            return null;
        }
        
        JitAsnRequest request = new JitAsnRequest();
        Map<String, Object> firstRow = rows.get(0); // 假設表頭資訊在所有行都相同

        request.setExternalNo(firstRow.get("YOUR_ASN_NUMBER_COLUMN").toString());
        request.setWhName(firstRow.get("YOUR_WAREHOUSE_COLUMN").toString());
        // ... 設定其他 Header 欄位 ...

        List<JitAsnLine> lines = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            JitAsnLine line = new JitAsnLine();
            line.setStorerLineNo(row.get("YOUR_LINE_NUMBER_COLUMN").toString());
            // ... 設定其他 Line 欄位 ...

            JitSkuInfo skuInfo = new JitSkuInfo();
            skuInfo.setSku(row.get("YOUR_SKU_COLUMN").toString());
            // ... 設定 SKU Info 欄位 ...
            line.setSkuInfo(skuInfo);

            JitAsnLineAttr attr = new JitAsnLineAttr();
            attr.setLoc(row.get("YOUR_LOC_COLUMN").toString());
            // ... 設定 Attr 欄位 ...
            line.setAsnLineAttr(attr);

            lines.add(line);
        }
        request.setLines(lines);

        return request;
        */
        return null; // 暫時返回 null
    }
} 