begin
EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_LANGUAGE= ''AMERICAN''';
insert into ZEN_B2B_JSON_SO_TMP (SEQ_ID,SENDER_CODE,RECEIVER_CODE,STATUS,DIRECTION,DOC_DATETIME,LAST_UPDATE_DATE,CREATION_DATE,USER_NO,ID,ORDER_NO,INV_SPLIT,INVOICE_NO,INVOICE_DATE,EXP_SHIP_DATE,CUST_NO,DEPT,CUST_NAME,INVOICE_ADDRESS,SHIP_TO_NO,DELIVERY_ADDRESS,SHIP_TO_CONTACT,SHIP_TO_PHONE,ZT_PART_NO,CUST_PART_NO,CUSTOMER_PO,SHIP_NOTICE,SHIP_NOTES,STATUS1,BRAND,QUANTITY,UNIT_PRICE,AMOUNT,CUST_PO,CUST_PO2,CUST_POLINE,CUST_PN,CUST_PN2,ITEM_DESC,ITEM_MODE,ORDER_LINE_MEMO,UNIT_WEIGHT,WEIGHT_UOM_CODE,FROM_SUBINVENTORY_CODE,SEGMENT1,SEGMENT2,KIND,SUPPLIER_PARTNO,INV_AND_PAC,CUST_PART_NO2,PO_REMARK,RMA_NUMBER,ORD_TYPE,QC_TYPE) 
select ZEN_B2B_JSON_SEQ.nextval,SENDER_CODE,RECEIVER_CODE,STATUS,DIRECTION,DOC_DATETIME,LAST_UPDATE_DATE,CREATION_DATE,USER_NO,ID,ORDER_NO,INV_SPLIT,INVOICE_NO,INVOICE_DATE,EXP_SHIP_DATE,CUST_NO,DEPT,CUST_NAME,INVOICE_ADDRESS,SHIP_TO_NO,DELIVERY_ADDRESS,SHIP_TO_CONTACT,SHIP_TO_PHONE,ZT_PART_NO,CUST_PART_NO,CUSTOMER_PO,SHIP_NOTICE,SHIP_NOTES,STATUS1,BRAND,QUANTITY,UNIT_PRICE,AMOUNT,CUST_PO,CUST_PO2,CUST_POLINE,CUST_PN,CUST_PN2,ITEM_DESC,ITEM_MODE,ORDER_LINE_MEMO,UNIT_WEIGHT,WEIGHT_UOM_CODE,FROM_SUBINVENTORY_CODE,SEGMENT1,SEGMENT2,KIND,SUPPLIER_PARTNO,INV_AND_PAC,CUST_PART_NO2,PO_REMARK,RMA_NUMBER,ORD_TYPE,QC_TYPE
from (
     SELECT 
     'ZEN' as SENDER_CODE,
     'FEILIKS' as RECEIVER_CODE,
      CASE 
        WHEN d.name IS NULL -- ORDER_NO
        OR O.invoice_no IS NULL -- INVOICE_NO
        OR O.invoice_date IS NULL -- INVOICE_DATE
        OR d.attribute12 IS NULL -- EXP_SHIP_DATE
        OR (SELECT ffvt.description
            FROM HZ_CUST_SITE_USES_ALL@zenprod rsua,
                 jtf_rs_salesreps@zenprod jrs,
                 gl_code_combinations@zenprod gcc,
                 fnd_id_flex_segments@zenprod fifs,
                 fnd_flex_values@zenprod ffv,
                 fnd_flex_values_tl@zenprod ffvt
           WHERE     rsua.site_use_code = 'SHIP_TO'
                 AND rsua.site_use_id = ship_su.site_use_id
                 AND jrs.salesrep_id = rsua.primary_salesrep_id
                 AND gcc.code_combination_id = jrs.gl_id_rev
                 AND fifs.id_flex_num = gcc.chart_of_accounts_id
                 AND fifs.segment_name = '部門別'
                 AND ffv.flex_value_set_id = fifs.flex_value_set_id
                 AND ffv.flex_value = gcc.segment2
                 AND ffvt.flex_value_id = ffv.flex_value_id
                 AND fifs.id_flex_code = 'GL#') IS NULL -- DEPT
        OR party.party_name IS NULL -- CUST_NAME
        OR bill_loc.address1 IS NULL  -- INVOICE_ADDRESS
        OR ship_ps.party_site_number IS NULL  -- SHIP_TO_NO
        OR NVL (d.attribute8, ship_loc.address1) IS NULL -- DELIVERY_ADDRESS
        OR mtls.segment1
         || DECODE (l.attribute8, NULL, NULL, ' #' || l.attribute8 || '#')
         || DECODE (mtls.attribute6, 'Y', '(' || mtls.description || ')', NULL) IS NULL -- ZT_PART_NO
        OR (SELECT mcb.segment1
            FROM mtl_item_categories@zenprod mic, mtl_categories_b@zenprod mcb
           WHERE     mic.category_set_id = 1
                 AND mic.category_id = mcb.category_id
                 AND mic.organization_id = mtls.organization_id
                 AND mic.inventory_item_id = mtls.inventory_item_id) IS NULL -- BRAND
        OR  DECODE (mtls.segment1,
                 '銷貨折讓', wdd.requested_quantity * -1,
                 wdd.requested_quantity) IS NULL -- QUANTITY
        OR l.UNIT_SELLING_PRICE IS NULL -- UNIT_PRICE
        OR DECODE (mtls.segment1,
                   '銷貨折讓', wdd.requested_quantity * -1,
                   wdd.requested_quantity)
         * l.UNIT_SELLING_PRICE IS NULL -- AMOUNT
        OR mtls.attribute1 IS NULL -- ITEM_MODE
        OR zs.FROM_SUBINVENTORY_CODE IS NULL -- FROM_SUBINVENTORY_CODE
        OR mil.segment1 IS NULL -- SEGMENT1 
        OR mil.segment2 IS NULL -- SEGMENT2
        THEN 'N'
        ELSE 'W'
      END as STATUS,
     'OUT' as DIRECTION,
     TO_CHAR(O.invoice_date, 'yyyy-MM-dd HH:mm:ss') as DOC_DATETIME,
     TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as LAST_UPDATE_DATE,
     TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as CREATION_DATE,
     'admin' as USER_NO,
     O.invoice_no || '-' || l.line_id as ID,
     d.name as ORDER_NO,
     d.ATTRIBUTE5 as INV_SPLIT,
     O.invoice_no as INVOICE_NO,
     TO_CHAR(TRUNC(O.invoice_date), 'yyyy/mm/dd') as INVOICE_DATE,
     TO_CHAR(TO_DATE(d.attribute12, 'DD-MON-YYYY', 'NLS_DATE_LANGUAGE=AMERICAN'), 'yyyy/mm/dd') as EXP_SHIP_DATE,
     cust_acct.account_number as CUST_NO,
      (SELECT ffvt.description
            FROM HZ_CUST_SITE_USES_ALL@zenprod rsua,
                 jtf_rs_salesreps@zenprod jrs,
                 gl_code_combinations@zenprod gcc,
                 fnd_id_flex_segments@zenprod fifs,
                 fnd_flex_values@zenprod ffv,
                 fnd_flex_values_tl@zenprod ffvt
           WHERE     rsua.site_use_code = 'SHIP_TO'
                 AND rsua.site_use_id = ship_su.site_use_id
                 AND jrs.salesrep_id = rsua.primary_salesrep_id
                 AND gcc.code_combination_id = jrs.gl_id_rev
                 AND fifs.id_flex_num = gcc.chart_of_accounts_id
                 AND fifs.segment_name = '部門別'
                 AND ffv.flex_value_set_id = fifs.flex_value_set_id
                 AND ffv.flex_value = gcc.segment2
                 AND ffvt.flex_value_id = ffv.flex_value_id
                 AND fifs.id_flex_code = 'GL#')
            as DEPT,
      party.party_name as CUST_NAME,
      bill_loc.address1 as INVOICE_ADDRESS,
      ship_ps.party_site_number as SHIP_TO_NO,
      NVL (d.attribute8, ship_loc.address1) as DELIVERY_ADDRESS,
      (SELECT first_name || last_name
            FROM ZEN_RA_CONTACTS_V@zenprod rcs
           WHERE     rcs.customer_id = l.sold_to_org_id
                 AND rcs.address_id = ship_cas.cust_acct_site_id
                 AND rcs.contact_id =
                        (SELECT MIN (contact_id)
                           FROM ZEN_RA_CONTACTS_V@zenprod rcs
                          WHERE     rcs.customer_id = l.sold_to_org_id
                                AND rcs.address_id = ship_cas.cust_acct_site_id
                                AND rcs.status = 'A'))
            as SHIP_TO_CONTACT,
      (SELECT DECODE (a.phone_area_code,
                         NULL, a.phone_number,
                         a.phone_area_code || '-' || a.phone_number)
            FROM zen_ra_phones_v@zenprod a
           WHERE     a.phone_id =
                        (  SELECT MAX (b.phone_id)
                             FROM zen_ra_phones_v@zenprod b
                            WHERE     a.customer_id = b.customer_id
                                  AND a.address_id = b.address_id
                                  AND b.phone_line_type = 'GEN'
                         GROUP BY b.customer_id, b.address_id)
                 AND a.phone_line_type = 'GEN'
                 AND a.customer_id = l.sold_to_org_id
                 AND a.address_id = ship_cas.cust_acct_site_id)
            as SHIP_TO_PHONE,
      mtls.segment1
         || DECODE (l.attribute8, NULL, NULL, ' #' || l.attribute8 || '#')
         || DECODE (mtls.attribute6, 'Y', '(' || mtls.description || ')', NULL)
            as ZT_PART_NO,
      NVL (l.attribute3, l.attribute5) as CUST_PART_NO,
      l.attribute2 || DECODE (l.attribute1, NULL, NULL, '-' || l.attribute1)
            as CUSTOMER_PO,
      (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute1)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute2)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute3)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute4)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute5)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute6)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute7)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute8)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute9)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute10)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute11)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute14)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute15)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute16)
         || ' '
         || (SELECT value_description
               FROM zen_fnd_flex_values_v@zenprod
              WHERE     flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute17)
         || ' '
         || CASE
               WHEN (NVL (d.ATTRIBUTE6, ship_su.ATTRIBUTE22)) IS NOT NULL
               THEN
                     'Shipping Mark:'
                  || NVL (d.ATTRIBUTE6, ship_su.ATTRIBUTE22)
                  || ' '
               ELSE
                  ' '
            END
         || ship_cas.attribute12
            as SHIP_NOTICE,
      d.attribute4 as SHIP_NOTES,
      DECODE (wdd.RELEASED_STATUS,
                 'B', 'BK',
                 'C', 'SH',
                 'D', 'CL',
                 'R', 'RR',
                 'S', 'RW',
                 'Y', 'SG',
                 'N', 'NR',
                 'X', 'NA')
            as STATUS1,
      (SELECT mcb.segment1
            FROM mtl_item_categories@zenprod mic, mtl_categories_b@zenprod mcb
           WHERE     mic.category_set_id = 1
                 AND mic.category_id = mcb.category_id
                 AND mic.organization_id = mtls.organization_id
                 AND mic.inventory_item_id = mtls.inventory_item_id)
            as BRAND,
      DECODE (mtls.segment1,
                 '銷貨折讓', wdd.requested_quantity * -1,
                 wdd.requested_quantity)
            as QUANTITY,
      TO_CHAR(l.UNIT_SELLING_PRICE,'FM9999999999999990.99999999999999999999') as UNIT_PRICE,
      DECODE (mtls.segment1,
                   '銷貨折讓', wdd.requested_quantity * -1,
                   wdd.requested_quantity)
         * l.UNIT_SELLING_PRICE
            as AMOUNT,
      l.ATTRIBUTE2 as CUST_PO,
      l.attribute6 as CUST_PO2,
      l.ATTRIBUTE1 as CUST_POLINE,
      l.ATTRIBUTE3 as CUST_PN,
      l.ATTRIBUTE5 as CUST_PN2,
      MCI.CUSTOMER_ITEM_DESC as ITEM_DESC,
      mtls.attribute1 as ITEM_MODE,
      l.attribute8 as ORDER_LINE_MEMO,
      TO_CHAR(mtls.UNIT_WEIGHT,'FM9999999999999990.99999999999999999999') as UNIT_WEIGHT,
      mtls.WEIGHT_UOM_CODE as WEIGHT_UOM_CODE,
      zs.FROM_SUBINVENTORY_CODE as FROM_SUBINVENTORY_CODE,
      mil.segment1 as SEGMENT1,
      mil.segment2 as SEGMENT2,
      '撿貨單' as KIND,
      CASE
            WHEN (SELECT msi.attribute9
                    FROM mtl_system_items@zenprod msi
                   WHERE     msi.organization_id = 3
                         AND msi.inventory_item_id = mtls.inventory_item_id)
                    IS NOT NULL
            THEN
               (SELECT msi.attribute9
                  FROM mtl_system_items@zenprod msi
                 WHERE     msi.organization_id = 3
                       AND msi.inventory_item_id = mtls.inventory_item_id)
            ELSE
               mtls.attribute9
         END
            as SUPPLIER_PARTNO,
      MCI.ATTRIBUTE1 as INV_AND_PAC,
      MCI.ATTRIBUTE2 as CUST_PART_NO2,
      h.ATTRIBUTE3 as PO_REMARK,
      '' as RMA_NUMBER,
      '0' as ORD_TYPE,
      'TY' as QC_TYPE 
    FROM apps.wsh_new_deliveries@zenprod d,
         apps.wsh_delivery_assignments@zenprod wda,
         apps.wsh_delivery_details@zenprod wdd,
         apps.mtl_system_items_b@zenprod mtls,
         apps.mtl_item_locations@zenprod mil,
         apps.oe_order_headers_all@zenprod h,
         apps.oe_order_lines_all@zenprod l,
         apps.ZEN_WSH_PICK_SLIP_V@zenprod zs,
         apps.hz_cust_accounts@zenprod cust_acct,
         apps.hz_parties@zenprod party,
         apps.hz_locations@zenprod bill_loc,
         apps.hz_party_sites@zenprod bill_ps,
         apps.hz_cust_acct_sites_all@zenprod bill_cas,
         apps.hz_cust_site_uses_all@zenprod bill_su,
         apps.hz_locations@zenprod ship_loc,
         apps.hz_party_sites@zenprod ship_ps,
         apps.hz_cust_acct_sites_all@zenprod ship_cas,
         apps.hz_cust_site_uses_all@zenprod ship_su,
         apps.MTL_CUSTOMER_ITEMS@zenprod MCI,
         (SELECT a.organization_id,
                 b.DELIVERY_ID,
                 b.DELIVERY_NAME,
                 a.INVOICE_NO,
                 a.INVOICE_DATE,
                 a.CUSTOMER_ID
            FROM apps.ZEN_PACKING_INVOICE_HEADER@zenprod A,
                 apps.ZEN_PACKING_INVOICE_LINE@zenprod B
           WHERE a.invoice_id = b.invoice_id AND b.INVOICE_SOURCE = 'DELIVERY') O,
         zenwms.WMS_SHIPPING@hr WS
   WHERE     d.DELIVERY_ID >= 2124472
         AND d.delivery_id = wda.delivery_id
         AND wda.delivery_detail_id = wdd.delivery_detail_id
         AND mil.organization_id(+) = zs.organization_id
         AND mil.subinventory_code(+) = zs.FROM_SUBINVENTORY_CODE
         AND mil.inventory_location_id(+) = zs.from_LOCATOR_ID
         AND wdd.inventory_item_id = mtls.inventory_item_id
         AND wdd.source_header_id = h.header_id
         AND wdd.source_line_id = l.line_id
         AND zs.move_order_line_id = wdd.move_order_line_id
         AND zs.ORGANIZATION_ID = wdd.ORGANIZATION_ID
         AND l.sold_to_org_id = MCI.customer_id(+)
         AND l.attribute3 = MCI.customer_item_number(+)
         AND l.sold_to_org_id = cust_acct.cust_account_id
         AND cust_acct.party_id = party.party_id
         AND bill_cas.party_site_id = bill_ps.party_site_id
         AND bill_ps.location_id = bill_loc.location_id
         AND bill_su.cust_acct_site_id = bill_cas.cust_acct_site_id
         AND l.invoice_to_org_id = bill_su.site_use_id
         AND l.ship_to_org_id = ship_su.site_use_id
         AND ship_su.cust_acct_site_id = ship_cas.cust_acct_site_id
         AND ship_cas.party_site_id = ship_ps.party_site_id
         AND ship_ps.location_id = ship_loc.location_id
         AND d.DELIVERY_ID = O.DELIVERY_ID
         AND D.name = O.DELIVERY_NAME
         AND mtls.organization_id = d.organization_id    
         AND O.invoice_no = WS.INVOICENO
         AND zs.FROM_SUBINVENTORY_CODE = '外存倉'
         AND (mil.segment1 like '%飛力達%' OR mil.segment2 like '%飛力達%')
         --AND TO_DATE(d.attribute12,'DD-MON-YYYY','NLS_DATE_LANGUAGE=AMERICAN') >= TRUNC(sysdate) --for正式上線第一週(手動執行)
         AND TO_DATE(d.attribute12,'DD-MON-YYYY','NLS_DATE_LANGUAGE=AMERICAN') BETWEEN TRUNC(sysdate) - 1 AND TRUNC(sysdate) + 1 --for正式上線一週後(自動化)
         AND NOT EXISTS(
            SELECT 1
            FROM ZEN_B2B_JSON_SO 
            WHERE 
                  ID = O.invoice_no || '-' || l.line_id 
                  AND (STATUS = 
                  (CASE 
                        WHEN d.name IS NULL -- ORDER_NO
                        OR O.invoice_no IS NULL -- INVOICE_NO
                        OR O.invoice_date IS NULL -- INVOICE_DATE
                        OR d.attribute12 IS NULL -- EXP_SHIP_DATE
                        OR (SELECT ffvt.description
                              FROM HZ_CUST_SITE_USES_ALL@zenprod rsua,
                              jtf_rs_salesreps@zenprod jrs,
                              gl_code_combinations@zenprod gcc,
                              fnd_id_flex_segments@zenprod fifs,
                              fnd_flex_values@zenprod ffv,
                              fnd_flex_values_tl@zenprod ffvt
                        WHERE     rsua.site_use_code = 'SHIP_TO'
                              AND rsua.site_use_id = ship_su.site_use_id
                              AND jrs.salesrep_id = rsua.primary_salesrep_id
                              AND gcc.code_combination_id = jrs.gl_id_rev
                              AND fifs.id_flex_num = gcc.chart_of_accounts_id
                              AND fifs.segment_name = '部門別'
                              AND ffv.flex_value_set_id = fifs.flex_value_set_id
                              AND ffv.flex_value = gcc.segment2
                              AND ffvt.flex_value_id = ffv.flex_value_id
                              AND fifs.id_flex_code = 'GL#') IS NULL -- DEPT
                        OR party.party_name IS NULL -- CUST_NAME
                        OR bill_loc.address1 IS NULL  -- INVOICE_ADDRESS
                        OR ship_ps.party_site_number IS NULL  -- SHIP_TO_NO
                        OR NVL (d.attribute8, ship_loc.address1) IS NULL -- DELIVERY_ADDRESS
                        OR mtls.segment1
                        || DECODE (l.attribute8, NULL, NULL, ' #' || l.attribute8 || '#')
                        || DECODE (mtls.attribute6, 'Y', '(' || mtls.description || ')', NULL) IS NULL -- ZT_PART_NO
                        OR (SELECT mcb.segment1
                              FROM mtl_item_categories@zenprod mic, mtl_categories_b@zenprod mcb
                        WHERE     mic.category_set_id = 1
                              AND mic.category_id = mcb.category_id
                              AND mic.organization_id = mtls.organization_id
                              AND mic.inventory_item_id = mtls.inventory_item_id) IS NULL -- BRAND
                        OR  DECODE (mtls.segment1,
                              '銷貨折讓', wdd.requested_quantity * -1,
                              wdd.requested_quantity) IS NULL -- QUANTITY
                        OR l.UNIT_SELLING_PRICE IS NULL -- UNIT_PRICE
                        OR DECODE (mtls.segment1,
                                    '銷貨折讓', wdd.requested_quantity * -1,
                                    wdd.requested_quantity)
                        * l.UNIT_SELLING_PRICE IS NULL -- AMOUNT
                        OR mtls.attribute1 IS NULL -- ITEM_MODE
                        OR zs.FROM_SUBINVENTORY_CODE IS NULL -- FROM_SUBINVENTORY_CODE
                        OR mil.segment1 IS NULL -- SEGMENT1 
                        OR mil.segment2 IS NULL -- SEGMENT2
                        THEN 'N'
                        ELSE 'W'
                        END) OR STATUS = 'S')
         )
ORDER BY ORDER_NO, ID
);
end;  