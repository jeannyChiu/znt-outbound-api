package com.znt.outbound.model.jit.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JitLoginResponse {

    @JsonProperty("userName")
    private String userName; // 實際上是字串類型，不是boolean

    @JsonProperty("appToken")
    private String appToken;
}