package com.listen.expensetracker.features.settings.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import kotlinx.coroutines.flow.Flow
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.settings.components.SettingsApmSection
import com.listen.expensetracker.features.settings.components.SettingsAppearanceSection
import com.listen.expensetracker.features.settings.components.SettingsDataCenterSection
import com.listen.expensetracker.features.settings.components.SettingsDialogHost
import com.listen.expensetracker.features.settings.components.SettingsFinanceSection
import com.listen.expensetracker.features.settings.components.SettingsVersionFooter
import com.listen.expensetracker.features.settings.viewmodel.SettingsDialog
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.uicomponent.components.BaseScreenScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pure Stateless Settings Screen cleanly orchestrating Finance Rules, Data Center, Appearance, and System Ops.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    targetMonthOffset: Int = 0,
    scrollToTopFlow: Flow<Unit>? = null
) {
    val lang = state.language
    val sym = state.currencySymbol

    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    LaunchedEffect(scrollToTopFlow) {
        scrollToTopFlow?.collect {
            listState.animateScrollToItem(0)
        }
    }

    // File-based JSON Export launcher
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onIntent(SettingsIntent.ExportJsonToFile(it)) }
    }

    // File-based JSON Import launcher
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onIntent(SettingsIntent.ImportJsonFromFile(it)) }
    }

    BaseScreenScaffold(
        title = AppStrings.settings_title.tr(lang),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
        ) {
            // 1. Finance Preferences & Rules Section (Monthly Budget, Categories, Accounts)
            item(key = "finance_section") {
                SettingsFinanceSection(
                    monthlyBudget = state.monthlyBudget,
                    currencySymbol = sym,
                    onOpenBudgetDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.MonthlyBudget)) },
                    onOpenCategoryDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.CategoryManage)) },
                    onOpenAccountDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.AccountManage)) },
                    lang = lang
                )
            }

            // 2. Data & Cloud Backup Center (Google Drive Sync + Local JSON Export/Import)
            item(key = "data_center_section") {
                SettingsDataCenterSection(
                    googleAccountEmail = state.googleAccountEmail,
                    googleDisplayName = state.googleDisplayName,
                    googleAvatarUrl = state.googleAvatarUrl,
                    autoBackupDrive = state.autoBackupDrive,
                    autoBackupWifiOnly = state.autoBackupWifiOnly,
                    syncState = state.syncState,
                    onLoginGoogle = { onIntent(SettingsIntent.TriggerGoogleSignIn) },
                    onLogoutGoogle = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.LogoutConfirm)) },
                    onToggleAutoBackupDrive = { onIntent(SettingsIntent.ToggleAutoBackupDrive(it)) },
                    onToggleAutoBackupWifiOnly = { onIntent(SettingsIntent.ToggleAutoBackupWifiOnly(it)) },
                    onTriggerBackup = { onIntent(SettingsIntent.TriggerCloudBackup) },
                    onTriggerRestore = { onIntent(SettingsIntent.TriggerCloudRestore) },
                    onExportJson = {
                        val fileName = "lexpense_backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.json"
                        exportJsonLauncher.launch(fileName)
                    },
                    onImportJson = {
                        importJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                    },
                    lang = lang,
                    isOperating = state.isOperating
                )
            }

            // 3. Personalization & Appearance Section
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

            // 4. System Ops & APM Observability Section (Developer Mode only)
            if (state.isDeveloperMode) {
                item(key = "apm_section") {
                    SettingsApmSection(
                        onOpenApmInspector = { onIntent(SettingsIntent.OpenApmInspector) },
                        onSeedDemoData = { onIntent(SettingsIntent.SeedDemoData(targetMonthOffset)) },
                        onConfirmClearAll = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.ClearConfirm)) },
                        lang = lang
                    )
                }
            }

            // 5. Version Footer (Rapid 5x taps trigger Developer Mode)
            item(key = "version_footer") {
                SettingsVersionFooter(
                    isDeveloperMode = state.isDeveloperMode,
                    onToggleDeveloperMode = { onIntent(SettingsIntent.ToggleDeveloperMode(it)) },
                    onOpenAboutDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.AboutApp)) },
                    lang = lang
                )
            }
        }
    }

    // Feature-Level Dialog Host
    SettingsDialogHost(state = state, onIntent = onIntent)
}
