package com.znt.outbound.model.json;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@JsonPropertyOrder({
    "ord_type", "busi_date", "ref_no", 
    "consignee_company", "bill_to", "ref_consignee_code", 
    "consignee_tel", "consignee_contact", "consignee_addr",
    "OrderLists", "ShippingInstructions"
})
public class FeiliksOrder {
    @JsonProperty("ord_type")
    private String ordType;

    @JsonProperty("busi_date")
    private String busiDate;

    @JsonProperty("ref_no")
    private String refNo;

    @JsonProperty("consignee_company")
    private String consigneeCompany;

    @JsonProperty("bill_to")
    private String billTo;

    @JsonProperty("ref_consignee_code")
    private String refConsigneeCode;

    @JsonProperty("consignee_tel")
    private String consigneeTel;

    @JsonProperty("consignee_contact")
    private String consigneeContact;

    @JsonProperty("consignee_addr")
    private String consigneeAddr;

    @JsonProperty("OrderLists")
    private List<FeiliksOrderList> orderLists;

    @JsonProperty("ShippingInstructions")
    private List<FeiliksShippingInstruction> shippingInstructions;
} 