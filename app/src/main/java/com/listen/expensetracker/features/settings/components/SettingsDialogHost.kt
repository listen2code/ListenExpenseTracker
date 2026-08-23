package com.listen.expensetracker.features.settings.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.settings.ui.ImportBackupSheet
import com.listen.expensetracker.features.settings.viewmodel.SettingsDialog
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.uicomponent.theme.ExpenseRed

/**
 * Dedicated Dialog Host for Settings Feature.
 * Encapsulates presentation and intent dispatching for budget, categories, currency, clear confirmation, and backup import.
 */
@Composable
fun SettingsDialogHost(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit
) {
    val lang = state.language
    val sym = state.currencySymbol

    when (state.activeDialog) {
        is SettingsDialog.ImportBackup -> {
            ImportBackupSheet(
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                onImport = { json ->
                    onIntent(SettingsIntent.ImportBackupData(json))
                    onIntent(SettingsIntent.DismissDialog)
                },
                lang = lang
            )
        }
        is SettingsDialog.CurrencySelect -> {
            CurrencySelectDialog(
                currentSymbol = sym,
                onSymbolSelected = {
                    onIntent(SettingsIntent.ChangeCurrencySymbol(it))
                    onIntent(SettingsIntent.DismissDialog)
                },
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                lang = lang
            )
        }
        is SettingsDialog.CategoryManage -> {
            CategoryManageDialog(
                type = "EXPENSE",
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                onCategoriesChanged = {},
                lang = lang
            )
        }
        is SettingsDialog.MonthlyBudget -> {
            var budgetInput by remember { mutableStateOf(state.monthlyBudget.toInt().toString()) }
            AlertDialog(
                onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
                title = { Text(AppStrings.budget_dialog_title.tr(lang), fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it },
                        label = { Text(AppStrings.monthly_budget.tr(lang)) },
                        singleLine = true,
                        shape = RoundedCornerShape(AppDimens.CornerButton),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = budgetInput.toDoubleOrNull() ?: state.monthlyBudget
                            onIntent(SettingsIntent.UpdateMonthlyBudget(amount))
                            onIntent(SettingsIntent.DismissDialog)
                        },
                        shape = RoundedCornerShape(AppDimens.CornerButton)
                    ) {
                        Text(AppStrings.btn_save.tr(lang))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(SettingsIntent.DismissDialog) }) {
                        Text(AppStrings.btn_cancel.tr(lang))
                    }
                }
            )
        }
        is SettingsDialog.ClearConfirm -> {
            AlertDialog(
                onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
                title = { Text(AppStrings.confirm_clear_title.tr(lang), fontWeight = FontWeight.Bold, color = ExpenseRed) },
                text = { Text(AppStrings.confirm_clear_desc.tr(lang)) },
                confirmButton = {
                    Button(
                        onClick = {
                            onIntent(SettingsIntent.ClearAllData)
                            onIntent(SettingsIntent.DismissDialog)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(AppDimens.CornerButton)
                    ) {
                        Text(AppStrings.btn_delete.tr(lang))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onIntent(SettingsIntent.DismissDialog) }) {
                        Text(AppStrings.btn_cancel.tr(lang))
                    }
                }
            )
        }
        is SettingsDialog.AboutApp -> {
            AboutAppDialog(
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                lang = lang
            )
        }
        null -> Unit
    }
}
