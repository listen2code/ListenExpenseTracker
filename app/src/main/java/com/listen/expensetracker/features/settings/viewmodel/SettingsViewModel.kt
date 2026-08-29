package com.listen.expensetracker.features.settings.viewmodel

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.i18n.StringsRes
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import com.listen.arch.sync.CloudSyncManager
import com.listen.arch.sync.SyncState
import com.listen.expensetracker.auth.GoogleAuthManager
import com.listen.expensetracker.data.backup.TransactionBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.update.UpdateCheckerService
import com.listen.expensetracker.data.update.UpdateResult
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.combine
import com.listen.expensetracker.data.engine.DemoDataEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel dedicated to user preferences, cloud sync, Google authentication, and data operations.
 */
class SettingsViewModel(
    private val application: Application
) : BaseViewModel<SettingsUiState, SettingsIntent, CommonUiEffect>(SettingsUiState()) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = ExpenseDataStoreManager(application)

    init {
        ApmLogger.i(tag = "VM", message = "SettingsViewModel initialized")
        observeSettings()
        observeGoogleAccount()
        observeSyncState()
    }

    override fun handleIntent(intent: SettingsIntent) {
        val traceId = TraceManager.newTraceId()
        when (intent) {
            is SettingsIntent.ChangeLanguage -> {
                viewModelScope.launch {
                    prefManager.setLanguage(intent.langCode)
                    updateState { copy(language = intent.langCode) }
                }
            }
            is SettingsIntent.ChangeThemeMode -> {
                viewModelScope.launch {
                    prefManager.setThemeMode(intent.mode.name)
                    updateState { copy(themeMode = intent.mode) }
                }
            }
            is SettingsIntent.ChangeAccentColor -> {
                viewModelScope.launch {
                    prefManager.setAccentColor(intent.accent.name)
                    updateState { copy(accentColor = intent.accent) }
                }
            }
            is SettingsIntent.ChangeCurrencySymbol -> {
                viewModelScope.launch {
                    prefManager.setCurrencySymbol(intent.symbol)
                    updateState { copy(currencySymbol = intent.symbol) }
                }
            }
            is SettingsIntent.UpdateMonthlyBudget -> {
                viewModelScope.launch {
                    prefManager.setMonthlyBudget(intent.budget)
                    updateState { copy(monthlyBudget = intent.budget) }
                }
            }
            is SettingsIntent.ToggleAutoBackupDrive -> {
                viewModelScope.launch {
                    prefManager.setAutoBackupDrive(intent.enabled)
                    updateState { copy(autoBackupDrive = intent.enabled) }
                    if (intent.enabled) {
                        com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 1000L)
                    }
                }
            }
            is SettingsIntent.ToggleAutoBackupWifiOnly -> {
                viewModelScope.launch {
                    prefManager.setAutoBackupWifiOnly(intent.enabled)
                    updateState { copy(autoBackupWifiOnly = intent.enabled) }
                }
            }
            is SettingsIntent.ToggleDeveloperMode -> {
                viewModelScope.launch {
                    prefManager.setDeveloperMode(intent.enabled)
                    updateState { copy(isDeveloperMode = intent.enabled) }
                    val lang = currentState.language
                    val msg = if (intent.enabled) {
                        AppStrings.developer_mode_enabled.tr(lang)
                    } else {
                        AppStrings.developer_mode_disabled.tr(lang)
                    }
                    emitEffect(CommonUiEffect.ShowToast(msg))
                }
            }
            is SettingsIntent.LinkGoogleAccount -> {
                viewModelScope.launch {
                    prefManager.setLoggedIn(
                        isLoggedIn = true,
                        userEmail = intent.email,
                        displayName = intent.displayName ?: "",
                        avatarUrl = intent.avatarUrl ?: ""
                    )
                    emitEffect(CommonUiEffect.ShowToast("Google 账号已成功连携: ${intent.email}"))
                    com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 2000L)
                }
            }
            is SettingsIntent.UnlinkGoogleAccount -> {
                viewModelScope.launch {
                    GoogleAuthManager.clearCredentials(application)
                    prefManager.setLoggedIn(false, "", "", "")
                    emitEffect(CommonUiEffect.ShowToast("已安全退出 Google 账号"))
                }
            }
            is SettingsIntent.TriggerCloudBackup -> triggerCloudBackup(traceId)
            is SettingsIntent.TriggerCloudRestore -> triggerCloudRestore(traceId)
            is SettingsIntent.SeedDemoData -> seedDemoData(traceId, intent.monthOffset)
            is SettingsIntent.ClearAllData -> clearAllData(traceId)
            is SettingsIntent.ExportJsonToFile -> exportJsonToFile(intent.uri)
            is SettingsIntent.ImportJsonFromFile -> importJsonFromFile(intent.uri)
            is SettingsIntent.TriggerGoogleSignIn -> emitEffect(CommonUiEffect.LaunchGoogleSignIn)
            is SettingsIntent.OpenApmInspector -> emitEffect(CommonUiEffect.OpenApmInspector)
            is SettingsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is SettingsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
            is SettingsIntent.CheckForUpdates -> checkForUpdates(intent.currentVersion)
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            prefManager.languageFlow.collectLatest { lang ->
                updateState { copy(language = lang) }
            }
        }
        viewModelScope.launch {
            prefManager.themeModeFlow.collectLatest { mode ->
                val themeEnum = try { ThemeMode.valueOf(mode) } catch (_: Exception) { ThemeMode.SYSTEM }
                updateState { copy(themeMode = themeEnum) }
            }
        }
        viewModelScope.launch {
            prefManager.accentColorFlow.collectLatest { accent ->
                val accentEnum = try { AccentColor.valueOf(accent) } catch (_: Exception) { AccentColor.EMERALD }
                updateState { copy(accentColor = accentEnum) }
            }
        }
        viewModelScope.launch {
            prefManager.currencySymbolFlow.collectLatest { sym ->
                updateState { copy(currencySymbol = sym) }
            }
        }
        viewModelScope.launch {
            prefManager.monthlyBudgetFlow.collectLatest { budget ->
                updateState { copy(monthlyBudget = budget) }
            }
        }
        viewModelScope.launch {
            prefManager.autoBackupDriveFlow.collectLatest { enabled ->
                updateState { copy(autoBackupDrive = enabled) }
            }
        }
        viewModelScope.launch {
            prefManager.autoBackupWifiOnlyFlow.collectLatest { enabled ->
                updateState { copy(autoBackupWifiOnly = enabled) }
            }
        }
        viewModelScope.launch {
            prefManager.isDeveloperModeFlow.collectLatest { enabled ->
                updateState { copy(isDeveloperMode = enabled) }
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
                if (isLoggedIn && email.isNotBlank()) {
                    Triple(email, displayName, avatarUrl)
                } else {
                    null
                }
            }.collectLatest { accountInfo ->
                updateState {
                    copy(
                        isLoggedIn = accountInfo != null,
                        googleAccountEmail = accountInfo?.first,
                        googleDisplayName = accountInfo?.second?.ifBlank { null },
                        googleAvatarUrl = accountInfo?.third?.ifBlank { null }
                    )
                }
            }
        }
    }

    private fun observeSyncState() {
        viewModelScope.launch {
            CloudSyncManager.syncStateFlow.collectLatest { syncState ->
                updateState {
                    copy(
                        syncState = syncState,
                        lastSyncTimestamp = syncState.lastSyncTimestamp
                    )
                }
            }
        }
    }

    private fun triggerCloudBackup(traceId: String) {
        viewModelScope.launch {
            val lang = currentState.language
            val email = currentState.googleAccountEmail
            if (email.isNullOrBlank()) {
                emitEffect(CommonUiEffect.ShowToast(AppStrings.login_google_required_toast.tr(lang)))
                return@launch
            }
            updateState { copy(isOperating = true) }
            try {
                val allList = dao.getAllTransactions()
                val payload = TransactionBackupManager.exportToJson(allList)
                val token = com.listen.expensetracker.data.cloud.GoogleDriveService.getAccessToken(application, email)
                val driveResult = com.listen.expensetracker.data.cloud.GoogleDriveService.uploadBackup(token, payload, traceId)
                driveResult.onSuccess { fileId ->
                    val now = System.currentTimeMillis()
                    prefManager.setLastSyncTimestamp(now)
                    CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
                    emitEffect(CommonUiEffect.ShowToast("已成功备份至 Google Drive 云端硬盘 (${allList.size} 条)"))
                }.onFailure { err ->
                    CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
                    emitEffect(CommonUiEffect.ShowToast("Google Drive 上传异常: ${err.message}"))
                }
            } catch (e: Throwable) {
                val allList = dao.getAllTransactions()
                val payload = TransactionBackupManager.exportToJson(allList)
                CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
                emitEffect(CommonUiEffect.ShowToast("已备份至本地快照 (Drive 凭据待授权: ${e.message})"))
            } finally {
                updateState { copy(isOperating = false) }
            }
        }
    }

    private fun triggerCloudRestore(traceId: String) {
        viewModelScope.launch {
            val lang = currentState.language
            val email = currentState.googleAccountEmail
            if (email.isNullOrBlank()) {
                emitEffect(CommonUiEffect.ShowToast(AppStrings.login_google_required_toast.tr(lang)))
                return@launch
            }
            updateState { copy(isOperating = true) }
            try {
                val token = com.listen.expensetracker.data.cloud.GoogleDriveService.getAccessToken(application, email)
                val driveResult = com.listen.expensetracker.data.cloud.GoogleDriveService.downloadBackup(token, traceId)
                driveResult.onSuccess { payload ->
                    val list = TransactionBackupManager.importFromJson(payload)
                    if (list.isNotEmpty()) {
                        dao.insertTransactions(list)
                        val now = System.currentTimeMillis()
                        prefManager.setLastSyncTimestamp(now)
                        emitEffect(CommonUiEffect.ShowToast("已从 Google Drive 成功恢复 ${list.size} 条账单"))
                    } else {
                        emitEffect(CommonUiEffect.ShowToast(AppStrings.restore_empty_toast.tr(lang)))
                    }
                }.onFailure { driveErr ->
                    val fallbackRes = CloudSyncManager.restoreFromCloud(email, traceId)
                    fallbackRes.onSuccess { payload ->
                        val list = TransactionBackupManager.importFromJson(payload)
                        if (list.isNotEmpty()) {
                            dao.insertTransactions(list)
                            emitEffect(CommonUiEffect.ShowToast("已从快照恢复 ${list.size} 条账单"))
                        }
                    }.onFailure {
                        emitEffect(CommonUiEffect.ShowToast("云端恢复失败: ${driveErr.message}"))
                    }
                }
            } catch (e: Throwable) {
                val fallbackRes = CloudSyncManager.restoreFromCloud(email, traceId)
                fallbackRes.onSuccess { payload ->
                    val list = TransactionBackupManager.importFromJson(payload)
                    if (list.isNotEmpty()) {
                        dao.insertTransactions(list)
                        emitEffect(CommonUiEffect.ShowToast("已从快照恢复 ${list.size} 条账单"))
                    }
                }.onFailure {
                    emitEffect(CommonUiEffect.ShowToast("恢复失败: ${e.message}"))
                }
            } finally {
                updateState { copy(isOperating = false) }
            }
        }
    }

    private fun seedDemoData(traceId: String, monthOffset: Int = 0) {
        viewModelScope.launch {
            val lang = currentState.language
            val accounts = AccountRepository.getAllAccounts().map { it.key }.ifEmpty { listOf("CASH", "BANK", "CREDIT") }
            val generated = DemoDataEngine.generate(monthOffset, lang, accounts)
            dao.insertTransactions(generated)
            val (_, _, title) = TransactionCalculationEngine.getMonthRangeAndTitle(monthOffset, lang)
            emitEffect(CommonUiEffect.ShowToast(AppStrings.seed_month_success_toast.tr(lang).format(title, generated.size)))
        }
    }

    private fun clearAllData(traceId: String) {
        viewModelScope.launch {
            val lang = currentState.language
            dao.deleteAll()
            emitEffect(CommonUiEffect.ShowToast(AppStrings.clear_all_success_toast.tr(lang)))
        }
    }

    private fun exportJsonToFile(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val lang = currentState.language
            try {
                val allList = dao.getAllTransactions()
                val json = TransactionBackupManager.exportToJson(allList)
                application.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray(Charsets.UTF_8))
                }
                emitEffect(CommonUiEffect.ShowToast(if (lang == "en") "Successfully exported ${allList.size} records to JSON file" else "已成功导出 ${allList.size} 条账单至 JSON 文件"))
            } catch (e: Throwable) {
                emitEffect(CommonUiEffect.ShowToast(if (lang == "en") "Export failed: ${e.message}" else "导出 JSON 文件失败: ${e.message}"))
            }
        }
    }

    private fun importJsonFromFile(uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val lang = currentState.language
            try {
                val json = application.contentResolver.openInputStream(uri)?.use { ins ->
                    ins.bufferedReader(Charsets.UTF_8).readText()
                } ?: ""
                val list = TransactionBackupManager.importFromJson(json)
                if (list.isNotEmpty()) {
                    dao.insertTransactions(list)
                    emitEffect(CommonUiEffect.ShowToast(if (lang == "en") "Successfully imported ${list.size} records" else "成功导入 ${list.size} 条账单数据"))
                } else {
                    emitEffect(CommonUiEffect.ShowToast(if (lang == "en") "JSON content is empty or invalid" else "JSON 文件内容解析失败或为空"))
                }
            } catch (e: Throwable) {
                emitEffect(CommonUiEffect.ShowToast(if (lang == "en") "Import failed: ${e.message}" else "导入 JSON 文件失败: ${e.message}"))
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
                androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
            } catch (_: Exception) {
                0L
            }
            when (val result = UpdateCheckerService.checkLatestRelease(currentVersion, currentBuildNumber, lang)) {
                is UpdateResult.NewVersionAvailable -> {
                    updateState {
                        copy(
                            isCheckingUpdate = false,
                            activeDialog = SettingsDialog.UpdateAvailable(result.releaseInfo)
                        )
                    }
                }
                is UpdateResult.AlreadyLatest -> {
                    updateState { copy(isCheckingUpdate = false) }
                    val msg = String.format(AppStrings.already_latest_version.tr(lang), currentVersion)
                    emitEffect(CommonUiEffect.ShowToast(msg))
                }
                is UpdateResult.Error -> {
                    updateState { copy(isCheckingUpdate = false) }
                    emitEffect(CommonUiEffect.ShowToast(AppStrings.check_update_failed.tr(lang)))
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
                handleIntent(SettingsIntent.LinkGoogleAccount(
                    email = profile.email,
                    displayName = profile.displayName,
                    avatarUrl = profile.avatarUrl
                ))
            }.onFailure { err ->
                emitEffect(CommonUiEffect.ShowToast("Google 授权解析失败: ${err.message}"))
            }
        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
            // 用户主动取消/关闭弹窗，不弹出错误提示
        } catch (e: Throwable) {
            com.listen.arch.apm.ApmLogger.e("GoogleAuth", "Login error: ${e.javaClass.name}: ${e.message}")
            emitEffect(CommonUiEffect.ShowToast("Google 登录未成功 (${e.javaClass.simpleName}): ${e.message}"))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
