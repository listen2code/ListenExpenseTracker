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
import com.listen.expensetracker.core.security.BiometricSecurityManager
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.engine.defaultCurrencySymbolForLanguage
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.pref.observeExpensePreferences
import com.listen.expensetracker.data.update.UpdateCheckerService
import com.listen.expensetracker.data.update.UpdateResult
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
    private val recurringDao = db.recurringRuleDao()
    private val prefManager = ExpenseDataStoreManager(application)
    private val syncDelegate = SettingsSyncDelegate(application, dao, recurringDao, prefManager)

    init {
        ApmLogger.i(tag = "VM", message = "SettingsViewModel initialized")
        observeSettings()
        observeGoogleAccount()
        observeSyncState()
        observeRecurringRules()
        observeTransactions()
    }

    override fun handleIntent(intent: SettingsIntent) {
        val traceId = TraceManager.newTraceId()
        when (intent) {
            is SettingsIntent.ChangeLanguage -> viewModelScope.launch {
                val symbol = defaultCurrencySymbolForLanguage(intent.langCode)
                prefManager.setLanguage(intent.langCode)
                prefManager.setCurrencySymbol(symbol)
                updateState { copy(language = intent.langCode, currencySymbol = symbol) }
            }
            is SettingsIntent.ChangeThemeMode -> viewModelScope.launch { prefManager.setThemeMode(intent.mode.name); updateState { copy(themeMode = intent.mode) } }
            is SettingsIntent.ChangeAccentColor -> viewModelScope.launch { prefManager.setAccentColor(intent.accent.name); updateState { copy(accentColor = intent.accent) } }
            is SettingsIntent.UpdateMonthlyBudget -> viewModelScope.launch { prefManager.setMonthlyBudget(intent.budget); updateState { copy(monthlyBudget = intent.budget) } }
            is SettingsIntent.UpdateCategoryBudgets -> viewModelScope.launch {
                prefManager.setMonthlyBudget(intent.budget)
                prefManager.setCategoryBudgetRatios(intent.ratios)
                updateState { copy(monthlyBudget = intent.budget, categoryBudgetRatios = intent.ratios) }
            }
            is SettingsIntent.SaveRecurringRule -> viewModelScope.launch { recurringDao.insertRule(intent.rule); emitEffect(CommonUiEffect.ShowToast("已保存周期规则")) }
            is SettingsIntent.DeleteRecurringRule -> viewModelScope.launch { recurringDao.deleteRuleById(intent.ruleId); emitEffect(CommonUiEffect.ShowToast("已删除周期规则")) }
            is SettingsIntent.ToggleRecurringRule -> viewModelScope.launch { recurringDao.updateRule(intent.rule.copy(isEnabled = intent.isEnabled)) }
            is SettingsIntent.ToggleAutoBackupDrive -> viewModelScope.launch {
                prefManager.setAutoBackupDrive(intent.enabled)
                updateState { copy(autoBackupDrive = intent.enabled) }
                if (intent.enabled) GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 1000L)
            }
            is SettingsIntent.ToggleAutoBackupWifiOnly -> viewModelScope.launch {
                prefManager.setAutoBackupWifiOnly(intent.enabled)
                updateState { copy(autoBackupWifiOnly = intent.enabled) }
            }
            is SettingsIntent.ToggleBiometricLock -> viewModelScope.launch { prefManager.setBiometricLockEnabled(intent.enabled); updateState { copy(biometricLockEnabled = intent.enabled) } }
            is SettingsIntent.ChangeLockTimeout -> viewModelScope.launch { prefManager.setLockTimeoutSeconds(intent.seconds); updateState { copy(lockTimeoutSeconds = intent.seconds) } }
            is SettingsIntent.ToggleRecentAppsShield -> viewModelScope.launch { prefManager.setRecentAppsShieldEnabled(intent.enabled); updateState { copy(recentAppsShieldEnabled = intent.enabled) } }
            is SettingsIntent.ToggleShakeToHideBalance -> viewModelScope.launch {
                prefManager.setShakeToHideBalanceEnabled(intent.enabled)
                // [Bugfix] 若关闭手势防窥，自动将当前隐额遮罩解除并恢复金额明文展示 (Rule 22)
                if (!intent.enabled) prefManager.setHideBalance(false)
                updateState { copy(shakeToHideBalanceEnabled = intent.enabled) }
            }
            is SettingsIntent.ToggleApmFloatingWindow -> viewModelScope.launch {
                prefManager.setApmFloatingWindowEnabled(intent.enabled)
                updateState { copy(apmFloatingWindowEnabled = intent.enabled) }
            }
            is SettingsIntent.ScrollToTop -> { emitEffect(SettingsEffect.ScrollToTop) }
            is SettingsIntent.ToggleDeveloperMode -> viewModelScope.launch {
                if (intent.enabled && !currentState.isDeveloperMode) {
                    prefManager.setDeveloperMode(true)
                    updateState { copy(isDeveloperMode = true) }
                    val lang = currentState.language
                    emitEffect(CommonUiEffect.ShowToast(AppStrings.DEVELOPER_MODE_ENABLED.tr(lang)))
                }
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
                syncDelegate.triggerCloudBackup(currentState.googleAccountEmail, currentState.language, traceId, { op -> updateState { copy(isOperating = op) } }, { emitEffect(CommonUiEffect.ShowToast(it)) })
            }
            is SettingsIntent.TriggerCloudRestore -> viewModelScope.launch {
                syncDelegate.triggerCloudRestore(currentState.googleAccountEmail, currentState.language, traceId, { op -> updateState { copy(isOperating = op) } }, { emitEffect(CommonUiEffect.ShowToast(it)) })
            }
            is SettingsIntent.SeedDemoData -> viewModelScope.launch { syncDelegate.seedDemoData(intent.monthOffset, currentState.language) { emitEffect(CommonUiEffect.ShowToast(it)) } }
            is SettingsIntent.ClearAllData -> viewModelScope.launch { syncDelegate.clearAllData(currentState.language) { emitEffect(CommonUiEffect.ShowToast(it)) } }
            is SettingsIntent.ExportJsonToFile -> viewModelScope.launch { syncDelegate.exportJsonToFile(intent.uri, currentState.language) { emitEffect(CommonUiEffect.ShowToast(it)) } }
            is SettingsIntent.ImportJsonFromFile -> viewModelScope.launch { syncDelegate.importJsonFromFile(intent.uri, currentState.language) { emitEffect(CommonUiEffect.ShowToast(it)) } }
            is SettingsIntent.ExportExcelToFile -> viewModelScope.launch { syncDelegate.exportExcelToFile(intent.uri, intent.startTs, intent.endTs, intent.typeFilter, currentState.language) { emitEffect(CommonUiEffect.ShowToast(it)) } }
            is SettingsIntent.ShareExcel -> viewModelScope.launch { syncDelegate.shareExcel(intent.startTs, intent.endTs, intent.typeFilter, currentState.language) { emitEffect(CommonUiEffect.ShowToast(it)) } }
            is SettingsIntent.TriggerGoogleSignIn -> { emitEffect(SettingsEffect.LaunchGoogleSignIn) }
            is SettingsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is SettingsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
            is SettingsIntent.CheckForUpdates -> checkForUpdates(intent.currentVersion)
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            dao.getAllTransactionsFlow().collectLatest { txs ->
                updateState { copy(transactions = txs) }
            }
        }
    }

    private fun observeRecurringRules() {
        viewModelScope.launch {
            recurringDao.getAllRulesFlow().collectLatest { rules ->
                updateState { copy(recurringRules = rules) }
            }
        }
    }

    private fun observeSettings() {
        val isBioSupported = BiometricSecurityManager.isBiometricOrCredentialAvailable(application)
        observeExpensePreferences(prefManager) { prefs ->
            updateState {
                copy(
                    language = prefs.language, themeMode = prefs.themeMode, accentColor = prefs.accentColor,
                    currencySymbol = prefs.currencySymbol, monthlyBudget = prefs.monthlyBudget,
                    categoryBudgetRatios = prefs.categoryBudgetRatios,
                    autoBackupDrive = prefs.autoBackupDrive, autoBackupWifiOnly = prefs.autoBackupWifiOnly,
                    isDeveloperMode = prefs.isDeveloperMode,
                    biometricLockEnabled = prefs.biometricLockEnabled, lockTimeoutSeconds = prefs.lockTimeoutSeconds,
                    recentAppsShieldEnabled = prefs.recentAppsShieldEnabled, shakeToHideBalanceEnabled = prefs.shakeToHideBalanceEnabled,
                    isBiometricSupported = isBioSupported, apmFloatingWindowEnabled = prefs.apmFloatingWindowEnabled
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
