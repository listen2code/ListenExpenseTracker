package com.listen.expensetracker.features.transactions.components

import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.listen.expensetracker.features.budget.components.BudgetDialogMode
import com.listen.expensetracker.features.budget.components.CategoryBudgetModalDialog
import com.listen.expensetracker.features.common.components.MonthPickerDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import java.util.Calendar

/**
 * Dedicated Dialog and Sheet Host for Transactions Feature.
 * Encapsulates presentation, parameter assembly, and intent dispatching for all ledger modals.
 */
@Composable
fun TransactionsDialogHost(
    state: TransactionsUiState,
    viewModel: TransactionsViewModel? = null
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
            TransactionSheet(
                currencySymbol = sym,
                initialTimestamp = initialDate,
                initialCategoryId = dialog.initialCategoryId,
                initialType = dialog.initialType,
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                onSave = { entity ->
                    viewModel?.handleIntent(TransactionsIntent.AddTransaction(
                        type = entity.type, categoryId = entity.categoryId, categoryName = entity.categoryName,
                        categoryIcon = entity.categoryIcon, categoryColorHex = entity.categoryColorHex,
                        amount = entity.amount, note = entity.note, accountType = entity.accountType, timestamp = entity.timestamp
                    ))
                    viewModel?.handleIntent(TransactionsIntent.DismissDialog)
                },
                onSaveAndContinue = { entity ->
                    viewModel?.handleIntent(TransactionsIntent.AddTransaction(
                        type = entity.type, categoryId = entity.categoryId, categoryName = entity.categoryName,
                        categoryIcon = entity.categoryIcon, categoryColorHex = entity.categoryColorHex,
                        amount = entity.amount, note = entity.note, accountType = entity.accountType, timestamp = entity.timestamp
                    ))
                },
                lang = lang
            )
        }
        is TransactionsDialog.EditTransaction -> {
            TransactionSheet(
                transaction = dialog.transaction,
                currencySymbol = sym,
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                onSave = { updated ->
                    viewModel?.handleIntent(TransactionsIntent.UpdateTransaction(updated))
                    viewModel?.handleIntent(TransactionsIntent.DismissDialog)
                },
                onDelete = {
                    viewModel?.handleIntent(TransactionsIntent.DeleteTransaction(dialog.transaction.id))
                    viewModel?.handleIntent(TransactionsIntent.DismissDialog)
                },
                lang = lang
            )
        }
        is TransactionsDialog.ConfirmDelete -> {
            val tx = dialog.transaction
            TransactionDeleteConfirmDialog(
                categoryName = tx.categoryName,
                currencySymbol = sym,
                amount = tx.amount,
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                onConfirm = {
                    viewModel?.handleIntent(TransactionsIntent.DeleteTransaction(tx.id))
                    viewModel?.handleIntent(TransactionsIntent.DismissDialog)
                },
                lang = lang
            )
        }
        is TransactionsDialog.MonthPicker -> {
            MonthPickerDialog(
                currentMonthOffset = state.selectedMonthOffset,
                currentYearOffset = state.selectedYearOffset,
                isYearMode = state.period == TransactionPeriod.YEAR,
                onOffsetSelected = { offset ->
                    viewModel?.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
                    viewModel?.handleIntent(TransactionsIntent.SelectMonth(offset))
                    viewModel?.handleIntent(TransactionsIntent.DismissDialog)
                },
                onYearSelected = { offset ->
                    viewModel?.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.YEAR))
                    viewModel?.handleIntent(TransactionsIntent.SelectYear(offset))
                    viewModel?.handleIntent(TransactionsIntent.DismissDialog)
                },
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                lang = lang
            )
        }
        is TransactionsDialog.ManageAccount -> {
            AccountManageDialog(
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                onAccountChanged = { newKey ->
                    viewModel?.handleIntent(TransactionsIntent.FilterAccountChange(newKey))
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
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                onReset = { viewModel?.handleIntent(TransactionsIntent.ResetAllFilters) },
                onApply = { type, categories, preset, min, max, sort ->
                    viewModel?.handleIntent(TransactionsIntent.ApplyCompoundFilter(type, categories, preset, min, max, sort))
                }
            )
        }
        is TransactionsDialog.MonthlyBudget, is TransactionsDialog.CategoryBudgetEdit -> {
            CategoryBudgetModalDialog(
                allTransactions = state.transactions,
                monthlyBudget = state.monthlyBudget,
                categoryRatios = state.categoryBudgetRatios,
                currencySymbol = sym,
                lang = lang,
                hideAmount = state.hideBalance,
                initialMonthOffset = state.selectedMonthOffset,
                initialMode = if (dialog is TransactionsDialog.CategoryBudgetEdit) BudgetDialogMode.EDIT else BudgetDialogMode.VIEW,
                onDismiss = { viewModel?.handleIntent(TransactionsIntent.DismissDialog) },
                onSave = { newBudget, newRatios ->
                    viewModel?.handleIntent(TransactionsIntent.UpdateCategoryBudgets(newBudget, newRatios))
                }
            )
        }
        null -> Unit
    }
}
@Preview(showBackground = true)
@Composable
fun TransactionsDialogHostPreview() {
    ExpenseStrings.init()
    val state = TransactionsUiState()
    ListenTheme {
        TransactionsDialogHost(state = state)
    }
}
