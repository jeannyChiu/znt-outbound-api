-- 樣品出貨資料準備
-- 帳本來源: ZSH/SCH
-- 樣品(出)需要取增你強上海/宏衢，基通倉出非基通倉入，只須拋一筆負數量

BEGIN
    DELETE ZEN_B2B_JSON_SO_TMP;
EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_LANGUAGE= ''AMERICAN''';

INSERT INTO ZEN_B2B_JSON_SO_TMP (
    SEQ_ID, SENDER_CODE, RECEIVER_CODE, STATUS, DIRECTION, DOC_DATETIME,
    LAST_UPDATE_DATE, CREATION_DATE, USER_NO, ID, ORDER_NO, INV_SPLIT,
    INVOICE_NO, INVOICE_DATE, EXP_SHIP_DATE, CUST_NO, DEPT, CUST_NAME,
    INVOICE_ADDRESS, SHIP_TO_NO, DELIVERY_ADDRESS, SHIP_TO_CONTACT,
    SHIP_TO_PHONE, ZT_PART_NO, CUST_PART_NO, CUSTOMER_PO, SHIP_NOTICE,
    SHIP_NOTES, STATUS1, BRAND, QUANTITY, UNIT_PRICE, AMOUNT, CUST_PO,
    CUST_PO2, CUST_POLINE, CUST_PN, CUST_PN2, ITEM_DESC, ITEM_MODE,
    ORDER_LINE_MEMO, UNIT_WEIGHT, WEIGHT_UOM_CODE, FROM_SUBINVENTORY_CODE,
    SEGMENT1, SEGMENT2, KIND, SUPPLIER_PARTNO, INV_AND_PAC, CUST_PART_NO2,
    PO_REMARK, RMA_NUMBER, ORD_TYPE, QC_TYPE
)
SELECT
    ZEN_B2B_JSON_SEQ.nextval, SENDER_CODE, RECEIVER_CODE, STATUS, DIRECTION,
    DOC_DATETIME, LAST_UPDATE_DATE, CREATION_DATE, USER_NO, ID, ORDER_NO,
    INV_SPLIT, INVOICE_NO, INVOICE_DATE, EXP_SHIP_DATE, CUST_NO, DEPT,
    CUST_NAME, INVOICE_ADDRESS, SHIP_TO_NO, DELIVERY_ADDRESS, SHIP_TO_CONTACT,
    SHIP_TO_PHONE, ZT_PART_NO, CUST_PART_NO, CUSTOMER_PO, SHIP_NOTICE,
    SHIP_NOTES, STATUS1, BRAND, QUANTITY, UNIT_PRICE, AMOUNT, CUST_PO,
    CUST_PO2, CUST_POLINE, CUST_PN, CUST_PN2, ITEM_DESC, ITEM_MODE,
    ORDER_LINE_MEMO, UNIT_WEIGHT, WEIGHT_UOM_CODE, FROM_SUBINVENTORY_CODE,
    SEGMENT1, SEGMENT2, KIND, SUPPLIER_PARTNO, INV_AND_PAC, CUST_PART_NO2,
    PO_REMARK, RMA_NUMBER, ORD_TYPE, QC_TYPE
FROM (
         SELECT
             'ZEN' AS SENDER_CODE,
             'JIT' AS RECEIVER_CODE,
             CASE
                 WHEN mmt.transaction_source_name IS NULL
                     OR mmt.transaction_date IS NULL
                     OR a.DEP_NAME IS NULL
                     OR b.ENAME IS NULL
                     OR b.CADDR IS NULL
                     OR c.USER_NAME IS NULL
                     OR c.EXT_NO IS NULL
                     OR c.PARTNO IS NULL
                     OR c.BRAND IS NULL
                     OR c.QTY IS NULL
                     OR msi.ATTRIBUTE1 IS NULL
                     OR d.SUBINVCODE IS NULL
                     OR d.LOCATOR IS NULL
                     THEN 'N'
                 ELSE 'W'
                 END AS STATUS,
             'OUT' AS DIRECTION,
             TO_CHAR(mmt.transaction_date, 'yyyy-MM-dd HH:mm:ss') AS DOC_DATETIME,
             TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS LAST_UPDATE_DATE,
             TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS CREATION_DATE,
             'admin' AS USER_NO,
             d.SEQ_MISFORM || '-' || d.SEQS || '-' || d.NUMS AS ID,
             '' AS ORDER_NO,
             '' AS INV_SPLIT,
             mmt.transaction_source_name AS INVOICE_NO,
             TO_CHAR(TRUNC(mmt.transaction_date), 'yyyy/mm/dd') AS INVOICE_DATE,
             TO_CHAR(TRUNC(mmt.transaction_date) + 1, 'yyyy/mm/dd') AS EXP_SHIP_DATE,
             '' AS CUST_NO,
             a.DEP_NAME AS DEPT,
             b.ENAME AS CUST_NAME,
             b.CADDR AS INVOICE_ADDRESS,
             '' AS SHIP_TO_NO,
             b.CADDR AS DELIVERY_ADDRESS,
             c.USER_NAME AS SHIP_TO_CONTACT,
             c.EXT_NO AS SHIP_TO_PHONE,
             c.PARTNO AS ZT_PART_NO,
             '' AS CUST_PART_NO,
             '' AS CUSTOMER_PO,
             '' AS SHIP_NOTICE,
             '' AS SHIP_NOTES,
             'SG' AS STATUS1,
             c.BRAND AS BRAND,
             c.QTY AS QUANTITY,
             NULL AS UNIT_PRICE,
             NULL AS AMOUNT,
             '' AS CUST_PO,
             '' AS CUST_PO2,
             '' AS CUST_POLINE,
             '' AS CUST_PN,
             '' AS CUST_PN2,
             '' AS ITEM_DESC,
             msi.ATTRIBUTE1 AS ITEM_MODE,
             '' AS ORDER_LINE_MEMO,
             '' AS UNIT_WEIGHT,
             '' AS WEIGHT_UOM_CODE,
             d.SUBINVCODE AS FROM_SUBINVENTORY_CODE,
             SUBSTR(d.LOCATOR, 1, INSTR(d.LOCATOR, '基通') + LENGTH('基通') - 1) AS SEGMENT1,
             SUBSTR(d.LOCATOR, INSTR(d.LOCATOR, '基通') + LENGTH('基通')) AS SEGMENT2,
             '樣品單' AS KIND,
             msi.segment1 AS SUPPLIER_PARTNO,
             '' AS INV_AND_PAC,
             '' AS CUST_PART_NO2,
             NVL(NVL(c.VENDOR_MATERIAL_INFO, ZEN_GET_WMS_ITEM_F(c.PARTNO,mmt.organization_id)), c.PARTNO) AS PO_REMARK, -- 原廠來料訊息
             '' AS RMA_NUMBER,
             '0' AS ORD_TYPE,
             'TY' AS QC_TYPE
         FROM
             OA_DEPTALL@hr a,
             OA_COMPANY@hr b,
             zenoanew.WEB015_SAMPLE_LINE@ZENOADBT1ZENWMSHR c,
             zenoanew.WEB015_SAMPLE_LINED1@ZENOADBT1ZENWMSHR d,
             mtl_material_transactions@PROD2 mmt,
             mtl_system_items_b@PROD2 msi
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
             FROM zenoanew.WEB015_SAMPLE_LINED1@ZENOADBT1ZENWMSHR
             WHERE
                 SEQ_MISFORM = c.SEQ_MISFORM
               AND SEQS = c.SEQS
               AND NUMS = c.NUMS
               AND SUBINVCODE = '外存倉'
               AND LOCATOR LIKE '%基通%'
         )
           AND EXISTS (
             SELECT 1
             FROM zenoanew.WEB015_SAMPLE_LINE_FLOWC@ZENOADBT1ZENWMSHR
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
           AND mmt.organization_id IN (169, 209) -- ZSH, SCH
           AND TRUNC(mmt.transaction_date) > TO_DATE('20250801', 'yyyymmdd')
           AND TRUNC(mmt.transaction_date) = TRUNC(SYSDATE)
           AND NOT EXISTS (
             SELECT 1
             FROM ZEN_B2B_JSON_SO
             WHERE
                 ID = d.SEQ_MISFORM || '-' || d.SEQS || '-' || d.NUMS
               AND (STATUS =
                    (CASE
                         WHEN mmt.transaction_source_name IS NULL
                             OR mmt.transaction_date IS NULL
                             OR a.DEP_NAME IS NULL
                             OR b.ENAME IS NULL
                             OR b.CADDR IS NULL
                             OR c.USER_NAME IS NULL
                             OR c.EXT_NO IS NULL
                             OR c.PARTNO IS NULL
                             OR c.BRAND IS NULL
                             OR c.QTY IS NULL
                             OR msi.ATTRIBUTE1 IS NULL
                             OR d.SUBINVCODE IS NULL
                             OR d.LOCATOR IS NULL
                             THEN 'N'
                         ELSE 'W'
                        END) OR STATUS = 'S')
         )
         ORDER BY
             c.SEQ_MISFORM, ID
     );
END;
