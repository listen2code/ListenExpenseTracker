package com.listen.expensetracker.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.listen.expensetracker.features.settings.components.AboutAppDialog
import com.listen.expensetracker.features.settings.components.CategoryManageDialog
import com.listen.expensetracker.features.settings.components.CurrencySelectDialog
import com.listen.expensetracker.features.settings.components.GoogleLinkDialog
import com.listen.expensetracker.features.settings.components.SettingsAppearanceSection
import com.listen.expensetracker.features.settings.components.SettingsApmSection
import com.listen.expensetracker.features.settings.components.SettingsCloudSection
import com.listen.expensetracker.features.settings.components.SettingsDataSection
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.theme.ExpenseRed

/**
 * Settings Screen cleanly orchestrating Cloud Sync, Appearance, Data Management, and System Ops.
 */
@Composable
fun SettingsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    onOpenApmInspector: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenImportSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showGoogleLinkDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
    ) {
        // 1. Cloud Sync & Google Account Section
        item(key = "cloud_section") {
            SettingsCloudSection(
                googleAccountEmail = state.googleAccountEmail,
                googleDisplayName = state.googleDisplayName,
                syncState = state.syncState,
                onLoginGoogle = { showGoogleLinkDialog = true },
                onLogoutGoogle = { onIntent(TransactionsIntent.UnlinkGoogleAccount) },
                onTriggerBackup = { onIntent(TransactionsIntent.TriggerCloudBackup) },
                onTriggerRestore = { onIntent(TransactionsIntent.TriggerCloudRestore) },
                lang = lang
            )
        }

        // 2. Personalization & Appearance Section
        item(key = "appearance_section") {
            SettingsAppearanceSection(
                themeMode = state.themeMode,
                accentColor = state.accentColor,
                currencySymbol = sym,
                language = lang,
                onChangeThemeMode = { onIntent(TransactionsIntent.ChangeThemeMode(it)) },
                onChangeAccentColor = { onIntent(TransactionsIntent.ChangeAccentColor(it)) },
                onOpenCurrencyDialog = { showCurrencyDialog = true },
                onLanguageChange = { onIntent(TransactionsIntent.ChangeLanguage(it)) },
                lang = lang
            )
        }

        // 3. Local Data Management Section
        item(key = "data_section") {
            SettingsDataSection(
                monthlyBudget = state.monthlyBudget,
                currencySymbol = sym,
                onOpenBudgetDialog = { showBudgetDialog = true },
                onOpenCategoryDialog = { showCategoryDialog = true },
                onExportJson = onExportJson,
                onExportCsv = onExportCsv,
                onOpenImportSheet = onOpenImportSheet,
                lang = lang
            )
        }

        // 4. System Ops & APM Observability Section
        item(key = "apm_section") {
            SettingsApmSection(
                onOpenApmInspector = onOpenApmInspector,
                onSeedDemoData = { onIntent(TransactionsIntent.SeedDemoData) },
                onConfirmClearAll = { showClearConfirmDialog = true },
                onOpenAboutDialog = { showAboutDialog = true },
                lang = lang
            )
        }
    }

    if (showGoogleLinkDialog) {
        GoogleLinkDialog(
            currentEmail = state.googleAccountEmail,
            onAccountLinked = { email ->
                onIntent(TransactionsIntent.LinkGoogleAccount(email = email, displayName = email.substringBefore("@")))
            },
            onDismiss = { showGoogleLinkDialog = false },
            lang = lang
        )
    }

    if (showCurrencyDialog) {
        CurrencySelectDialog(
            currentSymbol = sym,
            onSymbolSelected = { onIntent(TransactionsIntent.ChangeCurrencySymbol(it)) },
            onDismiss = { showCurrencyDialog = false },
            lang = lang
        )
    }

    if (showCategoryDialog) {
        CategoryManageDialog(
            type = "EXPENSE",
            onDismiss = { showCategoryDialog = false },
            onCategoriesChanged = {},
            lang = lang
        )
    }

    if (showBudgetDialog) {
        var budgetInput by remember { mutableStateOf(state.monthlyBudget.toInt().toString()) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(StringsRes.get("budget_dialog_title", lang), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text(StringsRes.get("monthly_budget", lang)) },
                    singleLine = true,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = budgetInput.toDoubleOrNull() ?: state.monthlyBudget
                        onIntent(TransactionsIntent.UpdateMonthlyBudget(amount))
                        showBudgetDialog = false
                    },
                    shape = RoundedCornerShape(AppDimens.CornerButton)
                ) {
                    Text(StringsRes.get("btn_save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(StringsRes.get("btn_cancel", lang))
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(StringsRes.get("confirm_clear_title", lang), fontWeight = FontWeight.Bold, color = ExpenseRed) },
            text = { Text(StringsRes.get("confirm_clear_desc", lang)) },
            confirmButton = {
                Button(
                    onClick = {
                        onIntent(TransactionsIntent.ClearAllData)
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                    shape = RoundedCornerShape(AppDimens.CornerButton)
                ) {
                    Text(StringsRes.get("btn_delete", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(StringsRes.get("btn_cancel", lang))
                }
            }
        )
    }

    if (showAboutDialog) {
        AboutAppDialog(
            onDismiss = { showAboutDialog = false },
            lang = lang
        )
    }
}
