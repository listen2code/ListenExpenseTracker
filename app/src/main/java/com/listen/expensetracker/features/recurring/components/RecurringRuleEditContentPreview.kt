package com.listen.expensetracker.features.recurring.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 周期性账单规则编辑内容预览 (RecurringRuleEditContentPreview)。
 */
@Preview(showBackground = true)
@Composable
fun RecurringRuleEditContentPreview() {
    ExpenseStrings.init()
    val state = RecurringEditState(initialRule = null, lang = "zh")
    ListenTheme {
        RecurringRuleEditContent(
            state = state,
            currencySymbol = "$",
            lang = "zh"
        )
    }
}
