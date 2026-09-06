package com.listen.expensetracker.features.transactions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.theme.ListenTheme

/**
 * 流水内容列表预览 (TransactionsContentListPreview)。
 */
@Preview(showBackground = true)
@Composable
fun TransactionsContentListPreview() {
    ExpenseStrings.init()
    val state = TransactionsUiState()
    ListenTheme {
        TransactionsContentList(
            state = state,
            monthOffset = 0,
            onIntent = {}
        )
    }
}
