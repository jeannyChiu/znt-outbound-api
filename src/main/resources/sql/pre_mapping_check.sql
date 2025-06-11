DECLARE
  l_w_count NUMBER;
BEGIN
  SELECT COUNT(*)
  INTO l_w_count
  FROM ZEN_B2B_JSON_SO
  WHERE STATUS = 'W';

  IF l_w_count = 0 THEN
    -- 拋出一個自定義的應用程式錯誤。
    -- Java 應用程式會捕捉這個錯誤碼並正常結束程式。
    RAISE_APPLICATION_ERROR(-20001, '沒有狀態為 ''W'' 的資料需要處理。');
  END IF;
END; 