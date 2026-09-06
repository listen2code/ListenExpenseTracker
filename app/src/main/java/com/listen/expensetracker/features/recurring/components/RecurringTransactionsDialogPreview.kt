package com.listen.expensetracker.features.recurring.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 周期性账单管理弹窗预览 (RecurringTransactionsDialogPreview)。
 */
@Preview(showBackground = true)
@Composable
fun RecurringTransactionsDialogPreview() {
    ExpenseStrings.init()
    ListenTheme {
        RecurringTransactionsDialog(
            rules = emptyList(),
            monthlyBudget = 5000.0,
            currencySymbol = "$",
            lang = "zh",
            onDismiss = {},
            onSaveRule = {},
            onDeleteRule = {},
            onToggleRule = { _, _ -> }
        )
    }
}
