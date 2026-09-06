package com.listen.expensetracker.features.statistics.components

import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

import androidx.compose.runtime.Composable
import com.listen.expensetracker.features.common.components.MonthPickerDialog
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState

/**
 * Dedicated Dialog Host for Statistics Feature.
 * Manages MonthPickerDialog presentation and intent forwarding.
 */
@Composable
fun StatisticsDialogHost(
    state: StatisticsUiState,
    onIntent: (StatisticsIntent) -> Unit
) {
    if (state.showMonthPicker) {
        MonthPickerDialog(
            currentOffset = state.selectedMonthOffset,
            onOffsetSelected = { offset ->
                onIntent(StatisticsIntent.SelectMonth(offset))
                onIntent(StatisticsIntent.DismissMonthPicker)
            },
            onDismiss = { onIntent(StatisticsIntent.DismissMonthPicker) },
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
        StatisticsDialogHost(state = state, onIntent = {})
    }
}
