package com.znt.outbound.model.jit.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JitLoginRequest {

    @JsonProperty("userName")
    private String userName;

    @JsonProperty("password")
    private String password;
} 