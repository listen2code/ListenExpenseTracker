package com.listen.expensetracker.features.statistics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.uicomponent.theme.ListenTheme

/**
 * 年度统计内容列表卡片集合预览 (AnnualStatisticsContentListPreview)。
 */
@Preview(showBackground = true)
@Composable
fun AnnualStatisticsContentListPreview() {
    ExpenseStrings.init()
    val state = StatisticsUiState(period = StatisticsPeriod.YEAR)
    ListenTheme {
        AnnualStatisticsContentList(
            state = state,
            yearOffset = 0,
            onIntent = {}
        )
    }
}
