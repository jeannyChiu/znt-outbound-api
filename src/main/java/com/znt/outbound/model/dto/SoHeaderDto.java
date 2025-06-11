package com.znt.outbound.model.dto;

import lombok.Data;

@Data
public class SoHeaderDto {
    private String ordType;
    private String expShipDate;
    private String invoiceNo;
    private String custName;
    private String invoiceAddress;
    private String shipToNo;
    private String shipToPhone;
    private String shipToContact;
    private String deliveryAddress;
} 