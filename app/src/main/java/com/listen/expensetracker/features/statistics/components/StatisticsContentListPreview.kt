package com.listen.expensetracker.features.statistics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.uicomponent.theme.ListenTheme

/**
 * 统计内容列表卡片集合预览 (StatisticsContentListPreview)。
 */
@Preview(showBackground = true)
@Composable
fun StatisticsContentListPreview() {
    ExpenseStrings.init()
    val state = StatisticsUiState()
    ListenTheme {
        StatisticsContentList(
            state = state,
            monthOffset = 0,
            onIntent = {}
        )
    }
}
