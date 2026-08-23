package com.listen.expensetracker.features.settings.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
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
            val email = currentState.googleAccountEmail
            if (email.isNullOrBlank()) {
                emitEffect(CommonUiEffect.ShowToast("请先登录 Google 账号再进行备份"))
                return@launch
            }
            val allList = dao.getAllTransactions()
            val payload = TransactionBackupManager.exportToJson(allList)
            val res = CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
            res.onSuccess { count ->
                prefManager.setLastSyncTimestamp(System.currentTimeMillis())
                emitEffect(CommonUiEffect.ShowToast("云端备份成功 (已备份 $count 条记录)"))
            }.onFailure { err ->
                emitEffect(CommonUiEffect.ShowToast("云端备份失败: ${err.message}"))
            }
        }
    }

    private fun triggerCloudRestore(traceId: String) {
        viewModelScope.launch {
            val email = currentState.googleAccountEmail
            if (email.isNullOrBlank()) {
                emitEffect(CommonUiEffect.ShowToast("请先登录 Google 账号再进行恢复"))
                return@launch
            }
            val res = CloudSyncManager.restoreFromCloud(email, traceId)
            res.onSuccess { payload ->
                val list = TransactionBackupManager.importFromJson(payload)
                if (list.isNotEmpty()) {
                    dao.insertTransactions(list)
                    emitEffect(CommonUiEffect.ShowToast("成功从云端恢复 ${list.size} 条账单记录"))
                } else {
                    emitEffect(CommonUiEffect.ShowToast("云端暂无可恢复的数据记录"))
                }
            }.onFailure { err ->
                emitEffect(CommonUiEffect.ShowToast("云端恢复失败: ${err.message}"))
            }
        }
    }

    private fun seedDemoData(traceId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val day = 86400000L
            val demoList = listOf(
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_food", categoryName = "餐饮", categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 42.0, timestamp = now - 20000, note = "午餐便当", accountType = "WECHAT"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_transport", categoryName = "交通", categoryIcon = "c_transport", categoryColorHex = "#3B82F6", amount = 6.0, timestamp = now - 40000, note = "地铁通勤", accountType = "ALIPAY"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_cafe", categoryName = "咖啡饮品", categoryIcon = "c_cafe", categoryColorHex = "#84CC16", amount = 28.0, timestamp = now - day, note = "拿铁咖啡", accountType = "WECHAT"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "INCOME", categoryId = "c_salary", categoryName = "工资", categoryIcon = "c_salary", categoryColorHex = "#10B981", amount = 15000.0, timestamp = now - day * 2, note = "月度薪酬发放", accountType = "BANK"),
                TransactionEntity(id = UUID.randomUUID().toString(), type = "EXPENSE", categoryId = "c_shopping", categoryName = "购物", categoryIcon = "c_shopping", categoryColorHex = "#EC4899", amount = 199.0, timestamp = now - day * 3, note = "优衣库短袖", accountType = "ALIPAY")
            )
            dao.insertTransactions(demoList)
            emitEffect(CommonUiEffect.ShowToast("已成功填充 5 条测试明细数据"))
        }
    }

    private fun clearAllData(traceId: String) {
        viewModelScope.launch {
            dao.deleteAll()
            emitEffect(CommonUiEffect.ShowToast("已清空本地所有明细数据"))
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
        val credentialManager = GoogleAuthManager.getCredentialManager(context)
        val request = GoogleAuthManager.buildGetCredentialRequest()
        try {
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
        } catch (e: Exception) {
            emitEffect(CommonUiEffect.ShowToast("Google 账户选择取消或未授权: ${e.message}"))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(application) as T
        }
    }
}
