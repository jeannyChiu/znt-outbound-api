package com.znt.outbound.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeiliksRequest {
    private String sign;
    @JsonProperty("part_code")
    private String partCode;
    @JsonProperty("request_time")
    private String requestTime;
    private FeiliksData data;
} 