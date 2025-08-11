package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * JIT 庫存位置物件
 * 對應 JIT API 回應中的 InvLocs 陣列元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class JitInvLoc {

    /**
     * 庫存位置ID
     */
    @JsonProperty("InvLocId")
    private Long invLocId;

    /**
     * 倉庫名稱
     */
    @JsonProperty("WhName")
    private String whName;

    /**
     * 貨主名稱
     */
    @JsonProperty("StorerAbbrName")
    private String storerAbbrName;

    /**
     * 儲區名稱
     */
    @JsonProperty("ZoneName")
    private String zoneName;

    /**
     * 儲位編號
     */
    @JsonProperty("Loc")
    private String loc;

    /**
     * 棧板號
     */
    @JsonProperty("Lpn")
    private String lpn;

    /**
     * 批號
     */
    @JsonProperty("Lot")
    private String lot;

    /**
     * 料號
     */
    @JsonProperty("Sku")
    private String sku;

    /**
     * 品名
     */
    @JsonProperty("SkuName")
    private String skuName;

    /**
     * 英文品名
     */
    @JsonProperty("SkuNameE")
    private String skuNameE;

    /**
     * 規格型號
     */
    @JsonProperty("Spec")
    private String spec;

    /**
     * 庫存量
     */
    @JsonProperty("Qty")
    private BigDecimal qty;

    /**
     * 已配貨量
     */
    @JsonProperty("QtyAllocated")
    private BigDecimal qtyAllocated;

    /**
     * 已揀貨量
     */
    @JsonProperty("QtyPicked")
    private BigDecimal qtyPicked;

    /**
     * 凍結量
     */
    @JsonProperty("QtyHold")
    private BigDecimal qtyHold;

    /**
     * 可用量
     */
    @JsonProperty("QtyAvailable")
    private BigDecimal qtyAvailable;

    /**
     * 描述
     */
    @JsonProperty("Descriptions")
    private String descriptions;

    /**
     * 創建時間
     */
    @JsonProperty("CreateDt")
    private LocalDateTime createDt;

    /**
     * 創建人
     */
    @JsonProperty("CreateP")
    private String createP;

    /**
     * 創建人名稱
     */
    @JsonProperty("CreatePn")
    private String createPn;

    /**
     * 更新時間
     */
    @JsonProperty("UpdateDt")
    private LocalDateTime updateDt;

    /**
     * 更新人
     */
    @JsonProperty("UpdateP")
    private String updateP;

    /**
     * 更新人名稱
     */
    @JsonProperty("UpdatePn")
    private String updatePn;

    /**
     * 收貨/入庫日期
     */
    @JsonProperty("ReceiveDt")
    private LocalDateTime receiveDt;

    /**
     * 庫齡
     */
    @JsonProperty("InvAgeDays")
    private Integer invAgeDays;

    /**
     * 製造日期
     */
    @JsonProperty("DateCode")
    private Long dateCode;

    /**
     * 有效期
     */
    @JsonProperty("ExpiredDt")
    private LocalDateTime expiredDt;

    /**
     * 產地
     */
    @JsonProperty("Coo")
    private String coo;

    /**
     * 包裝形態
     */
    @JsonProperty("PackageType")
    private String packageType;

    /**
     * 入庫方式
     */
    @JsonProperty("InboundType")
    private String inboundType;

    /**
     * 外部批號
     */
    @JsonProperty("VLot")
    private String vLot;

    /**
     * 單件裝量
     */
    @JsonProperty("QtyPEa")
    private BigDecimal qtyPEa;

    /**
     * 每箱數量
     */
    @JsonProperty("QtyPCase")
    private BigDecimal qtyPCase;

    /**
     * 每盒數量
     */
    @JsonProperty("QtyPip")
    private BigDecimal qtyPip;

    /**
     * 每內盒數量
     */
    @JsonProperty("QtyPPip")
    private BigDecimal qtyPPip;

    // 批次屬性欄位 01-20
    @JsonProperty("LotAttr01")
    private String lotAttr01;
    @JsonProperty("LotAttr02")
    private String lotAttr02;
    @JsonProperty("LotAttr03")
    private String lotAttr03;
    @JsonProperty("LotAttr04")
    private String lotAttr04;
    @JsonProperty("LotAttr05")
    private String lotAttr05;
    @JsonProperty("LotAttr06")
    private String lotAttr06;
    @JsonProperty("LotAttr07")
    private String lotAttr07;
    @JsonProperty("LotAttr08")
    private String lotAttr08;
    @JsonProperty("LotAttr09")
    private String lotAttr09;
    @JsonProperty("LotAttr10")
    private String lotAttr10;
    @JsonProperty("LotAttr11")
    private String lotAttr11;
    @JsonProperty("LotAttr12")
    private String lotAttr12;
    @JsonProperty("LotAttr13")
    private String lotAttr13;
    @JsonProperty("LotAttr14")
    private String lotAttr14;
    @JsonProperty("LotAttr15")
    private String lotAttr15;
    @JsonProperty("LotAttr16")
    private String lotAttr16;
    @JsonProperty("LotAttr17")
    private String lotAttr17;
    @JsonProperty("LotAttr18")
    private String lotAttr18;
    @JsonProperty("LotAttr19")
    private String lotAttr19;
    @JsonProperty("LotAttr20")
    private String lotAttr20;
}