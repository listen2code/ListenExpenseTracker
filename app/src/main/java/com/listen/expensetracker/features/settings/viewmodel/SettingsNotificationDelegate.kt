package com.listen.expensetracker.features.settings.viewmodel

import android.app.Application
import androidx.core.content.pm.PackageInfoCompat
import com.listen.arch.i18n.tr
import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.core.notification.NotificationPreferences
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.update.UpdateCheckerService
import com.listen.expensetracker.data.update.UpdateNotificationHelper
import com.listen.expensetracker.data.update.UpdateResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 设置中心通知开关管理与版本更新检查代理 (SettingsNotificationDelegate)。
 * 解耦 SettingsViewModel，遵循单一职责与单文件 250 行架构规则。
 */
class SettingsNotificationDelegate(
    private val application: Application,
    private val scope: CoroutineScope
) {
    fun populateInitialState(): NotificationStateSnapshot {
        return NotificationStateSnapshot(
            notificationsEnabled = NotificationPreferences.isNotificationsEnabled(application),
            budgetAlertsEnabled = NotificationPreferences.isBudgetAlertsEnabled(application),
            budgetWarningThresholdEnabled = NotificationPreferences.isBudgetWarningThresholdEnabled(application),
            recurringBillsAlertsEnabled = NotificationPreferences.isRecurringBillsAlertsEnabled(application),
            appUpdatesAlertsEnabled = NotificationPreferences.isAppUpdatesAlertsEnabled(application)
        )
    }

    fun handleIntent(
        intent: SettingsIntent,
        updateState: ((SettingsUiState) -> SettingsUiState) -> Unit
    ): Boolean {
        return when (intent) {
            is SettingsIntent.ToggleNotifications -> {
                NotificationPreferences.setNotificationsEnabled(application, intent.enabled)
                updateState { it.copy(notificationsEnabled = intent.enabled) }
                true
            }
            is SettingsIntent.ToggleBudgetAlerts -> {
                NotificationPreferences.setBudgetAlertsEnabled(application, intent.enabled)
                NotificationPreferences.setBudgetWarningThresholdEnabled(application, intent.enabled)
                updateState {
                    it.copy(
                        budgetAlertsEnabled = intent.enabled,
                        budgetWarningThresholdEnabled = intent.enabled
                    )
                }
                true
            }
            is SettingsIntent.ToggleBudgetWarningThreshold -> {
                NotificationPreferences.setBudgetWarningThresholdEnabled(application, intent.enabled)
                updateState { it.copy(budgetWarningThresholdEnabled = intent.enabled) }
                true
            }
            is SettingsIntent.ToggleRecurringBillsAlerts -> {
                NotificationPreferences.setRecurringBillsAlertsEnabled(application, intent.enabled)
                updateState { it.copy(recurringBillsAlertsEnabled = intent.enabled) }
                true
            }
            is SettingsIntent.ToggleAppUpdatesAlerts -> {
                NotificationPreferences.setAppUpdatesAlertsEnabled(application, intent.enabled)
                updateState { it.copy(appUpdatesAlertsEnabled = intent.enabled) }
                true
            }
            else -> false
        }
    }

    fun checkForUpdates(
        currentVersion: String,
        currentState: SettingsUiState,
        updateState: ((SettingsUiState) -> SettingsUiState) -> Unit,
        emitEffect: (CommonUiEffect) -> Unit
    ) {
        if (currentState.isCheckingUpdate) return
        scope.launch {
            updateState { it.copy(isCheckingUpdate = true) }
            val lang = currentState.language
            val currentBuildNumber = try {
                val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                PackageInfoCompat.getLongVersionCode(pInfo)
            } catch (_: Exception) { 0L }
            when (val result = UpdateCheckerService.checkLatestRelease(currentVersion, currentBuildNumber, lang)) {
                is UpdateResult.NewVersionAvailable -> {
                    updateState {
                        it.copy(isCheckingUpdate = false, activeDialog = SettingsDialog.UpdateAvailable(result.releaseInfo))
                    }
                    UpdateNotificationHelper.notifyUpdateAvailable(application, result.releaseInfo, lang)
                }
                is UpdateResult.AlreadyLatest -> {
                    updateState { it.copy(isCheckingUpdate = false) }
                    emitEffect(CommonUiEffect.ShowToast(String.format(AppStrings.ALREADY_LATEST_VERSION.tr(lang), currentVersion)))
                }
                is UpdateResult.Error -> {
                    updateState { it.copy(isCheckingUpdate = false) }
                    emitEffect(CommonUiEffect.ShowToast(AppStrings.CHECK_UPDATE_FAILED.tr(lang)))
                }
            }
        }
    }
}

data class NotificationStateSnapshot(
    val notificationsEnabled: Boolean,
    val budgetAlertsEnabled: Boolean,
    val budgetWarningThresholdEnabled: Boolean,
    val recurringBillsAlertsEnabled: Boolean,
    val appUpdatesAlertsEnabled: Boolean
)
