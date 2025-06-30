package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JitAsnLine {
    @JsonProperty("StorerLineNo")
    private String storerLineNo;

    @JsonProperty("SkuInfo")
    private JitSkuInfo skuInfo;

    @JsonProperty("QtyExpected")
    private int qtyExpected;

    @JsonProperty("Nw")
    private double nw;

    @JsonProperty("Gw")
    private double gw;

    @JsonProperty("Cube")
    private double cube;

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