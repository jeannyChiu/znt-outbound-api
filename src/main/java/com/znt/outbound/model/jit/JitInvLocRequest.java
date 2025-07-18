package com.znt.outbound.model.jit;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * JIT 庫存查詢請求物件
 * 對應資料表: JIT_INV_LOC_REQUEST
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JitInvLocRequest {

    /**
     * 請求ID (主鍵)
     */
    private Long requestId;

    /**
     * 倉庫名稱
     */
    private String whName;

    /**
     * 儲區名稱
     */
    private String zoneName;

    /**
     * 貨主名稱
     */
    private String storerAbbrName;

    /**
     * 料號
     */
    private String sku;

    /**
     * 處理狀態 (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    @Builder.Default
    private String status = "PENDING";

    /**
     * 創建時間
     */
    private LocalDateTime createdAt;

    /**
     * 更新時間
     */
    private LocalDateTime updatedAt;

    /**
     * 建構子 - 用於建立新的請求 (不包含 ID 和時間戳)
     */
    public JitInvLocRequest(String whName, String zoneName, String storerAbbrName, String sku) {
        this.whName = whName;
        this.zoneName = zoneName;
        this.storerAbbrName = storerAbbrName;
        this.sku = sku;
        this.status = "PENDING";
        this.createdAt = LocalDateTime.now();
    }
}