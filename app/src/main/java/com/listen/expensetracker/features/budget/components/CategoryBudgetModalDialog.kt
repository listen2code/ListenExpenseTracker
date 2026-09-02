package com.listen.expensetracker.features.budget.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog

enum class BudgetDialogMode {
    VIEW, EDIT
}

/**
 * 分类预算统一模态对话框 (CategoryBudgetModalDialog)。
 * 在同一个 Dialog 窗口内平滑推移与交叉淡入淡出切换看板与编辑模式，彻底消除窗口重构闪烁。
 */
@Composable
fun CategoryBudgetModalDialog(
    allTransactions: List<TransactionEntity>,
    monthlyBudget: Double,
    categoryRatios: Map<String, Float>,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, Map<String, Float>) -> Unit,
    modifier: Modifier = Modifier,
    initialMonthOffset: Int = 0,
    initialMode: BudgetDialogMode = BudgetDialogMode.VIEW
) {
    var mode by remember(initialMode) { mutableStateOf(initialMode) }
    var activeMonthOffset by remember(initialMonthOffset) { androidx.compose.runtime.mutableIntStateOf(initialMonthOffset) }

    var editBudgetInput by remember(monthlyBudget) {
        mutableStateOf(if (monthlyBudget > 0) "%.0f".format(monthlyBudget) else "5000")
    }
    var editRatios by remember(categoryRatios) { mutableStateOf(categoryRatios) }

    val editTotalBudget = editBudgetInput.toDoubleOrNull() ?: 0.0

    CommonDialog(
        onDismissRequest = {
            if (mode == BudgetDialogMode.EDIT && initialMode == BudgetDialogMode.VIEW) {
                mode = BudgetDialogMode.VIEW
            } else {
                onDismiss()
            }
        },
        title = if (mode == BudgetDialogMode.VIEW) {
            AppStrings.BUDGET_CENTER_TITLE.tr(lang)
        } else {
            AppStrings.BUDGET_ADJUST_TITLE.tr(lang)
        },
        modifier = modifier,
        confirmButton = {
            if (mode == BudgetDialogMode.VIEW) {
                CommonButton(
                    text = AppStrings.COMMON_DONE.tr(lang),
                    onClick = onDismiss,
                    style = CommonButtonStyle.Primary
                )
            } else {
                CommonButton(
                    text = AppStrings.COMMON_SAVE.tr(lang),
                    onClick = {
                        onSave(editTotalBudget.coerceAtLeast(1.0), editRatios)
                        if (initialMode == BudgetDialogMode.VIEW) {
                            mode = BudgetDialogMode.VIEW
                        } else {
                            onDismiss()
                        }
                    },
                    enabled = editTotalBudget > 0,
                    style = CommonButtonStyle.Primary
                )
            }
        },
        dismissButton = {
            if (mode == BudgetDialogMode.VIEW) {
                CommonButton(
                    text = AppStrings.BUDGET_ADJUST_TITLE.tr(lang),
                    onClick = { mode = BudgetDialogMode.EDIT },
                    style = CommonButtonStyle.Secondary,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            } else {
                CommonButton(
                    text = AppStrings.COMMON_CANCEL.tr(lang),
                    onClick = {
                        if (initialMode == BudgetDialogMode.VIEW) {
                            editBudgetInput = if (monthlyBudget > 0) "%.0f".format(monthlyBudget) else "5000"
                            editRatios = categoryRatios
                            mode = BudgetDialogMode.VIEW
                        } else {
                            onDismiss()
                        }
                    },
                    style = CommonButtonStyle.Text
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(440.dp)
        ) {
            AnimatedContent(
                targetState = mode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState == BudgetDialogMode.EDIT) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "BudgetDialogTransition"
            ) { currentMode ->
                if (currentMode == BudgetDialogMode.VIEW) {
                    CategoryBudgetCenterContent(
                        allTransactions = allTransactions,
                        monthlyBudget = monthlyBudget,
                        categoryRatios = categoryRatios,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        hideAmount = hideAmount,
                        initialMonthOffset = activeMonthOffset,
                        onMonthOffsetChange = { activeMonthOffset = it },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CategoryBudgetEditContent(
                        budgetInput = editBudgetInput,
                        onBudgetInputChange = { editBudgetInput = it },
                        ratios = editRatios,
                        onRatiosChange = { editRatios = it },
                        currencySymbol = currencySymbol,
                        lang = lang,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
