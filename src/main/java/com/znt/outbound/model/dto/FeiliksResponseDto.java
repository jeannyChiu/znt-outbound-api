package com.znt.outbound.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略我們不需要的欄位
public class FeiliksResponseDto {
    private boolean success;
    private String code;
    private String msg;
} 