begin
EXECUTE IMMEDIATE 'ALTER SESSION SET NLS_LANGUAGE= ''AMERICAN''';
insert into ZEN_B2B_JSON_SO_TMP
(SEQ_ID,
 SENDER_CODE,
 RECEIVER_CODE,
 STATUS,
 DIRECTION,
 DOC_DATETIME,
 LAST_UPDATE_DATE,
 CREATION_DATE,
 USER_NO,
 ID,
 ORDER_NO,
 INV_SPLIT,
 INVOICE_NO,
 INVOICE_DATE,
 EXP_SHIP_DATE,
 CUST_NO,
 DEPT,
 CUST_NAME,
 INVOICE_ADDRESS,
 SHIP_TO_NO,
 DELIVERY_ADDRESS,
 SHIP_TO_CONTACT,
 SHIP_TO_PHONE,
 ZT_PART_NO,
 CUST_PART_NO,
 CUSTOMER_PO,
 SHIP_NOTICE,
 SHIP_NOTES,
 STATUS1,
 BRAND,
 QUANTITY,
 UNIT_PRICE,
 AMOUNT,
 CUST_PO,
 CUST_PO2,
 CUST_POLINE,
 CUST_PN,
 CUST_PN2,
 ITEM_DESC,
 ITEM_MODE,
 ORDER_LINE_MEMO,
 UNIT_WEIGHT,
 WEIGHT_UOM_CODE,
 FROM_SUBINVENTORY_CODE,
 SEGMENT1,
 SEGMENT2,
 KIND,
 SUPPLIER_PARTNO,
 INV_AND_PAC,
 CUST_PART_NO2,
 PO_REMARK,
 RMA_NUMBER,
 ORD_TYPE,
 QC_TYPE)
select ZEN_B2B_JSON_SEQ.nextval,
       SENDER_CODE,
       RECEIVER_CODE,
       STATUS,
       DIRECTION,
       DOC_DATETIME,
       LAST_UPDATE_DATE,
       CREATION_DATE,
       USER_NO,
       ID,
       ORDER_NO,
       INV_SPLIT,
       INVOICE_NO,
       INVOICE_DATE,
       EXP_SHIP_DATE,
       CUST_NO,
       DEPT,
       CUST_NAME,
       INVOICE_ADDRESS,
       SHIP_TO_NO,
       DELIVERY_ADDRESS,
       SHIP_TO_CONTACT,
       SHIP_TO_PHONE,
       ZT_PART_NO,
       CUST_PART_NO,
       CUSTOMER_PO,
       SHIP_NOTICE,
       SHIP_NOTES,
       STATUS1,
       BRAND,
       QUANTITY,
       UNIT_PRICE,
       AMOUNT,
       CUST_PO,
       CUST_PO2,
       CUST_POLINE,
       CUST_PN,
       CUST_PN2,
       ITEM_DESC,
       ITEM_MODE,
       ORDER_LINE_MEMO,
       UNIT_WEIGHT,
       WEIGHT_UOM_CODE,
       FROM_SUBINVENTORY_CODE,
       SEGMENT1,
       SEGMENT2,
       KIND,
       SUPPLIER_PARTNO,
       INV_AND_PAC,
       CUST_PART_NO2,
       PO_REMARK,
       RMA_NUMBER,
       ORD_TYPE,
       QC_TYPE
