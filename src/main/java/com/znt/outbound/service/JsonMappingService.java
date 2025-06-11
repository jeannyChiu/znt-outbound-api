package com.znt.outbound.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.znt.outbound.model.dto.OrderListDto;
import com.znt.outbound.model.dto.ShippingInstructionDto;
import com.znt.outbound.model.dto.SoHeaderDto;
import com.znt.outbound.model.json.FeiliksData;
import com.znt.outbound.model.json.FeiliksOrder;
import com.znt.outbound.model.json.FeiliksOrderList;
import com.znt.outbound.model.json.FeiliksRequest;
import com.znt.outbound.model.json.FeiliksShippingInstruction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class JsonMappingService {

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final ApiConfigService apiConfigService;

    public FeiliksRequest buildRequest() throws JsonProcessingException {
        List<SoHeaderDto> headers = getHeadersToProcess();
        if (headers.isEmpty()) {
            log.info("沒有找到需要映射的訂單標頭。");
            return null;
        }

        List<FeiliksOrder> orders = headers.stream()
                .map(this::mapHeaderToOrder)
                .collect(Collectors.toList());

        String requestTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        FeiliksData feiliksData = FeiliksData.builder().orders(orders).build();

        // 1. 將 data 物件轉換為 JSON 字串以用於簽章
        // 這裡產生的 JSON 字串是緊湊的，沒有多餘的換行或空格
        String dataJson = objectMapper.writeValueAsString(feiliksData);

        // 從 ApiConfigService 獲取設定
        String partCode = apiConfigService.getPartCode();
        String secretKey = apiConfigService.getSecretKey();

        // 2. 根據 dataJson 和 secretKey 產生簽章
        String sign = generateSign(dataJson, secretKey);

        // 3. 建立完整的請求物件
        FeiliksRequest request = FeiliksRequest.builder()
                .sign(sign)
                .partCode(partCode)
                .requestTime(requestTime)
                .data(feiliksData)
                .build();

        log.info("建立請求完成，共包含 {} 筆訂單。簽章已產生。", orders.size());
        log.debug("產生的請求 JSON (包含簽章): {}", objectMapper.writeValueAsString(request));

        return request;
    }

    private List<SoHeaderDto> getHeadersToProcess() {
        try {
            Resource resource = resourceLoader.getResource("classpath:sql/select_so_for_mapping.sql");
            String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            log.info("正在執行查詢以獲取待映射的訂單標頭。");
            List<SoHeaderDto> headers = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(SoHeaderDto.class));
            log.info("查詢到 {} 筆待映射的訂單標頭。", headers.size());
            return headers;
        } catch (IOException e) {
            log.error("讀取 select_so_for_mapping.sql 失敗", e);
            throw new RuntimeException("無法讀取 SQL 檔案", e);
        }
    }

    private FeiliksOrder mapHeaderToOrder(SoHeaderDto header) {
        log.debug("正在映射 invoice_no: {}", header.getInvoiceNo());

        List<FeiliksOrderList> orderLists = getOrderListsByInvoiceNo(header.getInvoiceNo());
        List<FeiliksShippingInstruction> shippingInstructions = getShippingInstructionsByInvoiceNo(header.getInvoiceNo());

        return FeiliksOrder.builder()
                .ordType(toEmpty(header.getOrdType()))
                .busiDate(toEmpty(header.getExpShipDate()))
                .refNo(toEmpty(header.getInvoiceNo()))
                .consigneeCompany(toEmpty(header.getCustName()))
                .billTo(toEmpty(header.getInvoiceAddress()))
                .refConsigneeCode(toEmpty(header.getShipToNo()))
                .consigneeTel(toEmpty(header.getShipToPhone()))
                .consigneeContact(toEmpty(header.getShipToContact()))
                .consigneeAddr(toEmpty(header.getDeliveryAddress()))
                .orderLists(orderLists)
                .shippingInstructions(shippingInstructions)
                .build();
    }

    private List<FeiliksOrderList> getOrderListsByInvoiceNo(String invoiceNo) {
        String sql = "SELECT CUST_PN, ITEM_MODE, BRAND, ITEM_DESC, QUANTITY, UNIT_PRICE, " +
                     "AMOUNT, CUST_PO, CUST_POLINE, INVOICE_NO, INVOICE_DATE, ZT_PART_NO, " +
                     "SHIP_NOTICE, SEGMENT2, CUST_PN2, CUST_PO2, ORDER_NO, PO_REMARK, " +
                     "INV_SPLIT, CUST_PART_NO2, INV_AND_PAC, DEPT, RMA_NUMBER, ORI_PO_NO " +
                     "FROM ZEN_B2B_JSON_SO WHERE INVOICE_NO = ? AND STATUS = 'W' ORDER BY SEQ_ID";

        log.debug("查詢 OrderLists for invoice_no: {}", invoiceNo);

        List<OrderListDto> dtoList = jdbcTemplate.query(
                sql,
                new Object[]{invoiceNo},
                new BeanPropertyRowMapper<>(OrderListDto.class)
        );

        if (dtoList.isEmpty()) {
            log.warn("找不到 invoice_no: {} 的 OrderLists。", invoiceNo);
            return Collections.emptyList();
        }

        return dtoList.stream()
                .map(dto -> FeiliksOrderList.builder()
                        .refSkuCode(toEmpty(dto.getCustPn()))
                        .gName(toEmpty(dto.getItemMode()))
                        .gBrand(toEmpty(dto.getBrand()))
                        .gModel(toEmpty(dto.getItemDesc()))
                        .pieceNum(toEmpty(dto.getQuantity()))
                        .price(toEmpty(dto.getUnitPrice()))
                        .totalPrice(toEmpty(dto.getAmount()))
                        .poNo(toEmpty(dto.getCustPo()))
                        .poLine(toEmpty(dto.getCustPoline()))
                        .invoiceNo(toEmpty(dto.getInvoiceNo()))
                        .invoiceDate(toEmpty(dto.getInvoiceDate()))
                        .relabelSkuCode(toEmpty(dto.getZtPartNo()))
                        .note(toEmpty(dto.getShipNotice()))
                        .ext1(toEmpty(dto.getSegment2()))
                        .ext3(toEmpty(dto.getCustPn2()))
                        .ext4(toEmpty(dto.getCustPo2()))
                        .dnNo(toEmpty(dto.getOrderNo()))
                        .ext5(toEmpty(dto.getPoRemark()))
                        .shippingMark(toEmpty(dto.getInvSplit()))
                        .ext6(toEmpty(dto.getCustPartNo2()))
                        .ext7(toEmpty(dto.getInvAndPac()))
                        .refDivisionName(toEmpty(dto.getDept()))
                        .rmaNo(toEmpty(dto.getRmaNumber()))
                        .prePoNo(toEmpty(dto.getOriPoNo()))
                        .build())
                .collect(Collectors.toList());
    }

    private List<FeiliksShippingInstruction> getShippingInstructionsByInvoiceNo(String invoiceNo) {
        String sql = "SELECT DISTINCT SHIP_NOTES, QC_TYPE FROM ZEN_B2B_JSON_SO WHERE INVOICE_NO = ? AND STATUS = 'W'";
        log.debug("查詢 ShippingInstructions for invoice_no: {}", invoiceNo);

        List<ShippingInstructionDto> dtoList = jdbcTemplate.query(
                sql,
                new Object[]{invoiceNo},
                new BeanPropertyRowMapper<>(ShippingInstructionDto.class)
        );

        if(dtoList.isEmpty()) {
            log.warn("找不到 invoice_no: {} 的 ShippingInstructions。 ", invoiceNo);
            return Collections.emptyList();
        }

        return dtoList.stream()
                .map(dto -> FeiliksShippingInstruction.builder()
                        .qcparam(toEmpty(dto.getShipNotes()))
                        .qcType(toEmpty(dto.getQcType()))
                        .build())
                .collect(Collectors.toList());
    }

    private String generateSign(String dataJson, String secretKey) {
        try {
            String stringToSign = dataJson + secretKey;
            MessageDigest md = MessageDigest.getInstance("MD5");
            
            // 1. 取得 MD5 的原始位元組 (raw bytes)
            byte[] digest = md.digest(stringToSign.getBytes(StandardCharsets.UTF_8));

            // 2. 將原始位元組轉換為 32 字元的 16 進位字串 (hex string)
            // 這段邏輯與您提供的 sample code 中的 for 迴圈功能完全相同
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                hexString.append(String.format("%02x", b));
            }
            
            // 3. 將這個 16 進位字串進行 Base64 編碼
            return Base64.getEncoder().encodeToString(hexString.toString().getBytes(StandardCharsets.UTF_8));

        } catch (NoSuchAlgorithmException e) {
            log.error("MD5 演算法不存在!", e);
            throw new RuntimeException("MD5 not available", e);
        }
    }

    private String toEmpty(String s) {
        return s == null ? "" : s;
    }
} 