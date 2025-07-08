package com.znt.outbound.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * 數字格式化工具類
 */
public class NumberFormatUtil {

    /**
     * 將 double 值格式化為固定小數位數的字串，避免科學記號法
     * @param value 要格式化的數值
     * @param scale 小數位數
     * @return 格式化後的字串
     */
    public static String formatDouble(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(scale, RoundingMode.HALF_UP);
        return bd.toPlainString();
    }

    /**
     * 將 double 值格式化為最多10位小數的字串，避免科學記號法
     * @param value 要格式化的數值
     * @return 格式化後的字串
     */
    public static String formatDoubleToPlainString(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        
        // 使用 BigDecimal 來避免科學記號法
        BigDecimal bd = BigDecimal.valueOf(value);
        return bd.toPlainString();
    }

    /**
     * 使用 DecimalFormat 格式化數字
     * @param value 要格式化的數值
     * @return 格式化後的字串
     */
    public static String formatWithDecimalFormat(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return "0";
        }
        
        DecimalFormat df = new DecimalFormat("0.##########");
        return df.format(value);
    }
}
