begin
EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_LANGUAGE= ''AMERICAN''';
insert into ZEN_B2B_JSON_SO_TMP (SEQ_ID,SENDER_CODE,RECEIVER_CODE,STATUS,DIRECTION,DOC_DATETIME,LAST_UPDATE_DATE,CREATION_DATE,USER_NO,ID,ORDER_NO,INV_SPLIT,INVOICE_NO,INVOICE_DATE,EXP_SHIP_DATE,CUST_NO,DEPT,CUST_NAME,INVOICE_ADDRESS,SHIP_TO_NO,DELIVERY_ADDRESS,SHIP_TO_CONTACT,SHIP_TO_PHONE,ZT_PART_NO,CUST_PART_NO,CUSTOMER_PO,SHIP_NOTICE,SHIP_NOTES,STATUS1,BRAND,QUANTITY,UNIT_PRICE,AMOUNT,CUST_PO,CUST_PO2,CUST_POLINE,CUST_PN,CUST_PN2,ITEM_DESC,ITEM_MODE,ORDER_LINE_MEMO,UNIT_WEIGHT,WEIGHT_UOM_CODE,FROM_SUBINVENTORY_CODE,SEGMENT1,SEGMENT2,KIND,SUPPLIER_PARTNO,INV_AND_PAC,CUST_PART_NO2,PO_REMARK,RMA_NUMBER,ORD_TYPE,QC_TYPE) 
select ZEN_B2B_JSON_SEQ.nextval,SENDER_CODE,RECEIVER_CODE,STATUS,DIRECTION,DOC_DATETIME,LAST_UPDATE_DATE,CREATION_DATE,USER_NO,ID,ORDER_NO,INV_SPLIT,INVOICE_NO,INVOICE_DATE,EXP_SHIP_DATE,CUST_NO,DEPT,CUST_NAME,INVOICE_ADDRESS,SHIP_TO_NO,DELIVERY_ADDRESS,SHIP_TO_CONTACT,SHIP_TO_PHONE,ZT_PART_NO,CUST_PART_NO,CUSTOMER_PO,SHIP_NOTICE,SHIP_NOTES,STATUS1,BRAND,QUANTITY,UNIT_PRICE,AMOUNT,CUST_PO,CUST_PO2,CUST_POLINE,CUST_PN,CUST_PN2,ITEM_DESC,ITEM_MODE,ORDER_LINE_MEMO,UNIT_WEIGHT,WEIGHT_UOM_CODE,FROM_SUBINVENTORY_CODE,SEGMENT1,SEGMENT2,KIND,SUPPLIER_PARTNO,INV_AND_PAC,CUST_PART_NO2,PO_REMARK,RMA_NUMBER,ORD_TYPE,QC_TYPE
from (
SELECT 
     'ZEN' as SENDER_CODE,
     'FEILIKS' as RECEIVER_CODE,
     CASE 
        WHEN mmt.transaction_source_name IS NULL -- INVOICE_NO
        OR mmt.transaction_date IS NULL -- INVOICE_DATE / EXP_SHIP_DATE
        OR a.DEP_NAME IS NULL -- DEPT
        OR b.ENAME IS NULL -- CUST_NAME
        OR b.CADDR IS NULL  -- INVOICE_ADDRESS / DELIVERY_ADDRESS
        OR c.USER_NAME IS NULL -- SHIP_TO_CONTACT
        OR c.EXT_NO IS NULL -- SHIP_TO_PHONE
        OR c.PARTNO IS NULL -- ZT_PART_NO
        OR c.BRAND IS NULL -- BRAND
        OR c.QTY IS NULL -- QUANTITY
        OR c.PRICE IS NULL -- UNIT_PRICE
        OR c.PRICE_TOTAL IS NULL -- AMOUNT
        OR msi.ATTRIBUTE1 IS NULL -- ITEM_MODE
        OR d.SUBINVCODE IS NULL -- FROM_SUBINVENTORY_CODE
        OR d.LOCATOR IS NULL -- SEGMENT1 / SEGMENT2
        THEN 'N'
        ELSE 'W'
     END as STATUS,
     'OUT' as DIRECTION,
     TO_CHAR(mmt.transaction_date, 'yyyy-MM-dd HH:mm:ss') as DOC_DATETIME,
     TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as LAST_UPDATE_DATE,
     TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as CREATION_DATE,
     'admin' as USER_NO,
     d.SEQ_MISFORM || '-' || d.SEQS || '-' || d.NUMS as ID,
     '' as ORDER_NO,
     '' as INV_SPLIT,
     mmt.transaction_source_name as INVOICE_NO,
     TO_CHAR(trunc(mmt.transaction_date), 'yyyy/mm/dd') as INVOICE_DATE,
     TO_CHAR(TRUNC(mmt.transaction_date) + 1, 'yyyy/mm/dd') as EXP_SHIP_DATE,
     '' as CUST_NO,
     a.DEP_NAME AS DEPT, 
     b.ENAME AS CUST_NAME, 
     b.CADDR AS INVOICE_ADDRESS, 
     '' as SHIP_TO_NO,
     b.CADDR AS DELIVERY_ADDRESS,
     c.USER_NAME AS SHIP_TO_CONTACT, 
     c.EXT_NO AS SHIP_TO_PHONE,
     c.PARTNO AS ZT_PART_NO, 
     '' as CUST_PART_NO,
     '' as CUSTOMER_PO,
     '' as SHIP_NOTICE,
     '' as SHIP_NOTES,
     'SG' AS STATUS1, 
     c.BRAND AS BRAND, 
     c.QTY AS QUANTITY, 
     c.PRICE AS UNIT_PRICE, 
     c.PRICE_TOTAL AS AMOUNT, 
     '' as CUST_PO,
     '' as CUST_PO2,
     '' as CUST_POLINE,
     '' as CUST_PN,
     '' as CUST_PN2,
     '' as ITEM_DESC,
     msi.ATTRIBUTE1 AS ITEM_MODE,
     '' as ORDER_LINE_MEMO,
     '' as UNIT_WEIGHT,
     '' as WEIGHT_UOM_CODE,
     d.SUBINVCODE AS FROM_SUBINVENTORY_CODE, 
     SUBSTR(d.LOCATOR, 1, INSTR(d.LOCATOR, '飛力達') + LENGTH('飛力達') - 1) AS SEGMENT1,
     SUBSTR(d.LOCATOR, INSTR(d.LOCATOR, '飛力達') + LENGTH('飛力達')) AS SEGMENT2,
     '樣品單' AS KIND,
     msi.segment1 as SUPPLIER_PARTNO,
     '' as INV_AND_PAC,
     '' as CUST_PART_NO2,
     '' as PO_REMARK,
     '' as RMA_NUMBER,
     '0' as ORD_TYPE,
     'TY' as QC_TYPE 
FROM 
    OA_DEPTALL@hr a, 
    OA_COMPANY@hr b, 
    WEB015_SAMPLE_LINE@hr c, 
    WEB015_SAMPLE_LINED1@hr d, 
    mtl_material_transactions@zenprod mmt, 
    mtl_system_items_b@zenprod msi
WHERE 
    a.CPNYID = b.CPNYID
    AND a.DEP_NO = c.DEPT_NO
    AND d.SEQ_MISFORM = c.SEQ_MISFORM 
    AND d.SEQS = c.SEQS 
    AND d.NUMS = c.NUMS
    AND mmt.transaction_source_name = c.SENDS_MIXED_NO
    AND msi.inventory_item_id = mmt.inventory_item_id
    AND EXISTS (
        SELECT 1
        FROM 
            WEB015_SAMPLE_LINED1@hr 
        WHERE 
            SEQ_MISFORM = c.SEQ_MISFORM 
            AND SEQS = c.SEQS
            AND NUMS = c.NUMS
            AND SUBINVCODE = '外存倉' 
            AND LOCATOR LIKE '%飛力達%'
    )
    AND EXISTS (
        SELECT 1
        FROM 
            WEB015_SAMPLE_LINE_FLOWC@hr 
        WHERE 
            SEQ_MISFORM = c.SEQ_MISFORM 
            AND SEQS = c.SEQS
            AND NUMS = c.NUMS
            AND F_INP_STAT = 'END'
    )
    AND mmt.ORGANIZATION_ID = (
        SELECT ORGANIZATION_ID 
        FROM zenhrnew.Y17_COMPANY@hr
        WHERE CPNYID = c.OUTCPNYID
    )
    AND msi.segment1 = c.PARTNO
    AND mmt.organization_id = msi.organization_id
    --AND TRUNC(mmt.transaction_date) + 1 >= TRUNC(sysdate)  --for正式上線第一週(手動執行)
    AND TRUNC(mmt.transaction_date) + 1 BETWEEN TRUNC(sysdate) - 1 AND TRUNC(sysdate) + 1 --for正式上線一週後(自動化)
    AND NOT EXISTS(
        SELECT 1
        FROM ZEN_B2B_JSON_SO 
        WHERE 
            ID = d.SEQ_MISFORM || '-' || d.SEQS || '-' || d.NUMS
            AND (STATUS = 
                (CASE 
                    WHEN mmt.transaction_source_name IS NULL -- INVOICE_NO
                    OR mmt.transaction_date IS NULL -- INVOICE_DATE / EXP_SHIP_DATE
                    OR a.DEP_NAME IS NULL -- DEPT
                    OR b.ENAME IS NULL -- CUST_NAME
                    OR b.CADDR IS NULL  -- INVOICE_ADDRESS / DELIVERY_ADDRESS
                    OR c.USER_NAME IS NULL -- SHIP_TO_CONTACT
                    OR c.EXT_NO IS NULL -- SHIP_TO_PHONE
                    OR c.PARTNO IS NULL -- ZT_PART_NO
                    OR c.BRAND IS NULL -- BRAND
                    OR c.QTY IS NULL -- QUANTITY
                    OR c.PRICE IS NULL -- UNIT_PRICE
                    OR c.PRICE_TOTAL IS NULL -- AMOUNT
                    OR msi.ATTRIBUTE1 IS NULL -- ITEM_MODE
                    OR d.SUBINVCODE IS NULL -- FROM_SUBINVENTORY_CODE
                    OR d.LOCATOR IS NULL -- SEGMENT1 / SEGMENT2
                    THEN 'N'
                    ELSE 'W'
                END) OR STATUS = 'S')
    )
ORDER BY 
    c.SEQ_MISFORM, ID
);
end;