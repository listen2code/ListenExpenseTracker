package com.listen.expensetracker.features.statistics.viewmodel

import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.uicomponent.charts.BarChartItem
import com.listen.uicomponent.charts.LineChartPoint
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

sealed interface StatisticsEffect : CommonUiEffect {
    data class ScrollToMonth(val offset: Int) : StatisticsEffect
    data class ScrollToYear(val offset: Int) : StatisticsEffect
    data object ScrollToTop : StatisticsEffect
}

/**
 * 统计时间周期维度枚举 (按月 / 按年)。
 */
enum class StatisticsPeriod {
    MONTH, // 按月视图
    YEAR   // 按年视图
}

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
    val statisticsTab: StatisticsTab = StatisticsTab.EXPENSE,
    val period: StatisticsPeriod = StatisticsPeriod.MONTH,
    val selectedMonthOffset: Int = 0,
    val monthTitle: String = "本月",
    val selectedYearOffset: Int = 0,
    val yearTitle: String = "今年",
    val currencySymbol: String = "￥",
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD,
    val showMonthPicker: Boolean = false,
    val hideAmount: Boolean = false,
    val isLoading: Boolean = false
)

/**
 * 统计分析专属类型安全 Tab 标签枚举 (StatisticsTab)。
 */
enum class StatisticsTab {
    EXPENSE, // 支出分析维度
    INCOME   // 收入分析维度
}

/**
 * User Intents for Statistics feature.
 */
sealed interface StatisticsIntent {
    data class ChangePeriod(val period: StatisticsPeriod) : StatisticsIntent
    data class ChangeMonthOffset(val offsetDelta: Int) : StatisticsIntent
    data class SetMonthOffset(val offset: Int) : StatisticsIntent
    data class ChangeYearOffset(val offsetDelta: Int) : StatisticsIntent
    data class SetYearOffset(val offset: Int) : StatisticsIntent
    data class SelectYear(val offset: Int) : StatisticsIntent
    data class ChangeStatisticsTab(val tab: StatisticsTab) : StatisticsIntent
    data class ToggleHideAmount(val hide: Boolean) : StatisticsIntent
    data object OpenMonthPicker : StatisticsIntent
    data object DismissMonthPicker : StatisticsIntent
    data object ScrollToTop : StatisticsIntent
    data class SelectMonth(val offset: Int) : StatisticsIntent
}
