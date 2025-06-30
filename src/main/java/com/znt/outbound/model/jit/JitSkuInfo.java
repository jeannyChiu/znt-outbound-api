package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private double gw;

    @JsonProperty("Nw")
    private double nw;

    @JsonProperty("Uom")
    private String uom;

    @JsonProperty("Abc")
    private String abc;

    @JsonProperty("Moq")
    private int moq;

    @JsonProperty("SafetyStock")
    private int safetyStock;

    @JsonProperty("Length")
    private double length;

    @JsonProperty("Width")
    private double width;

    @JsonProperty("Height")
    private double height;

    @JsonProperty("Cube")
    private double cube;

    @JsonProperty("Area")
    private double area;

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