package com.listen.expensetracker.features.recurring.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.engine.RecurringTransactionEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonEmpty

enum class RecurringDialogMode {
    LIST, EDIT
}

/**
 * 周期账单与订阅统一模态弹窗 (RecurringTransactionsDialog)。
 * 在同一个固定高度 (480.dp) 的 Dialog 窗口内平滑切换列表看板与规则编辑模式，彻底消除窗口重构与高度跳动。
 */
@Composable
fun RecurringTransactionsDialog(
    rules: List<RecurringRuleEntity>,
    monthlyBudget: Double,
    currencySymbol: String,
    lang: String,
    onDismiss: () -> Unit,
    onSaveRule: (RecurringRuleEntity) -> Unit,
    onDeleteRule: (String) -> Unit,
    onToggleRule: (RecurringRuleEntity, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(RecurringDialogMode.LIST) }
    var editingRule by remember { mutableStateOf<RecurringRuleEntity?>(null) }
    val editState = remember(editingRule, mode) { RecurringEditState(editingRule, lang) }

    val baseline = remember(rules) {
        RecurringTransactionEngine.calculateMonthlyBaseline(rules)
    }

    val dialogTitle = if (mode == RecurringDialogMode.LIST) {
        AppStrings.RECURRING_TITLE.tr(lang)
    } else if (editingRule != null) {
        AppStrings.RECURRING_EDIT_RULE.tr(lang)
    } else {
        AppStrings.RECURRING_ADD_RULE.tr(lang)
    }

    CommonDialog(
        onDismissRequest = {
            if (mode == RecurringDialogMode.EDIT) {
                mode = RecurringDialogMode.LIST
            } else {
                onDismiss()
            }
        },
        title = dialogTitle,
        modifier = modifier,
        confirmButton = {
            if (mode == RecurringDialogMode.LIST) {
                CommonButton(
                    text = AppStrings.COMMON_DONE.tr(lang),
                    style = CommonButtonStyle.Primary,
                    onClick = onDismiss
                )
            } else {
                CommonButton(
                    text = AppStrings.COMMON_SAVE.tr(lang),
                    style = CommonButtonStyle.Primary,
                    enabled = editState.isValid,
                    onClick = {
                        onSaveRule(editState.buildEntity())
                        mode = RecurringDialogMode.LIST
                    }
                )
            }
        },
        dismissButton = {
            if (mode == RecurringDialogMode.LIST) {
                CommonButton(
                    text = AppStrings.RECURRING_ADD_RULE.tr(lang),
                    style = CommonButtonStyle.Tonal,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        editingRule = null
                        mode = RecurringDialogMode.EDIT
                    }
                )
            } else if (editingRule != null) {
                CommonButton(
                    text = AppStrings.COMMON_DELETE.tr(lang),
                    style = CommonButtonStyle.Danger,
                    onClick = {
                        onDeleteRule(editingRule!!.id)
                        mode = RecurringDialogMode.LIST
                    }
                )
            } else {
                CommonButton(
                    text = AppStrings.COMMON_CANCEL.tr(lang),
                    style = CommonButtonStyle.Text,
                    onClick = { mode = RecurringDialogMode.LIST }
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(420.dp)
        ) {
            AnimatedContent(
                targetState = mode,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    if (targetState == RecurringDialogMode.EDIT) {
                        (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut(animationSpec = tween(180)))
                    } else {
                        (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = tween(220))) togetherWith
                                (slideOutHorizontally { width -> width / 3 } + fadeOut(animationSpec = tween(180)))
                    }
                },
                label = "RecurringDialogTransition"
            ) { currentMode ->
                if (currentMode == RecurringDialogMode.LIST) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
                    ) {
                        RecurringOverviewCard(
                            baseline = baseline,
                            monthlyBudget = monthlyBudget,
                            currencySymbol = currencySymbol,
                            lang = lang
                        )

                        if (rules.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                CommonEmpty(
                                    message = "${AppStrings.RECURRING_EMPTY_TITLE.tr(lang)}\n${AppStrings.RECURRING_EMPTY_DESC.tr(lang)}"
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                            ) {
                                items(items = rules, key = { it.id }) { rule ->
                                    RecurringRuleItemCard(
                                        rule = rule,
                                        currencySymbol = currencySymbol,
                                        lang = lang,
                                        onToggleEnabled = { onToggleRule(rule, it) },
                                        onClick = {
                                            editingRule = rule
                                            mode = RecurringDialogMode.EDIT
                                        },
                                        onLongClick = {
                                            editingRule = rule
                                            mode = RecurringDialogMode.EDIT
                                        }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    RecurringRuleEditContent(
                        state = editState,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
