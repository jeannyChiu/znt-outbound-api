package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class JitAsnRequest {
    @JsonProperty("ExternalId")
    private String externalId;

    @JsonProperty("ExternalNo")
    private String externalNo;

    @JsonProperty("WhName")
    private String whName;

    @JsonProperty("StorerAbbrName")
    private String storerAbbrName;

    @JsonProperty("Priority")
    private boolean priority;

    @JsonProperty("DocType")
    private String docType;

    @JsonProperty("BizType")
    private String bizType;

    @JsonProperty("Voyage")
    private String voyage;

    @JsonProperty("BlNo")
    private String blNo;

    @JsonProperty("CaseCnt")
    private int caseCnt;

    @JsonProperty("PalletCnt")
    private int palletCnt;

    @JsonProperty("ContainerCnt")
    private int containerCnt;

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

    @JsonProperty("Lines")
    private List<JitAsnLine> lines;
} 