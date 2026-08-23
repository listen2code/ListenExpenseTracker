package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsUiStateTest {

    @Test
    fun testTransactionsDefaultStateValues() {
        val state = TransactionsUiState()
        assertTrue(state.transactions.isEmpty())
        assertTrue(state.filteredTransactions.isEmpty())
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(0.0, state.netBalance, 0.001)
        assertEquals(5000.0, state.monthlyBudget, 0.001)
        assertEquals(0.0f, state.budgetUsageRatio, 0.001f)
        assertFalse(state.isOverBudget)
        assertFalse(state.hideBalance)
        assertFalse(state.isLoading)
        assertEquals("￥", state.currencySymbol)
        assertEquals("zh", state.language)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(AccentColor.EMERALD, state.accentColor)
        assertEquals(TransactionSortOrder.DATE_DESC, state.sortOrder)
    }

    @Test
    fun testStatisticsDefaultStateValues() {
        val statsState = StatisticsUiState()
        assertEquals("EXPENSE", statsState.statisticsTab)
        assertEquals(0.0, statsState.totalExpense, 0.001)
        assertTrue(statsState.categoryShares.isEmpty())
        assertTrue(statsState.dailyTrendBars.isEmpty())
    }

    @Test
    fun testSettingsDefaultStateValues() {
        val settingsState = SettingsUiState()
        assertEquals("zh", settingsState.language)
        assertEquals(ThemeMode.SYSTEM, settingsState.themeMode)
        assertEquals(AccentColor.EMERALD, settingsState.accentColor)
        assertFalse(settingsState.isLoggedIn)
    }

    @Test
    fun testTransactionsStateCalculationsAndCopy() {
        val tx1 = TransactionEntity(
            id = "1",
            type = "EXPENSE",
            categoryId = "c_food",
            categoryName = "餐饮",
            categoryIcon = "c_food",
            categoryColorHex = "#EF4444",
            amount = 3000.0,
            note = "大餐",
            accountType = "WECHAT"
        )
        val tx2 = TransactionEntity(
            id = "2",
            type = "INCOME",
            categoryId = "c_salary",
            categoryName = "工资",
            categoryIcon = "c_salary",
            categoryColorHex = "#10B981",
            amount = 8000.0,
            note = "月薪",
            accountType = "BANK"
        )

        val state = TransactionsUiState(
            transactions = listOf(tx1, tx2),
            totalExpense = 3000.0,
            totalIncome = 8000.0,
            netBalance = 5000.0,
            monthlyBudget = 2000.0,
            budgetUsageRatio = 1.5f,
            isOverBudget = true
        )

        assertEquals(2, state.transactions.size)
        assertEquals(5000.0, state.netBalance, 0.001)
        assertTrue(state.isOverBudget)
        assertEquals(1.5f, state.budgetUsageRatio, 0.001f)

        val updated = state.copy(hideBalance = true, currencySymbol = "$")
        assertTrue(updated.hideBalance)
        assertEquals("$", updated.currencySymbol)
    }
}
