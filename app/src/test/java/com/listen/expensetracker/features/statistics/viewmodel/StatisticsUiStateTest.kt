package com.listen.expensetracker.features.statistics.viewmodel

import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsUiStateTest {
    @Test
    fun testDefaultValues() {
        val state = StatisticsUiState()
        assertTrue(state.categoryShares.isEmpty())
        assertTrue(state.progressSegments.isEmpty())
        assertEquals(0.0, state.totalExpense, 0.0)
        assertEquals(0.0, state.totalIncome, 0.0)
        assertEquals(5000.0, state.monthlyBudget, 0.0)
        assertEquals(5000.0, state.remainingBudget, 0.0)
        assertEquals(0.0f, state.budgetUsageRatio, 0.0f)
        assertFalse(state.isOverBudget)
        assertNull(state.maxExpenseTransaction)
        assertEquals("EXPENSE", state.statisticsTab)
        assertEquals("￥", state.currencySymbol)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(AccentColor.EMERALD, state.accentColor)
        assertFalse(state.showMonthPicker)
        assertFalse(state.hideAmount)
        assertFalse(state.isLoading)
    }

    @Test
    fun testIntents() {
        val intentChangeMonth = StatisticsIntent.ChangeMonthOffset(1)
        assertEquals(1, intentChangeMonth.offsetDelta)

        val intentSetMonth = StatisticsIntent.SetMonthOffset(-1)
        assertEquals(-1, intentSetMonth.offset)

        val intentChangeTab = StatisticsIntent.ChangeStatisticsTab("INCOME")
        assertEquals("INCOME", intentChangeTab.tab)

        val intentToggleHide = StatisticsIntent.ToggleHideAmount(true)
        assertTrue(intentToggleHide.hide)
        
        val intentOpenPicker = StatisticsIntent.OpenMonthPicker
        val intentDismissPicker = StatisticsIntent.DismissMonthPicker
        
        assertTrue(intentOpenPicker is StatisticsIntent)
        assertTrue(intentDismissPicker is StatisticsIntent)
    }
}
