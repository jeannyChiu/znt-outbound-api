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
    SELECT DISTINCT
        zshv.shipping_no,
        zshv.shipping_no,
        'GT',
        DECODE(zshv.organization_id, 169, 'ZCSH', 'ZTSH') AS StorerAbbrName,
        1 AS priorty,
        '一般方式' AS DocType,
        '採購入庫' AS BizType,
        NULL AS Voyage,
        NULL AS BlNo,
        NULL AS CaseCnt,
        NULL AS PalletCnt,
        NULL AS ContainerCnt,
        NULL AS Descriptions,
        NULL AS UserDef1,
        zshv.supplier_name AS UserDef2,
        NULL AS UserDef3,
        NULL AS UserDef4,
        NULL AS UserDef5
    FROM ZEN_SHIPPING_NOTICE_HEADERS_V@PROD2 zshv,
         zen_shipping_notice_lines_v@PROD2 zslv,
         apps.mtl_item_locations@PROD2 mil
    WHERE zshv.shipping_no = zslv.shipping_no
      AND zslv.subinventory_code = '外存倉'
      AND mil.organization_id(+) = zslv.organization_id
      AND mil.subinventory_code(+) = zslv.subinventory_code
      AND mil.inventory_location_id(+) = zslv.LOCATOR_ID
      AND zshv.organization_id IN (169, 209)
      AND zshv.status = 'Complete'
      AND zslv.status = 'Open'
      AND (mil.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
      AND mil.attribute3 = 'JIT'
      AND TRUNC(zshv.creation_date) > TO_DATE('20250501', 'yyyymmdd')
      AND NOT EXISTS (
          SELECT 1
          FROM jit_asn_header jah
          WHERE jah.external_id = TO_CHAR(zshv.shipping_no)
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
        sub_category,
        product_line,
        unit_coo,
        unit_gw,
        unit_nw,
        unit_uom,
        abc,
        moq,
        safety_stock,
        length,
        width,
        height,
        unit_cube,
        area,
        unit_descriptions,
        unit_user_def1,
        unit_user_def2,
        unit_user_def3,
        unit_user_def4,
        unit_user_def5,
        qty_expected,
        zone_name,
        line_nw,
        line_gw,
        line_cube,
        line_uom,
        line_user_def1,
        line_user_def2,
        line_user_def3,
        line_user_def4,
        line_user_def5,
        line_descriptions,
        loc,
        lpn,
        date_code,
        expired_dt,
        attr_coo,
        package_type,
        vlot,
        lot_attr01,
        lot_attr02,
        lot_attr03,
        lot_attr04,
        lot_attr05,
        lot_attr06,
        lot_attr07,
        lot_attr08,
        lot_attr09,
        lot_attr10,
        lot_attr11,
        lot_attr12,
        lot_attr13,
        lot_attr14,
        lot_attr15,
        lot_attr16,
        lot_attr17,
        lot_attr18,
        lot_attr19,
        lot_attr20
    )
    SELECT
        jah.header_id,
        zslv.line_seq || '-' || zslv.shipping_line_id AS storer_line_no,
        zslv.item AS Sku,
        DECODE(zshv.organization_id, 169, 'ZCSH', 'ZTSH') AS sku_storer_abbr_name,
        msi.DESCRIPTION AS sku_name,
        zslv.item AS sku_name_e,
        NULL AS spec,
        s1.segment3 AS category,
        NULL AS sub_category,
        NULL AS product_line,
        NULL AS unit_coo,
        NULL AS unit_gw,
        NULL AS unit_nw,
        NULL AS unit_uom,
        NULL AS abc,
        NULL AS moq,
        NULL AS safety_stock,
        NULL AS length,
        NULL AS width,
        NULL AS height,
        NULL AS unit_cube,
        NULL AS area,
        NULL AS unit_descriptions,
        NULL AS unit_user_def1,
        NULL AS unit_user_def2,
        NULL AS unit_user_def3,
        NULL AS unit_user_def4,
        NULL AS unit_user_def5,
        zslv.quantity AS QtyExpected,
        DECODE(mil.segment1, '', '', (mil.segment1 || '.' || mil.segment2)) AS zone_name,
        NULL AS line_nw,
        NULL AS line_gw,
        NULL AS line_cube,
        NULL AS line_uom,
        NULL AS line_user_def1,
        NULL AS line_user_def2,
        NULL AS line_user_def3,
        NULL AS line_user_def4,
        NULL AS line_user_def5,
        NULL AS line_descriptions,
        NULL AS loc,
        NULL AS lpn,
        NULL AS date_code,
        NULL AS expired_dt,
        NULL AS attr_coo,
        NULL AS package_type,
        NULL AS vlot,
        msi.attribute16 AS lot_attr01,
        DECODE(zshv.organization_id, 169, NVL(NVL(zslv.vendor_material_info, ZEN_GET_WMS_ITEM_F(zslv.item)), zslv.item),
               NVL(zslv.vendor_material_info, zslv.item)) AS lot_attr02,
        NULL AS lot_attr03,
        NULL AS lot_attr04,
        NULL AS lot_attr05,
        NULL AS lot_attr06,
        NULL AS lot_attr07,
        NULL AS lot_attr08,
        NULL AS lot_attr09,
        NULL AS lot_attr10,
        NULL AS lot_attr11,
        NULL AS lot_attr12,
        NULL AS lot_attr13,
        NULL AS lot_attr14,
        NULL AS lot_attr15,
        NULL AS lot_attr16,
        NULL AS lot_attr17,
        NULL AS lot_attr18,
        NULL AS lot_attr19,
        NULL AS lot_attr20
    FROM ZEN_SHIPPING_NOTICE_HEADERS_V@PROD2 zshv,
         zen_shipping_notice_lines_v@PROD2 zslv,
         apps.mtl_item_locations@PROD2 mil,
         jit_asn_header jah,
         mtl_system_items_b@PROD2 msi,
         apps.mtl_item_categories@PROD2 w,
         apps.mtl_categories_b@PROD2 s1
    WHERE zshv.shipping_no = zslv.shipping_no
      AND zslv.subinventory_code = '外存倉'
      AND mil.organization_id(+) = zslv.organization_id
      AND mil.subinventory_code(+) = zslv.subinventory_code
      AND mil.inventory_location_id(+) = zslv.LOCATOR_ID
      AND zshv.organization_id IN (169, 209)
      AND zshv.status = 'Complete'
      AND zslv.status = 'Open'
      AND (mil.segment1 LIKE '%基通%' OR mil.segment2 LIKE '%基通%')
      AND mil.attribute3 = 'JIT'
      AND zslv.inventory_item_id = msi.INVENTORY_ITEM_ID
      AND zslv.organization_id = msi.ORGANIZATION_ID
      AND msi.INVENTORY_ITEM_ID = w.inventory_item_id
      AND msi.ORGANIZATION_ID = w.organization_id
      AND w.category_set_id = 1
      AND w.category_id = s1.category_id
      AND jah.external_id = TO_CHAR(zshv.shipping_no)
      AND TRUNC(zshv.creation_date) > TO_DATE('20250501', 'yyyymmdd')
      AND jah.status = 'PENDING'
      AND NOT EXISTS (
          SELECT 1
          FROM jit_asn_line
          WHERE header_id = jah.header_id
      );

END;