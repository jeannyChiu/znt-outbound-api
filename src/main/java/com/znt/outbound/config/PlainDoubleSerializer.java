package com.znt.outbound.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 自定義 Double 序列化器，避免科學記號法
 */
public class PlainDoubleSerializer extends JsonSerializer<Double> {

    @Override
    public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            gen.writeNumber(0.0);
            return;
        }
        
        // 使用 BigDecimal 來避免科學記號法
        BigDecimal bd = BigDecimal.valueOf(value);
        // 設定最多12位小數以支援高精度數值（如 0.00000000288），移除尾隨的零
        bd = bd.setScale(12, RoundingMode.HALF_UP).stripTrailingZeros();
        
        // 直接寫入數字，不使用科學記號法
        gen.writeRawValue(bd.toPlainString());
    }
}
