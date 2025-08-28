BEGIN
INSERT INTO JIT_INV_EXCHANGE_HEADER
(
    external_id,
    external_no,
    wh_name,
    storer,
    exchange_type,
    apply_date,
    ref_no,
    remark
)
SELECT DISTINCT
    mmt.ATTRIBUTE11 as external_id,
    mmt.TRANSACTION_SOURCE_NAME as external_no,
    'GT' as WhName,
    DECODE(mmt.organization_id, 169, 'ZCSH', 'ZTSH') as Storer,
    'Combine' as ExchangeType,
    CURRENT_TIMESTAMP as ApplyDate,
    WMT.kind_memo as RefNo,
    mmt.ATTRIBUTE4
FROM MTL_MATERIAL_TRANSACTIONS@ZENPROD mmt,
     zenoanew.WEB017_MIXINOUT@hr wmt,
     apps.mtl_item_locations@ZENPROD mil
WHERE mmt.ORGANIZATION_ID in (169, 209)
  AND mmt.attribute11 = WMT.seq_misform
  AND wmt.CPNYID = 'SH'
  AND wmt.APPLY_TYPE = 'A0009'
  AND wmt.MR_NO IS NOT NULL
  AND mmt.TRANSACTION_TYPE_ID = 150
  AND mmt.organization_id = mil.organization_id
  AND mmt.subinventory_code = mil.subinventory_code (+)
  AND mmt.LOCATOR_ID = mil.inventory_location_id (+)
  AND TRUNC(mmt.transaction_date) = TRUNC(sysdate)
  AND MMT.SUBINVENTORY_CODE = '外存倉'
  AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
  AND MIL.attribute3 = 'JIT'
  AND EXISTS (SELECT 1 FROM zenoanew.WEB017_MIXINOUT@hr wmt1
              WHERE WMT.kind_memo = wmt1.seq_misform
                AND wmt1.CPNYID = 'SH'
                AND wmt1.APPLY_TYPE = 'A0008'
                AND WMT1.MI_NO IS NOT NULL)
  AND NOT EXISTS(SELECT 1
                 FROM JIT_INV_EXCHANGE_HEADER
                 WHERE external_ID = mmt.ATTRIBUTE11);

INSERT INTO JIT_INV_EXCHANGE_FINAL
(
    header_id,
    product,
    qty,
    mf_sku,
    zone_name
)
SELECT
    TRX_IN.HEADER_ID,
    TRX_IN.ITEM_NO,
    TRX_IN.QTY,
    TRX_IN.MF_SKU,
    TRX_IN.subinventory_code
FROM
    (
        SELECT jieh.header_id,
               mmt.ATTRIBUTE11 TRX_NO,
               substr(mmt.ATTRIBUTE12, 2, 4) LINE_NO,
               msi.SEGMENT1 ITEM_NO,
               mmt.TRANSACTION_QUANTITY QTY,
               nvl(nvl(wmt.VENDOR_MATERIAL_INFO, ZEN_GET_WMS_ITEM_F(msi.SEGMENT1,mmt.ORGANIZATION_ID)), msi.SEGMENT1) as mf_sku,
               DECODE(mil.segment1, '', '', (mil.segment1 || '.' || mil.segment2)) as subinventory_code
        FROM MTL_MATERIAL_TRANSACTIONS@ZENPROD mmt,
             apps.mtl_item_locations@ZENPROD mil,
             mtl_system_items_b@ZENPROD msi,
             JIT_INV_EXCHANGE_HEADER JIEH,
             zenoanew.WEB017_MIXINOUT_D@hr wmt
        WHERE mmt.ORGANIZATION_ID in (169, 209)
          AND mmt.TRANSACTION_TYPE_ID = 150
          AND mmt.INVENTORY_ITEM_ID = msi.INVENTORY_ITEM_ID
          AND mmt.ORGANIZATION_ID = msi.ORGANIZATION_ID
          AND mmt.organization_id = mil.organization_id (+)
          AND mmt.subinventory_code = mil.subinventory_code(+)
          AND mmt.LOCATOR_ID = mil.inventory_location_id (+)
          AND TRUNC(mmt.transaction_date) = TRUNC(sysdate)
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND mmt.TRANSACTION_QUANTITY > 0
          AND JIEH.EXTERNAL_ID = mmt.ATTRIBUTE11
          AND JIEH.status = 'PENDING'
          AND mmt.ATTRIBUTE11 = WMT.seq_misform
          AND MMT.SOURCE_LINE_ID = WMT.SEQ_NO
    ) TRX_IN
WHERE NOT EXISTS(SELECT 1
                 FROM JIT_INV_EXCHANGE_FINAL
                 WHERE header_id = TRX_IN.header_id);

INSERT INTO JIT_INV_EXCHANGE_MATERIAL
(
    final_id,
    material,
    qty,
    mf_sku,
    zone_name
)
SELECT JIEF.FINAL_ID,
       msi.SEGMENT1 ITEM_NO,
       ABS(mmt.TRANSACTION_QUANTITY) QTY,
       nvl(nvl(wmt.VENDOR_MATERIAL_INFO, ZEN_GET_WMS_ITEM_F(msi.SEGMENT1,mmt.ORGANIZATION_ID)), msi.SEGMENT1) as mf_sku,
       DECODE(mil.segment1, '', '', (mil.segment1 || '.' || mil.segment2)) as subinventory_code
FROM MTL_MATERIAL_TRANSACTIONS@ZENPROD mmt,
     apps.mtl_item_locations@ZENPROD mil,
     mtl_system_items_b@ZENPROD msi,
     JIT_INV_EXCHANGE_HEADER JIEH,
     zenoanew.WEB017_MIXINOUT_D@hr wmt,
     JIT_INV_EXCHANGE_FINAL JIEF
WHERE mmt.ORGANIZATION_ID in (169, 209)
  AND mmt.TRANSACTION_TYPE_ID = 149
  AND mmt.INVENTORY_ITEM_ID = msi.INVENTORY_ITEM_ID
  AND mmt.ORGANIZATION_ID = msi.ORGANIZATION_ID
  AND mmt.organization_id = mil.organization_id (+)
  AND mmt.subinventory_code = mil.subinventory_code(+)
  AND mmt.LOCATOR_ID = mil.inventory_location_id (+)
  AND TRUNC(mmt.transaction_date) = TRUNC(sysdate)
  AND MMT.SUBINVENTORY_CODE = '外存倉'
  AND (mil.segment1 like '%基通%' OR mil.segment2 like '%基通%')
  AND MIL.attribute3 = 'JIT'
  AND mmt.TRANSACTION_QUANTITY < 0
  AND JIEH.REF_NO = mmt.ATTRIBUTE11
  AND JIEH.status = 'PENDING'
  AND mmt.ATTRIBUTE11 = WMT.seq_misform
  AND MMT.SOURCE_LINE_ID = WMT.SEQ_NO
  AND JIEH.HEADER_ID = JIEF.HEADER_ID
  AND NOT EXISTS(SELECT 1
                 FROM JIT_INV_EXCHANGE_HEADER JEH,
                      JIT_INV_EXCHANGE_FINAL JEF,
                      JIT_INV_EXCHANGE_MATERIAL JIM
                 WHERE JEH.HEADER_ID = JEF.HEADER_ID
                   AND JIEH.header_id = JEH.header_id
                   AND JIEF.FINAL_ID = JEF.FINAL_ID
                   AND JEF.FINAL_ID = JIM.FINAL_ID);
END;
