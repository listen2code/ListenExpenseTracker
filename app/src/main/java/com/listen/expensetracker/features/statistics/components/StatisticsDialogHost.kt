package com.listen.expensetracker.features.statistics.components

import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

import androidx.compose.runtime.Composable
import com.listen.expensetracker.features.common.components.MonthPickerDialog
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel

/**
 * Dedicated Dialog Host for Statistics Feature.
 * Manages MonthPickerDialog presentation and intent forwarding.
 */
@Composable
fun StatisticsDialogHost(
    state: StatisticsUiState,
    viewModel: StatisticsViewModel? = null
) {
    if (state.showMonthPicker) {
        MonthPickerDialog(
            currentMonthOffset = state.selectedMonthOffset,
            currentYearOffset = state.selectedYearOffset,
            isYearMode = state.period == StatisticsPeriod.YEAR,
            onOffsetSelected = { offset ->
                viewModel?.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
                viewModel?.handleIntent(StatisticsIntent.SelectMonth(offset))
                viewModel?.handleIntent(StatisticsIntent.DismissMonthPicker)
            },
            onYearSelected = { offset ->
                viewModel?.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.YEAR))
                viewModel?.handleIntent(StatisticsIntent.SelectYear(offset))
                viewModel?.handleIntent(StatisticsIntent.DismissMonthPicker)
            },
            onDismiss = { viewModel?.handleIntent(StatisticsIntent.DismissMonthPicker) },
            lang = state.language
        )
    }
}
@Preview(showBackground = true)
@Composable
fun StatisticsDialogHostPreview() {
    ExpenseStrings.init()
    val state = StatisticsUiState()
    ListenTheme {
        StatisticsDialogHost(state = state)
    }
}
