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
import com.listen.expensetracker.features.settings.components.SettingsNotificationSection
import com.listen.expensetracker.features.settings.components.SettingsSecuritySection
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
            contentPadding = PaddingValues(top = AppDimens.SpaceSmall, bottom = AppDimens.SpaceBottomFab),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // 1. Finance Preferences & Rules Section (Monthly Budget, Categories, Accounts, Recurring)
            item(key = "finance_section") {
                SettingsFinanceSection(
                    monthlyBudget = state.monthlyBudget,
                    currencySymbol = sym,
                    onOpenBudgetDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.MonthlyBudget)) },
                    onOpenCategoryDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.CategoryManage)) },
                    onOpenAccountDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.AccountManage)) },
                    onOpenRecurringDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.RecurringManage)) },
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
                    onExportExcel = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.ExportExcelOptions)) },
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
                    language = lang,
                    onChangeThemeMode = { onIntent(SettingsIntent.ChangeThemeMode(it)) },
                    onChangeAccentColor = { onIntent(SettingsIntent.ChangeAccentColor(it)) },
                    onLanguageChange = { onIntent(SettingsIntent.ChangeLanguage(it)) },
                    lang = lang
                )
            }

            // 4. Notifications & Alert Hub Section
            item(key = "notification_section") {
                SettingsNotificationSection(
                    notificationsEnabled = state.notificationsEnabled,
                    budgetAlertsEnabled = state.budgetAlertsEnabled,
                    recurringBillsAlertsEnabled = state.recurringBillsAlertsEnabled,
                    appUpdatesAlertsEnabled = state.appUpdatesAlertsEnabled,
                    onToggleNotifications = { onIntent(SettingsIntent.ToggleNotifications(it)) },
                    onToggleBudgetAlerts = { onIntent(SettingsIntent.ToggleBudgetAlerts(it)) },
                    onToggleRecurringBillsAlerts = { onIntent(SettingsIntent.ToggleRecurringBillsAlerts(it)) },
                    onToggleAppUpdatesAlerts = { onIntent(SettingsIntent.ToggleAppUpdatesAlerts(it)) },
                    lang = lang
                )
            }

            // 5. Security & Privacy Shield Section
            item(key = "security_section") {
                SettingsSecuritySection(
                    biometricLockEnabled = state.biometricLockEnabled,
                    lockTimeoutSeconds = state.lockTimeoutSeconds,
                    recentAppsShieldEnabled = state.recentAppsShieldEnabled,
                    shakeToHideBalanceEnabled = state.shakeToHideBalanceEnabled,
                    isBiometricSupported = state.isBiometricSupported,
                    onToggleBiometricLock = { onIntent(SettingsIntent.ToggleBiometricLock(it)) },
                    onChangeLockTimeout = { onIntent(SettingsIntent.ChangeLockTimeout(it)) },
                    onToggleRecentAppsShield = { onIntent(SettingsIntent.ToggleRecentAppsShield(it)) },
                    onToggleShakeToHideBalance = { onIntent(SettingsIntent.ToggleShakeToHideBalance(it)) },
                    lang = lang
                )
            }

            // 5. System Ops & APM Observability Section (Developer Mode only)
            if (state.isDeveloperMode) {
                item(key = "apm_section") {
                    SettingsApmSection(
                        apmFloatingWindowEnabled = state.apmFloatingWindowEnabled,
                        onToggleApmFloatingWindow = { onIntent(SettingsIntent.ToggleApmFloatingWindow(it)) },
                        onSeedDemoData = { onIntent(SettingsIntent.SeedDemoData(targetMonthOffset)) },
                        onConfirmClearAll = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.ClearConfirm)) },
                        targetMonthTitle = holder.currentMonthTitle,
                        lang = lang,
                        onOpenSimulateNotifications = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.SimulateNotifications)) }
                    )
                }
            }

            // 5. Version Footer (Rapid 5x taps trigger Developer Mode)
            item(key = "version_footer") {
                SettingsVersionFooter(
                    isDeveloperMode = state.isDeveloperMode,
                    isCheckingUpdate = state.isCheckingUpdate,
                    onCheckForUpdates = { onIntent(SettingsIntent.CheckForUpdates(it)) },
                    onToggleDeveloperMode = { onIntent(SettingsIntent.ToggleDeveloperMode(it)) },
                    onOpenAboutDialog = { onIntent(SettingsIntent.OpenDialog(SettingsDialog.AboutApp)) },
                    lang = lang
                )
            }
        }
    }

    // Feature-Level Dialog Host
    SettingsDialogHost(
        state = state,
        onIntent = onIntent,
        onSaveExcelToFile = { startTs, endTs, type, fileName ->
            onIntent(SettingsIntent.DismissDialog)
            holder.onPrepareExportExcel(startTs, endTs, type, fileName)
        }
    )
}
