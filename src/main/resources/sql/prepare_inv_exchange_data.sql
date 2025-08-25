BEGIN
    INSERT INTO JIT_INV_EXCHANGE_HEADER
    (
        EXTERNAL_ID,
        EXTERNAL_NO,
        WH_NAME,
        STORER,
        EXCHANGE_TYPE,
        APPLY_DATE,
        REF_NO,
        REMARK
    )
    SELECT DISTINCT
        mmt.ATTRIBUTE11 AS EXTERNAL_ID,
        mmt.ATTRIBUTE11 AS EXTERNAL_NO,
        'GT' AS WH_NAME,
        DECODE(mmt.ORGANIZATION_ID, 169, 'ZCSH', 'ZTSH') AS STORER,
        'Exchange' AS EXCHANGE_TYPE,
        CURRENT_TIMESTAMP AS APPLY_DATE,
        mmt.ATTRIBUTE11 AS REF_NO,
        mmt.ATTRIBUTE4 AS REMARK
    FROM 
        MTL_MATERIAL_TRANSACTIONS@PROD2 mmt,
        APPS.MTL_ITEM_LOCATIONS@PROD2 mil
    WHERE 
        mmt.ORGANIZATION_ID IN (169, 209)
        AND mmt.TRANSACTION_TYPE_ID IN (125, 124)
        AND mmt.ORGANIZATION_ID = mil.ORGANIZATION_ID(+)
        AND mmt.SUBINVENTORY_CODE = mil.SUBINVENTORY_CODE(+)
        AND mmt.LOCATOR_ID = mil.INVENTORY_LOCATION_ID(+)
        AND TRUNC(mmt.TRANSACTION_DATE) = TRUNC(SYSDATE)
        AND mmt.SUBINVENTORY_CODE = '外存倉'
        AND (mil.SEGMENT1 LIKE '%基通%' OR mil.SEGMENT2 LIKE '%基通%')
        AND mil.ATTRIBUTE3 = 'JIT'
        AND NOT EXISTS (
            SELECT 1
            FROM JIT_INV_EXCHANGE_HEADER
            WHERE EXTERNAL_ID = mmt.ATTRIBUTE11
        );

    INSERT INTO JIT_INV_EXCHANGE_FINAL
    (
        HEADER_ID,
        PRODUCT,
        QTY,
        MF_SKU,
        ZONE_NAME
    )
    SELECT
        TRX_IN.HEADER_ID,
        TRX_IN.ITEM_NO,
        TRX_IN.QTY,
        TRX_IN.MF_SKU,
        TRX_IN.SUBINVENTORY_CODE
    FROM
        (
            SELECT 
                jieh.HEADER_ID,
                mmt.ATTRIBUTE11 AS TRX_NO,
                SUBSTR(mmt.ATTRIBUTE12, 2, 4) AS LINE_NO,
                msi.SEGMENT1 AS ITEM_NO,
                mmt.TRANSACTION_QUANTITY AS QTY,
                DECODE(mil.SEGMENT1, '', '', 
                    (mil.SEGMENT1 || '.' || mil.SEGMENT2)) AS SUBINVENTORY_CODE,
                NVL(NVL(wmt.VENDOR_MATERIAL_INFO, ZEN_GET_WMS_ITEM_F(msi.SEGMENT1,mmt.ORGANIZATION_ID)), msi.SEGMENT1) AS MF_SKU
            FROM 
                MTL_MATERIAL_TRANSACTIONS@PROD2 mmt,
                APPS.MTL_ITEM_LOCATIONS@PROD2 mil,
                MTL_SYSTEM_ITEMS_B@PROD2 msi,
                JIT_INV_EXCHANGE_HEADER jieh,
                ZENOANEW.WEB017_MIXINOUT_D@ZENOADBT1ZENWMSHR wmt
            WHERE 
                mmt.ORGANIZATION_ID IN (169, 209)
                AND mmt.TRANSACTION_TYPE_ID = 124
                AND mmt.INVENTORY_ITEM_ID = msi.INVENTORY_ITEM_ID
                AND mmt.ORGANIZATION_ID = msi.ORGANIZATION_ID
                AND mmt.ORGANIZATION_ID = mil.ORGANIZATION_ID(+)
                AND mmt.SUBINVENTORY_CODE = mil.SUBINVENTORY_CODE(+)
                AND mmt.LOCATOR_ID = mil.INVENTORY_LOCATION_ID(+)
                AND mmt.TRANSACTION_DATE > TO_DATE('2025/05/01', 'YYYY/MM/DD')
                AND TRUNC(mmt.TRANSACTION_DATE) = TRUNC(SYSDATE)
                AND mmt.SUBINVENTORY_CODE = '外存倉'
                AND (mil.SEGMENT1 LIKE '%基通%' OR mil.SEGMENT2 LIKE '%基通%')
                AND mil.ATTRIBUTE3 = 'JIT'
                AND mmt.TRANSACTION_QUANTITY > 0
                AND mmt.ATTRIBUTE11 = wmt.SEQ_MISFORM
                AND mmt.SOURCE_LINE_ID = wmt.SEQ_NO
                AND jieh.EXTERNAL_ID = mmt.ATTRIBUTE11
                AND jieh.STATUS = 'PENDING'
        ) TRX_IN,
        (
            SELECT 
                mmt1.ATTRIBUTE11 AS TRX_NO1,
                SUBSTR(mmt1.ATTRIBUTE12, 2, 4) AS LINE_NO1,
                msi1.SEGMENT1 AS ITEM_NO1,
                ABS(mmt1.TRANSACTION_QUANTITY) AS QTY1,
                jieh.HEADER_ID
            FROM 
                MTL_MATERIAL_TRANSACTIONS@PROD2 mmt1,
                MTL_SYSTEM_ITEMS_B@PROD2 msi1,
                APPS.MTL_ITEM_LOCATIONS@PROD2 mil1,
                JIT_INV_EXCHANGE_HEADER jieh
            WHERE 
                mmt1.ORGANIZATION_ID IN (169, 209)
                AND mmt1.TRANSACTION_TYPE_ID = 125
                AND mmt1.INVENTORY_ITEM_ID = msi1.INVENTORY_ITEM_ID
                AND mmt1.ORGANIZATION_ID = msi1.ORGANIZATION_ID
                AND TRUNC(mmt1.TRANSACTION_DATE) = TRUNC(SYSDATE)
                AND mmt1.ORGANIZATION_ID = mil1.ORGANIZATION_ID(+)
                AND mmt1.SUBINVENTORY_CODE = mil1.SUBINVENTORY_CODE(+)
                AND mmt1.LOCATOR_ID = mil1.INVENTORY_LOCATION_ID(+)
                AND mmt1.SUBINVENTORY_CODE = '外存倉'
                AND (mil1.SEGMENT1 LIKE '%基通%' OR mil1.SEGMENT2 LIKE '%基通%')
                AND mil1.ATTRIBUTE3 = 'JIT'
                AND mmt1.TRANSACTION_QUANTITY < 0
                AND jieh.EXTERNAL_ID = mmt1.ATTRIBUTE11
                AND jieh.STATUS = 'PENDING'
        ) TRX_OUT
    WHERE 
        TRX_IN.TRX_NO = TRX_OUT.TRX_NO1
        AND TRX_IN.LINE_NO = TRX_OUT.LINE_NO1
        AND TRX_IN.HEADER_ID = TRX_OUT.HEADER_ID
        AND NOT EXISTS (
            SELECT 1
            FROM JIT_INV_EXCHANGE_FINAL
            WHERE HEADER_ID = TRX_IN.HEADER_ID
        );

    INSERT INTO JIT_INV_EXCHANGE_MATERIAL
    (
        FINAL_ID,
        MATERIAL,
        QTY,
        MF_SKU,
        ZONE_NAME
    )
    SELECT
        jief.FINAL_ID,
        TRX_OUT.ITEM_NO1,
        TRX_OUT.QTY1,
        TRX_OUT.MF_SKU,
        TRX_OUT.SUBINVENTORY_CODE
    FROM
        (
            SELECT 
                jieh.HEADER_ID,
                mmt.ATTRIBUTE11 AS TRX_NO,
                SUBSTR(mmt.ATTRIBUTE12, 2, 4) AS LINE_NO,
                msi.SEGMENT1 AS ITEM_NO,
                mmt.TRANSACTION_QUANTITY AS QTY
            FROM 
                MTL_MATERIAL_TRANSACTIONS@PROD2 mmt,
                APPS.MTL_ITEM_LOCATIONS@PROD2 mil,
                MTL_SYSTEM_ITEMS_B@PROD2 msi,
                JIT_INV_EXCHANGE_HEADER jieh
            WHERE 
                mmt.ORGANIZATION_ID IN (169, 209)
                AND mmt.TRANSACTION_TYPE_ID = 124
                AND mmt.INVENTORY_ITEM_ID = msi.INVENTORY_ITEM_ID
                AND mmt.ORGANIZATION_ID = msi.ORGANIZATION_ID
                AND TRUNC(mmt.TRANSACTION_DATE) = TRUNC(SYSDATE)
                AND mmt.ORGANIZATION_ID = mil.ORGANIZATION_ID(+)
                AND mmt.SUBINVENTORY_CODE = mil.SUBINVENTORY_CODE(+)
                AND mmt.LOCATOR_ID = mil.INVENTORY_LOCATION_ID(+)
                AND mmt.SUBINVENTORY_CODE = '外存倉'
                AND (mil.SEGMENT1 LIKE '%基通%' OR mil.SEGMENT2 LIKE '%基通%')
                AND mil.ATTRIBUTE3 = 'JIT'
                AND mmt.TRANSACTION_QUANTITY > 0
                AND jieh.EXTERNAL_ID = mmt.ATTRIBUTE11
                AND jieh.STATUS = 'PENDING'
        ) TRX_IN,
        (
            SELECT 
                mmt1.ATTRIBUTE11 AS TRX_NO1,
                SUBSTR(mmt1.ATTRIBUTE12, 2, 4) AS LINE_NO1,
                msi1.SEGMENT1 AS ITEM_NO1,
                ABS(mmt1.TRANSACTION_QUANTITY) AS QTY1,
                NVL(NVL(wmt.VENDOR_MATERIAL_INFO, ZEN_GET_WMS_ITEM_F(msi1.SEGMENT1,mmt1.ORGANIZATION_ID)), msi1.SEGMENT1) AS MF_SKU,
                DECODE(mil1.SEGMENT1, '', '',
                    (mil1.SEGMENT1 || '.' || mil1.SEGMENT2)) AS SUBINVENTORY_CODE,
                jieh.HEADER_ID
            FROM 
                MTL_MATERIAL_TRANSACTIONS@PROD2 mmt1,
                MTL_SYSTEM_ITEMS_B@PROD2 msi1,
                APPS.MTL_ITEM_LOCATIONS@PROD2 mil1,
                JIT_INV_EXCHANGE_HEADER jieh,
                ZENOANEW.WEB017_MIXINOUT_D@ZENOADBT1ZENWMSHR wmt
            WHERE 
                mmt1.ORGANIZATION_ID IN (169, 209)
                AND mmt1.TRANSACTION_TYPE_ID = 125
                AND mmt1.INVENTORY_ITEM_ID = msi1.INVENTORY_ITEM_ID
                AND mmt1.ORGANIZATION_ID = msi1.ORGANIZATION_ID
                AND TRUNC(mmt1.TRANSACTION_DATE) = TRUNC(SYSDATE)
                AND mmt1.ORGANIZATION_ID = mil1.ORGANIZATION_ID(+)
                AND mmt1.SUBINVENTORY_CODE = mil1.SUBINVENTORY_CODE(+)
                AND mmt1.LOCATOR_ID = mil1.INVENTORY_LOCATION_ID(+)
                AND mmt1.SUBINVENTORY_CODE = '外存倉'
                AND (mil1.SEGMENT1 LIKE '%基通%' OR mil1.SEGMENT2 LIKE '%基通%')
                AND mil1.ATTRIBUTE3 = 'JIT'
                AND mmt1.TRANSACTION_QUANTITY < 0
                AND mmt1.ATTRIBUTE11 = wmt.SEQ_MISFORM
                AND mmt1.SOURCE_LINE_ID = wmt.SEQ_NO
                AND jieh.EXTERNAL_ID = mmt1.ATTRIBUTE11
                AND jieh.STATUS = 'PENDING'
        ) TRX_OUT,
        JIT_INV_EXCHANGE_FINAL jief
    WHERE 
        TRX_IN.TRX_NO = TRX_OUT.TRX_NO1
        AND TRX_IN.LINE_NO = TRX_OUT.LINE_NO1
        AND TRX_IN.HEADER_ID = TRX_OUT.HEADER_ID
        AND TRX_IN.HEADER_ID = jief.HEADER_ID
        AND TRX_IN.ITEM_NO = jief.PRODUCT
        AND NOT EXISTS (
            SELECT 1
            FROM JIT_INV_EXCHANGE_HEADER jeh,
                 JIT_INV_EXCHANGE_FINAL jef,
                 JIT_INV_EXCHANGE_MATERIAL jim
            WHERE jeh.HEADER_ID = jef.HEADER_ID
                AND TRX_OUT.HEADER_ID = jeh.HEADER_ID
                AND jief.FINAL_ID = jef.FINAL_ID
                AND jef.FINAL_ID = jim.FINAL_ID
        );
END;
