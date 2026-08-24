package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.common.components.MonthPickerDialog
import com.listen.expensetracker.features.transactions.ui.AddTransactionSheet
import com.listen.expensetracker.features.transactions.ui.EditTransactionSheet
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState

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
            AlertDialog(
                onDismissRequest = { onIntent(TransactionsIntent.DismissDialog) },
                title = {
                    Text(
                        text = AppStrings.delete_transaction_title.tr(lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TextTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    val desc = AppStrings.delete_transaction_desc.tr(lang).format(tx.categoryName, sym, tx.amount)
                    Text(
                        text = desc,
                        fontSize = AppDimens.TextBody,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onIntent(TransactionsIntent.DeleteTransaction(tx.id))
                            onIntent(TransactionsIntent.DismissDialog)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(AppStrings.common_delete.tr(lang), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onIntent(TransactionsIntent.DismissDialog) }
                    ) {
                        Text(AppStrings.common_cancel.tr(lang))
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = MaterialTheme.colorScheme.surface
            )
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
