package com.listen.expensetracker.features.recurring.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

@Preview(showBackground = true)
@Composable
fun RecurringRuleItemCardPreview() {
    ExpenseStrings.init()
    val sampleRule = RecurringRuleEntity(
        title = "Netflix Subscription",
        type = TransactionType.EXPENSE,
        categoryId = "c_entertainment",
        categoryName = "Entertainment",
        categoryIcon = "c_entertainment",
        categoryColorHex = "#8B5CF6",
        amount = 15.99,
        frequency = RecurringFrequency.MONTHLY,
        dayOfPeriod = 15,
        accountType = "CREDIT",
        isEnabled = true,
        nextExecutionDate = System.currentTimeMillis() + 5 * 24 * 60 * 60 * 1000L
    )
    ListenTheme {
        RecurringRuleItemCard(
            rule = sampleRule,
            currencySymbol = "$",
            lang = "en",
            onToggleEnabled = {},
            onClick = {},
            onLongClick = {}
        )
    }
}
