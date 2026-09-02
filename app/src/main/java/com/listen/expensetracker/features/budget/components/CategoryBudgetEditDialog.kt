package com.listen.expensetracker.features.budget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * 分类预算与比例编辑弹窗兼容入口 (委托至 CategoryBudgetModalDialog)。
 */
@Composable
fun CategoryBudgetEditDialog(
    initialTotalBudget: Double,
    initialRatios: Map<String, Float>,
    currencySymbol: String,
    lang: String,
    onDismiss: () -> Unit,
    onSave: (Double, Map<String, Float>) -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryBudgetModalDialog(
        allTransactions = emptyList(),
        monthlyBudget = initialTotalBudget,
        categoryRatios = initialRatios,
        currencySymbol = currencySymbol,
        lang = lang,
        hideAmount = false,
        onDismiss = onDismiss,
        onSave = onSave,
        modifier = modifier,
        initialMode = BudgetDialogMode.EDIT
    )
}
