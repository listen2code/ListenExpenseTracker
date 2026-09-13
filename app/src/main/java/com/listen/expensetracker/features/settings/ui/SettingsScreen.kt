package com.listen.expensetracker.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.listen.expensetracker.core.i18n.tr
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

/**
 * 纯无状态设置主画面 (SettingsScreen)。
 *
 * Google 官方 UI State Holder 架构规范：
 * 1. 业务与偏好数据由 [state] ([SettingsUiState]) 纯数据类驱动；
 * 2. 界面交互状态（列表滚动、月份标题计算）由 [rememberSettingsStateHolder] 纯状态持有者管理；
 * 3. 画面级副作用与系统 ActivityResult 契约监听由 [SettingsEffects] 独立挂载；
 * 4. 页面布局与各设置分组（财务参数、云端同步中心、外观偏好、运维工具）彻底解耦。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    modifier: Modifier = Modifier,
    targetMonthOffset: Int = 0,
    viewModel: SettingsViewModel? = null
) {
    // 🌟 UI 交互状态（滚动位置、标题计算）
    val holder = rememberSettingsStateHolder(state, targetMonthOffset)
    val context = LocalContext.current
    val sym = state.currencySymbol

    // 🌟 独立挂载画面专用副作用监听与系统契约交互 (区分于 UI 状态持有者 SettingsStateHolder)
    SettingsEffects(
        viewModel = viewModel,
        context = context,
        listState = holder.listState
    )

    BaseScreenScaffold(
        title = AppStrings.SETTINGS_TITLE.tr(), modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            state = holder.listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = AppDimens.SpaceLarge),
            contentPadding = PaddingValues(top = AppDimens.SpaceSmall, bottom = AppDimens.SpaceSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // 1. Finance Preferences & Rules Section (Monthly Budget, Categories, Accounts, Recurring)
            item(key = "finance_section") {
                SettingsFinanceSection(
                    monthlyBudget = state.monthlyBudget,
                    currencySymbol = sym,
                    onOpenBudgetDialog = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.MonthlyBudget)) },
                    onOpenCategoryDialog = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.CategoryManage)) },
                    onOpenAccountDialog = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.AccountManage)) },
                    onOpenRecurringDialog = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.RecurringManage)) }
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
                    onLoginGoogle = { viewModel?.handleIntent(SettingsIntent.TriggerGoogleSignIn) },
                    onLogoutGoogle = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.LogoutConfirm)) },
                    onToggleAutoBackupDrive = { viewModel?.handleIntent(SettingsIntent.ToggleAutoBackupDrive(it)) },
                    onToggleAutoBackupWifiOnly = { viewModel?.handleIntent(SettingsIntent.ToggleAutoBackupWifiOnly(it)) },
                    onTriggerBackup = { viewModel?.handleIntent(SettingsIntent.TriggerCloudBackup) },
                    onTriggerRestore = { viewModel?.handleIntent(SettingsIntent.TriggerCloudRestore) },
                    onExportExcel = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.ExportExcelOptions)) },
                    onExportJson = { viewModel?.handleIntent(SettingsIntent.RequestExportJson) },
                    onImportJson = { viewModel?.handleIntent(SettingsIntent.RequestImportJson) },
                    isOperating = state.isOperating
                )
            }

            // 3. Personalization & Appearance Section
            item(key = "appearance_section") {
                SettingsAppearanceSection(
                    themeMode = state.themeMode,
                    accentColor = state.accentColor,
                    onChangeThemeMode = { viewModel?.handleIntent(SettingsIntent.ChangeThemeMode(it)) },
                    onChangeAccentColor = { viewModel?.handleIntent(SettingsIntent.ChangeAccentColor(it)) },
                    onLanguageChange = { viewModel?.handleIntent(SettingsIntent.ChangeLanguage(it)) },
                    isPureBlackDark = state.isPureBlackDark,
                    onTogglePureBlackDark = { viewModel?.handleIntent(SettingsIntent.TogglePureBlackDark(it)) })
            }

            // 4. Notifications & Alert Hub Section
            item(key = "notification_section") {
                SettingsNotificationSection(
                    notificationsEnabled = state.notificationsEnabled,
                    budgetAlertsEnabled = state.budgetAlertsEnabled,
                    recurringBillsAlertsEnabled = state.recurringBillsAlertsEnabled,
                    appUpdatesAlertsEnabled = state.appUpdatesAlertsEnabled,
                    onToggleNotifications = { viewModel?.handleIntent(SettingsIntent.ToggleNotifications(it)) },
                    onToggleBudgetAlerts = { viewModel?.handleIntent(SettingsIntent.ToggleBudgetAlerts(it)) },
                    onToggleRecurringBillsAlerts = { viewModel?.handleIntent(SettingsIntent.ToggleRecurringBillsAlerts(it)) },
                    onToggleAppUpdatesAlerts = { viewModel?.handleIntent(SettingsIntent.ToggleAppUpdatesAlerts(it)) }
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
                    onToggleBiometricLock = { viewModel?.handleIntent(SettingsIntent.ToggleBiometricLock(it)) },
                    onChangeLockTimeout = { viewModel?.handleIntent(SettingsIntent.ChangeLockTimeout(it)) },
                    onToggleRecentAppsShield = { viewModel?.handleIntent(SettingsIntent.ToggleRecentAppsShield(it)) },
                    onToggleShakeToHideBalance = { viewModel?.handleIntent(SettingsIntent.ToggleShakeToHideBalance(it)) }
                )
            }

            // 5. System Ops & APM Observability Section (Developer Mode only)
            if (state.isDeveloperMode) {
                item(key = "apm_section") {
                    SettingsApmSection(
                        apmFloatingWindowEnabled = state.apmFloatingWindowEnabled,
                        onToggleApmFloatingWindow = { viewModel?.handleIntent(SettingsIntent.ToggleApmFloatingWindow(it)) },
                        onSeedDemoData = { viewModel?.handleIntent(SettingsIntent.SeedDemoData(targetMonthOffset)) },
                        onConfirmClearAll = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.ClearConfirm)) },
                        targetMonthTitle = holder.currentMonthTitle,
                        onOpenSimulateNotifications = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.SimulateNotifications)) })
                }
            }

            // 5. Version Footer (Rapid 5x taps trigger Developer Mode)
            item(key = "version_footer") {
                SettingsVersionFooter(
                    isDeveloperMode = state.isDeveloperMode,
                    isCheckingUpdate = state.isCheckingUpdate,
                    onCheckForUpdates = { viewModel?.handleIntent(SettingsIntent.CheckForUpdates(it)) },
                    onToggleDeveloperMode = { viewModel?.handleIntent(SettingsIntent.ToggleDeveloperMode(it)) },
                    onOpenAboutDialog = { viewModel?.handleIntent(SettingsIntent.OpenDialog(SettingsDialog.AboutApp)) }
                )
            }
        }
    }

    // Feature-Level Dialog Host
    SettingsDialogHost(
        state = state,
        viewModel = viewModel,
        onSaveExcelToFile = { startTs, endTs, type, fileName ->
            viewModel?.handleIntent(SettingsIntent.DismissDialog)
            viewModel?.handleIntent(SettingsIntent.RequestExportExcel(startTs, endTs, type, fileName))
        }
    )
}
