package com.listen.expensetracker

import com.listen.expensetracker.data.i18n.ExpenseStrings
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ExpenseStringsTest {

    @Before
    fun setUp() {
        ExpenseStrings.init()
    }

    @Test
    fun testExpenseLocalizedStrings() {
        assertEquals("流水", ExpenseStrings.get("nav_transactions", "zh"))
        assertEquals("Transactions", ExpenseStrings.get("nav_transactions", "en"))
        assertEquals("明細", ExpenseStrings.get("nav_transactions", "ja"))

        assertEquals("总支出", ExpenseStrings.get("total_expense", "zh"))
        assertEquals("Total Expense", ExpenseStrings.get("total_expense", "en"))
        assertEquals("支出合計", ExpenseStrings.get("total_expense", "ja"))

        assertEquals("搜索分类或备注...", ExpenseStrings.get("search_placeholder", "zh"))
        assertEquals("Search category or note...", ExpenseStrings.get("search_placeholder", "en"))
        assertEquals("カテゴリーやメモを検索...", ExpenseStrings.get("search_placeholder", "ja"))

        // Common strings fallback
        assertEquals("确定", ExpenseStrings.get("common_ok", "zh"))
        assertEquals("OK", ExpenseStrings.get("common_ok", "en"))
    }
}
