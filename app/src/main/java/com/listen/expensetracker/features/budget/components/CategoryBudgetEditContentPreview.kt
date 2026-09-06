package com.listen.expensetracker.features.budget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 分类预算配置内容区域预览 (CategoryBudgetEditContentPreview)。
 */
@Preview(showBackground = true)
@Composable
fun CategoryBudgetEditContentPreview() {
    ExpenseStrings.init()
    ListenTheme {
        CategoryBudgetEditContent(
            budgetInput = "5000",
            onBudgetInputChange = {},
            ratios = mapOf("c_food" to 0.4f, "c_transport" to 0.2f, "c_shopping" to 0.4f),
            onRatiosChange = {},
            currencySymbol = "$",
            lang = "zh"
        )
    }
}
