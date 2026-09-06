package com.listen.expensetracker.features.transactions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.theme.ListenTheme

/**
 * 账单多维过滤抽屉预览 (TransactionFilterBottomSheetPreview)。
 */
@Preview(showBackground = true)
@Composable
fun TransactionFilterBottomSheetPreview() {
    ExpenseStrings.init()
    ListenTheme {
        TransactionFilterBottomSheet(
            currentType = "ALL",
            currentCategories = emptySet(),
            currentPreset = AmountFilterPreset.ALL,
            currentSortOrder = TransactionSortOrder.DATE_DESC,
            currentMin = null,
            currentMax = null,
            currencySymbol = "$",
            lang = "zh",
            onDismiss = {},
            onReset = {},
            onApply = { _, _, _, _, _, _ -> }
        )
    }
}
