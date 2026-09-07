package com.listen.expensetracker.features.common.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 月份选择弹窗预览 (MonthPickerDialogPreview)。
 */
@Preview(showBackground = true)
@Composable
fun MonthPickerDialogPreview() {
    ExpenseStrings.init()
    ListenTheme {
        MonthPickerDialog(
            currentMonthOffset = 0,
            onOffsetSelected = {},
            onDismiss = {},
            lang = "zh"
        )
    }
}
