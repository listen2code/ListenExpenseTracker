package com.listen.expensetracker.features.settings.viewmodel

import android.net.Uri
import com.listen.arch.sync.SyncState
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

/**
 * Dialog presentation states for Settings feature.
 */
sealed interface SettingsDialog {
    data object MonthlyBudget : SettingsDialog
    data object CategoryManage : SettingsDialog
    data object CurrencySelect : SettingsDialog
    data object ClearConfirm : SettingsDialog
    data object LogoutConfirm : SettingsDialog
    data object AboutApp : SettingsDialog
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
    val syncState: SyncState = SyncState(),
    val googleAccountEmail: String? = null,
    val googleDisplayName: String? = null,
    val googleAvatarUrl: String? = null,
    val isLoggedIn: Boolean = false,
    val lastSyncTimestamp: Long = 0L,
    val activeDialog: SettingsDialog? = null,
    val isOperating: Boolean = false
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
    data object TriggerGoogleSignIn : SettingsIntent
    data class LinkGoogleAccount(val email: String, val displayName: String? = null, val avatarUrl: String? = null) : SettingsIntent
    data object UnlinkGoogleAccount : SettingsIntent
    data object TriggerCloudBackup : SettingsIntent
    data object TriggerCloudRestore : SettingsIntent
    data object SeedDemoData : SettingsIntent
    data object ClearAllData : SettingsIntent
    data class ExportJsonToFile(val uri: Uri) : SettingsIntent
    data class ImportJsonFromFile(val uri: Uri) : SettingsIntent
    data object OpenApmInspector : SettingsIntent
    data class OpenDialog(val dialog: SettingsDialog) : SettingsIntent
    data object DismissDialog : SettingsIntent
}
