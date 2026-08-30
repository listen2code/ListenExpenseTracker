package com.listen.expensetracker.features.settings.viewmodel

import android.app.Application
import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.i18n.tr
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import com.listen.arch.sync.CloudSyncManager
import com.listen.expensetracker.auth.GoogleAuthManager
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.pref.observeExpensePreferences
import com.listen.expensetracker.data.update.UpdateCheckerService
import com.listen.expensetracker.data.update.UpdateResult
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * ViewModel dedicated to user preferences, cloud sync, Google authentication, and data operations.
 */
class SettingsViewModel(
    private val application: Application
) : BaseViewModel<SettingsUiState, SettingsIntent>(SettingsUiState()) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = ExpenseDataStoreManager(application)
    private val syncDelegate = SettingsSyncDelegate(application, dao, prefManager)

    init {
        ApmLogger.i(tag = "VM", message = "SettingsViewModel initialized")
        observeSettings()
        observeGoogleAccount()
        observeSyncState()
    }

    override fun handleIntent(intent: SettingsIntent) {
        val traceId = TraceManager.newTraceId()
        when (intent) {
            is SettingsIntent.ChangeLanguage -> viewModelScope.launch {
                prefManager.setLanguage(intent.langCode)
                updateState { copy(language = intent.langCode) }
            }
            is SettingsIntent.ChangeThemeMode -> viewModelScope.launch {
                prefManager.setThemeMode(intent.mode.name)
                updateState { copy(themeMode = intent.mode) }
            }
            is SettingsIntent.ChangeAccentColor -> viewModelScope.launch {
                prefManager.setAccentColor(intent.accent.name)
                updateState { copy(accentColor = intent.accent) }
            }
            is SettingsIntent.ChangeCurrencySymbol -> viewModelScope.launch {
                prefManager.setCurrencySymbol(intent.symbol)
                updateState { copy(currencySymbol = intent.symbol) }
            }
            is SettingsIntent.UpdateMonthlyBudget -> viewModelScope.launch {
                prefManager.setMonthlyBudget(intent.budget)
                updateState { copy(monthlyBudget = intent.budget) }
            }
            is SettingsIntent.ToggleAutoBackupDrive -> viewModelScope.launch {
                prefManager.setAutoBackupDrive(intent.enabled)
                updateState { copy(autoBackupDrive = intent.enabled) }
                if (intent.enabled) GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 1000L)
            }
            is SettingsIntent.ToggleAutoBackupWifiOnly -> viewModelScope.launch {
                prefManager.setAutoBackupWifiOnly(intent.enabled)
                updateState { copy(autoBackupWifiOnly = intent.enabled) }
            }
            is SettingsIntent.ScrollToTop -> { emitEffect(SettingsEffect.ScrollToTop) }
            is SettingsIntent.ToggleDeveloperMode -> viewModelScope.launch {
                prefManager.setDeveloperMode(intent.enabled)
                updateState { copy(isDeveloperMode = intent.enabled) }
                val lang = currentState.language
                val msg = if (intent.enabled) AppStrings.DEVELOPER_MODE_ENABLED.tr(lang) else AppStrings.DEVELOPER_MODE_DISABLED.tr(lang)
                emitEffect(CommonUiEffect.ShowToast(msg))
            }
            is SettingsIntent.LinkGoogleAccount -> viewModelScope.launch {
                prefManager.setLoggedIn(true, intent.email, intent.displayName ?: "", intent.avatarUrl ?: "")
                emitEffect(CommonUiEffect.ShowToast("Google 账号已成功连携: ${intent.email}"))
                GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 2000L)
            }
            is SettingsIntent.UnlinkGoogleAccount -> viewModelScope.launch {
                GoogleAuthManager.clearCredentials(application)
                prefManager.setLoggedIn(false, "", "", "")
                emitEffect(CommonUiEffect.ShowToast("已安全退出 Google 账号"))
            }
            is SettingsIntent.TriggerCloudBackup -> viewModelScope.launch {
                syncDelegate.triggerCloudBackup(currentState.googleAccountEmail, currentState.language, traceId,
                    onOperating = { op -> updateState { copy(isOperating = op) } },
                    onToast = { msg -> emitEffect(CommonUiEffect.ShowToast(msg)) })
            }
            is SettingsIntent.TriggerCloudRestore -> viewModelScope.launch {
                syncDelegate.triggerCloudRestore(currentState.googleAccountEmail, currentState.language, traceId,
                    onOperating = { op -> updateState { copy(isOperating = op) } },
                    onToast = { msg -> emitEffect(CommonUiEffect.ShowToast(msg)) })
            }
            is SettingsIntent.SeedDemoData -> viewModelScope.launch {
                syncDelegate.seedDemoData(intent.monthOffset, currentState.language) { msg ->
                    emitEffect(CommonUiEffect.ShowToast(msg))
                }
            }
            is SettingsIntent.ClearAllData -> viewModelScope.launch {
                syncDelegate.clearAllData(currentState.language) { msg ->
                    emitEffect(CommonUiEffect.ShowToast(msg))
                }
            }
            is SettingsIntent.ExportJsonToFile -> viewModelScope.launch {
                syncDelegate.exportJsonToFile(intent.uri, currentState.language) { msg ->
                    emitEffect(CommonUiEffect.ShowToast(msg))
                }
            }
            is SettingsIntent.ImportJsonFromFile -> viewModelScope.launch {
                syncDelegate.importJsonFromFile(intent.uri, currentState.language) { msg ->
                    emitEffect(CommonUiEffect.ShowToast(msg))
                }
            }
            is SettingsIntent.TriggerGoogleSignIn -> {
                emitEffect(SettingsEffect.LaunchGoogleSignIn)
            }
            is SettingsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is SettingsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
            is SettingsIntent.CheckForUpdates -> checkForUpdates(intent.currentVersion)
        }
    }

    private fun observeSettings() {
        observeExpensePreferences(prefManager) { prefs ->
            updateState {
                copy(
                    language = prefs.language,
                    themeMode = prefs.themeMode,
                    accentColor = prefs.accentColor,
                    currencySymbol = prefs.currencySymbol,
                    monthlyBudget = prefs.monthlyBudget,
                    autoBackupDrive = prefs.autoBackupDrive,
                    autoBackupWifiOnly = prefs.autoBackupWifiOnly,
                    isDeveloperMode = prefs.isDeveloperMode
                )
            }
        }
    }

    private fun observeGoogleAccount() {
        viewModelScope.launch {
            combine(
                prefManager.isLoggedInFlow,
                prefManager.userEmailFlow,
                prefManager.userDisplayNameFlow,
                prefManager.userAvatarUrlFlow
            ) { isLoggedIn, email, displayName, avatarUrl ->
                updateState {
                    copy(
                        isLoggedIn = isLoggedIn,
                        googleAccountEmail = email,
                        googleDisplayName = displayName,
                        googleAvatarUrl = avatarUrl
                    )
                }
            }.collectLatest { }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            prefManager.lastSyncTimestampFlow.collectLatest { ts -> updateState { copy(lastSyncTimestamp = ts) } }
        }
        viewModelScope.launch {
            CloudSyncManager.syncStateFlow.collectLatest { syncState ->
                updateState { copy(syncState = syncState, lastSyncTimestamp = syncState.lastSyncTimestamp) }
            }
        }
    }

    private fun checkForUpdates(currentVersion: String) {
        if (currentState.isCheckingUpdate) return
        viewModelScope.launch {
            updateState { copy(isCheckingUpdate = true) }
            val lang = currentState.language
            val currentBuildNumber = try {
                val pInfo = application.packageManager.getPackageInfo(application.packageName, 0)
                PackageInfoCompat.getLongVersionCode(pInfo)
            } catch (_: Exception) { 0L }
            when (val result = UpdateCheckerService.checkLatestRelease(currentVersion, currentBuildNumber, lang)) {
                is UpdateResult.NewVersionAvailable -> updateState {
                    copy(isCheckingUpdate = false, activeDialog = SettingsDialog.UpdateAvailable(result.releaseInfo))
                }
                is UpdateResult.AlreadyLatest -> {
                    updateState { copy(isCheckingUpdate = false) }
                    emitEffect(CommonUiEffect.ShowToast(String.format(AppStrings.ALREADY_LATEST_VERSION.tr(lang), currentVersion)))
                }
                is UpdateResult.Error -> {
                    updateState { copy(isCheckingUpdate = false) }
                    emitEffect(CommonUiEffect.ShowToast(AppStrings.CHECK_UPDATE_FAILED.tr(lang)))
                }
            }
        }
    }

    suspend fun launchGoogleAccountPicker(context: Context) {
        try {
            val credentialManager = GoogleAuthManager.getCredentialManager(context)
            val request = GoogleAuthManager.buildGetCredentialRequest()
            val response = credentialManager.getCredential(context = context, request = request)
            val profileResult = GoogleAuthManager.parseGoogleIdCredential(response)
            profileResult.onSuccess { profile ->
                handleIntent(SettingsIntent.LinkGoogleAccount(profile.email, profile.displayName, profile.avatarUrl))
            }.onFailure { err ->
                emitEffect(CommonUiEffect.ShowToast("Google 授权解析失败: ${err.message}"))
            }
        } catch (e: GetCredentialCancellationException) {
            // Cancelled by user
        } catch (e: Throwable) {
            ApmLogger.e("GoogleAuth", "Login error: ${e.javaClass.name}: ${e.message}")
            emitEffect(CommonUiEffect.ShowToast("Google 登录未成功 (${e.javaClass.simpleName}): ${e.message}"))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(application) as T
    }
}
