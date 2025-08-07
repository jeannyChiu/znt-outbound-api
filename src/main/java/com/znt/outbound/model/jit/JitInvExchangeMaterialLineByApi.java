package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * JIT庫內換料原材料明細DTO
 * Exchange模式：成品和原材料之間的比例對關係是1:1，Material是原料號，數量代表減少
 * 其他模式：成品和原材料之間的比例對關係是1:N
 */
@Data
public class JitInvExchangeMaterialLineByApi {
    @JsonProperty("Material")
    private String material;    // 原材料料號
                               // Exchange-換料情況下，成品和原材料之間的比例對關係是1:1，Material是原料號，數量代表減少

    @JsonProperty("MfSku")
    private String mfSku;        // 原廠料號（必填）

    @JsonProperty("Qty")
    private Integer qty;        // 轉換數量（保持為正數）

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
}