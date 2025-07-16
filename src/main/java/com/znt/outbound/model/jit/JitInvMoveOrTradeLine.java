package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JitInvMoveOrTradeLine {
    @JsonProperty("Sku")
    private String sku;

    @JsonProperty("Qty")
    private Integer qty;

    @JsonProperty("FromStorerCate")
    private String fromStorerCate;

    @JsonProperty("ToStorerCate")
    private String toStorerCate;

    @JsonProperty("Lot")
    private String lot;

    @JsonProperty("Batch")
    private String batch;

    @JsonProperty("DateCode")
    private String dateCode;

    @JsonProperty("Coo")
    private String coo;

    @JsonProperty("Remark")
    private String remark;
}