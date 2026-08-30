package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.KeyboardType
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.features.common.components.MonthPickerDialog
import com.listen.expensetracker.features.transactions.ui.AddTransactionSheet
import com.listen.expensetracker.features.transactions.ui.EditTransactionSheet
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonText
import java.util.Calendar

/**
 * Dedicated Dialog and Sheet Host for Transactions Feature.
 * Encapsulates presentation, parameter assembly, and intent dispatching for all ledger modals.
 */
@Composable
fun TransactionsDialogHost(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit
) {
    val lang = state.language
    val sym = state.currencySymbol

    when (val dialog = state.activeDialog) {
        is TransactionsDialog.AddTransaction -> {
            val initialDate = remember(state.selectedMonthOffset) {
                if (state.selectedMonthOffset != 0) {
                    Calendar.getInstance().apply {
                        add(Calendar.MONTH, state.selectedMonthOffset)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                } else {
                    System.currentTimeMillis()
                }
            }
            AddTransactionSheet(
                currencySymbol = sym,
                initialTimestamp = initialDate,
                onDismiss = { onIntent(TransactionsIntent.DismissDialog) },
                onSave = { type, catId, catName, catIcon, catColor, amt, note, acct, ts ->
                    onIntent(TransactionsIntent.AddTransaction(type, catId, catName, catIcon, catColor, amt, note, acct, ts))
                    onIntent(TransactionsIntent.DismissDialog)
                },
                lang = lang
            )
        }
        is TransactionsDialog.EditTransaction -> {
            EditTransactionSheet(
                transaction = dialog.transaction,
                currencySymbol = sym,
                onDismiss = { onIntent(TransactionsIntent.DismissDialog) },
                onSave = { updated ->
                    onIntent(TransactionsIntent.UpdateTransaction(updated))
                    onIntent(TransactionsIntent.DismissDialog)
                },
                onDelete = {
                    onIntent(TransactionsIntent.DeleteTransaction(dialog.transaction.id))
                    onIntent(TransactionsIntent.DismissDialog)
                },
                lang = lang
            )
        }
        is TransactionsDialog.ConfirmDelete -> {
            val tx = dialog.transaction
            val desc = AppStrings.DELETE_TRANSACTION_DESC.tr(lang).format(tx.categoryName, sym, tx.amount)
            CommonDialog(
                onDismissRequest = { onIntent(TransactionsIntent.DismissDialog) },
                title = AppStrings.DELETE_TRANSACTION_TITLE.tr(lang),
                confirmButton = {
                    CommonButton(
                        text = AppStrings.COMMON_DELETE.tr(lang),
                        onClick = {
                            onIntent(TransactionsIntent.DeleteTransaction(tx.id))
                            onIntent(TransactionsIntent.DismissDialog)
                        },
                        style = CommonButtonStyle.Danger
                    )
                },
                dismissButton = {
                    CommonButton(
                        text = AppStrings.COMMON_CANCEL.tr(lang),
                        onClick = { onIntent(TransactionsIntent.DismissDialog) },
                        style = CommonButtonStyle.Outlined
                    )
                }
            ) {
                CommonText(
                    text = desc,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is TransactionsDialog.MonthPicker -> {
            MonthPickerDialog(
                currentOffset = state.selectedMonthOffset,
                onOffsetSelected = { offset ->
                    onIntent(TransactionsIntent.SelectMonth(offset))
                    onIntent(TransactionsIntent.DismissDialog)
                },
                onDismiss = { onIntent(TransactionsIntent.DismissDialog) },
                lang = lang
            )
        }
        is TransactionsDialog.ManageAccount -> {
            AccountManageDialog(
                onDismiss = { onIntent(TransactionsIntent.DismissDialog) },
                onAccountChanged = { newKey ->
                    onIntent(TransactionsIntent.FilterAccountChange(newKey))
                },
                lang = lang
            )
        }
        is TransactionsDialog.FilterSheet -> {
            TransactionFilterBottomSheet(
                currentType = state.typeFilter,
                currentCategories = state.selectedCategories,
                currentPreset = state.amountPreset,
                currentSortOrder = state.sortOrder,
                currentMin = state.customMinAmount,
                currentMax = state.customMaxAmount,
                currencySymbol = sym,
                lang = lang,
                onDismiss = { onIntent(TransactionsIntent.DismissDialog) },
                onReset = { onIntent(TransactionsIntent.ResetAllFilters) },
                onApply = { type, categories, preset, min, max, sort ->
                    onIntent(TransactionsIntent.ApplyCompoundFilter(type, categories, preset, min, max, sort))
                }
            )
        }
        is TransactionsDialog.MonthlyBudget -> {
            MonthlyBudgetDialog(
                currentBudget = state.monthlyBudget,
                currencySymbol = sym,
                lang = lang,
                spentAmount = state.totalExpense,
                onDismiss = { onIntent(TransactionsIntent.DismissDialog) },
                onConfirm = { newBudget ->
                    onIntent(TransactionsIntent.UpdateMonthlyBudget(newBudget))
                    onIntent(TransactionsIntent.DismissDialog)
                }
            )
        }
        null -> Unit
    }
}
