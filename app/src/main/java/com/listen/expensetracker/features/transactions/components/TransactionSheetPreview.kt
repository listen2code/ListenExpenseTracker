package com.listen.expensetracker.features.transactions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 记账抽屉预览组件 (TransactionSheetPreview)。
 */
@Preview(showBackground = true)
@Composable
fun TransactionSheetAddPreview() {
    ExpenseStrings.init()
    ListenTheme {
        TransactionSheet(
            currencySymbol = "$",
            onDismiss = {},
            onSave = {},
            lang = "en"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TransactionSheetEditPreview() {
    ExpenseStrings.init()
    val sampleTransaction = TransactionEntity(
        type = TransactionType.EXPENSE,
        categoryId = "c_food",
        categoryName = "Food",
        categoryIcon = "c_food",
        categoryColorHex = "#EF4444",
        amount = 42.5,
        note = "Lunch at McDonald's",
        accountType = "CASH"
    )
    ListenTheme {
        TransactionSheet(
            currencySymbol = "$",
            onDismiss = {},
            onSave = {},
            transaction = sampleTransaction,
            onDelete = {},
            lang = "en"
        )
    }
}
