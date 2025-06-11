package com.znt.outbound.model.json;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeiliksShippingInstruction {
    @JsonProperty("qc_param")
    private String qcparam;
    @JsonProperty("qc_type")
    private String qcType;
} 