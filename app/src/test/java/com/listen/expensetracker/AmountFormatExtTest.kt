package com.listen.expensetracker

import com.listen.expensetracker.data.engine.defaultCurrencySymbolForLanguage
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.engine.formatPercentage
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 金额格式化扩展函数单元测试 (AmountFormatExtTest)。
 * 严格验证 Rule 21:
 * 1. 整数或末尾带 .00 / .0 的金额，必须转换为纯整数形式。
 * 2. 存在非零有效小数位的金额，完整保留两位小数。
 */
class AmountFormatExtTest {

    @Test
    fun formatAmount_trailingZeros_convertsToInteger() {
        assertEquals("100", 100.00.formatAmount())
        assertEquals("50", 50.0.formatAmount())
        assertEquals("0", 0.0.formatAmount())
        assertEquals("0", 0.00.formatAmount())
        assertEquals("1234", 1234.0.formatAmount())
    }

    @Test
    fun formatAmount_nonZeroDecimals_preservesDecimals() {
        assertEquals("12.34", 12.34.formatAmount())
        assertEquals("99.99", 99.99.formatAmount())
        assertEquals("0.5", 0.50.formatAmount())
        assertEquals("0.05", 0.05.formatAmount())
    }

    @Test
    fun formatAmount_negativeAmounts_worksCorrectly() {
        assertEquals("-100", (-100.00).formatAmount())
        assertEquals("-50", (-50.0).formatAmount())
        assertEquals("-12.34", (-12.34).formatAmount())
    }

    @Test
    fun defaultCurrencySymbolForLanguage_mapsCorrectly() {
        assertEquals("￥", defaultCurrencySymbolForLanguage("zh"))
        assertEquals("¥", defaultCurrencySymbolForLanguage("ja"))
        assertEquals("$", defaultCurrencySymbolForLanguage("en"))
        assertEquals("$", defaultCurrencySymbolForLanguage("fr"))
        assertEquals("$", defaultCurrencySymbolForLanguage(""))
    }

    @Test
    fun formatPercentage_trailingZeros_convertsToInteger() {
        assertEquals("100", 100.0.formatPercentage())
        assertEquals("100", 100.0f.formatPercentage())
        assertEquals("50", 50.0.formatPercentage())
        assertEquals("0", 0.0.formatPercentage())
        assertEquals("12.5", 12.50.formatPercentage())
        assertEquals("12.3", 12.34.formatPercentage())
    }
}

