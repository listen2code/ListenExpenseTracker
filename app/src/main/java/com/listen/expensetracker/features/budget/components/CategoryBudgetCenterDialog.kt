package com.listen.expensetracker.features.budget.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.listen.expensetracker.data.db.TransactionEntity

/**
 * 分类预算管理中心看板弹窗兼容入口 (委托至 CategoryBudgetModalDialog)。
 */
@Composable
fun CategoryBudgetCenterDialog(
    allTransactions: List<TransactionEntity>,
    monthlyBudget: Double,
    categoryRatios: Map<String, Float>,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean,
    onDismiss: () -> Unit,
    onOpenEdit: () -> Unit,
    modifier: Modifier = Modifier,
    initialMonthOffset: Int = 0
) {
    CategoryBudgetModalDialog(
        allTransactions = allTransactions,
        monthlyBudget = monthlyBudget,
        categoryRatios = categoryRatios,
        currencySymbol = currencySymbol,
        lang = lang,
        hideAmount = hideAmount,
        onDismiss = onDismiss,
        onSave = { _, _ -> },
        modifier = modifier,
        initialMonthOffset = initialMonthOffset,
        initialMode = BudgetDialogMode.VIEW
    )
}
