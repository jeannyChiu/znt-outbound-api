package com.znt.outbound.model.jit;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * JIT 庫存查詢 API 回應物件
 * 用於接收 JIT API 的回應格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JitInvLocApiResponse {

    /**
     * 庫存位置列表
     */
    @JsonProperty("InvLocs")
    private List<JitInvLoc> invLocs;
}