package com.znt.outbound.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@JsonPropertyOrder({"Orders"})
public class FeiliksData {
    @JsonProperty("Orders")
    private List<FeiliksOrder> orders;
} 