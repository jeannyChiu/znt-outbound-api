-- Warehouse Transfer Outbound Processing
-- Sources: ZSH (上海倉), JY (景央), HHW (海宏微)
-- Target: JIT WMS System
-- Date Added: 2025-08-08 (Vendor Material Info)

BEGIN
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
        -- ZSH (上海倉) / SHC (宏衢) Transfer
        SELECT
            'ZEN' AS SENDER_CODE,
            'JIT' AS RECEIVER_CODE,
            CASE    
                WHEN ZMTH.HEADER_ID IS NULL
                    OR ZMTH.DATE_REQUIRED IS NULL
                    OR dep.DEP_NAME IS NULL
                    OR FU.USER_NAME IS NULL
                    OR MSIB.SEGMENT1 IS NULL
                    OR ZEN_GET_BRAND_ALIAS_F@PROD2(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) IS NULL
                    OR ZMTL.QUANTITY IS NULL
                    OR MSIB.ATTRIBUTE1 IS NULL
                    OR ZMTL.FROM_SUBINVENTORY_CODE IS NULL
                    OR mil.segment1 IS NULL
                    OR mil.segment2 IS NULL
                THEN 'N'
                ELSE 'W'
            END AS STATUS,
            'OUT' AS DIRECTION,
            TO_CHAR(ZMTH.DATE_REQUIRED, 'yyyy-MM-dd HH:mm:ss') AS DOC_DATETIME,
            TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS LAST_UPDATE_DATE,
            TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS CREATION_DATE,
            'admin' AS USER_NO,
            ZMTH.HEADER_ID || '-' || ZMTL.line_id AS ID,
            ZMTH.HEADER_ID AS ORDER_NO,
            '' AS INV_SPLIT,
            ZMTH.HEADER_ID || '-' || DECODE(ZMTL.ORGANIZATION_ID, 169, 'ZSH', 'SHC') AS INVOICE_NO,
            TO_CHAR(TRUNC(ZMTH.DATE_REQUIRED), 'yyyy/mm/dd') AS INVOICE_DATE,
            TO_CHAR(TRUNC(ZMTH.DATE_REQUIRED) + 1, 'yyyy/mm/dd') AS EXP_SHIP_DATE,
            '' AS CUST_NO,
            dep.DEP_NAME AS DEPT,
            ZMTH.CUST_NAME AS CUST_NAME,
            ZMTH.SHIP_TO_ADDRESS AS INVOICE_ADDRESS,
            '' AS SHIP_TO_NO,
            ZMTH.SHIP_TO_ADDRESS AS DELIVERY_ADDRESS,
            ZMTH.SHIP_TO_CONTACT AS SHIP_TO_CONTACT,
            NULL AS SHIP_TO_PHONE,
            MSIB.SEGMENT1 AS ZT_PART_NO,
            '' AS CUST_PART_NO,
            '' AS CUSTOMER_PO,
            '' AS SHIP_NOTICE,
            '' AS SHIP_NOTES,
            'Approved' AS STATUS1,
            ZEN_GET_BRAND_ALIAS_F@PROD2(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) AS BRAND,
            ZMTL.QUANTITY AS QUANTITY,
            NULL AS UNIT_PRICE,
            NULL AS AMOUNT,
            '' AS CUST_PO,
            '' AS CUST_PO2,
            '' AS CUST_POLINE,
            '' AS CUST_PN,
            '' AS CUST_PN2,
            '' AS ITEM_DESC,
            MSIB.ATTRIBUTE1 AS ITEM_MODE,
            '' AS ORDER_LINE_MEMO,
            '' AS UNIT_WEIGHT,
            '' AS WEIGHT_UOM_CODE,
            ZMTL.FROM_SUBINVENTORY_CODE AS FROM_SUBINVENTORY_CODE,
            mil.segment1 AS SEGMENT1,
            mil.segment2 AS SEGMENT2,
            '移倉單' AS KIND,
            MSIB.SEGMENT1 AS SUPPLIER_PARTNO,
            '' AS INV_AND_PAC,
            '' AS CUST_PART_NO2,
            NVL(NVL(ZMTL.vendor_material_info, ZEN_GET_WMS_ITEM_F(MSIB.SEGMENT1)), MSIB.SEGMENT1) AS PO_REMARK,
            '' AS RMA_NUMBER,
            '0' AS ORD_TYPE,
            'TY' AS QC_TYPE
        FROM 
            ZEN_MTL_TXN_REQUEST_HEADERS@PROD2 ZMTH
            INNER JOIN ZEN_MTL_TXN_REQUEST_LINES@PROD2 ZMTL ON ZMTH.HEADER_ID = ZMTL.HEADER_ID
            INNER JOIN MTL_SYSTEM_ITEMS_B@PROD2 MSIB ON MSIB.INVENTORY_ITEM_ID = ZMTL.INVENTORY_ITEM_ID
                AND MSIB.ORGANIZATION_ID = ZMTH.ORGANIZATION_ID
            INNER JOIN FND_USER@PROD2 FU ON ZMTH.CREATED_BY = FU.USER_ID
            INNER JOIN MFG_LOOKUPS@PROD2 MFGL ON MFGL.LOOKUP_TYPE = 'MTL_TXN_REQUEST_STATUS'
                AND MFGL.LOOKUP_CODE = ZMTL.LINE_STATUS
            INNER JOIN ZEN_ORGANIZATION_DEFINITIONS_V@PROD2 OOD ON ZMTH.ORGANIZATION_ID = OOD.ORGANIZATION_ID
            LEFT JOIN apps.mtl_item_locations@PROD2 mil ON mil.organization_id = ZMTL.organization_id
                AND mil.subinventory_code = ZMTL.FROM_SUBINVENTORY_CODE
                AND mil.inventory_location_id = ZMTL.from_LOCATOR_ID
            LEFT JOIN (
                SELECT DISTINCT 
                    d.DEP_NAME,
                    f.USER_NAME
                FROM OA_DEPTALL@hr d
                    JOIN OA_USERALL@hr u ON d.DEP_NO = u.USER_DEPTNO
                    JOIN HR_EMPLOYEES@PROD2 e ON u.USER_NO = e.ATTRIBUTE1
                    JOIN FND_USER@PROD2 f ON e.EMPLOYEE_ID = f.EMPLOYEE_ID
            ) dep ON FU.USER_NAME = dep.USER_NAME
            INNER JOIN MTL_MATERIAL_TRANSACTIONS@PROD2 mmt ON mmt.transaction_type_id = 2
                AND TO_CHAR(ZMTH.HEADER_ID) = mmt.source_code
                AND ZMTL.ORGANIZATION_ID = mmt.ORGANIZATION_ID
                AND ZMTL.INVENTORY_ITEM_ID = mmt.INVENTORY_ITEM_ID
                AND mmt.SUBINVENTORY_CODE = '外存倉'
                AND mmt.TRANSACTION_QUANTITY < 0
            LEFT JOIN apps.mtl_item_locations@PROD2 mil1 ON ZMTL.organization_id = mil1.organization_id
                AND ZMTL.TO_SUBINVENTORY_CODE = mil1.subinventory_code
                AND ZMTL.TO_LOCATOR_ID = mil1.inventory_location_id
        WHERE ZMTL.ORGANIZATION_ID IN (169, 209)  -- 169: ZSH, 209: 宏衢
            AND ZMTL.LINE_STATUS = 5  -- CLOSED
            AND ZMTH.HEADER_STATUS = 3  -- Approved
            AND ZMTL.FROM_SUBINVENTORY_CODE = '外存倉'
            AND (mil.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
            AND mil.attribute3 = 'JIT'
            AND NVL(mil1.attribute3, 'X') != 'JIT'
            AND TRUNC(mmt.transaction_date) = TRUNC(SYSDATE)
            AND NOT EXISTS (
                SELECT 1
                FROM ZEN_B2B_JSON_SO 
                WHERE ID = ZMTH.HEADER_ID || '-' || ZMTL.line_id
                    AND (STATUS = (
                        CASE    
                            WHEN ZMTH.HEADER_ID IS NULL
                                OR ZMTH.DATE_REQUIRED IS NULL
                                OR dep.DEP_NAME IS NULL
                                OR FU.USER_NAME IS NULL
                                OR MSIB.SEGMENT1 IS NULL
                                OR ZEN_GET_BRAND_ALIAS_F@PROD2(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) IS NULL
                                OR ZMTL.QUANTITY IS NULL
                                OR MSIB.ATTRIBUTE1 IS NULL
                                OR ZMTL.FROM_SUBINVENTORY_CODE IS NULL
                                OR mil.segment1 IS NULL
                                OR mil.segment2 IS NULL
                            THEN 'N'
                            ELSE 'W'
                        END
                    ) OR STATUS = 'S')
            )
        
        UNION ALL
        
        -- JY (景央) Transfer
        SELECT
            'ZEN' AS SENDER_CODE,
            'JIT' AS RECEIVER_CODE,
            CASE    
                WHEN ZMTH.HEADER_ID IS NULL
                    OR ZMTH.DATE_REQUIRED IS NULL
                    OR dep.DEP_NAME IS NULL
                    OR FU.USER_NAME IS NULL
                    OR MSIB.SEGMENT1 IS NULL
                    OR ZEN_GET_BRAND_ALIAS_F@jy(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) IS NULL
                    OR ZMTL.QUANTITY IS NULL
                    OR MSIB.ATTRIBUTE1 IS NULL
                    OR ZMTL.FROM_SUBINVENTORY_CODE IS NULL
                    OR mil.segment1 IS NULL
                    OR mil.segment2 IS NULL
                THEN 'N'
                ELSE 'W'
            END AS STATUS,
            'OUT' AS DIRECTION,
            TO_CHAR(ZMTH.DATE_REQUIRED, 'yyyy-MM-dd HH:mm:ss') AS DOC_DATETIME,
            TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS LAST_UPDATE_DATE,
            TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS CREATION_DATE,
            'admin' AS USER_NO,
            ZMTH.HEADER_ID || '-' || ZMTL.line_id AS ID,
            ZMTH.HEADER_ID AS ORDER_NO,
            '' AS INV_SPLIT,
            ZMTH.HEADER_ID || '-' || 'JY' AS INVOICE_NO,
            TO_CHAR(TRUNC(ZMTH.DATE_REQUIRED), 'yyyy/mm/dd') AS INVOICE_DATE,
            TO_CHAR(TRUNC(ZMTH.DATE_REQUIRED) + 1, 'yyyy/mm/dd') AS EXP_SHIP_DATE,
            '' AS CUST_NO,
            dep.DEP_NAME AS DEPT,
            ZMTH.CUST_NAME AS CUST_NAME,
            ZMTH.SHIP_TO_ADDRESS AS INVOICE_ADDRESS,
            '' AS SHIP_TO_NO,
            ZMTH.SHIP_TO_ADDRESS AS DELIVERY_ADDRESS,
            ZMTH.SHIP_TO_CONTACT AS SHIP_TO_CONTACT,
            NULL AS SHIP_TO_PHONE,
            MSIB.SEGMENT1 AS ZT_PART_NO,
            '' AS CUST_PART_NO,
            '' AS CUSTOMER_PO,
            '' AS SHIP_NOTICE,
            '' AS SHIP_NOTES,
            'Approved' AS STATUS1,
            ZEN_GET_BRAND_ALIAS_F@jy(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) AS BRAND,
            ZMTL.QUANTITY AS QUANTITY,
            NULL AS UNIT_PRICE,
            NULL AS AMOUNT,
            '' AS CUST_PO,
            '' AS CUST_PO2,
            '' AS CUST_POLINE,
            '' AS CUST_PN,
            '' AS CUST_PN2,
            '' AS ITEM_DESC,
            MSIB.ATTRIBUTE1 AS ITEM_MODE,
            '' AS ORDER_LINE_MEMO,
            '' AS UNIT_WEIGHT,
            '' AS WEIGHT_UOM_CODE,
            ZMTL.FROM_SUBINVENTORY_CODE AS FROM_SUBINVENTORY_CODE,
            'SHC基通' AS SEGMENT1,
            'IT' AS SEGMENT2,
            '移倉單' AS KIND,
            MSIB.SEGMENT1 AS SUPPLIER_PARTNO,
            '' AS INV_AND_PAC,
            '' AS CUST_PART_NO2,
            NVL(NVL(ZMTL.vendor_material_info, ZEN_GET_WMS_ITEM_F(MSIB.SEGMENT1)), MSIB.SEGMENT1) AS PO_REMARK,
            '' AS RMA_NUMBER,
            '0' AS ORD_TYPE,
            'TY' AS QC_TYPE
        FROM 
            ZEN_MTL_TXN_REQUEST_HEADERS@jy ZMTH
            INNER JOIN ZEN_MTL_TXN_REQUEST_LINES@jy ZMTL ON ZMTH.HEADER_ID = ZMTL.HEADER_ID
            INNER JOIN MTL_SYSTEM_ITEMS_B@jy MSIB ON MSIB.INVENTORY_ITEM_ID = ZMTL.INVENTORY_ITEM_ID
                AND MSIB.ORGANIZATION_ID = ZMTH.ORGANIZATION_ID
            INNER JOIN FND_USER@jy FU ON ZMTH.CREATED_BY = FU.USER_ID
            INNER JOIN MFG_LOOKUPS@jy MFGL ON MFGL.LOOKUP_TYPE = 'MTL_TXN_REQUEST_STATUS'
                AND MFGL.LOOKUP_CODE = ZMTL.LINE_STATUS
            INNER JOIN ZEN_ORGANIZATION_DEFINITIONS_V@jy OOD ON ZMTH.ORGANIZATION_ID = OOD.ORGANIZATION_ID
            LEFT JOIN apps.mtl_item_locations@jy mil ON mil.organization_id = ZMTL.organization_id
                AND mil.subinventory_code = ZMTL.FROM_SUBINVENTORY_CODE
                AND mil.inventory_location_id = ZMTL.from_LOCATOR_ID
            LEFT JOIN (
                SELECT DISTINCT 
                    d.DEP_NAME,
                    f.USER_NAME
                FROM OA_DEPTALL@hr d
                    JOIN OA_USERALL@hr u ON d.DEP_NO = u.USER_DEPTNO
                    JOIN HR_EMPLOYEES@jy e ON u.USER_NO = e.ATTRIBUTE1
                    JOIN FND_USER@jy f ON e.EMPLOYEE_ID = f.EMPLOYEE_ID
            ) dep ON FU.USER_NAME = dep.USER_NAME
            INNER JOIN MTL_MATERIAL_TRANSACTIONS@jy mmt ON mmt.transaction_type_id = 2
                AND TO_CHAR(ZMTH.HEADER_ID) = mmt.source_code
                AND ZMTL.ORGANIZATION_ID = mmt.ORGANIZATION_ID
                AND ZMTL.INVENTORY_ITEM_ID = mmt.INVENTORY_ITEM_ID
                AND mmt.SUBINVENTORY_CODE = '外存倉'
                AND mmt.TRANSACTION_QUANTITY < 0
            LEFT JOIN apps.mtl_item_locations@jy mil1 ON ZMTL.TO_SUBINVENTORY_CODE = mil1.subinventory_code
                AND ZMTL.TO_LOCATOR_ID = mil1.inventory_location_id
        WHERE ZMTL.ORGANIZATION_ID = 307
            AND ZMTL.LINE_STATUS = 5  -- CLOSED
            AND ZMTH.HEADER_STATUS = 3  -- Approved
            AND ZMTL.FROM_SUBINVENTORY_CODE = '外存倉'
            AND (mil.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
            AND mil.attribute3 = 'JIT'
            AND NVL(mil1.attribute3, 'X') != 'JIT'
            AND TRUNC(mmt.transaction_date) = TRUNC(SYSDATE)
            AND NOT EXISTS (
                SELECT 1
                FROM ZEN_B2B_JSON_SO 
                WHERE ID = ZMTH.HEADER_ID || '-' || ZMTL.line_id
                    AND (STATUS = (
                        CASE    
                            WHEN ZMTH.HEADER_ID IS NULL
                                OR ZMTH.DATE_REQUIRED IS NULL
                                OR dep.DEP_NAME IS NULL
                                OR FU.USER_NAME IS NULL
                                OR MSIB.SEGMENT1 IS NULL
                                OR ZEN_GET_BRAND_ALIAS_F@jy(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) IS NULL
                                OR ZMTL.QUANTITY IS NULL
                                OR MSIB.ATTRIBUTE1 IS NULL
                                OR ZMTL.FROM_SUBINVENTORY_CODE IS NULL
                                OR mil.segment1 IS NULL
                                OR mil.segment2 IS NULL
                            THEN 'N'
                            ELSE 'W'
                        END
                    ) OR STATUS = 'S')
            )
        
        UNION ALL
        
        -- HHW (海宏微) Transfer
        SELECT
            'ZEN' AS SENDER_CODE,
            'JIT' AS RECEIVER_CODE,
            CASE    
                WHEN ZMTH.HEADER_ID IS NULL
                    OR ZMTH.DATE_REQUIRED IS NULL
                    OR dep.DEP_NAME IS NULL
                    OR FU.USER_NAME IS NULL
                    OR MSIB.SEGMENT1 IS NULL
                    OR ZEN_GET_BRAND_ALIAS_F@SHCTRADING0(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) IS NULL
                    OR ZMTL.QUANTITY IS NULL
                    OR MSIB.ATTRIBUTE1 IS NULL
                    OR ZMTL.FROM_SUBINVENTORY_CODE IS NULL
                    OR mil.segment1 IS NULL
                    OR mil.segment2 IS NULL
                THEN 'N'
                ELSE 'W'
            END AS STATUS,
            'OUT' AS DIRECTION,
            TO_CHAR(ZMTH.DATE_REQUIRED, 'yyyy-MM-dd HH:mm:ss') AS DOC_DATETIME,
            TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS LAST_UPDATE_DATE,
            TO_CHAR(SYSDATE, 'yyyy-MM-dd HH:mm:ss') AS CREATION_DATE,
            'admin' AS USER_NO,
            ZMTH.HEADER_ID || '-' || ZMTL.line_id AS ID,
            ZMTH.HEADER_ID AS ORDER_NO,
            '' AS INV_SPLIT,
            ZMTH.HEADER_ID || '-' || 'HHW' AS INVOICE_NO,
            TO_CHAR(TRUNC(ZMTH.DATE_REQUIRED), 'yyyy/mm/dd') AS INVOICE_DATE,
            TO_CHAR(TRUNC(ZMTH.DATE_REQUIRED) + 1, 'yyyy/mm/dd') AS EXP_SHIP_DATE,
            '' AS CUST_NO,
            dep.DEP_NAME AS DEPT,
            ZMTH.CUST_NAME AS CUST_NAME,
            ZMTH.SHIP_TO_ADDRESS AS INVOICE_ADDRESS,
            '' AS SHIP_TO_NO,
            ZMTH.SHIP_TO_ADDRESS AS DELIVERY_ADDRESS,
            ZMTH.SHIP_TO_CONTACT AS SHIP_TO_CONTACT,
            NULL AS SHIP_TO_PHONE,
            MSIB.SEGMENT1 AS ZT_PART_NO,
            '' AS CUST_PART_NO,
            '' AS CUSTOMER_PO,
            '' AS SHIP_NOTICE,
            '' AS SHIP_NOTES,
            'Approved' AS STATUS1,
            ZEN_GET_BRAND_ALIAS_F@SHCTRADING0(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) AS BRAND,
            ZMTL.QUANTITY AS QUANTITY,
            NULL AS UNIT_PRICE,
            NULL AS AMOUNT,
            '' AS CUST_PO,
            '' AS CUST_PO2,
            '' AS CUST_POLINE,
            '' AS CUST_PN,
            '' AS CUST_PN2,
            '' AS ITEM_DESC,
            MSIB.ATTRIBUTE1 AS ITEM_MODE,
            '' AS ORDER_LINE_MEMO,
            '' AS UNIT_WEIGHT,
            '' AS WEIGHT_UOM_CODE,
            ZMTL.FROM_SUBINVENTORY_CODE AS FROM_SUBINVENTORY_CODE,
            'SHC基通' AS SEGMENT1,
            'IT' AS SEGMENT2,
            '移倉單' AS KIND,
            MSIB.SEGMENT1 AS SUPPLIER_PARTNO,
            '' AS INV_AND_PAC,
            '' AS CUST_PART_NO2,
            NVL(NVL(ZMTL.vendor_material_info, ZEN_GET_WMS_ITEM_F(MSIB.SEGMENT1)), MSIB.SEGMENT1) AS PO_REMARK,
            '' AS RMA_NUMBER,
            '0' AS ORD_TYPE,
            'TY' AS QC_TYPE
        FROM 
            ZEN_MTL_TXN_REQUEST_HEADERS@SHCTRADING0 ZMTH
            INNER JOIN ZEN_MTL_TXN_REQUEST_LINES@SHCTRADING0 ZMTL ON ZMTH.HEADER_ID = ZMTL.HEADER_ID
            INNER JOIN MTL_SYSTEM_ITEMS_B@SHCTRADING0 MSIB ON MSIB.INVENTORY_ITEM_ID = ZMTL.INVENTORY_ITEM_ID
                AND MSIB.ORGANIZATION_ID = ZMTH.ORGANIZATION_ID
            INNER JOIN FND_USER@SHCTRADING0 FU ON ZMTH.CREATED_BY = FU.USER_ID
            INNER JOIN MFG_LOOKUPS@SHCTRADING0 MFGL ON MFGL.LOOKUP_TYPE = 'MTL_TXN_REQUEST_STATUS'
                AND MFGL.LOOKUP_CODE = ZMTL.LINE_STATUS
            INNER JOIN ZEN_ORGANIZATION_DEFINITIONS_V@SHCTRADING0 OOD ON ZMTH.ORGANIZATION_ID = OOD.ORGANIZATION_ID
            LEFT JOIN apps.mtl_item_locations@SHCTRADING0 mil ON mil.organization_id = ZMTL.organization_id
                AND mil.subinventory_code = ZMTL.FROM_SUBINVENTORY_CODE
                AND mil.inventory_location_id = ZMTL.from_LOCATOR_ID
            LEFT JOIN (
                SELECT DISTINCT 
                    d.DEP_NAME,
                    f.USER_NAME
                FROM OA_DEPTALL@hr d
                    JOIN OA_USERALL@hr u ON d.DEP_NO = u.USER_DEPTNO
                    JOIN HR_EMPLOYEES@SHCTRADING0 e ON u.USER_NO = e.ATTRIBUTE1
                    JOIN FND_USER@SHCTRADING0 f ON e.EMPLOYEE_ID = f.EMPLOYEE_ID
            ) dep ON FU.USER_NAME = dep.USER_NAME
            INNER JOIN MTL_MATERIAL_TRANSACTIONS@SHCTRADING0 mmt ON mmt.transaction_type_id = 2
                AND TO_CHAR(ZMTH.HEADER_ID) = mmt.source_code
                AND ZMTL.ORGANIZATION_ID = mmt.ORGANIZATION_ID
                AND ZMTL.INVENTORY_ITEM_ID = mmt.INVENTORY_ITEM_ID
                AND mmt.SUBINVENTORY_CODE = '外存倉'
                AND mmt.TRANSACTION_QUANTITY < 0
            LEFT JOIN apps.mtl_item_locations@SHCTRADING0 mil1 ON ZMTL.TO_SUBINVENTORY_CODE = mil1.subinventory_code
                AND ZMTL.TO_LOCATOR_ID = mil1.inventory_location_id
        WHERE ZMTL.ORGANIZATION_ID = 209
            AND ZMTL.LINE_STATUS = 5  -- CLOSED
            AND ZMTH.HEADER_STATUS = 3  -- Approved
            AND ZMTL.FROM_SUBINVENTORY_CODE = '外存倉'
            AND (mil.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
            AND mil.attribute3 = 'JIT'
            AND NVL(mil1.attribute3, 'X') != 'JIT'
            AND TRUNC(mmt.transaction_date) = TRUNC(SYSDATE)
            AND NOT EXISTS (
                SELECT 1
                FROM ZEN_B2B_JSON_SO 
                WHERE ID = ZMTH.HEADER_ID || '-' || ZMTL.line_id
                    AND (STATUS = (
                        CASE    
                            WHEN ZMTH.HEADER_ID IS NULL
                                OR ZMTH.DATE_REQUIRED IS NULL
                                OR dep.DEP_NAME IS NULL
                                OR FU.USER_NAME IS NULL
                                OR MSIB.SEGMENT1 IS NULL
                                OR ZEN_GET_BRAND_ALIAS_F@SHCTRADING0(MSIB.ORGANIZATION_ID, ZMTL.INVENTORY_ITEM_ID) IS NULL
                                OR ZMTL.QUANTITY IS NULL
                                OR MSIB.ATTRIBUTE1 IS NULL
                                OR ZMTL.FROM_SUBINVENTORY_CODE IS NULL
                                OR mil.segment1 IS NULL
                                OR mil.segment2 IS NULL
                            THEN 'N'
                            ELSE 'W'
                        END
                    ) OR STATUS = 'S')
            )
        
        ORDER BY ORDER_NO, ID
    );
END;
