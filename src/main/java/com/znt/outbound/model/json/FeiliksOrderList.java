package com.znt.outbound.model.json;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonPropertyOrder({
    "ref_sku_code", "g_name", "g_brand", "g_model", "piece_num", "price", "total_price",
    "po_no", "po_line", "invoice_no", "invoice_date", "relabel_sku_code", "note",
    "ext_1", "ext_3", "ext_4", "dn_no", "ext_5", "shipping_mark", "ext_6", "ext_7",
    "ref_division_name", "rma_no", "pre_po_no"
})
public class FeiliksOrderList {
    @JsonProperty("ref_sku_code") private String refSkuCode;
    @JsonProperty("g_name") private String gName;
    @JsonProperty("g_brand") private String gBrand;
    @JsonProperty("g_model") private String gModel;
    @JsonProperty("piece_num") private String pieceNum;
    private String price;
    @JsonProperty("total_price") private String totalPrice;
    @JsonProperty("po_no") private String poNo;
    @JsonProperty("po_line") private String poLine;
    @JsonProperty("invoice_no") private String invoiceNo;
    @JsonProperty("invoice_date") private String invoiceDate;
    @JsonProperty("relabel_sku_code") private String relabelSkuCode;
    private String note;
    @JsonProperty("ext_1") private String ext1;
    @JsonProperty("ext_3") private String ext3;
    @JsonProperty("ext_4") private String ext4;
    @JsonProperty("dn_no") private String dnNo;
    @JsonProperty("ext_5") private String ext5;
    @JsonProperty("shipping_mark") private String shippingMark;
    @JsonProperty("ext_6") private String ext6;
    @JsonProperty("ext_7") private String ext7;
    @JsonProperty("ref_division_name") private String refDivisionName;
    @JsonProperty("rma_no") private String rmaNo;
    @JsonProperty("pre_po_no") private String prePoNo;
} 