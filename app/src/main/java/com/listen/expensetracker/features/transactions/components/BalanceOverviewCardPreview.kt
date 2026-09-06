package com.listen.expensetracker.features.transactions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

@Preview(showBackground = true)
@Composable
fun BalanceOverviewCardPreview() {
    ExpenseStrings.init()
    ListenTheme {
        BalanceOverviewCard(
            currencySymbol = "$",
            netBalance = 1500.0,
            totalExpense = 2000.0,
            totalIncome = 3500.0,
            budgetUsageRatio = 0.65f,
            isOverBudget = false,
            hideBalance = false,
            lang = "en",
            monthlyBudget = 3000.0,
            remainingBudget = 1000.0
        )
    }
}
