package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.znt.outbound.config.PlainDoubleSerializer;
import lombok.Data;

@Data
public class JitSkuInfo {
    @JsonProperty("Sku")
    private String sku;

    @JsonProperty("SkuName")
    private String skuName;

    @JsonProperty("SkuNameE")
    private String skuNameE;

    @JsonProperty("Spec")
    private String spec;

    @JsonProperty("StorerAbbrName")
    private String storerAbbrName;

    @JsonProperty("Category")
    private String category;

    @JsonProperty("SubCategory")
    private String subCategory;

    @JsonProperty("ProductLine")
    private String productLine;

    @JsonProperty("Coo")
    private String coo;

    @JsonProperty("Gw")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double gw;

    @JsonProperty("Nw")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double nw;

    @JsonProperty("Uom")
    private String uom;

    @JsonProperty("Abc")
    private String abc;

    @JsonProperty("Moq")
    private Integer moq;

    @JsonProperty("SafetyStock")
    private Integer safetyStock;

    @JsonProperty("Length")
    private Integer length;

    @JsonProperty("Width")
    private Integer width;

    @JsonProperty("Height")
    private Integer height;

    @JsonProperty("Cube")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double cube;

    @JsonProperty("Area")
    @JsonSerialize(using = PlainDoubleSerializer.class)
    private Double area;

    @JsonProperty("Descriptions")
    private String descriptions;

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
} 