BEGIN
INSERT INTO jit_asn_header (
    external_id,
    external_no,
    wh_name,
    storer_abbr_name,
    priority,
    doc_type,
    biz_type,
    voyage,
    bl_no,
    case_cnt,
    pallet_cnt,
    container_cnt,
    descriptions,
    user_def1,
    user_def2,
    user_def3,
    user_def4,
    user_def5
)
SELECT
    JTT.ExternalID,
    JTT.ExternalNo,
    JTT.WhName,
    JTT.STORERABBRNAME,
    JTT.priorty,
    JTT.DOCTYPE,
    JTT.BIZTYPE,
    JTT.VOYAGE,
    JTT.BLNO,
    JTT.CASECNT,
    JTT.PALLETCNT,
    JTT.CONTAINERCNT,
    JTT.DESCRIPTIONS,
    JTT.USERDEF1,
    JTT.USERDEF2,
    JTT.USERDEF3,
    JTT.USERDEF4,
    JTT.USERDEF5
FROM (
         SELECT DISTINCT
             'Z' || ZMTH.REQUEST_NUMBER AS ExternalID,
             ZMTH.HEADER_ID AS ExternalNo,
             'GT' AS WhName,
             DECODE(ZMTH.ORGANIZATION_ID,169,'ZCSH','ZTSH') AS StorerAbbrName,
             1 AS priorty,
             '一般方式' AS DocType,
             '移倉入庫' AS BizType,
             NULL AS Voyage,
             NULL AS BlNo,
             NULL AS CaseCnt,
             NULL AS PalletCnt,
             NULL AS ContainerCnt,
             NULL AS Descriptions,
             NULL AS UserDef1,
             NULL AS UserDef2,
             NULL AS UserDef3,
             NULL AS UserDef4,
             NULL AS UserDef5
         FROM ZEN_MTL_TXN_REQUEST_HEADERS@ZENPROD ZMTH,
              ZEN_MTL_TXN_REQUEST_LINES@ZENPROD ZMTL,
              MTL_MATERIAL_TRANSACTIONS@ZENPROD MMT,
              MTL_ITEM_LOCATIONS@ZENPROD MIL,
              MTL_ITEM_LOCATIONS@ZENPROD MIL1
         WHERE ZMTH.HEADER_ID = ZMTL.HEADER_ID
           AND ZMTH.ORGANIZATION_ID IN (169,209)
           AND ZMTL.LINE_STATUS = 5
           AND ZMTH.HEADER_STATUS = 3
           AND ZMTL.LINE_ID = MMT.SOURCE_LINE_ID
           AND ZMTL.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
           AND ZMTL.ORGANIZATION_ID = MMT.ORGANIZATION_ID
           AND MMT.TRANSACTION_TYPE_ID = 2
           AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
           AND ZMTL.ORGANIZATION_ID = MIL.ORGANIZATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
           AND ZMTL.TO_LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = '外存倉'
           AND ZMTL.TO_SUBINVENTORY_CODE = MMT.SUBINVENTORY_CODE
           AND (MIL.SEGMENT1 LIKE '%基通%' OR MIL.SEGMENT2 LIKE '%基通%')
           AND MIL.attribute3 = 'JIT'
           AND ZMTL.ORGANIZATION_ID = MIL1.ORGANIZATION_ID(+)
           AND ZMTL.FROM_SUBINVENTORY_CODE = MIL1.SUBINVENTORY_CODE(+)
           AND ZMTL.FROM_LOCATOR_ID = MIL1.INVENTORY_LOCATION_ID (+)
           AND NVL(MIL1.attribute3,'X') != 'JIT'
          AND MMT.TRANSACTION_QUANTITY > 0

         UNION ALL

         SELECT DISTINCT
             'J'|| ZMTH.REQUEST_NUMBER AS ExternalID,
             ZMTH.HEADER_ID AS ExternalNo,
             'GT' AS WhName,
             'ZTSH' AS StorerAbbrName,
             1 AS priorty,
             '一般方式' AS DocType,
             '移倉入庫' AS BizType,
             NULL AS Voyage,
             NULL AS BlNo,
             NULL AS CaseCnt,
             NULL AS PalletCnt,
             NULL AS ContainerCnt,
             NULL AS Descriptions,
             NULL AS UserDef1,
             NULL AS UserDef2,
             NULL AS UserDef3,
             NULL AS UserDef4,
             NULL AS UserDef5
         FROM ZEN_MTL_TXN_REQUEST_HEADERS@jy ZMTH,
             ZEN_MTL_TXN_REQUEST_LINES@jy ZMTL,
             MTL_MATERIAL_TRANSACTIONS@jy MMT,
             MTL_ITEM_LOCATIONS@jy MIL,
             MTL_ITEM_LOCATIONS@jy MIL1
         WHERE ZMTH.HEADER_ID = ZMTL.HEADER_ID
           AND ZMTH.ORGANIZATION_ID IN (371)
           AND ZMTL.LINE_STATUS = 5
           AND ZMTH.HEADER_STATUS = 3
           AND ZMTL.LINE_ID = MMT.SOURCE_LINE_ID
           AND ZMTL.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
           AND ZMTL.ORGANIZATION_ID = MMT.ORGANIZATION_ID
           AND MMT.TRANSACTION_TYPE_ID = 2
           AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
           AND ZMTL.ORGANIZATION_ID = MIL.ORGANIZATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
           AND ZMTL.TO_LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = '外存倉'
           AND ZMTL.TO_SUBINVENTORY_CODE = MMT.SUBINVENTORY_CODE
           AND (MIL.SEGMENT1 LIKE '%基通%' OR MIL.SEGMENT2 LIKE '%基通%')
           AND MIL.attribute3 = 'JIT'
           AND MMT.TRANSACTION_QUANTITY > 0
           AND ZMTL.ORGANIZATION_ID = MIL1.ORGANIZATION_ID(+)
           AND ZMTL.FROM_SUBINVENTORY_CODE = MIL1.SUBINVENTORY_CODE(+)
           AND ZMTL.FROM_LOCATOR_ID = MIL1.INVENTORY_LOCATION_ID (+)
           AND NVL(MIL1.attribute3,'X') != 'JIT'

         UNION ALL

         SELECT DISTINCT
             'S'|| ZMTH.REQUEST_NUMBER AS ExternalID,
             ZMTH.HEADER_ID AS ExternalNo,
             'GT' AS WhName,
             'ZTSH' AS StorerAbbrName,
             1 AS priorty,
             '一般方式' AS DocType,
             '移倉入庫' AS BizType,
             NULL AS Voyage,
             NULL AS BlNo,
             NULL AS CaseCnt,
             NULL AS PalletCnt,
             NULL AS ContainerCnt,
             NULL AS Descriptions,
             NULL AS UserDef1,
             NULL AS UserDef2,
             NULL AS UserDef3,
             NULL AS UserDef4,
             NULL AS UserDef5
         FROM ZEN_MTL_TXN_REQUEST_HEADERS@SHCTRADING0 ZMTH,
             ZEN_MTL_TXN_REQUEST_LINES@SHCTRADING0 ZMTL,
             MTL_MATERIAL_TRANSACTIONS@SHCTRADING0 MMT,
             MTL_ITEM_LOCATIONS@SHCTRADING0 MIL,
             MTL_ITEM_LOCATIONS@SHCTRADING0 MIL1
         WHERE ZMTH.HEADER_ID = ZMTL.HEADER_ID
           AND ZMTH.ORGANIZATION_ID IN (209)
           AND ZMTL.LINE_STATUS = 5
           AND ZMTH.HEADER_STATUS = 3
           AND ZMTL.LINE_ID = MMT.SOURCE_LINE_ID
           AND ZMTL.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
           AND ZMTL.ORGANIZATION_ID = MMT.ORGANIZATION_ID
           AND MMT.TRANSACTION_TYPE_ID = 2
           AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
           AND ZMTL.ORGANIZATION_ID = MIL.ORGANIZATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
           AND ZMTL.TO_LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = '外存倉'
           AND ZMTL.TO_SUBINVENTORY_CODE = MMT.SUBINVENTORY_CODE
           AND (MIL.SEGMENT1 LIKE '%基通%' OR MIL.SEGMENT2 LIKE '%基通%')
           AND MMT.TRANSACTION_QUANTITY > 0
           AND MIL.attribute3 = 'JIT'
           AND ZMTL.ORGANIZATION_ID = MIL1.ORGANIZATION_ID(+)
           AND ZMTL.FROM_SUBINVENTORY_CODE = MIL1.SUBINVENTORY_CODE(+)
           AND ZMTL.FROM_LOCATOR_ID = MIL1.INVENTORY_LOCATION_ID (+)
           AND NVL(MIL1.attribute3,'X') != 'JIT'
     ) JTT
