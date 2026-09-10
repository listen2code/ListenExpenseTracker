package com.listen.expensetracker.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * Excel / CSV 导出选项弹窗预览 (ExportOptionsSheetPreview)。
 */
@Preview(showBackground = true)
@Composable
fun ExportOptionsSheetPreview() {
    ExpenseStrings.init()
    val sampleTransactions = listOf(
        TransactionEntity(
            type = TransactionType.EXPENSE,
            categoryId = "c_food",
            categoryName = "Food",
            categoryIcon = "c_food",
            categoryColorHex = "#EF4444",
            amount = 35.0,
            note = "Lunch"
        ),
        TransactionEntity(
            type = TransactionType.INCOME,
            categoryId = "c_salary",
            categoryName = "Salary",
            categoryIcon = "c_salary",
            categoryColorHex = "#10B981",
            amount = 8000.0,
            note = "Monthly salary"
        )
    )
    ListenTheme {
        ExportOptionsSheet(
            transactions = sampleTransactions,
            currencySymbol = "¥",
            onSaveToFile = { _, _, _, _ -> },
            onShare = { _, _, _ -> },
            onDismiss = {},
            lang = "zh"
        )
    }
}
