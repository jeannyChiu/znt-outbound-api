package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JIT庫內換料請求DTO
 * 支援三種換料類型：
 * - Exchange: 1:1換料
 * - Combine: 1:N組裝
 * - Separate: 1:N拆解
 */
@Data
public class JitInvExchangeRequest {
    @JsonProperty("ExternalId")
    private String externalId;  // 外部ID

    @JsonProperty("ExternalNo")
    private String externalNo;  // 外部編號

    @JsonProperty("WhName")
    private String whName;      // 倉庫名稱（默認GT）

    @JsonProperty("Storer")
    private String storer;      // 貨主（帳本）

    @JsonProperty("ExchangeType")
    private String exchangeType; // 換料類型：Combine-組裝, Separate-拆解, Exchange-換料

    @JsonProperty("ApplyDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime applyDate; // 日期

    @JsonProperty("RefNo")
    private String refNo;       // 唯一單號

    @JsonProperty("Remark")
    private String remark;      // 備註

    @JsonProperty("FinalLines")
    private List<JitInvExchangeSkuFinalLineByApi> finalLines; // 成品明細集合
}