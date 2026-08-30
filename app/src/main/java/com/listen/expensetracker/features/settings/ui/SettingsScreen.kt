package com.listen.expensetracker.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
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
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.uicomponent.components.BaseScreenScaffold
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 纯无状态设置主画面 (SettingsScreen)。
 *
 * Google 官方 UI State Holder 架构规范：
 * 1. 业务与偏好数据由 [state] ([SettingsUiState]) 纯数据类驱动；
 * 2. 界面系统契约调用与滚动位置由 [rememberSettingsStateHolder] 封装接管；
 * 3. 页面布局与各设置分组（财务参数、云端同步中心、外观偏好、运维工具）彻底解耦。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
    targetMonthOffset: Int = 0,
    onOpenApm: () -> Unit = {},
    viewModel: SettingsViewModel? = null
) {
    // 🌟 一行收拢所有列表滚动、月份标题与系统文件选择器
    val holder = rememberSettingsStateHolder(state, onIntent, targetMonthOffset, viewModel)
    val lang = state.language
    val sym = state.currencySymbol

    BaseScreenScaffold(
        title = AppStrings.SETTINGS_TITLE.tr(lang),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = holder.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = AppDimens.SpaceLarge),
            contentPadding = PaddingValues(bottom = AppDimens.SpaceLarge),
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
                        holder.exportJsonLauncher.launch(fileName)
                    },
                    onImportJson = {
                        holder.importJsonLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
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
                        onOpenApmInspector = onOpenApm,
                        onSeedDemoData = { onIntent(SettingsIntent.SeedDemoData(targetMonthOffset)) },
                        onConfirmClearAll = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.ClearConfirm)) },
                        targetMonthTitle = holder.currentMonthTitle,
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
