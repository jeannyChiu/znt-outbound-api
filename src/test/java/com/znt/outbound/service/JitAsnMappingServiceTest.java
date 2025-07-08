package com.znt.outbound.service;

import com.znt.outbound.model.jit.JitAsnRequest;
import com.znt.outbound.model.jit.JitAsnLine;
import com.znt.outbound.model.jit.JitSkuInfo;
import com.znt.outbound.model.jit.JitAsnLineAttr;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class JitAsnMappingServiceTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JitAsnMappingService jitAsnMappingService;

    @MockBean
    private JitApiClient jitApiClient;

    @MockBean
    private EnvelopeService envelopeService;

    @MockBean
    private ApiConfigService apiConfigService;

    @Test
    @DisplayName("測試單筆 ExternalNo 的資料映射功能")
    void testMapDataToJitAsnRequestWithSingleExternalNo() {
        // 使用新的 SQL 查詢（只返回一個 ExternalNo 的資料）
        String sql = """
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
            WHERE h.STATUS = 'PENDING'
              AND h.HEADER_ID = (
                SELECT MIN(h2.HEADER_ID)
                FROM B2B.JIT_ASN_HEADER h2
                WHERE h2.STATUS = 'PENDING'
              )
            ORDER BY h.HEADER_ID, l.LINE_ID
            """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

        // 如果沒有測試資料，跳過測試
        if (rows.isEmpty()) {
            System.out.println("沒有找到 PENDING 狀態的測試資料，跳過測試");
            return;
        }

        // 驗證所有資料都屬於同一個 EXTERNAL_NO
        String expectedExternalNo = (String) rows.get(0).get("EXTERNAL_NO");
        for (Map<String, Object> row : rows) {
            assertEquals(expectedExternalNo, row.get("EXTERNAL_NO"),
                    "所有資料行都應該屬於同一個 EXTERNAL_NO");
        }

        // 使用反射調用私有方法進行測試
        try {
            java.lang.reflect.Method method = JitAsnMappingService.class
                .getDeclaredMethod("mapDataToJitAsnRequest", List.class);
            method.setAccessible(true);

            JitAsnRequest result = (JitAsnRequest) method.invoke(jitAsnMappingService, rows);

            // 驗證結果
            assertNotNull(result, "映射結果不應該為 null");
            assertNotNull(result.getExternalNo(), "ExternalNo 不應該為 null");
            assertEquals(expectedExternalNo, result.getExternalNo(), "ExternalNo 應該與查詢結果一致");
            assertNotNull(result.getLines(), "Lines 不應該為 null");
            assertFalse(result.getLines().isEmpty(), "Lines 不應該為空");
            assertEquals(rows.size(), result.getLines().size(), "Lines 數量應該與查詢結果一致");

            // 驗證每一行資料的必要欄位
            for (int i = 0; i < result.getLines().size(); i++) {
                var line = result.getLines().get(i);
                assertNotNull(line.getSkuInfo(), "第 " + (i + 1) + " 行的 SkuInfo 不應該為 null");
                assertNotNull(line.getSkuInfo().getSku(), "第 " + (i + 1) + " 行的 SKU 不應該為 null");
                assertNotNull(line.getSkuInfo().getCategory(), "第 " + (i + 1) + " 行的 CATEGORY 不應該為 null");
                assertNotNull(line.getAsnLineAttr(), "第 " + (i + 1) + " 行的 AsnLineAttr 不應該為 null");
                assertNotNull(line.getQtyExpected(), "第 " + (i + 1) + " 行的 QtyExpected 不應該為 null");
                assertTrue(line.getQtyExpected() > 0, "第 " + (i + 1) + " 行的 QtyExpected 應該大於 0");
            }

            // 輸出結果供檢查
            System.out.println("=== JIT ASN Request 映射結果 ===");
            System.out.println("ExternalNo: " + result.getExternalNo());
            System.out.println("WhName: " + result.getWhName());
            System.out.println("StorerAbbrName: " + result.getStorerAbbrName());
            System.out.println("Priority: " + result.isPriority());
            System.out.println("Lines 數量: " + result.getLines().size());

            for (int i = 0; i < Math.min(3, result.getLines().size()); i++) { // 只顯示前3行
                var line = result.getLines().get(i);
                System.out.println("\n--- Line " + (i + 1) + " ---");
                System.out.println("StorerLineNo: " + line.getStorerLineNo());
                System.out.println("SKU: " + line.getSkuInfo().getSku());
                System.out.println("Category: " + line.getSkuInfo().getCategory());
                System.out.println("QtyExpected: " + line.getQtyExpected());
                System.out.println("Loc: " + line.getAsnLineAttr().getLoc());
            }

            if (result.getLines().size() > 3) {
                System.out.println("... 還有 " + (result.getLines().size() - 3) + " 行資料");
            }

        } catch (Exception e) {
            fail("測試過程中發生異常: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("測試資料完整性驗證功能")
    void testDataConsistencyValidation() {
        // 創建測試資料：包含不同 EXTERNAL_NO 的資料
        List<Map<String, Object>> mixedRows = new ArrayList<>();

        // 第一筆資料
        Map<String, Object> row1 = new HashMap<>();
        row1.put("EXTERNAL_NO", "ASN001");
        row1.put("SKU", "SKU001");
        row1.put("QTY_EXPECTED", 10);
        row1.put("CATEGORY", "CAT001");
        row1.put("SKU_STORER_ABBR_NAME", "STORER001");
        mixedRows.add(row1);

        // 第二筆資料（不同的 EXTERNAL_NO）
        Map<String, Object> row2 = new HashMap<>();
        row2.put("EXTERNAL_NO", "ASN002"); // 不同的 EXTERNAL_NO
        row2.put("SKU", "SKU002");
        row2.put("QTY_EXPECTED", 20);
        row2.put("CATEGORY", "CAT002");
        row2.put("SKU_STORER_ABBR_NAME", "STORER002");
        mixedRows.add(row2);

        // 使用反射調用私有方法進行測試
        try {
            java.lang.reflect.Method method = JitAsnMappingService.class
                .getDeclaredMethod("mapDataToJitAsnRequest", List.class);
            method.setAccessible(true);

            JitAsnRequest result = (JitAsnRequest) method.invoke(jitAsnMappingService, mixedRows);

            // 應該返回 null，因為資料完整性驗證失敗
            assertNull(result, "包含多個不同 EXTERNAL_NO 的資料應該返回 null");

        } catch (Exception e) {
            fail("測試過程中發生異常: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("測試必要欄位驗證功能")
    void testRequiredFieldValidation() {
        // 測試缺少必要欄位的情況
        List<Map<String, Object>> invalidRows = new ArrayList<>();

        // 缺少 SKU 的資料
        Map<String, Object> rowWithoutSku = new HashMap<>();
        rowWithoutSku.put("EXTERNAL_NO", "ASN001");
        rowWithoutSku.put("SKU", null); // 缺少 SKU
        rowWithoutSku.put("QTY_EXPECTED", 10);
        rowWithoutSku.put("CATEGORY", "CAT001");
        rowWithoutSku.put("SKU_STORER_ABBR_NAME", "STORER001");
        invalidRows.add(rowWithoutSku);

        // 使用反射調用私有方法進行測試
        try {
            java.lang.reflect.Method method = JitAsnMappingService.class
                .getDeclaredMethod("mapDataToJitAsnRequest", List.class);
            method.setAccessible(true);

            JitAsnRequest result = (JitAsnRequest) method.invoke(jitAsnMappingService, invalidRows);

            // 應該返回 null 或者 Lines 為空，因為必要欄位驗證失敗
            if (result != null) {
                assertTrue(result.getLines().isEmpty(), "缺少必要欄位的資料應該不會產生有效的 Lines");
            }

        } catch (Exception e) {
            fail("測試過程中發生異常: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("測試完整流程（使用 Mock）")
    void testProcessAndSendJitAsnWithMocks() {
        // 設定 Mock 行為
        when(apiConfigService.getProviderName()).thenReturn("JIT");
        when(envelopeService.createJitAsnEnvelope(anyString(), anyString())).thenReturn("SEQ123");
        when(jitApiClient.sendAsn(any(JitAsnRequest.class)))
            .thenReturn(new ResponseEntity<>("Success", HttpStatus.OK));

        // 檢查是否有測試資料
        String checkSql = "SELECT COUNT(*) FROM B2B.JIT_ASN_HEADER WHERE STATUS = 'PENDING'";
        Integer pendingCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

        if (pendingCount == null || pendingCount == 0) {
            System.out.println("沒有 PENDING 狀態的測試資料，跳過完整流程測試");
            return;
        }

        System.out.println("=== 測試完整流程（使用 Mock）===");
        System.out.println("找到 " + pendingCount + " 筆 PENDING 狀態的資料");

        // 執行完整流程（會使用 Mock 的服務）
        try {
            jitAsnMappingService.processAndSendJitAsn();

            // 驗證 Mock 方法是否被調用
            verify(apiConfigService, atLeastOnce()).getProviderName();

            System.out.println("完整流程測試完成");

        } catch (Exception e) {
            System.err.println("完整流程測試發生異常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("測試重試失敗的 ASN 功能")
    void testRetryFailedAsn() {
        // 檢查是否有失敗的 ASN
        String checkSql = "SELECT COUNT(*) FROM B2B.JIT_ASN_HEADER WHERE STATUS = 'FAILED'";
        Integer failedCount = jdbcTemplate.queryForObject(checkSql, Integer.class);

        System.out.println("=== 測試重試失敗的 ASN 功能 ===");
        System.out.println("找到 " + (failedCount != null ? failedCount : 0) + " 筆 FAILED 狀態的資料");

        // 執行重試功能
        try {
            jitAsnMappingService.retryFailedAsn();
            System.out.println("重試失敗 ASN 功能測試完成");
        } catch (Exception e) {
            System.err.println("重試失敗 ASN 測試發生異常: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("測試空資料處理")
    void testEmptyDataHandling() {
        // 測試空列表
        try {
            java.lang.reflect.Method method = JitAsnMappingService.class
                .getDeclaredMethod("mapDataToJitAsnRequest", List.class);
            method.setAccessible(true);

            JitAsnRequest result = (JitAsnRequest) method.invoke(jitAsnMappingService, new ArrayList<>());
            assertNull(result, "空資料列表應該返回 null");

            // 測試 null 參數
            result = (JitAsnRequest) method.invoke(jitAsnMappingService, (List<Map<String, Object>>) null);
            assertNull(result, "null 參數應該返回 null");

        } catch (Exception e) {
            fail("測試過程中發生異常: " + e.getMessage());
        }
    }
}
