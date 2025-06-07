begin
EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_LANGUAGE= ''AMERICAN''';
insert into ZEN_B2B_JSON_SO_TMP (SEQ_ID,SENDER_CODE,RECEIVER_CODE,STATUS,DIRECTION,DOC_DATETIME,LAST_UPDATE_DATE,CREATION_DATE,USER_NO,ID,ORDER_NO,INV_SPLIT,INVOICE_NO,INVOICE_DATE,EXP_SHIP_DATE,CUST_NO,DEPT,CUST_NAME,INVOICE_ADDRESS,SHIP_TO_NO,DELIVERY_ADDRESS,SHIP_TO_CONTACT,SHIP_TO_PHONE,ZT_PART_NO,CUST_PART_NO,CUSTOMER_PO,SHIP_NOTICE,SHIP_NOTES,STATUS1,BRAND,QUANTITY,UNIT_PRICE,AMOUNT,CUST_PO,CUST_PO2,CUST_POLINE,CUST_PN,CUST_PN2,ITEM_DESC,ITEM_MODE,ORDER_LINE_MEMO,UNIT_WEIGHT,WEIGHT_UOM_CODE,FROM_SUBINVENTORY_CODE,SEGMENT1,SEGMENT2,KIND,SUPPLIER_PARTNO,INV_AND_PAC,CUST_PART_NO2,PO_REMARK,RMA_NUMBER,ORD_TYPE,QC_TYPE) 
select ZEN_B2B_JSON_SEQ.nextval,SENDER_CODE,RECEIVER_CODE,STATUS,DIRECTION,DOC_DATETIME,LAST_UPDATE_DATE,CREATION_DATE,USER_NO,ID,ORDER_NO,INV_SPLIT,INVOICE_NO,INVOICE_DATE,EXP_SHIP_DATE,CUST_NO,DEPT,CUST_NAME,INVOICE_ADDRESS,SHIP_TO_NO,DELIVERY_ADDRESS,SHIP_TO_CONTACT,SHIP_TO_PHONE,ZT_PART_NO,CUST_PART_NO,CUSTOMER_PO,SHIP_NOTICE,SHIP_NOTES,STATUS1,BRAND,QUANTITY,UNIT_PRICE,AMOUNT,CUST_PO,CUST_PO2,CUST_POLINE,CUST_PN,CUST_PN2,ITEM_DESC,ITEM_MODE,ORDER_LINE_MEMO,UNIT_WEIGHT,WEIGHT_UOM_CODE,FROM_SUBINVENTORY_CODE,SEGMENT1,SEGMENT2,KIND,SUPPLIER_PARTNO,INV_AND_PAC,CUST_PART_NO2,PO_REMARK,RMA_NUMBER,ORD_TYPE,QC_TYPE
from (
     SELECT 
     'ZEN' as SENDER_CODE,
     'FEILIKS' as RECEIVER_CODE,
      CASE 
        WHEN mtrh.request_number IS NULL -- ORDER_NO
        OR ZPIH.invoice_no IS NULL -- INVOICE_NO
        OR ZPIH.invoice_date IS NULL -- INVOICE_DATE
        OR mtrh.attribute2 IS NULL -- EXP_SHIP_DATE
        OR (SELECT ffvt.description
            FROM HZ_CUST_SITE_USES_ALL@zenprod rsua,
                jtf_rs_salesreps@zenprod jrs,
                gl_code_combinations@zenprod gcc,
                fnd_id_flex_segments@zenprod fifs,
                fnd_flex_values@zenprod ffv,
                fnd_flex_values_tl@zenprod ffvt
            WHERE     rsua.site_use_code = 'SHIP_TO'
                AND rsua.site_use_id = rsua_s.site_use_id
                AND jrs.salesrep_id = rsua.primary_salesrep_id
                AND gcc.code_combination_id = jrs.gl_id_rev
                AND fifs.id_flex_num = gcc.chart_of_accounts_id
                AND fifs.segment_name = '部門別'
                AND ffv.flex_value_set_id = fifs.flex_value_set_id
                AND ffv.flex_value = gcc.segment2
                AND ffvt.flex_value_id = ffv.flex_value_id
                AND fifs.id_flex_code = 'GL#') IS NULL -- DEPT
        OR rc.customer_name IS NULL -- CUST_NAME
        OR raa_b.address1 IS NULL  -- INVOICE_ADDRESS
        OR raa_s.site_number IS NULL  -- SHIP_TO_NO
        OR CASE
            WHEN zlv.customer_number IN ('38672', '6573') THEN raa_o.address1
            ELSE NVL (mtrh.attribute11, raa_s.address1)
           END IS NULL -- DELIVERY_ADDRESS
        OR msib.segment1 IS NULL  -- ZT_PART_NO
        OR (SELECT mcb.segment1
            FROM mtl_item_categories@zenprod mic, mtl_categories_b@zenprod mcb
            WHERE mic.category_set_id = 1
            AND mic.category_id = mcb.category_id
            AND mic.organization_id = msib.organization_id
            AND mic.inventory_item_id = msib.inventory_item_id) IS NULL  -- BRAND
        OR mtrl.quantity IS NULL  -- QUANTITY
        OR l.UNIT_SELLING_PRICE IS NULL  -- UNIT_PRICE
        OR mtrl.quantity * l.UNIT_SELLING_PRICE IS NULL  -- AMOUNT
        OR msib.attribute1 IS NULL -- ITEM_MODE
        OR mtrl.FROM_SUBINVENTORY_CODE IS NULL -- FROM_SUBINVENTORY_CODE
        OR mil2.segment1 IS NULL -- SEGMENT1 
        OR mil2.segment2 IS NULL -- SEGMENT2 
        THEN 'N'
        ELSE 'W'
      END as STATUS,
     'OUT' as DIRECTION,
     TO_CHAR(ZPIH.invoice_date, 'yyyy-MM-dd HH:mm:ss') as DOC_DATETIME,
     TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as LAST_UPDATE_DATE,
     TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as CREATION_DATE,
     'admin' as USER_NO,
     ZPIH.invoice_no || '-' || mtrl.line_id as ID,
     mtrh.request_number as ORDER_NO,
     mtrh.ATTRIBUTE7 as INV_SPLIT,
     ZPIH.invoice_no as INVOICE_NO,
     TO_CHAR(TRUNC(ZPIH.invoice_date), 'yyyy/mm/dd') as INVOICE_DATE,
     TO_CHAR(TO_DATE(mtrh.attribute2, 'DD-MON-YYYY', 'NLS_DATE_LANGUAGE=AMERICAN'), 'yyyy/mm/dd') as EXP_SHIP_DATE,
     zlv.customer_number as CUST_NO,
     (SELECT ffvt.description
        FROM HZ_CUST_SITE_USES_ALL@zenprod rsua,
            jtf_rs_salesreps@zenprod jrs,
            gl_code_combinations@zenprod gcc,
            fnd_id_flex_segments@zenprod fifs,
            fnd_flex_values@zenprod ffv,
            fnd_flex_values_tl@zenprod ffvt
        WHERE     rsua.site_use_code = 'SHIP_TO'
            AND rsua.site_use_id = rsua_s.site_use_id
            AND jrs.salesrep_id = rsua.primary_salesrep_id
            AND gcc.code_combination_id = jrs.gl_id_rev
            AND fifs.id_flex_num = gcc.chart_of_accounts_id
            AND fifs.segment_name = '部門別'
            AND ffv.flex_value_set_id = fifs.flex_value_set_id
            AND ffv.flex_value = gcc.segment2
            AND ffvt.flex_value_id = ffv.flex_value_id
            AND fifs.id_flex_code = 'GL#')
        as DEPT,
     rc.customer_name as CUST_NAME,
     raa_b.address1 as INVOICE_ADDRESS,
     raa_s.site_number as SHIP_TO_NO,
     CASE
        WHEN zlv.customer_number IN ('38672', '6573') THEN raa_o.address1
        ELSE NVL (mtrh.attribute11, raa_s.address1)
     END
     as DELIVERY_ADDRESS,
     (SELECT MAX (rcs.first_name || rcs.last_name)
     FROM ZEN_RA_CONTACTS_V@zenprod rcs
     WHERE     rcs.status = 'A'
        AND rcs.CUSTOMER_ID = rc.customer_id
        AND rcs.ADDRESS_ID =
        (CASE
            WHEN zlv.customer_number IN ('38672', '6573')
            THEN
                TO_CHAR (raa_o.address_id)
            ELSE
                zlv.ship_address_id
        END))
     as SHIP_TO_CONTACT,
     (SELECT MAX (
            DECODE (A.PHONE_AREA_CODE,
                    NULL, A.PHONE_NUMBER,
                    A.PHONE_AREA_CODE || '-' || A.PHONE_NUMBER))
     FROM ZEN_RA_PHONES_V@zenprod A
     WHERE     A.PHONE_LINE_TYPE = 'GEN'
        AND A.CUSTOMER_ID = rc.customer_id
        AND A.PHONE_STATUS = 'A'
        AND A.ADDRESS_ID =
        (CASE
            WHEN zlv.customer_number IN ('38672', '6573')
            THEN
                TO_CHAR (raa_o.address_id)
            ELSE
                zlv.ship_address_id
        END))
     as SHIP_TO_PHONE,
     msib.segment1 as ZT_PART_NO,
     l.attribute3 as CUST_PART_NO,
     l.attribute2 || DECODE (l.attribute1, NULL, NULL, '-' || l.attribute1) as CUSTOMER_PO,
     (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute1)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute2)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute3)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute4)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute5)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute6)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute7)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute8)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute9)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute10)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute11)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute14)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute15)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute16)
     || ' '
     || (SELECT value_description
        FROM zen_fnd_flex_values_v@zenprod
        WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
            AND flex_value = raa_s.attribute17)
     || ' '
     || raa_s.attribute12
     as SHIP_NOTICE,
     mtrh.attribute3 as SHIP_NOTES,
     NVL (mtrl.attribute5, 'RR') as STATUS1,
     (SELECT mcb.segment1
     FROM mtl_item_categories@zenprod mic, mtl_categories_b@zenprod mcb
     WHERE     mic.category_set_id = 1
            AND mic.category_id = mcb.category_id
            AND mic.organization_id = msib.organization_id
            AND mic.inventory_item_id = msib.inventory_item_id)
     as BRAND,
     mtrl.quantity as QUANTITY,
     TO_CHAR(l.UNIT_SELLING_PRICE,'FM9999999999999990.99999999999999999999') as UNIT_PRICE,
     mtrl.quantity * l.UNIT_SELLING_PRICE as AMOUNT,
     l.ATTRIBUTE2 as CUST_PO,
     l.attribute6 as CUST_PO2,
     l.ATTRIBUTE1 as CUST_POLINE,
     l.ATTRIBUTE3 as CUST_PN,
     l.ATTRIBUTE5 as CUST_PN2,
     MCI.CUSTOMER_ITEM_DESC as ITEM_DESC,
     msib.attribute1 as ITEM_MODE,
     l.attribute8 as ORDER_LINE_MEMO,
     TO_CHAR(msib.UNIT_WEIGHT,'FM9999999999999990.99999999999999999999') as UNIT_WEIGHT,
     msib.WEIGHT_UOM_CODE as WEIGHT_UOM_CODE,
     mtrl.FROM_SUBINVENTORY_CODE as FROM_SUBINVENTORY_CODE,
     mil2.segment1 as SEGMENT1,
     mil2.segment2 as SEGMENT2,
     '調出單' as KIND,
     CASE
        WHEN (SELECT msi.attribute9
                FROM mtl_system_items@zenprod msi
             WHERE     msi.organization_id = 3
                    AND msi.inventory_item_id = msib.inventory_item_id)
             IS NOT NULL
        THEN
            (SELECT msi.attribute9
            FROM mtl_system_items@zenprod msi
            WHERE     msi.organization_id = 3
                AND msi.inventory_item_id = msib.inventory_item_id)
        ELSE
            msib.attribute9
     END
     as SUPPLIER_PARTNO,
     MCI.ATTRIBUTE1 as INV_AND_PAC,
     MCI.ATTRIBUTE2 as CUST_PART_NO2,
     h.ATTRIBUTE3 as PO_REMARK,
     '' as RMA_NUMBER,
     '0' as ORD_TYPE,
     'TY' as QC_TYPE 
     FROM mtl_txn_request_headers@zenprod mtrh,
         mtl_txn_request_lines@zenprod mtrl,
         mtl_item_locations@zenprod mil,
         mtl_item_locations@zenprod mil2,
         mtl_system_items_b@zenprod msib,
         zen_hub_cust_v@zenprod zlv,
         HZ_CUST_SITE_USES_ALL@zenprod rsua_b,
         HZ_CUST_SITE_USES_ALL@zenprod rsua_s,
         ZEN_RA_ADDRESSES_V@zenprod raa_b,
         ZEN_RA_ADDRESSES_V@zenprod raa_s,
         ZEN_RA_CUSTOMERS_V@zenprod rc,
         oe_order_headers_all@zenprod h,
         oe_order_lines_all@zenprod l,
         HZ_CUST_SITE_USES_ALL@zenprod rsua_o,
         ZEN_RA_ADDRESSES_V@zenprod raa_o,
         MTL_CUSTOMER_ITEMS@zenprod MCI,
         ZEN_PACKING_INVOICE_HEADER@zenprod ZPIH,
         ZEN_PACKING_INVOICE_LINE@zenprod ZPIL,
         mtl_material_transactions@zenprod mmt
    WHERE     mtrh.header_id >= 3567503
         AND mtrl.line_status <> 6
         AND mtrl.transaction_type_id IN (100, 169)
         AND mtrh.header_id = mtrl.header_id
         AND DECODE (mtrl.transaction_type_id,
                     169, NVL (mtrl.from_locator_id, mtrl.to_locator_id),
                     NVL (mtrl.to_locator_id, mtrl.from_locator_id)) =
                mil.inventory_location_id(+)
         AND mtrl.inventory_item_id = msib.inventory_item_id
         AND msib.organization_id = mtrh.organization_id
         AND mmt.organization_id(+) = mtrh.organization_id
         AND mmt.TRANSACTION_TYPE_ID(+) = mtrh.TRANSACTION_TYPE_ID
         AND mil2.organization_id(+) = mtrl.organization_id
         AND mil2.subinventory_code(+) = mtrl.FROM_SUBINVENTORY_CODE
         AND mil2.inventory_location_id(+) = mtrl.FROM_LOCATOR_ID
         AND mmt.source_line_id(+) = mtrl.line_id
         AND mmt.TRANSACTION_QUANTITY(+) < 0
         AND mil.inventory_location_id = zlv.inventory_location_id
         AND zlv.bill_address_id = raa_b.address_id
         AND zlv.ship_address_id = raa_s.address_id
         AND raa_b.address_id = rsua_b.CUST_ACCT_SITE_ID
         AND raa_s.address_id = rsua_s.CUST_ACCT_SITE_ID
         AND rsua_b.site_use_code = 'BILL_TO'
         AND rsua_s.site_use_code = 'SHIP_TO'
         AND zlv.customer_number = rc.customer_number
         AND SUBSTR (mtrl.attribute1,
                     1,
                       INSTR (mtrl.attribute1,
                              '-',
                              1,
                              1)
                     - 1) = h.order_number
         AND SUBSTR (mtrl.attribute1,
                       INSTR (mtrl.attribute1,
                              '-',
                              1,
                              1)
                     + 1,
                       INSTR (mtrl.attribute1,
                              '-',
                              1,
                              2)
                     - (  INSTR (mtrl.attribute1,
                                 '-',
                                 1,
                                 1)
                        + 1)) = l.line_id
         AND h.header_id = l.header_id
         AND raa_b.org_id = l.org_id
         AND raa_s.org_id = l.org_id
         AND raa_o.org_id = l.org_id
         AND rsua_b.org_id = l.org_id
         AND rsua_s.org_id = l.org_id
         AND rsua_o.org_id = l.org_id
         AND raa_o.address_id = rsua_o.CUST_ACCT_SITE_ID
         AND rsua_o.site_use_id = l.ship_to_org_id
         AND l.sold_to_org_id = MCI.customer_id(+)
         AND l.attribute3 = MCI.customer_item_number(+)
         AND mtrh.REQUEST_NUMBER = ZPIL.DELIVERY_NAME
         AND mtrh.organization_id = ZPIH.organization_id
         AND ZPIL.invoice_id = ZPIH.invoice_id
         AND ZPIL.organization_id = ZPIH.organization_id
         AND ZPIL.INVOICE_SOURCE = 'MOVEORDER'
         AND mtrl.FROM_SUBINVENTORY_CODE = '外存倉'
         AND (mil2.segment1 like '%飛力達%' OR mil2.segment2 like '%飛力達%')
         --AND TO_DATE(mtrh.attribute2,'DD-MON-YYYY','NLS_DATE_LANGUAGE=AMERICAN') >= TRUNC(sysdate) --for正式上線第一週(手動執行)
         AND TO_DATE(mtrh.attribute2,'DD-MON-YYYY','NLS_DATE_LANGUAGE=AMERICAN') BETWEEN TRUNC(sysdate) - 1 AND TRUNC(sysdate) + 1 --for正式上線一週後(自動化)
--ORDER BY mtrl.line_number, msib.segment1
ORDER BY ORDER_NO, ID
);

DELETE FROM ZEN_B2B_JSON_SO_TMP tmp
WHERE (
    EXISTS (
        SELECT 1
        FROM ZEN_B2B_JSON_SO so
        WHERE so.ID = tmp.ID
        AND (so.STATUS = tmp.STATUS OR so.STATUS = 'S')
    )
    OR NOT EXISTS (
        SELECT 1
        FROM zenwms.WMS_SHIPPING@hr WS
        WHERE WS.INVOICENO = tmp.INVOICE_NO 
    )
) AND tmp.KIND = '調出單';

end;