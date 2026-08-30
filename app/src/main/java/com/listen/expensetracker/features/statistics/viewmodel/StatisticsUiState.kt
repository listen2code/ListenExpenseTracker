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
    data object ScrollToTop : StatisticsEffect
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
 * 统计分析专属类型安全 Tab 标签枚举 (StatisticsTab)。
 *
 * 【教学重点 - 枚举替代魔数字符串】：
 * 1. 严格遵守 PROMPTS.md Rule 17，彻底杜绝 `"EXPENSE"` 与 `"INCOME"` 字符串直接在 UI 与 State 中流转。
 * 2. 赋予编译器完整的类型检查能力，结合 `when (tab)` 表达式可享受穷举安全性（Exhaustive Check），新增 Tab 时未处理的分支会触发编译报错，避免隐蔽 Bug。
 */
enum class StatisticsTab {
    EXPENSE, // 支出分析维度
    INCOME   // 收入分析维度
}

/**
 * User Intents for Statistics feature.
 */
sealed interface StatisticsIntent {
    data class ChangeMonthOffset(val offsetDelta: Int) : StatisticsIntent
    data class SetMonthOffset(val offset: Int) : StatisticsIntent
    data class ChangeStatisticsTab(val tab: StatisticsTab) : StatisticsIntent
    data class ToggleHideAmount(val hide: Boolean) : StatisticsIntent
    data object OpenMonthPicker : StatisticsIntent
    data object DismissMonthPicker : StatisticsIntent
    data object ScrollToTop : StatisticsIntent
    data class SelectMonth(val offset: Int) : StatisticsIntent
}

