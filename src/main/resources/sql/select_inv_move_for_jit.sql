-- JIT 庫內移倉/交易資料查詢 SQL (單筆 EXTERNAL_ID 處理版本)
-- 此 SQL 將 JIT_MOVE_TRADE_HEADER 和 JIT_MOVE_TRADE_LINE 表格進行 JOIN，
-- 只查詢狀態為 'PENDING' 的庫內移倉/交易資料，每次只返回一個 EXTERNAL_ID 的所有明細行
-- 這樣確保符合 JIT 系統「每次 API 調用只能傳送一筆 ExternalId 資料」的限制
-- 
-- 處理邏輯：
-- 1. 只處理 PENDING 狀態的資料，避免重複處理已失敗的資料
-- 2. 處理失敗的資料狀態會變為 FAILED，需要人工檢查問題並修復後重置為 PENDING
-- 3. 這樣可以避免無窮迴圈，並確保後續的 PENDING 資料能正常處理

SELECT
    -- Header 欄位 (來自 JIT_MOVE_TRADE_HEADER)
    h.EXTERNAL_ID,
    h.EXTERNAL_NO,
    h.WH_NAME,
    h.TRADE_TYPE,
    h.FROM_STORER,
    h.TO_STORER,
    h.APPLY_DATE,
    h.REF_NO,
    h.REMARK as HEADER_REMARK,

    -- Line 欄位 (來自 JIT_MOVE_TRADE_LINE)
    l.SKU,
    l.MF_SKU,
    l.QTY,
    l.FROM_STORER_CATE,
    l.TO_STORER_CATE,
    l.LOT,
    l.BATCH,
    l.DATE_CODE,
    l.COO,
    l.REMARK as LINE_REMARK

FROM B2B.JIT_MOVE_TRADE_HEADER h
INNER JOIN B2B.JIT_MOVE_TRADE_LINE l ON h.HEADER_ID = l.HEADER_ID
WHERE h.STATUS = 'PENDING'
  AND h.HEADER_ID = (
    -- 子查詢：取得第一個待處理的 HEADER_ID 
    -- 使用 MIN 確保每次都取得相同的第一筆，避免併發問題
    SELECT MIN(h2.HEADER_ID)
    FROM B2B.JIT_MOVE_TRADE_HEADER h2
    WHERE h2.STATUS = 'PENDING'
  )
ORDER BY h.HEADER_ID, l.LINE_ID