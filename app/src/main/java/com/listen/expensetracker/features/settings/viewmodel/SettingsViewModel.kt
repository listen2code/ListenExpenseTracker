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
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.combine
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
            is SettingsIntent.LinkGoogleAccount -> {
                viewModelScope.launch {
                    prefManager.setLoggedIn(
                        isLoggedIn = true,
                        userEmail = intent.email,
                        displayName = intent.displayName ?: "",
                        avatarUrl = intent.avatarUrl ?: ""
                    )
                    emitEffect(CommonUiEffect.ShowToast("Google 账号已成功连携: ${intent.email}"))
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
            is SettingsIntent.SeedDemoData -> seedDemoData(traceId)
            is SettingsIntent.ClearAllData -> clearAllData(traceId)
            is SettingsIntent.ImportBackupData -> importBackupData(intent.json, traceId)
            is SettingsIntent.ShareBackupJson -> shareBackupJson()
            is SettingsIntent.ShareBackupCsv -> shareBackupCsv()
            is SettingsIntent.TriggerGoogleSignIn -> emitEffect(CommonUiEffect.LaunchGoogleSignIn)
            is SettingsIntent.OpenApmInspector -> emitEffect(CommonUiEffect.OpenApmInspector)
            is SettingsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is SettingsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
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
            val allList = dao.getAllTransactions()
            val payload = TransactionBackupManager.exportToJson(allList)
            val res = CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
            res.onSuccess { count ->
                prefManager.setLastSyncTimestamp(System.currentTimeMillis())
                val msg = AppStrings.backup_success_toast.tr(lang).format(count)
                emitEffect(CommonUiEffect.ShowToast(msg))
            }.onFailure { err ->
                val msg = AppStrings.backup_failed_toast.tr(lang).format(err.message ?: "Unknown")
                emitEffect(CommonUiEffect.ShowToast(msg))
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
            val res = CloudSyncManager.restoreFromCloud(email, traceId)
            res.onSuccess { payload ->
                val list = TransactionBackupManager.importFromJson(payload)
                if (list.isNotEmpty()) {
                    dao.insertTransactions(list)
                    val msg = AppStrings.restore_success_toast.tr(lang).format(list.size)
                    emitEffect(CommonUiEffect.ShowToast(msg))
                } else {
                    emitEffect(CommonUiEffect.ShowToast(AppStrings.restore_empty_toast.tr(lang)))
                }
            }.onFailure { err ->
                val msg = AppStrings.restore_failed_toast.tr(lang).format(err.message ?: "Unknown")
                emitEffect(CommonUiEffect.ShowToast(msg))
            }
        }
    }

    private fun seedDemoData(traceId: String) {
        viewModelScope.launch {
            val lang = currentState.language
            val now = System.currentTimeMillis()
            val day = 86400000L
            val demoList = listOf(
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_food", categoryName = AppStrings.cat_food.tr(lang), categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 42.0, timestamp = now - 20000, note = "Lunch Bento", accountType = "WECHAT"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_transport", categoryName = AppStrings.cat_transport.tr(lang), categoryIcon = "c_transport", categoryColorHex = "#3B82F6", amount = 6.0, timestamp = now - 40000, note = "Metro Train", accountType = "ALIPAY"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_cafe", categoryName = AppStrings.cat_cafe.tr(lang), categoryIcon = "c_cafe", categoryColorHex = "#84CC16", amount = 28.0, timestamp = now - day, note = "Latte Coffee", accountType = "WECHAT"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "INCOME", categoryId = "c_salary", categoryName = AppStrings.cat_salary.tr(lang), categoryIcon = "c_salary", categoryColorHex = "#10B981", amount = 15000.0, timestamp = now - day * 2, note = "Monthly Salary", accountType = "BANK"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_shopping", categoryName = AppStrings.cat_shopping.tr(lang), categoryIcon = "c_shopping", categoryColorHex = "#EC4899", amount = 199.0, timestamp = now - day * 3, note = "Uniqlo Clothes", accountType = "ALIPAY")
            )
            dao.insertTransactions(demoList)
            emitEffect(CommonUiEffect.ShowToast(AppStrings.seed_data_success_toast.tr(lang)))
        }
    }

    private fun clearAllData(traceId: String) {
        viewModelScope.launch {
            val lang = currentState.language
            dao.deleteAll()
            emitEffect(CommonUiEffect.ShowToast(AppStrings.clear_all_success_toast.tr(lang)))
        }
    }

    private fun importBackupData(json: String, traceId: String) {
        viewModelScope.launch {
            val list = TransactionBackupManager.importFromJson(json)
            if (list.isNotEmpty()) {
                dao.insertTransactions(list)
                emitEffect(CommonUiEffect.ShowToast("成功导入 ${list.size} 条账单数据"))
            } else {
                emitEffect(CommonUiEffect.ShowToast("JSON 数据格式解析失败或为空"))
            }
        }
    }

    private fun shareBackupJson() {
        viewModelScope.launch {
            val allList = dao.getAllTransactions()
            val json = TransactionBackupManager.exportToJson(allList)
            emitEffect(CommonUiEffect.ShareText("lExpense 账单数据导出 (JSON)", json))
        }
    }

    private fun shareBackupCsv() {
        viewModelScope.launch {
            val allList = dao.getAllTransactions()
            val csv = TransactionBackupManager.exportToCsv(allList)
            emitEffect(CommonUiEffect.ShareText("lExpense 账单数据导出 (CSV)", csv))
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
        } catch (e: Throwable) {
            emitEffect(CommonUiEffect.ShowToast("Google 登录已取消或暂未配置 Google 凭据服务"))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
