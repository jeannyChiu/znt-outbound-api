package com.znt.outbound.model.jit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JitAsnLineAttrTest {

    @Test
    public void testVLotSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        
        JitAsnLineAttr attr = new JitAsnLineAttr();
        attr.setLoc("IC-A-01");
        attr.setVLotValue("VL230612001");
        attr.setLotAttr01("无铅");
        
        String json = objectMapper.writeValueAsString(attr);
        System.out.println("Serialized JSON: " + json);
        
        // 驗證 JSON 中只有一個 VLot 欄位，沒有重複的 vlot
        assertTrue(json.contains("\"VLot\":\"VL230612001\""), "應該包含正確的 VLot 欄位");
        assertFalse(json.contains("\"vlot\":"), "不應該包含小寫的 vlot 欄位");
        
        // 計算 VLot 出現的次數
        int count = 0;
        int index = 0;
        while ((index = json.indexOf("VLot", index)) != -1) {
            count++;
            index += 4;
        }
        assertEquals(1, count, "VLot 應該只出現一次");
    }
}
