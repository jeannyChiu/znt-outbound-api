-- 查詢待處理的JIT庫內換料主表資料
-- 只查詢狀態為 PENDING 的換料單，按 HEADER_ID 排序，確保處理順序一致
-- 
-- 處理邏輯：
-- 1. 只處理 PENDING 狀態的資料，避免重複處理已失敗的資料
-- 2. 處理失敗的資料狀態會變為 FAILED，需要人工檢查問題並修復後重置為 PENDING
-- 3. 這樣可以避免無窮迴圈，並確保後續的 PENDING 資料能正常處理
SELECT 
    h.HEADER_ID,
    h.EXTERNAL_ID,
    h.EXTERNAL_NO,
    h.WH_NAME,
    h.STORER,
    h.EXCHANGE_TYPE,
    h.APPLY_DATE,
    h.REF_NO,
    h.REMARK,
    h.STATUS,
    h.CREATED_AT,
    h.UPDATED_AT
FROM JIT_INV_EXCHANGE_HEADER h
WHERE h.STATUS = 'PENDING'
ORDER BY h.HEADER_ID ASC