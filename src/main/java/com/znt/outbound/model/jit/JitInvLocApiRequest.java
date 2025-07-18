package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

/**
 * JIT 庫存查詢 API 請求物件
 * 用於發送到 JIT API 的請求格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JitInvLocApiRequest {

    /**
     * 倉庫名稱
     */
    @JsonProperty("WhName")
    private String whName;

    /**
     * 儲區名稱
     */
    @JsonProperty("ZoneName")
    private String zoneName;

    /**
     * 貨主名稱
     */
    @JsonProperty("StorerAbbrName")
    private String storerAbbrName;

    /**
     * 料號
     */
    @JsonProperty("Sku")
    private String sku;
}