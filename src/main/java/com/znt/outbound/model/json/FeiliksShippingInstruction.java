package com.znt.outbound.model.json;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({"qc_param", "qc_type"})
public class FeiliksShippingInstruction {
    @JsonProperty("qc_param")
    private String qcparam;
    @JsonProperty("qc_type")
    private String qcType;
} 