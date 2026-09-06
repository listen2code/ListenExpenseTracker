package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.theme.ListenTheme

/**
 * 记账表单核心子选择器预览 (TransactionFormComponentsPreview)。
 */
@Preview(showBackground = true)
@Composable
fun TransactionFormComponentsPreview() {
    ExpenseStrings.init()
    val categories = CategoryRepository.expenseCategories
    val accounts = AccountRepository.getAllAccounts()
    ListenTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            TransactionCategoryPicker(
                categories = categories,
                selectedCategory = categories.first(),
                onCategorySelected = {},
                lang = "zh"
            )
            TransactionAccountPicker(
                accounts = accounts,
                selectedAccount = accounts.first().key,
                onAccountSelected = {},
                onAccountLongClick = {},
                lang = "zh"
            )
            TransactionDatePickerButton(
                selectedTimestamp = System.currentTimeMillis(),
                onDateSelected = {}
            )
        }
    }
}
