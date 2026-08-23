package com.listen.expensetracker.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.settings.components.SettingsAppearanceSection
import com.listen.expensetracker.features.settings.components.SettingsApmSection
import com.listen.expensetracker.features.settings.components.SettingsCloudSection
import com.listen.expensetracker.features.settings.components.SettingsDataSection
import com.listen.expensetracker.features.settings.components.SettingsDialogHost
import com.listen.expensetracker.features.settings.viewmodel.SettingsDialog
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.uicomponent.components.BaseScreenScaffold
import kotlinx.coroutines.launch

/**
 * Stateful entry route for Settings Screen.
 */
@Composable
fun SettingsRoute(
    viewModel: SettingsViewModel = viewModel(),
    onLaunchGooglePicker: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsState()
    SettingsScreen(
        state = state,
        onIntent = viewModel::handleIntent,
        onLaunchGooglePicker = onLaunchGooglePicker,
        modifier = modifier
    )
}

/**
 * Pure Stateless Settings Screen cleanly orchestrating Cloud Sync, Appearance, Data Management, and System Ops.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    onLaunchGooglePicker: suspend () -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol
    val scope = rememberCoroutineScope()

    BaseScreenScaffold(
        title = StringsRes.get("settings_title", lang),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
        ) {
            // 1. Cloud Sync & Google Account Section
            item(key = "cloud_section") {
                SettingsCloudSection(
                    googleAccountEmail = state.googleAccountEmail,
                    googleDisplayName = state.googleDisplayName,
                    syncState = state.syncState,
                    onLoginGoogle = {
                        scope.launch { onLaunchGooglePicker() }
                    },
                    onLogoutGoogle = { onIntent(SettingsIntent.UnlinkGoogleAccount) },
                    onTriggerBackup = { onIntent(SettingsIntent.TriggerCloudBackup) },
                    onTriggerRestore = { onIntent(SettingsIntent.TriggerCloudRestore) },
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
                    onChangeThemeMode = { onIntent(SettingsIntent.ChangeThemeMode(it)) },
                    onChangeAccentColor = { onIntent(SettingsIntent.ChangeAccentColor(it)) },
                    onOpenCurrencyDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.CurrencySelect)) },
                    onLanguageChange = { onIntent(SettingsIntent.ChangeLanguage(it)) },
                    lang = lang
                )
            }

            // 3. Local Data Management Section
            item(key = "data_section") {
                SettingsDataSection(
                    monthlyBudget = state.monthlyBudget,
                    currencySymbol = sym,
                    onOpenBudgetDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.MonthlyBudget)) },
                    onOpenCategoryDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.CategoryManage)) },
                    onExportJson = { onIntent(SettingsIntent.ShareBackupJson) },
                    onExportCsv = { onIntent(SettingsIntent.ShareBackupCsv) },
                    onOpenImportSheet = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.ImportBackup)) },
                    lang = lang
                )
            }

            // 4. System Ops & APM Observability Section
            item(key = "apm_section") {
                SettingsApmSection(
                    onOpenApmInspector = { onIntent(SettingsIntent.OpenApmInspector) },
                    onSeedDemoData = { onIntent(SettingsIntent.SeedDemoData) },
                    onConfirmClearAll = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.ClearConfirm)) },
                    onOpenAboutDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.AboutApp)) },
                    lang = lang
                )
            }
        }
    }

    // Feature-Level Dialog Host
    SettingsDialogHost(state = state, onIntent = onIntent)
}
