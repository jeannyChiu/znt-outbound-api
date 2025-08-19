-- 銷退入庫資料預處理 SQL
-- 用於將銷退入庫資料從生產系統插入到 JIT 中介表格
BEGIN
    -- 插入 ASN Header 資料
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
        RTH.ExternalID,
        RTH.ExternalNo,
        RTH.WhName,
        RTH.STORERABBRNAME,
        RTH.priorty,
        RTH.DOCTYPE,
        RTH.BIZTYPE,
        RTH.VOYAGE,
        RTH.BLNO,
        RTH.CASECNT,
        RTH.PALLETCNT,
        RTH.CONTAINERCNT,
        RTH.DESCRIPTIONS,
        RTH.USERDEF1,
        RTH.USERDEF2,
        RTH.USERDEF3,
        RTH.USERDEF4,
        RTH.USERDEF5
    FROM (
        -- PROD2 系統銷退入庫資料 (ZSH/SHC)
        SELECT DISTINCT
            'Z' || TO_CHAR(OH.HEADER_ID) AS ExternalID,
            TO_CHAR(OH.ORDER_NUMBER) AS ExternalNo,
            'GT' AS WhName,
            DECODE(OH.ORG_ID, 168, 'ZCSH', 'ZTSH') AS StorerAbbrName,
            1 AS priorty,
            '一般方式' AS DocType,
            '銷退入庫' AS BizType,
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
        FROM
            OE_ORDER_HEADERS_ALL@PROD2 OH,
            OE_ORDER_LINES_ALL@PROD2 OL,
            OE_TRANSACTION_TYPES_TL@PROD2 OTTL,
            OE_TRANSACTION_TYPES_ALL@PROD2 OTTA,
            MTL_MATERIAL_TRANSACTIONS@PROD2 MMT,
            MTL_ITEM_LOCATIONS@PROD2 MIL,
            mtl_system_items_b@PROD2 MSI
        WHERE OH.HEADER_ID = OL.HEADER_ID
          AND OH.ORG_ID IN (168, 208)
          AND OH.ORDER_TYPE_ID = OTTL.TRANSACTION_TYPE_ID
          AND OH.ORG_ID = OTTA.ORG_ID
          AND OTTL.TRANSACTION_TYPE_ID = OTTA.TRANSACTION_TYPE_ID
          AND (OTTL.NAME LIKE '11%退%' OR OTTL.NAME LIKE '41%一般回轉%')
          AND MMT.TRX_SOURCE_LINE_ID = OL.LINE_ID
          AND MMT.TRANSACTION_TYPE_ID = 15
          AND MMT.TRANSACTION_SOURCE_TYPE_ID = 12
          AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
          AND OH.FLOW_STATUS_CODE = 'CLOSED'
          AND MMT.INVENTORY_ITEM_ID <> 71285
          AND MMT.SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
          AND MMT.ORGANIZATION_ID = MIL.ORGANIZATION_ID
          AND MMT.LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (MIL.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND MSI.ORGANIZATION_ID = MMT.ORGANIZATION_ID
          AND MSI.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID

        UNION ALL

        -- JY 系統領料退回入庫資料
        SELECT DISTINCT
            'J' || TO_CHAR(OH.HEADER_ID) AS ExternalID,
            TO_CHAR(OH.ORDER_NUMBER) AS ExternalNo,
            'GT' AS WhName,
            'ZTSH' AS StorerAbbrName,
            1 AS priorty,
            '一般方式' AS DocType,
            '銷退入庫' AS BizType,
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
        FROM
            OE_ORDER_HEADERS_ALL@jy OH,
            OE_ORDER_LINES_ALL@jy OL,
            OE_TRANSACTION_TYPES_TL@jy OTTL,
            OE_TRANSACTION_TYPES_ALL@jy OTTA,
            MTL_MATERIAL_TRANSACTIONS@jy MMT,
            MTL_ITEM_LOCATIONS@jy MIL,
            mtl_system_items_b@jy MSI
        WHERE OH.HEADER_ID = OL.HEADER_ID
          AND OH.ORG_ID = 370
          AND OH.ORDER_TYPE_ID = OTTL.TRANSACTION_TYPE_ID
          AND OH.ORG_ID = OTTA.ORG_ID
          AND OTTL.TRANSACTION_TYPE_ID = OTTA.TRANSACTION_TYPE_ID
          AND (OTTL.NAME LIKE '11%退%' OR OTTL.NAME LIKE '41%一般回轉%')
          AND MMT.TRX_SOURCE_LINE_ID = OL.LINE_ID
          AND MMT.TRANSACTION_TYPE_ID = 15
          AND MMT.TRANSACTION_SOURCE_TYPE_ID = 12
          AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
          AND OH.FLOW_STATUS_CODE = 'CLOSED'
          AND MMT.SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
          AND MMT.ORGANIZATION_ID = MIL.ORGANIZATION_ID
          AND MMT.LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (MIL.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND MSI.ORGANIZATION_ID = MMT.ORGANIZATION_ID
          AND MSI.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID

        UNION ALL

        -- 海宏微系統領料退回入庫資料
        SELECT DISTINCT
            'S' || TO_CHAR(OH.HEADER_ID) AS ExternalID,
            TO_CHAR(OH.ORDER_NUMBER) AS ExternalNo,
            'GT' AS WhName,
            'ZTSH' AS StorerAbbrName,
            1 AS priorty,
            '一般方式' AS DocType,
            '銷退入庫' AS BizType,
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
        FROM
            OE_ORDER_HEADERS_ALL@SHCTRADING0 OH,
            OE_ORDER_LINES_ALL@SHCTRADING0 OL,
            OE_TRANSACTION_TYPES_TL@SHCTRADING0 OTTL,
            OE_TRANSACTION_TYPES_ALL@SHCTRADING0 OTTA,
            MTL_MATERIAL_TRANSACTIONS@SHCTRADING0 MMT,
            MTL_ITEM_LOCATIONS@SHCTRADING0 MIL,
            mtl_system_items_b@SHCTRADING0 MSI
        WHERE OH.HEADER_ID = OL.HEADER_ID
          AND OH.ORG_ID = 208
          AND OH.ORDER_TYPE_ID = OTTL.TRANSACTION_TYPE_ID
          AND OH.ORG_ID = OTTA.ORG_ID
          AND OTTL.TRANSACTION_TYPE_ID = OTTA.TRANSACTION_TYPE_ID
          AND (OTTL.NAME LIKE '11%退%' OR OTTL.NAME LIKE '41%一般回轉%')
          AND MMT.TRX_SOURCE_LINE_ID = OL.LINE_ID
          AND MMT.TRANSACTION_TYPE_ID = 15
          AND MMT.TRANSACTION_SOURCE_TYPE_ID = 12
          AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
          AND OH.FLOW_STATUS_CODE = 'CLOSED'
          AND MMT.INVENTORY_ITEM_ID <> 71285
          AND MMT.SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
          AND MMT.ORGANIZATION_ID = MIL.ORGANIZATION_ID
          AND MMT.LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (MIL.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND MSI.ORGANIZATION_ID = MMT.ORGANIZATION_ID
          AND MSI.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
    ) RTH
    WHERE NOT EXISTS (
        SELECT 1 
        FROM jit_asn_header jah
        WHERE jah.external_id = RTH.ExternalID
    );

    -- 插入 ASN Line 資料
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
        -- PROD2 系統明細資料
        SELECT 
            jah.header_id,
            OL.LINE_NUMBER || '-' || OL.LINE_ID AS StorerLineNo,
            MSI.SEGMENT1 AS Sku,
            DECODE(OH.ORG_ID, 168, 'ZCSH', 'ZTSH') AS sku_storer_abbr_name,
            msi.DESCRIPTION AS sku_name,
            MSI.SEGMENT1 AS sku_name_e,
            NULL AS spec,
            s1.segment3 AS category,
            MMT.TRANSACTION_QUANTITY AS QtyExpected,
            DECODE(mil.segment1, '', '', (mil.segment1 || '.' || mil.segment2)) AS zone_name,
            msi.attribute16 AS lot_attr01,
            NVL(NVL(ol.ATTRIBUTE16, ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1)), MSI.SEGMENT1) AS lot_attr02
        FROM
            OE_ORDER_HEADERS_ALL@PROD2 OH,
            OE_ORDER_LINES_ALL@PROD2 OL,
            OE_TRANSACTION_TYPES_TL@PROD2 OTTL,
            OE_TRANSACTION_TYPES_ALL@PROD2 OTTA,
            MTL_MATERIAL_TRANSACTIONS@PROD2 MMT,
            MTL_ITEM_LOCATIONS@PROD2 MIL,
            mtl_system_items_b@PROD2 MSI,
            jit_asn_header jah,
            apps.mtl_item_categories@PROD2 w,
            apps.mtl_categories_b@PROD2 s1
        WHERE OH.HEADER_ID = OL.HEADER_ID
          AND OH.ORG_ID IN (168, 208)
          AND OH.ORDER_TYPE_ID = OTTL.TRANSACTION_TYPE_ID
          AND OH.ORG_ID = OTTA.ORG_ID
          AND OTTL.TRANSACTION_TYPE_ID = OTTA.TRANSACTION_TYPE_ID
          AND (OTTL.NAME LIKE '11%退%' OR OTTL.NAME LIKE '41%一般回轉%')
          AND MMT.TRX_SOURCE_LINE_ID = OL.LINE_ID
          AND MMT.TRANSACTION_TYPE_ID = 15
          AND MMT.TRANSACTION_SOURCE_TYPE_ID = 12
          AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
          AND OH.FLOW_STATUS_CODE = 'CLOSED'
          AND MMT.INVENTORY_ITEM_ID <> 71285
          AND MMT.SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
          AND MMT.ORGANIZATION_ID = MIL.ORGANIZATION_ID
          AND MMT.LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (MIL.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND MSI.ORGANIZATION_ID = MMT.ORGANIZATION_ID
          AND MSI.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
          AND jah.external_id = 'Z' || TO_CHAR(OH.HEADER_ID)
          AND jah.status = 'PENDING'
          AND msi.INVENTORY_ITEM_ID = w.inventory_item_id
          AND msi.ORGANIZATION_ID = w.organization_id
          AND w.category_set_id = 1
          AND w.category_id = s1.category_id

        UNION ALL

        -- JY 系統明細資料
        SELECT 
            jah.header_id,
            OL.LINE_NUMBER || '-' || OL.LINE_ID AS StorerLineNo,
            MSI.SEGMENT1 AS Sku,
            'ZTSH' AS sku_storer_abbr_name,
            msi.DESCRIPTION AS sku_name,
            MSI.SEGMENT1 AS sku_name_e,
            NULL AS spec,
            s1.segment3 AS category,
            MMT.TRANSACTION_QUANTITY AS QtyExpected,
            'SHC基通.IT' AS zone_name,
            msi.attribute16 AS lot_attr01,
            NVL(NVL(ol.ATTRIBUTE16, ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1)), MSI.SEGMENT1) AS lot_attr02
        FROM
            OE_ORDER_HEADERS_ALL@jy OH,
            OE_ORDER_LINES_ALL@jy OL,
            OE_TRANSACTION_TYPES_TL@jy OTTL,
            OE_TRANSACTION_TYPES_ALL@jy OTTA,
            MTL_MATERIAL_TRANSACTIONS@jy MMT,
            MTL_ITEM_LOCATIONS@jy MIL,
            mtl_system_items_b@jy MSI,
            jit_asn_header jah,
            apps.mtl_item_categories@jy w,
            apps.mtl_categories_b@jy s1
        WHERE OH.HEADER_ID = OL.HEADER_ID
          AND OH.ORG_ID = 370
          AND OH.ORDER_TYPE_ID = OTTL.TRANSACTION_TYPE_ID
          AND OH.ORG_ID = OTTA.ORG_ID
          AND OTTL.TRANSACTION_TYPE_ID = OTTA.TRANSACTION_TYPE_ID
          AND (OTTL.NAME LIKE '11%退%' OR OTTL.NAME LIKE '41%一般回轉%')
          AND MMT.TRX_SOURCE_LINE_ID = OL.LINE_ID
          AND MMT.TRANSACTION_TYPE_ID = 15
          AND MMT.TRANSACTION_SOURCE_TYPE_ID = 12
          AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
          AND OH.FLOW_STATUS_CODE = 'CLOSED'
          AND MMT.INVENTORY_ITEM_ID <> 71285
          AND MMT.SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
          AND MMT.ORGANIZATION_ID = MIL.ORGANIZATION_ID
          AND MMT.LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (MIL.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND MSI.ORGANIZATION_ID = MMT.ORGANIZATION_ID
          AND MSI.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
          AND jah.external_id = 'J' || TO_CHAR(OH.HEADER_ID)
          AND jah.status = 'PENDING'
          AND msi.INVENTORY_ITEM_ID = w.inventory_item_id
          AND msi.ORGANIZATION_ID = w.organization_id
          AND w.category_set_id = 1
          AND w.category_id = s1.category_id

        UNION ALL

        -- 海宏微系統明細資料
        SELECT 
            jah.header_id,
            OL.LINE_NUMBER || '-' || OL.LINE_ID AS StorerLineNo,
            MSI.SEGMENT1 AS Sku,
            'ZTSH' AS sku_storer_abbr_name,
            msi.DESCRIPTION AS sku_name,
            MSI.SEGMENT1 AS sku_name_e,
            NULL AS spec,
            s1.segment3 AS category,
            MMT.TRANSACTION_QUANTITY AS QtyExpected,
            'SHC基通.IT' AS zone_name,
            msi.attribute16 AS lot_attr01,
            NVL(NVL(ol.ATTRIBUTE16, ZEN_GET_WMS_ITEM_F(MSI.SEGMENT1)), MSI.SEGMENT1) AS lot_attr02
        FROM
            OE_ORDER_HEADERS_ALL@SHCTRADING0 OH,
            OE_ORDER_LINES_ALL@SHCTRADING0 OL,
            OE_TRANSACTION_TYPES_TL@SHCTRADING0 OTTL,
            OE_TRANSACTION_TYPES_ALL@SHCTRADING0 OTTA,
            MTL_MATERIAL_TRANSACTIONS@SHCTRADING0 MMT,
            MTL_ITEM_LOCATIONS@SHCTRADING0 MIL,
            mtl_system_items_b@SHCTRADING0 MSI,
            jit_asn_header jah,
            apps.mtl_item_categories@SHCTRADING0 w,
            apps.mtl_categories_b@SHCTRADING0 s1
        WHERE OH.HEADER_ID = OL.HEADER_ID
          AND OH.ORG_ID = 208
          AND OH.ORDER_TYPE_ID = OTTL.TRANSACTION_TYPE_ID
          AND OH.ORG_ID = OTTA.ORG_ID
          AND OTTL.TRANSACTION_TYPE_ID = OTTA.TRANSACTION_TYPE_ID
          AND (OTTL.NAME LIKE '11%退%' OR OTTL.NAME LIKE '41%一般回轉%')
          AND MMT.TRX_SOURCE_LINE_ID = OL.LINE_ID
          AND MMT.TRANSACTION_TYPE_ID = 15
          AND MMT.TRANSACTION_SOURCE_TYPE_ID = 12
          AND TRUNC(MMT.TRANSACTION_DATE) = TRUNC(SYSDATE)
          AND OH.FLOW_STATUS_CODE = 'CLOSED'
          AND MMT.INVENTORY_ITEM_ID <> 71285
          AND MMT.SUBINVENTORY_CODE = MIL.SUBINVENTORY_CODE
          AND MMT.ORGANIZATION_ID = MIL.ORGANIZATION_ID
          AND MMT.LOCATOR_ID = MIL.INVENTORY_LOCATION_ID
          AND MMT.SUBINVENTORY_CODE = '外存倉'
          AND (MIL.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
          AND MIL.attribute3 = 'JIT'
          AND MSI.ORGANIZATION_ID = MMT.ORGANIZATION_ID
          AND MSI.INVENTORY_ITEM_ID = MMT.INVENTORY_ITEM_ID
          AND jah.external_id = 'S' || TO_CHAR(OH.HEADER_ID)
          AND jah.status = 'PENDING'
          AND msi.INVENTORY_ITEM_ID = w.inventory_item_id
          AND msi.ORGANIZATION_ID = w.organization_id
          AND w.category_set_id = 1
          AND w.category_id = s1.category_id
    ) RTL
    WHERE NOT EXISTS (
        SELECT 1
        FROM jit_asn_line
        WHERE header_id = RTL.header_id
    );
END;