WHERE NOT EXISTS (
    SELECT 1
    FROM jit_asn_header JAH
    WHERE JAH.EXTERNAL_ID = JTT.ExternalID
);

INSERT INTO jit_asn_line (
    header_id,
    storer_line_no,
    sku,
    sku_storer_abbr_name,
    sku_name,
    sku_name_e,
    spec,
    category,
    qty_expected,
    zone_name,
    lot_attr01,
    lot_attr02
)
SELECT
    HEADER_ID,
    STORERLINENO,
    SKU,
    SKU_STORER_ABBR_NAME,
    SKU_NAME,
    SKU_NAME_E,
    SPEC,
    CATEGORY,
    QTYEXPECTED,
    zone_name,
    LOT_ATTR01,
    LOT_ATTR02
FROM (
         SELECT
             jah.header_id,
             ZMTH.HEADER_ID || '-' || ZMTL.LINE_ID AS StorerLineNo,
             MSI.SEGMENT1 AS Sku,
             DECODE(ZMTH.ORGANIZATION_ID,169,'ZCSH','ZTSH') AS sku_storer_abbr_name,
             MSI.DESCRIPTION AS sku_name,
             MSI.SEGMENT1 AS sku_name_e,
             NULL AS spec,
             s1.segment3 AS category,
             MMT.TRANSACTION_QUANTITY AS QtyExpected,
             DECODE(MIL.segment1, '', '', (MIL.segment1 || '.' || MIL.segment2)) AS zone_name,
             MSI.attribute16 AS lot_attr01,
             NVL(NVL(ZMTL.VENDOR_MATERIAL_INFO,ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1,ZMTH.ORGANIZATION_ID)),MSI.SEGMENT1) AS lot_attr02
         FROM ZEN_MTL_TXN_REQUEST_HEADERS@ZENPROD ZMTH,
              ZEN_MTL_TXN_REQUEST_LINES@ZENPROD ZMTL,
              MTL_MATERIAL_TRANSACTIONS@ZENPROD MMT,
              MTL_ITEM_LOCATIONS@ZENPROD MIL,
              mtl_system_items_b@ZENPROD MSI,
              jit_asn_header jah,
              apps.mtl_item_categories@ZENPROD w,
              apps.mtl_categories_b@ZENPROD s1,
              MTL_ITEM_LOCATIONS@ZENPROD MIL1
         WHERE ZMTH.HEADER_ID = ZMTL.HEADER_ID
           AND ZMTH.ORGANIZATION_ID IN (169,209)
           AND ZMTL.LINE_STATUS = 5
           AND ZMTH.HEADER_STATUS = 3
           AND ZMTL.LINE_ID = MMT.SOURCE_LINE_ID
           AND ZMTL.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
           AND ZMTL.ORGANIZATION_ID = MMT.ORGANIZATION_ID
           AND MMT.TRANSACTION_TYPE_ID = 2
           AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
           AND ZMTL.ORGANIZATION_ID = MIL.ORGANIZATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
           AND ZMTL.TO_LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = '外存倉'
           AND ZMTL.TO_SUBINVENTORY_CODE = MMT.SUBINVENTORY_CODE
           AND (MIL.SEGMENT1 LIKE '%基通%' OR MIL.SEGMENT2 LIKE '%基通%')
           AND MIL.attribute3 = 'JIT'
           AND ZMTL.ORGANIZATION_ID = MIL1.ORGANIZATION_ID(+)
           AND ZMTL.FROM_SUBINVENTORY_CODE = MIL1.SUBINVENTORY_CODE(+)
           AND ZMTL.FROM_LOCATOR_ID = MIL1.INVENTORY_LOCATION_ID (+)
           AND NVL(MIL1.attribute3,'X') != 'JIT'
          AND MMT.INVENTORY_ITEM_ID = MSI.INVENTORY_ITEM_ID
          AND MMT.ORGANIZATION_ID = MSI.ORGANIZATION_ID
          AND MMT.TRANSACTION_QUANTITY > 0
          AND MSI.INVENTORY_ITEM_ID = w.inventory_item_id
          AND MSI.ORGANIZATION_ID = w.organization_id
          AND w.category_set_id = 1
          AND w.category_id = s1.category_id
          AND jah.external_id = 'Z' || ZMTH.REQUEST_NUMBER
          AND jah.status = 'PENDING'

         UNION ALL

         SELECT
             jah.header_id,
             ZMTH.HEADER_ID || '-' || ZMTL.LINE_ID AS StorerLineNo,
             MSI.SEGMENT1 AS Sku,
             'ZTSH' AS sku_storer_abbr_name,
             MSI.DESCRIPTION AS sku_name,
             MSI.SEGMENT1 AS sku_name_e,
             NULL AS spec,
             s1.segment3 AS category,
             MMT.TRANSACTION_QUANTITY AS QtyExpected,
             'SHC基通.IT' AS zone_name,
             MSI.attribute16 AS lot_attr01,
             NVL(NVL(ZMTL.VENDOR_MATERIAL_INFO,ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1,ZMTH.ORGANIZATION_ID)),MSI.SEGMENT1) AS lot_attr02
         FROM ZEN_MTL_TXN_REQUEST_HEADERS@jy ZMTH,
             ZEN_MTL_TXN_REQUEST_LINES@jy ZMTL,
             MTL_MATERIAL_TRANSACTIONS@jy MMT,
             MTL_ITEM_LOCATIONS@jy MIL,
             mtl_system_items_b@jy MSI,
             jit_asn_header jah,
             apps.mtl_item_categories@jy w,
             apps.mtl_categories_b@jy s1,
             MTL_ITEM_LOCATIONS@jy MIL1
         WHERE ZMTH.HEADER_ID = ZMTL.HEADER_ID
           AND ZMTH.ORGANIZATION_ID IN (371)
           AND ZMTL.LINE_STATUS = 5
           AND ZMTH.HEADER_STATUS = 3
           AND ZMTL.LINE_ID = MMT.SOURCE_LINE_ID
           AND ZMTL.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
           AND ZMTL.ORGANIZATION_ID = MMT.ORGANIZATION_ID
           AND MMT.TRANSACTION_TYPE_ID = 2
           AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
           AND ZMTL.ORGANIZATION_ID = MIL.ORGANIZATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
           AND ZMTL.TO_LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = '外存倉'
           AND ZMTL.TO_SUBINVENTORY_CODE = MMT.SUBINVENTORY_CODE
           AND (MIL.SEGMENT1 LIKE '%基通%' OR MIL.SEGMENT2 LIKE '%基通%')
           AND MIL.attribute3 = 'JIT'
           AND ZMTL.ORGANIZATION_ID = MIL1.ORGANIZATION_ID(+)
           AND ZMTL.FROM_SUBINVENTORY_CODE = MIL1.SUBINVENTORY_CODE(+)
           AND ZMTL.FROM_LOCATOR_ID = MIL1.INVENTORY_LOCATION_ID (+)
           AND NVL(MIL1.attribute3,'X') != 'JIT'
           AND MMT.INVENTORY_ITEM_ID = MSI.INVENTORY_ITEM_ID
           AND MMT.ORGANIZATION_ID = MSI.ORGANIZATION_ID
           AND MMT.TRANSACTION_QUANTITY > 0
           AND MSI.INVENTORY_ITEM_ID = w.inventory_item_id
           AND MSI.ORGANIZATION_ID = w.organization_id
           AND w.category_set_id = 1
           AND w.category_id = s1.category_id
           AND jah.external_id = 'J' || ZMTH.REQUEST_NUMBER
           AND jah.status = 'PENDING'

         UNION ALL

         SELECT
             jah.header_id,
             ZMTH.HEADER_ID || '-' || ZMTL.LINE_ID AS StorerLineNo,
             MSI.SEGMENT1 AS Sku,
             'ZTSH' AS sku_storer_abbr_name,
             MSI.DESCRIPTION AS sku_name,
             MSI.SEGMENT1 AS sku_name_e,
             NULL AS spec,
             s1.segment3 AS category,
             MMT.TRANSACTION_QUANTITY AS QtyExpected,
             'SHC基通.IT' AS zone_name,
             MSI.attribute16 AS lot_attr01,
             NVL(NVL(ZMTL.VENDOR_MATERIAL_INFO,ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1,ZMTH.ORGANIZATION_ID)),MSI.SEGMENT1) AS lot_attr02
         FROM ZEN_MTL_TXN_REQUEST_HEADERS@SHCTRADING0 ZMTH,
             ZEN_MTL_TXN_REQUEST_LINES@SHCTRADING0 ZMTL,
             MTL_MATERIAL_TRANSACTIONS@SHCTRADING0 MMT,
             MTL_ITEM_LOCATIONS@SHCTRADING0 MIL,
             mtl_system_items_b@SHCTRADING0 MSI,
             jit_asn_header jah,
             apps.mtl_item_categories@SHCTRADING0 w,
             apps.mtl_categories_b@SHCTRADING0 s1,
             MTL_ITEM_LOCATIONS@SHCTRADING0 MIL1
         WHERE ZMTH.HEADER_ID = ZMTL.HEADER_ID
           AND ZMTH.ORGANIZATION_ID IN (209)
           AND ZMTL.LINE_STATUS = 5
           AND ZMTH.HEADER_STATUS = 3
           AND ZMTL.LINE_ID = MMT.SOURCE_LINE_ID
           AND ZMTL.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
           AND ZMTL.ORGANIZATION_ID = MMT.ORGANIZATION_ID
           AND MMT.TRANSACTION_TYPE_ID = 2
           AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
           AND ZMTL.ORGANIZATION_ID = MIL.ORGANIZATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
           AND ZMTL.TO_LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
           AND ZMTL.TO_SUBINVENTORY_CODE = '外存倉'
           AND ZMTL.TO_SUBINVENTORY_CODE = MMT.SUBINVENTORY_CODE
           AND (MIL.SEGMENT1 LIKE '%基通%' OR MIL.SEGMENT2 LIKE '%基通%')
           AND MIL.attribute3 = 'JIT'
           AND ZMTL.ORGANIZATION_ID = MIL1.ORGANIZATION_ID(+)
           AND ZMTL.FROM_SUBINVENTORY_CODE = MIL1.SUBINVENTORY_CODE(+)
           AND ZMTL.FROM_LOCATOR_ID = MIL1.INVENTORY_LOCATION_ID (+)
           AND NVL(MIL1.attribute3,'X') != 'JIT'
           AND MMT.INVENTORY_ITEM_ID = MSI.INVENTORY_ITEM_ID
           AND MMT.ORGANIZATION_ID = MSI.ORGANIZATION_ID
           AND MMT.TRANSACTION_QUANTITY > 0
           AND MSI.INVENTORY_ITEM_ID = w.inventory_item_id
           AND MSI.ORGANIZATION_ID = w.organization_id
           AND w.category_set_id = 1
           AND w.category_id = s1.category_id
           AND jah.external_id = 'S' || ZMTH.REQUEST_NUMBER
           AND jah.status = 'PENDING'
     ) JTL
WHERE NOT EXISTS (
    SELECT 1
    FROM jit_asn_line
    WHERE header_id = JTL.header_id
);

END;
