package com.znt.outbound.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class FeiliksData {
    @JsonProperty("Orders")
    private List<FeiliksOrder> orders;
} 