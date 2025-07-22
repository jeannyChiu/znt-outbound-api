package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * JIT庫內換料成品明細DTO
 * 包含成品信息和原材料明細集合
 */
@Data
public class JitInvExchangeSkuFinalLineByApi {
    @JsonProperty("Product")
    private String product;     // 成品料號

    @JsonProperty("Qty")
    private Integer qty;        // 轉換數量（保持為正數）
                               // Exchange-換料情況下，Product指新料號，數量代表增加

    @JsonProperty("ZoneName")
    private String zoneName;    // 儲區 (倉別)（必填）

    @JsonProperty("Lot")
    private String lot;         // 指定Lot（如果需要指定庫存進行料號轉換則請提供值，否則留空，以下字段類同）

    @JsonProperty("Batch")
    private String batch;       // 指定原廠Batch

    @JsonProperty("DateCode")
    private String dateCode;    // 指定Date Code

    @JsonProperty("Coo")
    private String coo;         // 指定產地

    @JsonProperty("MaterialLines")
    private List<JitInvExchangeMaterialLineByApi> materialLines; // 原材料明細集合
}