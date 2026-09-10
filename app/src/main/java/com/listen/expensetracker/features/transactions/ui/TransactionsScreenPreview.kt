package com.listen.expensetracker.features.transactions.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.theme.ListenTheme

/**
 * 流水主画面预览 (TransactionsScreenPreview)。
 */
@Preview(showBackground = true)
@Composable
fun TransactionsScreenPreview() {
    ExpenseStrings.init()
    val now = System.currentTimeMillis()
    val sampleTransactions = listOf(
        TransactionEntity(
            type = TransactionType.EXPENSE,
            categoryId = "c_food",
            categoryName = "餐饮",
            categoryIcon = "c_food",
            categoryColorHex = "#EF4444",
            amount = 45.0,
            timestamp = now,
            note = "午餐"
        ),
        TransactionEntity(
            type = TransactionType.EXPENSE,
            categoryId = "c_shopping",
            categoryName = "购物",
            categoryIcon = "c_shopping",
            categoryColorHex = "#3B82F6",
            amount = 120.0,
            timestamp = now - 3600000L,
            note = "超市采买"
        ),
        TransactionEntity(
            type = TransactionType.INCOME,
            categoryId = "c_salary",
            categoryName = "工资",
            categoryIcon = "c_salary",
            categoryColorHex = "#10B981",
            amount = 15000.0,
            timestamp = now - 86400000L,
            note = "月度薪资"
        )
    )
    val state = TransactionsUiState(
        transactions = sampleTransactions,
        filteredTransactions = sampleTransactions,
        totalExpense = 165.0,
        totalIncome = 15000.0,
        netBalance = 14835.0,
        monthlyBudget = 5000.0,
        currencySymbol = "¥",
        language = "zh"
    )
    ListenTheme {
        TransactionsScreen(
            state = state,
            onIntent = {}
        )
    }
}
