package com.listen.expensetracker.features.statistics.viewmodel

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.uicomponent.charts.BarChartItem
import com.listen.uicomponent.charts.LineChartPoint
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

/**
 * Immutable UI State representing the multi-dimensional statistics and financial analytics presentation.
 */
data class StatisticsUiState(
    val allTransactions: List<TransactionEntity> = emptyList(),
    val categoryShares: List<PieChartItem> = emptyList(),
    val progressSegments: List<ProgressSegment> = emptyList(),
    val incomeCategoryShares: List<PieChartItem> = emptyList(),
    val incomeProgressSegments: List<ProgressSegment> = emptyList(),
    val dailyTrendBars: List<BarChartItem> = emptyList(),
    val dailyTrendPoints: List<LineChartPoint> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val monthlyBudget: Double = 5000.0,
    val remainingBudget: Double = 5000.0,
    val budgetUsageRatio: Float = 0.0f,
    val isOverBudget: Boolean = false,
    val dailyAverageExpense: Double = 0.0,
    val dailyAverageIncome: Double = 0.0,
    val maxExpenseTransaction: TransactionEntity? = null,
    val maxIncomeTransaction: TransactionEntity? = null,
    val statisticsTab: String = "EXPENSE",
    val selectedMonthOffset: Int = 0,
    val monthTitle: String = "本月",
    val currencySymbol: String = "￥",
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD,
    val showMonthPicker: Boolean = false,
    val hideAmount: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * User Intents for Statistics feature.
 */
sealed interface StatisticsIntent {
    data class ChangeMonthOffset(val offsetDelta: Int) : StatisticsIntent
    data class SetMonthOffset(val offset: Int) : StatisticsIntent
    data class ChangeStatisticsTab(val tab: String) : StatisticsIntent
    data class ToggleHideAmount(val hide: Boolean) : StatisticsIntent
    data object OpenMonthPicker : StatisticsIntent
    data object DismissMonthPicker : StatisticsIntent
}
