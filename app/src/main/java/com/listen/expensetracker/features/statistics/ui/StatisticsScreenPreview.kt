package com.listen.expensetracker.features.statistics.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.theme.ListenTheme

/**
 * 统计主画面预览 (StatisticsScreenPreview)。
 */
@Preview(showBackground = true)
@Composable
fun StatisticsScreenPreview() {
    ExpenseStrings.init()
    val sampleCategories = listOf(
        PieChartItem(label = "餐饮", colorHex = "#EF4444", value = 1250.0, percentage = 50.8f),
        PieChartItem(label = "购物", colorHex = "#3B82F6", value = 890.0, percentage = 36.2f),
        PieChartItem(label = "交通", colorHex = "#10B981", value = 320.0, percentage = 13.0f)
    )
    val state = StatisticsUiState(
        categoryShares = sampleCategories,
        totalExpense = 2460.0,
        totalIncome = 8500.0,
        netBalance = 6040.0,
        currencySymbol = "¥",
        language = "zh"
    )
    ListenTheme {
        StatisticsScreen(
            state = state,
            onIntent = {}
        )
    }
}
