package com.listen.expensetracker.features.transactions.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
import com.listen.uicomponent.components.CommonText

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
            AddTransactionSheet(
                currencySymbol = sym,
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
            val desc = AppStrings.delete_transaction_desc.tr(lang).format(tx.categoryName, sym, tx.amount)
            CommonDialog(
                onDismissRequest = { onIntent(TransactionsIntent.DismissDialog) },
                title = AppStrings.delete_transaction_title.tr(lang),
                confirmButton = {
                    CommonButton(
                        text = AppStrings.common_delete.tr(lang),
                        onClick = {
                            onIntent(TransactionsIntent.DeleteTransaction(tx.id))
                            onIntent(TransactionsIntent.DismissDialog)
                        },
                        style = CommonButtonStyle.Danger
                    )
                },
                dismissButton = {
                    CommonButton(
                        text = AppStrings.common_cancel.tr(lang),
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
                    onIntent(TransactionsIntent.SetMonthOffset(offset))
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
        null -> Unit
    }
}
