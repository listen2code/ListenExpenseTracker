package com.listen.expensetracker.data.engine

import java.util.Locale

/**
 * 全局统一金额格式化扩展函数 (Amount Formatting Standard Ext)。
 *
 * 遵循规范 (Rule 21):
 * 若金额格式化后末尾为 ".00" 或 ".0"，自动转换为整数形式，移除无意义的小数点和零。
 * 若有非零小数部分，则保留最多两位小数（例如 12.34、12.50）。
 *
 * 示例:
 * 100.00 -> "100"
 * 50.0   -> "50"
 * 12.34  -> "12.34"
 */
fun Double.formatAmount(): String {
    val str = String.format(Locale.US, "%.2f", this)
    return when {
        str.endsWith(".00") -> str.removeSuffix(".00")
        str.endsWith(".0") -> str.removeSuffix(".0")
        else -> str
    }
}

/**
 * 带有币种符号的快捷格式化扩展。
 * 示例: 100.0.formatWithCurrency("￥") -> "￥100"
 */
fun Double.formatWithCurrency(currencySymbol: String = "￥"): String {
    return currencySymbol + this.formatAmount()
}
