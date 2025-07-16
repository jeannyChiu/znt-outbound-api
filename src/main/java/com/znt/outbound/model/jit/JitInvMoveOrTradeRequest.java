package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class JitInvMoveOrTradeRequest {
    @JsonProperty("ExternalId")
    private String externalId;

    @JsonProperty("ExternalNo")
    private String externalNo;

    @JsonProperty("WhName")
    private String whName;

    @JsonProperty("TradeType")
    private String tradeType;

    @JsonProperty("FromStorer")
    private String fromStorer;

    @JsonProperty("ToStorer")
    private String toStorer;

    @JsonProperty("ApplyDate")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime applyDate;

    @JsonProperty("RefNo")
    private String refNo;

    @JsonProperty("Remark")
    private String remark;

    @JsonProperty("Lines")
    private List<JitInvMoveOrTradeLine> lines;
}