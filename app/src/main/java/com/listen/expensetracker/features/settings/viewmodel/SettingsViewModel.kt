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
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID

private data class DemoTemplate(
    val categoryId: String,
    val categoryNameKey: String,
    val notes: List<String>,
    val minAmount: Int,
    val maxAmount: Int,
    val colorHex: String
)

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

    private fun seedDemoData(traceId: String) {
        viewModelScope.launch {
            val lang = currentState.language
            val cal = java.util.Calendar.getInstance()
            val currentDay = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val accounts = AccountRepository.getAllAccounts().map { it.key }.ifEmpty { listOf("CASH", "BANK", "CREDIT") }

            val expenseTemplates = listOf(
                DemoTemplate("c_food", AppStrings.cat_food, listOf("午餐便当", "麻辣烫", "日料寿喜烧", "麦当劳套餐", "火锅聚餐", "早点豆浆油条", "牛排晚餐", "精酿啤酒馆"), 18, 360, "#EF4444"),
                DemoTemplate("c_transport", AppStrings.cat_transport, listOf("地铁出行", "打车回家", "公交车", "滴滴快车", "加油充值", "停车费"), 4, 220, "#3B82F6"),
                DemoTemplate("c_cafe", AppStrings.cat_cafe, listOf("星巴克拿铁", "喜茶多肉葡萄", "瑞幸生椰拿铁", "Manner澳白", "一点点奶茶"), 12, 48, "#84CC16"),
                DemoTemplate("c_shopping", AppStrings.cat_shopping, listOf("优衣库服饰", "网购日用品", "数码配件", "超市大采购", "降噪耳机", "护肤品"), 39, 699, "#EC4899"),
                DemoTemplate("c_entertainment", AppStrings.cat_entertainment, listOf("电影票", "Steam游戏", "剧本杀", "音乐会门票", "KTV唱歌"), 45, 380, "#8B5CF6"),
                DemoTemplate("c_fitness", AppStrings.cat_fitness, listOf("羽毛球包场", "游泳馆门票", "蛋白粉补给", "运动跑鞋"), 30, 450, "#F59E0B"),
                DemoTemplate("c_pets", AppStrings.cat_pets, listOf("猫粮罐头", "宠物驱虫", "猫砂补货", "洗澡美容"), 35, 300, "#14B8A6"),
                DemoTemplate("c_medical", AppStrings.cat_medical, listOf("感冒药", "口腔检查洗牙", "维生素补剂"), 20, 280, "#06B6D4")
            )

            val incomeTemplates = listOf(
                DemoTemplate("c_salary", AppStrings.cat_salary, listOf("月度薪酬发放", "绩效奖金"), 12000, 26000, "#10B981"),
                DemoTemplate("c_investment", AppStrings.cat_investment, listOf("基金定投收益", "理财结息", "股票分红"), 300, 3500, "#6366F1"),
                DemoTemplate("c_gift", AppStrings.cat_gift, listOf("生日红包", "长辈过节礼金", "抽奖红包"), 200, 1000, "#F43F5E")
            )

            val count = kotlin.random.Random.nextInt(12, 18)
            val generated = mutableListOf<TransactionEntity>()

            // 1. Generate 1-2 Income transactions
            val incomeItem = incomeTemplates.random()
            val incAmt = kotlin.random.Random.nextInt(incomeItem.minAmount, incomeItem.maxAmount).toDouble()
            val incDay = kotlin.random.Random.nextInt(1, currentDay.coerceAtLeast(2))
            val incCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.DAY_OF_MONTH, incDay)
                set(java.util.Calendar.HOUR_OF_DAY, kotlin.random.Random.nextInt(9, 18))
                set(java.util.Calendar.MINUTE, kotlin.random.Random.nextInt(0, 59))
            }
            generated.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = "INCOME",
                    categoryId = incomeItem.categoryId,
                    categoryName = incomeItem.categoryNameKey.tr(lang),
                    categoryIcon = incomeItem.categoryId,
                    categoryColorHex = incomeItem.colorHex,
                    amount = incAmt,
                    timestamp = incCal.timeInMillis,
                    note = incomeItem.notes.random(),
                    accountType = "BANK"
                )
            )

            // 2. Generate varied Expense transactions
            for (i in 1 until count) {
                val exp = expenseTemplates.random()
                val amt = kotlin.random.Random.nextInt(exp.minAmount, exp.maxAmount).toDouble()
                val expDay = kotlin.random.Random.nextInt(1, (currentDay + 1).coerceAtLeast(2))
                val expCal = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.DAY_OF_MONTH, expDay)
                    set(java.util.Calendar.HOUR_OF_DAY, kotlin.random.Random.nextInt(7, 23))
                    set(java.util.Calendar.MINUTE, kotlin.random.Random.nextInt(0, 59))
                }
                generated.add(
                    TransactionEntity(
                        id = UUID.randomUUID().toString(),
                        type = "EXPENSE",
                        categoryId = exp.categoryId,
                        categoryName = exp.categoryNameKey.tr(lang),
                        categoryIcon = exp.categoryId,
                        categoryColorHex = exp.colorHex,
                        amount = amt,
                        timestamp = expCal.timeInMillis,
                        note = exp.notes.random(),
                        accountType = accounts.random()
                    )
                )
            }

            dao.insertTransactions(generated)
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
