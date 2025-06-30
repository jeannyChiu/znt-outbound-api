package com.znt.outbound.model.jit.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JitLoginResponse {

    @JsonProperty("userName")
    private boolean userNameSuccess; // 根據文件，此欄位為 boolean

    @JsonProperty("appToken")
    private String appToken;
} 