package com.listen.expensetracker.features.settings.viewmodel

import android.net.Uri
import com.listen.arch.mvi.CommonUiEffect
import com.listen.arch.sync.SyncState
import com.listen.expensetracker.data.update.ReleaseInfo
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

sealed interface SettingsEffect : CommonUiEffect {
    data object LaunchGoogleSignIn : SettingsEffect
    data object ScrollToTop : SettingsEffect
}

/**
 * Dialog presentation states for Settings feature.
 */
sealed interface SettingsDialog {
    data object MonthlyBudget : SettingsDialog
    data object CategoryManage : SettingsDialog
    data object AccountManage : SettingsDialog
    data object CurrencySelect : SettingsDialog
    data object ClearConfirm : SettingsDialog
    data object LogoutConfirm : SettingsDialog
    data object AboutApp : SettingsDialog
    data class UpdateAvailable(val releaseInfo: ReleaseInfo) : SettingsDialog
}

/**
 * Immutable UI State representing application preferences, cloud sync, authentication, and data operations.
 */
data class SettingsUiState(
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD,
    val currencySymbol: String = "￥",
    val monthlyBudget: Double = 5000.0,
    val autoBackupDrive: Boolean = true,
    val autoBackupWifiOnly: Boolean = false,
    val syncState: SyncState = SyncState(),
    val googleAccountEmail: String? = null,
    val googleDisplayName: String? = null,
    val googleAvatarUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val activeDialog: SettingsDialog? = null,
    val isOperating: Boolean = false,
    val isDeveloperMode: Boolean = false,
    val isCheckingUpdate: Boolean = false
)

/**
 * User Intents for Settings and System Management.
 */
sealed interface SettingsIntent {
    data class ChangeLanguage(val langCode: String) : SettingsIntent
    data class ChangeThemeMode(val mode: ThemeMode) : SettingsIntent
    data class ChangeAccentColor(val accent: AccentColor) : SettingsIntent
    data class ChangeCurrencySymbol(val symbol: String) : SettingsIntent
    data class UpdateMonthlyBudget(val budget: Double) : SettingsIntent
    data class ToggleAutoBackupDrive(val enabled: Boolean) : SettingsIntent
    data class ToggleAutoBackupWifiOnly(val enabled: Boolean) : SettingsIntent
    data class ToggleDeveloperMode(val enabled: Boolean) : SettingsIntent
    data object TriggerGoogleSignIn : SettingsIntent
    data object ScrollToTop : SettingsIntent
    data class LinkGoogleAccount(val email: String, val displayName: String? = null, val avatarUrl: String? = null) : SettingsIntent
    data object UnlinkGoogleAccount : SettingsIntent
    data object TriggerCloudBackup : SettingsIntent
    data object TriggerCloudRestore : SettingsIntent
    data class SeedDemoData(val monthOffset: Int = 0) : SettingsIntent
    data object ClearAllData : SettingsIntent
    data class ExportJsonToFile(val uri: Uri) : SettingsIntent
    data class ImportJsonFromFile(val uri: Uri) : SettingsIntent
    data class OpenDialog(val dialog: SettingsDialog) : SettingsIntent
    data object DismissDialog : SettingsIntent
    data class CheckForUpdates(val currentVersion: String) : SettingsIntent
}

