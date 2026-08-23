package com.listen.expensetracker.features.statistics.components

import androidx.compose.runtime.Composable
import com.listen.expensetracker.features.settings.components.MonthPickerDialog
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
                onIntent(StatisticsIntent.ChangeMonthOffset(offset - state.selectedMonthOffset))
                onIntent(StatisticsIntent.DismissMonthPicker)
            },
            onDismiss = { onIntent(StatisticsIntent.DismissMonthPicker) },
            lang = state.language
        )
    }
}
