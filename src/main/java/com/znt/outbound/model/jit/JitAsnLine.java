package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.znt.outbound.config.PlainDoubleSerializer;
import lombok.Data;

@Data
public class JitAsnLine {
    @JsonProperty("StorerLineNo")
    private String storerLineNo;

    @JsonProperty("SkuInfo")
    private JitSkuInfo skuInfo;

    @JsonProperty("QtyExpected")
    private Integer qtyExpected;

    @JsonProperty("ZoneName")
    private String zoneName;

    @JsonProperty("Nw")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double nw;

    @JsonProperty("Gw")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double gw;

    @JsonProperty("Cube")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double cube;

    @JsonProperty("Uom")
    private String uom;

    @JsonProperty("UserDef1")
    private String userDef1;

    @JsonProperty("UserDef2")
    private String userDef2;

    @JsonProperty("UserDef3")
    private String userDef3;

    @JsonProperty("UserDef4")
    private String userDef4;

    @JsonProperty("UserDef5")
    private String userDef5;

    @JsonProperty("Descriptions")
    private String descriptions;

    @JsonProperty("AsnLineAttr")
    private JitAsnLineAttr asnLineAttr;
} 