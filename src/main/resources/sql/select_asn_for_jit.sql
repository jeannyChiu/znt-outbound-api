-- JIT ASN 資料查詢 SQL (單筆 EXTERNAL_ID 處理版本)
-- 此 SQL 將 JIT_ASN_HEADER 和 JIT_ASN_LINE 表格進行 JOIN，
-- 查詢狀態為 'PENDING' 或 'FAILED' 的入庫單資料，但每次只返回一個 EXTERNAL_ID 的所有明細行
-- 這樣確保符合 JIT 系統「每次 API 調用只能傳送一筆 ExternalId 資料」的限制

SELECT
    -- Header 欄位 (來自 JIT_ASN_HEADER)
    h.EXTERNAL_ID,
    h.EXTERNAL_NO,
    h.WH_NAME,
    h.STORER_ABBR_NAME,
    h.PRIORITY,
    h.DOC_TYPE,
    h.BIZ_TYPE,
    h.VOYAGE,
    h.BL_NO,
    h.CASE_CNT,
    h.PALLET_CNT,
    h.CONTAINER_CNT,
    h.DESCRIPTIONS,
    h.USER_DEF1,
    h.USER_DEF2,
    h.USER_DEF3,
    h.USER_DEF4,
    h.USER_DEF5,

    -- Line 基本欄位 (來自 JIT_ASN_LINE)
    l.STORER_LINE_NO,
    l.QTY_EXPECTED,
    l.ZONE_NAME,
    l.LINE_NW,
    l.LINE_GW,
    l.LINE_CUBE,
    l.LINE_UOM,
    l.LINE_USER_DEF1,
    l.LINE_USER_DEF2,
    l.LINE_USER_DEF3,
    l.LINE_USER_DEF4,
    l.LINE_USER_DEF5,
    l.LINE_DESCRIPTIONS,

    -- SKU 資訊欄位 (來自 JIT_ASN_LINE，對應 SkuInfo)
    l.SKU,
    l.SKU_STORER_ABBR_NAME,
    l.SKU_NAME,
    l.SKU_NAME_E,
    l.SPEC,
    l.CATEGORY,
    l.SUB_CATEGORY,
    l.PRODUCT_LINE,
    l.UNIT_COO,
    l.UNIT_GW,
    l.UNIT_NW,
    l.UNIT_UOM,
    l.ABC,
    l.MOQ,
    l.SAFETY_STOCK,
    l.LENGTH,
    l.WIDTH,
    l.HEIGHT,
    l.UNIT_CUBE,
    l.AREA,
    l.UNIT_DESCRIPTIONS,
    l.UNIT_USER_DEF1,
    l.UNIT_USER_DEF2,
    l.UNIT_USER_DEF3,
    l.UNIT_USER_DEF4,
    l.UNIT_USER_DEF5,

    -- 屬性欄位 (來自 JIT_ASN_LINE，對應 AsnLineAttr)
    l.LOC,
    l.LPN,
    l.DATE_CODE,
    l.EXPIRED_DT,
    l.ATTR_COO,
    l.PACKAGE_TYPE,
    l.VLOT,
    l.LOT_ATTR01,
    l.LOT_ATTR02,
    l.LOT_ATTR03,
    l.LOT_ATTR04,
    l.LOT_ATTR05,
    l.LOT_ATTR06,
    l.LOT_ATTR07,
    l.LOT_ATTR08,
    l.LOT_ATTR09,
    l.LOT_ATTR10,
    l.LOT_ATTR11,
    l.LOT_ATTR12,
    l.LOT_ATTR13,
    l.LOT_ATTR14,
    l.LOT_ATTR15,
    l.LOT_ATTR16,
    l.LOT_ATTR17,
    l.LOT_ATTR18,
    l.LOT_ATTR19,
    l.LOT_ATTR20

FROM B2B.JIT_ASN_HEADER h
INNER JOIN B2B.JIT_ASN_LINE l ON h.HEADER_ID = l.HEADER_ID
WHERE h.STATUS = 'PENDING' OR h.STATUS = 'FAILED'
  AND h.HEADER_ID = (
    -- 子查詢：取得第一個待處理的 HEADER_ID 
    -- 使用 MIN 確保每次都取得相同的第一筆，避免併發問題
    SELECT MIN(h2.HEADER_ID)
    FROM B2B.JIT_ASN_HEADER h2
    WHERE h2.STATUS = 'PENDING' OR h2.STATUS = 'FAILED'
  )
ORDER BY h.HEADER_ID, l.LINE_ID