from (
         SELECT  'ZEN' as SENDER_CODE,
                 'JIT' as RECEIVER_CODE,
                 CASE
                     WHEN d.name IS NULL
                         OR d.attribute12 IS NULL
                         OR
                          (SELECT ffvt.description
                           FROM HZ_CUST_SITE_USES_ALL@PROD2 rsua,
                                jtf_rs_salesreps@PROD2      jrs,
                                gl_code_combinations@PROD2  gcc,
                                fnd_id_flex_segments@PROD2  fifs,
                                fnd_flex_values@PROD2       ffv,
                                fnd_flex_values_tl@PROD2    ffvt
                           WHERE rsua.site_use_code = 'SHIP_TO'
                             AND rsua.site_use_id = ship_su.site_use_id
                             AND jrs.salesrep_id = rsua.primary_salesrep_id
                             AND gcc.code_combination_id = jrs.gl_id_rev
                             AND fifs.id_flex_num = gcc.chart_of_accounts_id
                             AND fifs.segment_name = '部門別'
                             AND ffv.flex_value_set_id =
                                 fifs.flex_value_set_id
                             AND ffv.flex_value = gcc.segment2
                             AND ffvt.flex_value_id = ffv.flex_value_id
                             AND fifs.id_flex_code = 'GL#') IS NULL
                         OR party.party_name IS NULL
                         OR bill_loc.address1 IS NULL
                         OR ship_ps.party_site_number IS NULL
                         OR NVL(d.attribute8, ship_loc.address1) IS NULL
                         OR mtls.segment1 ||
                            DECODE(l.attribute8,
                                   NULL,
                                   NULL,
                                   ' #' || l.attribute8 || '#') ||
                            DECODE(mtls.attribute6,
                                   'Y',
                                   '(' || mtls.description || ')',
                                   NULL) IS NULL
                         OR
                          (SELECT mcb.segment1
                           FROM mtl_item_categories@PROD2 mic, mtl_categories_b@PROD2 mcb
                           WHERE mic.category_set_id = 1
                             AND mic.category_id = mcb.category_id
                             AND mic.organization_id = mtls.organization_id
                             AND mic.inventory_item_id =
                                 mtls.inventory_item_id) IS NULL
                         OR DECODE(mtls.segment1,
                                   '銷貨折讓',
                                   wdd.requested_quantity * -1,
                                   wdd.requested_quantity) IS NULL
                         OR mtls.attribute1 IS NULL
                         OR zs.FROM_SUBINVENTORY_CODE IS NULL
                         OR mil.segment1 IS NULL
                         OR mil.segment2 IS NULL
                         THEN
                         'N'
                     ELSE
                         'W'
                     END as STATUS,
                 'OUT' as DIRECTION ,
                 TO_CHAR(d.CONFIRM_DATE, 'yyyy-MM-dd HH:mm:ss') as DOC_DATETIME,
                 TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as LAST_UPDATE_DATE,
                 TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as CREATION_DATE,
                 'admin' as USER_NO,
                 d.NAME || '-' || l.line_id as ID,
                 d.name as ORDER_NO,
                 d.ATTRIBUTE5 as INV_SPLIT,
                 d.NAME || '-'|| DECODE(D.ORGANIZATION_ID,169,'ZSH','SHC') as INVOICE_NO,
                 NULL as INVOICE_DATE,
                 TO_CHAR(TO_DATE(d.attribute12,
                                 'DD-MON-YYYY',
                                 'NLS_DATE_LANGUAGE=AMERICAN'),
                         'yyyy/mm/dd') as EXP_SHIP_DATE,
                 cust_acct.account_number as CUST_NO,
                 (SELECT ffvt.description
                  FROM HZ_CUST_SITE_USES_ALL@PROD2 rsua,
                       jtf_rs_salesreps@PROD2      jrs,
                       gl_code_combinations@PROD2  gcc,
                       fnd_id_flex_segments@PROD2  fifs,
                       fnd_flex_values@PROD2       ffv,
                       fnd_flex_values_tl@PROD2    ffvt
                  WHERE rsua.site_use_code = 'SHIP_TO'
                    AND rsua.site_use_id = ship_su.site_use_id
                    AND jrs.salesrep_id = rsua.primary_salesrep_id
                    AND gcc.code_combination_id = jrs.gl_id_rev
                    AND fifs.id_flex_num = gcc.chart_of_accounts_id
                    AND fifs.segment_name = '部門別'
                    AND ffv.flex_value_set_id = fifs.flex_value_set_id
                    AND ffv.flex_value = gcc.segment2
                    AND ffvt.flex_value_id = ffv.flex_value_id
                    AND fifs.id_flex_code = 'GL#') as DEPT,
                 party.party_name as CUST_NAME,
                 bill_loc.address1 as INVOICE_ADDRESS,
                 ship_ps.party_site_number as SHIP_TO_NO,
                 NVL(d.attribute8, ship_loc.address1) as DELIVERY_ADDRESS,
                 (SELECT first_name || last_name
                  FROM ZEN_RA_CONTACTS_V@PROD2 rcs
                  WHERE rcs.customer_id = l.sold_to_org_id
                    AND rcs.address_id = ship_cas.cust_acct_site_id
                    AND rcs.contact_id =
                        (SELECT MIN(contact_id)
                         FROM ZEN_RA_CONTACTS_V@PROD2 rcs
                         WHERE rcs.customer_id = l.sold_to_org_id
                           AND rcs.address_id = ship_cas.cust_acct_site_id
                           AND rcs.status = 'A')) as SHIP_TO_CONTACT,
                 (SELECT DECODE(a.phone_area_code,
                                NULL,
                                a.phone_number,
                                a.phone_area_code || '-' || a.phone_number)
                  FROM zen_ra_phones_v@PROD2 a
                  WHERE a.phone_id =
                        (SELECT MAX(b.phone_id)
                         FROM zen_ra_phones_v@PROD2 b
                         WHERE a.customer_id = b.customer_id
                           AND a.address_id = b.address_id
                           AND b.phone_line_type = 'GEN'
                         GROUP BY b.customer_id, b.address_id)
                    AND a.phone_line_type = 'GEN'
                    AND a.customer_id = l.sold_to_org_id
                    AND a.address_id = ship_cas.cust_acct_site_id) as SHIP_TO_PHONE,
                 mtls.segment1 ||
                 DECODE(l.attribute8,
                        NULL,
                        NULL,
                        ' #' || l.attribute8 || '#') ||
                 DECODE(mtls.attribute6,
                        'Y',
                        '(' || mtls.description || ')',
                        NULL) as ZT_PART_NO,
                 NVL(l.attribute3, l.attribute5) as CUST_PART_NO,
                 l.attribute2 ||
                 DECODE(l.attribute1, NULL, NULL, '-' || l.attribute1) as CUSTOMER_PO,
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute1) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute2) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute3) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute4) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute5) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute6) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute7) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute8) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute9) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute10) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute11) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute14) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute15) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute16) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@PROD2
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute17) || ' ' || CASE
                                                                         WHEN (NVL(d.ATTRIBUTE6, ship_su.ATTRIBUTE22)) IS NOT NULL THEN
                                                                             'Shipping Mark:' ||
                                                                             NVL(d.ATTRIBUTE6, ship_su.ATTRIBUTE22) || ' '
                                                                         ELSE
                                                                             ' '
                     END || ship_cas.attribute12 as SHIP_NOTICE,
                 d.attribute4 as SHIP_NOTES,
                 DECODE(wdd.RELEASED_STATUS,
                        'B',
                        'BK',
                        'C',
                        'SH',
                        'D',
                        'CL',
                        'R',
                        'RR',
                        'S',
                        'RW',
                        'Y',
                        'SG',
                        'N',
                        'NR',
                        'X',
                        'NA') as STATUS1,
                 (SELECT mcb.segment1
                  FROM mtl_item_categories@PROD2 mic, mtl_categories_b@PROD2 mcb
                  WHERE mic.category_set_id = 1
                    AND mic.category_id = mcb.category_id
                    AND mic.organization_id = mtls.organization_id
                    AND mic.inventory_item_id = mtls.inventory_item_id) as BRAND,
                 DECODE(mtls.segment1,
                        '銷貨折讓',
                        wdd.requested_quantity * -1,
                        wdd.requested_quantity) as QUANTITY,
                 NULL as UNIT_PRICE,
                 NULL as AMOUNT,
                 l.ATTRIBUTE2 as CUST_PO,
                 l.attribute6 as CUST_PO2,
                 l.ATTRIBUTE1 as CUST_POLINE,
                 l.ATTRIBUTE3 as CUST_PN,
                 l.ATTRIBUTE5 as CUST_PN2,
                 MCI.CUSTOMER_ITEM_DESC as ITEM_DESC,
                 mtls.attribute1 as ITEM_MODE,
                 l.attribute8 as ORDER_LINE_MEMO,
                 TO_CHAR(mtls.UNIT_WEIGHT,
                         'FM9999999999999990.99999999999999999999') as UNIT_WEIGHT,
                 mtls.WEIGHT_UOM_CODE as WEIGHT_UOM_CODE,
                 zs.FROM_SUBINVENTORY_CODE as FROM_SUBINVENTORY_CODE,
                 mil.segment1 as SEGMENT1,
                 mil.segment2 as SEGMENT2,
                 '撿貨單' as KIND,
                 CASE
                     WHEN (SELECT msi.attribute9
                           FROM mtl_system_items@PROD2 msi
                           WHERE msi.organization_id = 3
                             AND msi.inventory_item_id =
                                 mtls.inventory_item_id) IS NOT NULL THEN
                         (SELECT msi.attribute9
                          FROM mtl_system_items@PROD2 msi
                          WHERE msi.organization_id = 3
                            AND msi.inventory_item_id = mtls.inventory_item_id)
                     ELSE
                         mtls.attribute9
                     END as SUPPLIER_PARTNO,
                 MCI.ATTRIBUTE1 as INV_AND_PAC,
                 MCI.ATTRIBUTE2 as CUST_PART_NO2,
                 nvl(nvl(l.ATTRIBUTE16,ZEN_GET_WMS_ITEM_F(mtls.segment1,D.ORGANIZATION_ID)),mtls.segment1) as PO_REMARK,
                 '' as RMA_NUMBER,
                 '0' as ORD_TYPE,
                 'TY' as QC_TYPE
         FROM apps.wsh_new_deliveries@PROD2       d,
              apps.wsh_delivery_assignments@PROD2 wda,
              apps.wsh_delivery_details@PROD2     wdd,
              apps.mtl_system_items_b@PROD2      mtls,
              apps.mtl_item_locations@PROD2       mil,
              apps.oe_order_headers_all@PROD2     h,
              apps.oe_order_lines_all@PROD2       l,
              apps.ZEN_WSH_PICK_SLIP_V@PROD2      zs,
              apps.hz_cust_accounts@PROD2         cust_acct,
              apps.hz_parties@PROD2              party,
              apps.hz_locations@PROD2             bill_loc,
              apps.hz_party_sites@PROD2           bill_ps,
              apps.hz_cust_acct_sites_all@PROD2   bill_cas,
              apps.hz_cust_site_uses_all@PROD2    bill_su,
              apps.hz_locations@PROD2             ship_loc,
              apps.hz_party_sites@PROD2           ship_ps,
              apps.hz_cust_acct_sites_all@PROD2   ship_cas,
              apps.hz_cust_site_uses_all@PROD2    ship_su,
              apps.MTL_CUSTOMER_ITEMS@PROD2       MCI
         WHERE
             ( D.ORGANIZATION_ID = 169
                 OR (D.ORGANIZATION_ID = 209 AND h.SOLD_TO_ORG_ID not in (1366747,32228,39440)))
           AND D.STATUS_CODE = 'CL'
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
           AND mtls.organization_id = d.organization_id
           AND zs.FROM_SUBINVENTORY_CODE = '外存倉'
           AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
           AND mil.attribute3='JIT'
           AND TRUNC(D.CONFIRM_DATE) BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE+30)
           AND NOT EXISTS
             (SELECT 1
              FROM ZEN_B2B_JSON_SO
              WHERE ID = D.name || '-' || l.line_id
                AND (STATUS = (CASE
                                   WHEN d.name IS NULL
                                       OR d.attribute12 IS NULL
                                       OR
                                        (SELECT ffvt.description
                                         FROM HZ_CUST_SITE_USES_ALL@PROD2 rsua,
                                              jtf_rs_salesreps@PROD2      jrs,
                                              gl_code_combinations@PROD2  gcc,
                                              fnd_id_flex_segments@PROD2  fifs,
                                              fnd_flex_values@PROD2       ffv,
                                              fnd_flex_values_tl@PROD2    ffvt
                                         WHERE rsua.site_use_code = 'SHIP_TO'
                                           AND rsua.site_use_id = ship_su.site_use_id
                                           AND jrs.salesrep_id = rsua.primary_salesrep_id
                                           AND gcc.code_combination_id = jrs.gl_id_rev
                                           AND fifs.id_flex_num = gcc.chart_of_accounts_id
                                           AND fifs.segment_name = '部門別'
                                           AND ffv.flex_value_set_id =
                                               fifs.flex_value_set_id
                                           AND ffv.flex_value = gcc.segment2
                                           AND ffvt.flex_value_id = ffv.flex_value_id
                                           AND fifs.id_flex_code = 'GL#') IS NULL
                                       OR party.party_name IS NULL
                                       OR bill_loc.address1 IS NULL
                                       OR ship_ps.party_site_number IS NULL
                                       OR NVL(d.attribute8, ship_loc.address1) IS NULL
                                       OR mtls.segment1 ||
                                          DECODE(l.attribute8,
                                                 NULL,
                                                 NULL,
                                                 ' #' || l.attribute8 || '#') ||
                                          DECODE(mtls.attribute6,
                                                 'Y',
                                                 '(' || mtls.description || ')',
                                                 NULL) IS NULL
                                       OR
                                        (SELECT mcb.segment1
                                         FROM mtl_item_categories@PROD2 mic,
                                              mtl_categories_b@PROD2    mcb
                                         WHERE mic.category_set_id = 1
                                           AND mic.category_id = mcb.category_id
                                           AND mic.organization_id = mtls.organization_id
                                           AND mic.inventory_item_id =
                                               mtls.inventory_item_id) IS NULL
                                       OR DECODE(mtls.segment1,
                                                 '銷貨折讓',
                                                 wdd.requested_quantity * -1,
                                                 wdd.requested_quantity) IS NULL
                                       OR mtls.attribute1 IS NULL
                                       OR zs.FROM_SUBINVENTORY_CODE IS NULL
                                       OR mil.segment1 IS NULL
                                       OR mil.segment2 IS NULL
                                       THEN
                                       'N'
                                   ELSE
                                       'W'
                  END) OR STATUS = 'S'))
         UNION ALL
         SELECT  'ZEN' as SENDER_CODE,
                 'JIT' as RECEIVER_CODE,
                 CASE
                     WHEN d.name IS NULL
                         OR d.attribute12 IS NULL
                         OR
                          (SELECT ffvt.description
                           FROM HZ_CUST_SITE_USES_ALL@jy rsua,
                                jtf_rs_salesreps@jy      jrs,
                                gl_code_combinations@jy  gcc,
                                fnd_id_flex_segments@jy  fifs,
                                fnd_flex_values@jy       ffv,
                                fnd_flex_values_tl@jy    ffvt
                           WHERE rsua.site_use_code = 'SHIP_TO'
                             AND rsua.site_use_id = ship_su.site_use_id
                             AND jrs.salesrep_id = rsua.primary_salesrep_id
                             AND gcc.code_combination_id = jrs.gl_id_rev
                             AND fifs.id_flex_num = gcc.chart_of_accounts_id
                             AND fifs.segment_name = '部門別'
                             AND ffv.flex_value_set_id =
                                 fifs.flex_value_set_id
                             AND ffv.flex_value = gcc.segment2
                             AND ffvt.flex_value_id = ffv.flex_value_id
                             AND fifs.id_flex_code = 'GL#') IS NULL
                         OR party.party_name IS NULL
                         OR bill_loc.address1 IS NULL
                         OR ship_ps.party_site_number IS NULL
                         OR NVL(d.attribute8, ship_loc.address1) IS NULL
                         OR mtls.segment1 ||
                            DECODE(l.attribute8,
                                   NULL,
                                   NULL,
                                   ' #' || l.attribute8 || '#') ||
                            DECODE(mtls.attribute6,
                                   'Y',
                                   '(' || mtls.description || ')',
                                   NULL) IS NULL
                         OR
                          (SELECT mcb.segment1
                           FROM mtl_item_categories@jy mic, mtl_categories_b@jy mcb
                           WHERE mic.category_set_id = 1
                             AND mic.category_id = mcb.category_id
                             AND mic.organization_id = mtls.organization_id
                             AND mic.inventory_item_id =
                                 mtls.inventory_item_id) IS NULL
                         OR DECODE(mtls.segment1,
                                   '銷貨折讓',
                                   wdd.requested_quantity * -1,
                                   wdd.requested_quantity) IS NULL
                         OR mtls.attribute1 IS NULL
                         OR zs.FROM_SUBINVENTORY_CODE IS NULL
                         OR mil.segment1 IS NULL
                         OR mil.segment2 IS NULL
                         THEN
                         'N'
                     ELSE
                         'W'
                     END as STATUS,
                 'OUT' as DIRECTION,
                 TO_CHAR(d.CONFIRM_DATE, 'yyyy-MM-dd HH:mm:ss') as DOC_DATETIME,
                 TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as LAST_UPDATE_DATE,
                 TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as CREATION_DATE,
                 'admin' as USER_NO,
                 d.NAME || '-' || l.line_id as ID,
                 d.name as ORDER_NO,
                 d.ATTRIBUTE5 as INV_SPLIT,
                 d.NAME || '-'||'JY' as INVOICE_NO,
                 NULL as INVOICE_DATE,
                 TO_CHAR(TO_DATE(d.attribute12,
                                 'DD-MON-YYYY',
                                 'NLS_DATE_LANGUAGE=AMERICAN'),
                         'yyyy/mm/dd') as EXP_SHIP_DATE,
                 cust_acct.account_number as CUST_NO,
                 (SELECT ffvt.description
                  FROM HZ_CUST_SITE_USES_ALL@jy rsua,
                       jtf_rs_salesreps@jy      jrs,
                       gl_code_combinations@jy  gcc,
                       fnd_id_flex_segments@jy  fifs,
                       fnd_flex_values@jy       ffv,
                       fnd_flex_values_tl@jy    ffvt
                  WHERE rsua.site_use_code = 'SHIP_TO'
                    AND rsua.site_use_id = ship_su.site_use_id
                    AND jrs.salesrep_id = rsua.primary_salesrep_id
                    AND gcc.code_combination_id = jrs.gl_id_rev
                    AND fifs.id_flex_num = gcc.chart_of_accounts_id
                    AND fifs.segment_name = '部門別'
                    AND ffv.flex_value_set_id = fifs.flex_value_set_id
                    AND ffv.flex_value = gcc.segment2
                    AND ffvt.flex_value_id = ffv.flex_value_id
                    AND fifs.id_flex_code = 'GL#') as DEPT,
                 party.party_name as CUST_NAME,
                 bill_loc.address1 as INVOICE_ADDRESS,
                 ship_ps.party_site_number as SHIP_TO_NO,
                 NVL(d.attribute8, ship_loc.address1) as DELIVERY_ADDRESS,
                 (SELECT first_name || last_name
                  FROM ZEN_RA_CONTACTS_V@jy rcs
                  WHERE rcs.customer_id = l.sold_to_org_id
                    AND rcs.address_id = ship_cas.cust_acct_site_id
                    AND rcs.contact_id =
                        (SELECT MIN(contact_id)
                         FROM ZEN_RA_CONTACTS_V@jy rcs
                         WHERE rcs.customer_id = l.sold_to_org_id
                           AND rcs.address_id = ship_cas.cust_acct_site_id
                           AND rcs.status = 'A')) as SHIP_TO_CONTACT,
                 (SELECT DECODE(a.phone_area_code,
                                NULL,
                                a.phone_number,
                                a.phone_area_code || '-' || a.phone_number)
                  FROM zen_ra_phones_v@jy a
                  WHERE a.phone_id =
                        (SELECT MAX(b.phone_id)
                         FROM zen_ra_phones_v@jy b
                         WHERE a.customer_id = b.customer_id
                           AND a.address_id = b.address_id
                           AND b.phone_line_type = 'GEN'
                         GROUP BY b.customer_id, b.address_id)
                    AND a.phone_line_type = 'GEN'
                    AND a.customer_id = l.sold_to_org_id
                    AND a.address_id = ship_cas.cust_acct_site_id) as SHIP_TO_PHONE,
                 mtls.segment1 ||
                 DECODE(l.attribute8,
                        NULL,
                        NULL,
                        ' #' || l.attribute8 || '#') ||
                 DECODE(mtls.attribute6,
                        'Y',
                        '(' || mtls.description || ')',
                        NULL) as ZT_PART_NO,
                 NVL(l.attribute3, l.attribute5) as CUST_PART_NO,
                 l.attribute2 ||
                 DECODE(l.attribute1, NULL, NULL, '-' || l.attribute1) as CUSTOMER_PO,
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute1) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute2) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute3) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute4) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute5) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute6) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute7) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute8) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute9) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute10) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute11) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute14) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute15) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute16) || ' ' ||
                 (SELECT value_description
                  FROM zen_fnd_flex_values_v@jy
                  WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                    AND flex_value = ship_cas.attribute17) || ' ' || CASE
                                                                         WHEN (NVL(d.ATTRIBUTE6, ship_su.ATTRIBUTE22)) IS NOT NULL THEN
                                                                             'Shipping Mark:' ||
                                                                             NVL(d.ATTRIBUTE6, ship_su.ATTRIBUTE22) || ' '
                                                                         ELSE
                                                                             ' '
                     END || ship_cas.attribute12 as SHIP_NOTICE,
                 d.attribute4 as SHIP_NOTES,
                 DECODE(wdd.RELEASED_STATUS,
                        'B',
                        'BK',
                        'C',
                        'SH',
                        'D',
                        'CL',
                        'R',
                        'RR',
                        'S',
                        'RW',
                        'Y',
                        'SG',
                        'N',
                        'NR',
                        'X',
                        'NA') as STATUS1,
                 (SELECT mcb.segment1
                  FROM mtl_item_categories@jy mic, mtl_categories_b@jy mcb
                  WHERE mic.category_set_id = 1
                    AND mic.category_id = mcb.category_id
                    AND mic.organization_id = mtls.organization_id
                    AND mic.inventory_item_id = mtls.inventory_item_id) as BRAND,
                 DECODE(mtls.segment1,
                        '銷貨折讓',
                        wdd.requested_quantity * -1,
                        wdd.requested_quantity) as QUANTITY,
                 NULL as UNIT_PRICE,
                 NULL as AMOUNT,
                 l.ATTRIBUTE2 as CUST_PO,
                 l.attribute6 as CUST_PO2,
                 l.ATTRIBUTE1 as CUST_POLINE,
                 l.ATTRIBUTE3 as CUST_PN,
                 l.ATTRIBUTE5 as CUST_PN2,
                 MCI.CUSTOMER_ITEM_DESC as ITEM_DESC,
                 mtls.attribute1 as ITEM_MODE,
                 l.attribute8 as ORDER_LINE_MEMO,
                 TO_CHAR(mtls.UNIT_WEIGHT,
                         'FM9999999999999990.99999999999999999999') as UNIT_WEIGHT,
                 mtls.WEIGHT_UOM_CODE as WEIGHT_UOM_CODE,
                 zs.FROM_SUBINVENTORY_CODE as FROM_SUBINVENTORY_CODE,
                 'SHC基通' as SEGMENT1,
                 'IT' as SEGMENT2,
                 '撿貨單' as KIND,
                 CASE
                     WHEN (SELECT msi.attribute9
                           FROM mtl_system_items@jy msi
                           WHERE msi.organization_id = 3
                             AND msi.inventory_item_id =
                                 mtls.inventory_item_id) IS NOT NULL THEN
                         (SELECT msi.attribute9
                          FROM mtl_system_items@jy msi
                          WHERE msi.organization_id = 3
                            AND msi.inventory_item_id = mtls.inventory_item_id)
                     ELSE
                         mtls.attribute9
                     END as SUPPLIER_PARTNO,
                 MCI.ATTRIBUTE1 as INV_AND_PAC,
                 MCI.ATTRIBUTE2 as CUST_PART_NO2,
                 nvl(nvl(l.ATTRIBUTE16,ZEN_GET_WMS_ITEM_F(mtls.segment1,D.ORGANIZATION_ID)),mtls.segment1) as PO_REMARK,
                 '' as RMA_NUMBER,
                 '0' as ORD_TYPE,
                 'TY' as QC_TYPE
         FROM apps.wsh_new_deliveries@jy       d,
              apps.wsh_delivery_assignments@jy wda,
              apps.wsh_delivery_details@jy     wdd,
              apps.mtl_system_items_b@jy       mtls,
              apps.mtl_item_locations@jy       mil,
              apps.oe_order_headers_all@jy     h,
              apps.oe_order_lines_all@jy       l,
              apps.ZEN_WSH_PICK_SLIP_V@jy      zs,
              apps.hz_cust_accounts@jy         cust_acct,
              apps.hz_parties@jy               party,
              apps.hz_locations@jy             bill_loc,
              apps.hz_party_sites@jy           bill_ps,
              apps.hz_cust_acct_sites_all@jy   bill_cas,
              apps.hz_cust_site_uses_all@jy    bill_su,
              apps.hz_locations@jy             ship_loc,
              apps.hz_party_sites@jy           ship_ps,
              apps.hz_cust_acct_sites_all@jy   ship_cas,
              apps.hz_cust_site_uses_all@jy    ship_su,
              apps.MTL_CUSTOMER_ITEMS@jy       MCI
         WHERE
             D.ORGANIZATION_ID = 371
           AND h.SOLD_TO_ORG_ID not in (39440)
           AND D.STATUS_CODE = 'CL'
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
           AND mtls.organization_id = d.organization_id
           AND zs.FROM_SUBINVENTORY_CODE = '外存倉'
           AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
           AND mil.attribute3='JIT'
           AND TRUNC(D.CONFIRM_DATE) BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE+30)
           AND NOT EXISTS
             (SELECT 1
              FROM ZEN_B2B_JSON_SO@B2BDB
              WHERE ID = D.name || '-' || l.line_id
                AND (STATUS = (CASE
                                   WHEN d.name IS NULL
                                       OR d.attribute12 IS NULL
                                       OR
                                        (SELECT ffvt.description@jy
                                         FROM HZ_CUST_SITE_USES_ALL@jy rsua,
                                              jtf_rs_salesreps@jy      jrs,
                                              gl_code_combinations@jy  gcc,
                                              fnd_id_flex_segments@jy  fifs,
                                              fnd_flex_values@jy       ffv,
                                              fnd_flex_values_tl@jy    ffvt
                                         WHERE rsua.site_use_code = 'SHIP_TO'
                                           AND rsua.site_use_id = ship_su.site_use_id
                                           AND jrs.salesrep_id = rsua.primary_salesrep_id
                                           AND gcc.code_combination_id = jrs.gl_id_rev
                                           AND fifs.id_flex_num = gcc.chart_of_accounts_id
                                           AND fifs.segment_name = '部門別'
                                           AND ffv.flex_value_set_id =
                                               fifs.flex_value_set_id
                                           AND ffv.flex_value = gcc.segment2
                                           AND ffvt.flex_value_id = ffv.flex_value_id
                                           AND fifs.id_flex_code = 'GL#') IS NULL
                                       OR party.party_name IS NULL
                                       OR bill_loc.address1 IS NULL
                                       OR ship_ps.party_site_number IS NULL
                                       OR NVL(d.attribute8, ship_loc.address1) IS NULL
                                       OR mtls.segment1 ||
                                          DECODE(l.attribute8,
                                                 NULL,
                                                 NULL,
                                                 ' #' || l.attribute8 || '#') ||
                                          DECODE(mtls.attribute6,
                                                 'Y',
                                                 '(' || mtls.description || ')',
                                                 NULL) IS NULL
                                       OR
                                        (SELECT mcb.segment1
                                         FROM mtl_item_categories@jy mic,
                                              mtl_categories_b@jy    mcb
                                         WHERE mic.category_set_id = 1
                                           AND mic.category_id = mcb.category_id
                                           AND mic.organization_id = mtls.organization_id
                                           AND mic.inventory_item_id =
                                               mtls.inventory_item_id) IS NULL
                                       OR DECODE(mtls.segment1,
                                                 '銷貨折讓',
                                                 wdd.requested_quantity * -1,
                                                 wdd.requested_quantity) IS NULL
                                       OR mtls.attribute1 IS NULL
                                       OR zs.FROM_SUBINVENTORY_CODE IS NULL
                                       OR mil.segment1 IS NULL
                                       OR mil.segment2 IS NULL
                                       THEN
                                       'N'
                                   ELSE
                                       'W'
                  END) OR STATUS = 'S'))
         UNION ALL
         SELECT 'ZEN' as SENDER_CODE,
                'JIT' as RECEIVER_CODE,
                CASE
                    WHEN d.name IS NULL
                        OR d.attribute12 IS NULL
                        OR
                         (SELECT ffvt.description@SHCTRADING0
                          FROM HZ_CUST_SITE_USES_ALL@SHCTRADING0 rsua,
                               jtf_rs_salesreps@SHCTRADING0      jrs,
                               gl_code_combinations@SHCTRADING0  gcc,
                               fnd_id_flex_segments@SHCTRADING0  fifs,
                               fnd_flex_values@SHCTRADING0       ffv,
                               fnd_flex_values_tl@SHCTRADING0    ffvt
                          WHERE rsua.site_use_code = 'SHIP_TO'
                            AND rsua.site_use_id = ship_su.site_use_id
                            AND jrs.salesrep_id = rsua.primary_salesrep_id
                            AND gcc.code_combination_id = jrs.gl_id_rev
                            AND fifs.id_flex_num = gcc.chart_of_accounts_id
                            AND fifs.segment_name = '部門別'
                            AND ffv.flex_value_set_id =
                                fifs.flex_value_set_id
                            AND ffv.flex_value = gcc.segment2
                            AND ffvt.flex_value_id = ffv.flex_value_id
                            AND fifs.id_flex_code = 'GL#') IS NULL
                        OR party.party_name IS NULL
                        OR bill_loc.address1 IS NULL
                        OR ship_ps.party_site_number IS NULL
                        OR NVL(d.attribute8, ship_loc.address1) IS NULL
                        OR mtls.segment1 ||
                           DECODE(l.attribute8,
                                  NULL,
                                  NULL,
                                  ' #' || l.attribute8 || '#') ||
                           DECODE(mtls.attribute6,
                                  'Y',
                                  '(' || mtls.description || ')',
                                  NULL) IS NULL
                        OR
                         (SELECT mcb.segment1
                          FROM mtl_item_categories@SHCTRADING0 mic, mtl_categories_b@SHCTRADING0 mcb
                          WHERE mic.category_set_id = 1
                            AND mic.category_id = mcb.category_id
                            AND mic.organization_id = mtls.organization_id
                            AND mic.inventory_item_id =
                                mtls.inventory_item_id) IS NULL
                        OR DECODE(mtls.segment1,
                                  '銷貨折讓',
                                  wdd.requested_quantity * -1,
                                  wdd.requested_quantity) IS NULL
                        OR mtls.attribute1 IS NULL
                        OR zs.FROM_SUBINVENTORY_CODE IS NULL
                        OR mil.segment1 IS NULL
                        OR mil.segment2 IS NULL
                        THEN
                        'N'
                    ELSE
                        'W'
                    END as STATUS,
                'OUT' as DIRECTION,
                TO_CHAR(d.CONFIRM_DATE, 'yyyy-MM-dd HH:mm:ss') as DOC_DATETIME,
                TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as LAST_UPDATE_DATE,
                TO_CHAR(sysdate, 'yyyy-MM-dd HH:mm:ss') as CREATION_DATE,
                'admin' as USER_NO,
                d.NAME || '-' || l.line_id as ID,
                d.name as ORDER_NO,
                d.ATTRIBUTE5 as INV_SPLIT,
                d.NAME || '-'||'HHW' as INVOICE_NO,
                NULL as INVOICE_DATE,
                TO_CHAR(TO_DATE(d.attribute12,
                                'DD-MON-YYYY',
                                'NLS_DATE_LANGUAGE=AMERICAN'),
                        'yyyy/mm/dd') as EXP_SHIP_DATE,
                cust_acct.account_number as CUST_NO,
                (SELECT ffvt.description
                 FROM HZ_CUST_SITE_USES_ALL@SHCTRADING0 rsua,
                      jtf_rs_salesreps@SHCTRADING0      jrs,
                      gl_code_combinations@SHCTRADING0  gcc,
                      fnd_id_flex_segments@SHCTRADING0  fifs,
                      fnd_flex_values@SHCTRADING0       ffv,
                      fnd_flex_values_tl@SHCTRADING0    ffvt
                 WHERE rsua.site_use_code = 'SHIP_TO'
                   AND rsua.site_use_id = ship_su.site_use_id
                   AND jrs.salesrep_id = rsua.primary_salesrep_id
                   AND gcc.code_combination_id = jrs.gl_id_rev
                   AND fifs.id_flex_num = gcc.chart_of_accounts_id
                   AND fifs.segment_name = '部門別'
                   AND ffv.flex_value_set_id = fifs.flex_value_set_id
                   AND ffv.flex_value = gcc.segment2
                   AND ffvt.flex_value_id = ffv.flex_value_id
                   AND fifs.id_flex_code = 'GL#') as DEPT,
                party.party_name as CUST_NAME,
                bill_loc.address1 as INVOICE_ADDRESS,
                ship_ps.party_site_number as SHIP_TO_NO,
                NVL(d.attribute8, ship_loc.address1) as DELIVERY_ADDRESS,
                (SELECT first_name || last_name
                 FROM ZEN_RA_CONTACTS_V@SHCTRADING0 rcs
                 WHERE rcs.customer_id = l.sold_to_org_id
                   AND rcs.address_id = ship_cas.cust_acct_site_id
                   AND rcs.contact_id =
                       (SELECT MIN(contact_id)
                        FROM ZEN_RA_CONTACTS_V@SHCTRADING0 rcs
                        WHERE rcs.customer_id = l.sold_to_org_id
                          AND rcs.address_id = ship_cas.cust_acct_site_id
                          AND rcs.status = 'A')) as SHIP_TO_CONTACT,
                (SELECT DECODE(a.phone_area_code,
                               NULL,
                               a.phone_number,
                               a.phone_area_code || '-' || a.phone_number)
                 FROM zen_ra_phones_v@SHCTRADING0 a
                 WHERE a.phone_id =
                       (SELECT MAX(b.phone_id)
                        FROM zen_ra_phones_v@SHCTRADING0 b
                        WHERE a.customer_id = b.customer_id
                          AND a.address_id = b.address_id
                          AND b.phone_line_type = 'GEN'
                        GROUP BY b.customer_id, b.address_id)
                   AND a.phone_line_type = 'GEN'
                   AND a.customer_id = l.sold_to_org_id
                   AND a.address_id = ship_cas.cust_acct_site_id) as SHIP_TO_PHONE,
                mtls.segment1 ||
                DECODE(l.attribute8,
                       NULL,
                       NULL,
                       ' #' || l.attribute8 || '#') ||
                DECODE(mtls.attribute6,
                       'Y',
                       '(' || mtls.description || ')',
                       NULL) as ZT_PART_NO,
                NVL(l.attribute3, l.attribute5) as CUST_PART_NO,
                l.attribute2 ||
                DECODE(l.attribute1, NULL, NULL, '-' || l.attribute1) as CUSTOMER_PO,
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute1) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute2) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute3) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute4) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute5) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute6) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute7) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute8) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute9) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute10) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute11) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute14) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute15) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute16) || ' ' ||
                (SELECT value_description
                 FROM zen_fnd_flex_values_v@SHCTRADING0
                 WHERE flex_value_set_name = 'ZEN_ADDRESS_ATTRIBUTE'
                   AND flex_value = ship_cas.attribute17) || ' ' || CASE
                                                                        WHEN (NVL(d.ATTRIBUTE6, ship_su.ATTRIBUTE22)) IS NOT NULL THEN
                                                                            'Shipping Mark:' ||
                                                                            NVL(d.ATTRIBUTE6, ship_su.ATTRIBUTE22) || ' '
                                                                        ELSE
                                                                            ' '
                    END || ship_cas.attribute12 as SHIP_NOTICE,
                d.attribute4 as SHIP_NOTES,
                DECODE(wdd.RELEASED_STATUS,
                       'B',
                       'BK',
                       'C',
                       'SH',
                       'D',
                       'CL',
                       'R',
                       'RR',
                       'S',
                       'RW',
                       'Y',
                       'SG',
                       'N',
                       'NR',
                       'X',
                       'NA') as STATUS1,
                (SELECT mcb.segment1
                 FROM mtl_item_categories@SHCTRADING0 mic, mtl_categories_b@SHCTRADING0 mcb
                 WHERE mic.category_set_id = 1
                   AND mic.category_id = mcb.category_id
                   AND mic.organization_id = mtls.organization_id
                   AND mic.inventory_item_id = mtls.inventory_item_id) as BRAND,
                DECODE(mtls.segment1,
                       '銷貨折讓',
                       wdd.requested_quantity * -1,
                       wdd.requested_quantity) as QUANTITY,
                NULL as UNIT_PRICE,
                NULL as AMOUNT,
                l.ATTRIBUTE2 as CUST_PO,
                l.attribute6 as CUST_PO2,
                l.ATTRIBUTE1 as CUST_POLINE,
                l.ATTRIBUTE3 as CUST_PN,
                l.ATTRIBUTE5 as CUST_PN2,
                MCI.CUSTOMER_ITEM_DESC as ITEM_DESC,
                mtls.attribute1 as ITEM_MODE,
                l.attribute8 as ORDER_LINE_MEMO,
                TO_CHAR(mtls.UNIT_WEIGHT,
                        'FM9999999999999990.99999999999999999999') as UNIT_WEIGHT,
                mtls.WEIGHT_UOM_CODE as WEIGHT_UOM_CODE,
                zs.FROM_SUBINVENTORY_CODE as FROM_SUBINVENTORY_CODE,
                'SHC基通' as SEGMENT1,
                'IT' as SEGMENT2,
                '撿貨單' as KIND,
                CASE
                    WHEN (SELECT msi.attribute9
                          FROM mtl_system_items@SHCTRADING0 msi
                          WHERE msi.organization_id = 3
                            AND msi.inventory_item_id =
                                mtls.inventory_item_id) IS NOT NULL THEN
                        (SELECT msi.attribute9
                         FROM mtl_system_items@SHCTRADING0 msi
                         WHERE msi.organization_id = 3
                           AND msi.inventory_item_id = mtls.inventory_item_id)
                    ELSE
                        mtls.attribute9
                    END as SUPPLIER_PARTNO,
                MCI.ATTRIBUTE1 as INV_AND_PAC,
                MCI.ATTRIBUTE2 as CUST_PART_NO2,
                nvl(nvl(l.ATTRIBUTE16,ZEN_GET_WMS_ITEM_F(mtls.segment1,D.ORGANIZATION_ID)),mtls.segment1) as PO_REMARK,
                '' as RMA_NUMBER,
                '0' as ORD_TYPE,
                'TY' as QC_TYPE
         FROM apps.wsh_new_deliveries@SHCTRADING0       d,
              apps.wsh_delivery_assignments@SHCTRADING0 wda,
              apps.wsh_delivery_details@SHCTRADING0     wdd,
              apps.mtl_system_items_b@SHCTRADING0      mtls,
              apps.mtl_item_locations@SHCTRADING0       mil,
              apps.oe_order_headers_all@SHCTRADING0     h,
              apps.oe_order_lines_all@SHCTRADING0       l,
              apps.ZEN_WSH_PICK_SLIP_V@SHCTRADING0      zs,
              apps.hz_cust_accounts@SHCTRADING0         cust_acct,
              apps.hz_parties@SHCTRADING0               party,
              apps.hz_locations@SHCTRADING0             bill_loc,
              apps.hz_party_sites@SHCTRADING0           bill_ps,
              apps.hz_cust_acct_sites_all@SHCTRADING0   bill_cas,
              apps.hz_cust_site_uses_all@SHCTRADING0    bill_su,
              apps.hz_locations@SHCTRADING0             ship_loc,
              apps.hz_party_sites@SHCTRADING0           ship_ps,
              apps.hz_cust_acct_sites_all@SHCTRADING0   ship_cas,
              apps.hz_cust_site_uses_all@SHCTRADING0    ship_su,
              apps.MTL_CUSTOMER_ITEMS@SHCTRADING0       MCI
         WHERE
             D.ORGANIZATION_ID = 209
           AND h.SOLD_TO_ORG_ID not in (39440)
           AND D.STATUS_CODE = 'CL'
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
           AND mtls.organization_id = d.organization_id
           AND zs.FROM_SUBINVENTORY_CODE = '外存倉'
           AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
           AND mil.attribute3='JIT'
           AND TRUNC(D.CONFIRM_DATE) BETWEEN TRUNC(SYSDATE) AND TRUNC(SYSDATE+30)
           AND NOT EXISTS
             (SELECT 1
              FROM ZEN_B2B_JSON_SO
              WHERE ID = D.name || '-' || l.line_id
                AND (STATUS = (CASE
                                   WHEN d.name IS NULL
                                       OR d.attribute12 IS NULL
                                       OR
                                        (SELECT ffvt.description
                                         FROM HZ_CUST_SITE_USES_ALL@SHCTRADING0 rsua,
                                              jtf_rs_salesreps@SHCTRADING0      jrs,
                                              gl_code_combinations@SHCTRADING0  gcc,
                                              fnd_id_flex_segments@SHCTRADING0  fifs,
                                              fnd_flex_values@SHCTRADING0      ffv,
                                              fnd_flex_values_tl@SHCTRADING0    ffvt
                                         WHERE rsua.site_use_code = 'SHIP_TO'
                                           AND rsua.site_use_id = ship_su.site_use_id
                                           AND jrs.salesrep_id = rsua.primary_salesrep_id
                                           AND gcc.code_combination_id = jrs.gl_id_rev
                                           AND fifs.id_flex_num = gcc.chart_of_accounts_id
                                           AND fifs.segment_name = '部門別'
                                           AND ffv.flex_value_set_id =
                                               fifs.flex_value_set_id
                                           AND ffv.flex_value = gcc.segment2
                                           AND ffvt.flex_value_id = ffv.flex_value_id
                                           AND fifs.id_flex_code = 'GL#') IS NULL
                                       OR party.party_name IS NULL
                                       OR bill_loc.address1 IS NULL
                                       OR ship_ps.party_site_number IS NULL
                                       OR NVL(d.attribute8, ship_loc.address1) IS NULL
                                       OR mtls.segment1 ||
                                          DECODE(l.attribute8,
                                                 NULL,
                                                 NULL,
                                                 ' #' || l.attribute8 || '#') ||
                                          DECODE(mtls.attribute6,
                                                 'Y',
                                                 '(' || mtls.description || ')',
                                                 NULL) IS NULL
                                       OR
                                        (SELECT mcb.segment1
                                         FROM mtl_item_categories@SHCTRADING0 mic,
                                              mtl_categories_b@SHCTRADING0    mcb
                                         WHERE mic.category_set_id = 1
                                           AND mic.category_id = mcb.category_id
                                           AND mic.organization_id = mtls.organization_id
                                           AND mic.inventory_item_id =
                                               mtls.inventory_item_id) IS NULL
                                       OR DECODE(mtls.segment1,
                                                 '銷貨折讓',
                                                 wdd.requested_quantity * -1,
                                                 wdd.requested_quantity) IS NULL
                                       OR mtls.attribute1 IS NULL
                                       OR zs.FROM_SUBINVENTORY_CODE IS NULL
                                       OR mil.segment1 IS NULL
                                       OR mil.segment2 IS NULL
                                       THEN
                                       'N'
                                   ELSE
                                       'W'
                  END) OR STATUS = 'S'))

         ORDER BY ORDER_NO, ID );
end;
