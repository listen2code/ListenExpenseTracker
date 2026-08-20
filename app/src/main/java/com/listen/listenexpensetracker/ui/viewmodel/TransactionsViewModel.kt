package com.listen.listenexpensetracker.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.data.backup.TransactionBackupManager
import com.listen.arch.data.db.AppDatabase
import com.listen.arch.data.db.TransactionEntity
import com.listen.arch.data.pref.BaseDataStoreManager
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.sync.CloudSyncManager
import com.listen.listenexpensetracker.data.engine.TransactionCalculationEngine
import com.listen.listenexpensetracker.ui.state.TransactionSortOrder
import com.listen.listenexpensetracker.ui.state.TransactionsEffect
import com.listen.listenexpensetracker.ui.state.TransactionsIntent
import com.listen.listenexpensetracker.ui.state.TransactionsUiState
import com.listen.listenexpensetracker.widget.ListenExpenseAppWidgetProvider
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class TransactionsViewModel(
    private val application: Application
) : BaseViewModel<TransactionsUiState, TransactionsIntent, TransactionsEffect>(
    initialState = TransactionsUiState()
) {
    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = BaseDataStoreManager(application)

    val apmLogsUiFlow: StateFlow<List<LogEntryUi>> = ApmLogger.logsFlow.map { list ->
        list.map { entry ->
            LogEntryUi(
                id = entry.id,
                timestamp = entry.timestamp,
                levelName = entry.level.name,
                channelName = entry.channel.name,
                tag = entry.tag,
                message = entry.message,
                traceId = entry.traceId,
                stackTrace = entry.stackTrace
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ApmLogger.i(tag = "VM", message = "TransactionsViewModel initialized")
        observeSettings()
        observeGoogleAccount()
        observeSyncState()
        observeTransactions()
    }

    override fun handleIntent(intent: TransactionsIntent) {
        val traceId = TraceManager.newTraceId()
        ApmLogger.i(tag = "Intent", message = "Handling Intent: ${intent.javaClass.simpleName}", traceId = traceId)

        when (intent) {
            is TransactionsIntent.LoadData -> observeTransactions()
            is TransactionsIntent.AddTransaction -> addTransaction(intent, traceId)
            is TransactionsIntent.UpdateTransaction -> updateTransaction(intent.transaction, traceId)
            is TransactionsIntent.DeleteTransaction -> deleteTransaction(intent.id, traceId)
            is TransactionsIntent.RestoreDeletedTransaction -> restoreDeletedTransaction(intent.transaction, traceId)
            is TransactionsIntent.ToggleHideBalance -> toggleHideBalance(intent.hide)
            is TransactionsIntent.SearchQueryChange -> updateSearchQuery(intent.query)
            is TransactionsIntent.FilterAccountChange -> updateAccountFilter(intent.accountType)
            is TransactionsIntent.ChangeMonthOffset -> updateMonthOffset(intent.offsetDelta)
            is TransactionsIntent.ChangeSortOrder -> updateSortOrder(intent.order)
            is TransactionsIntent.ChangeStatisticsTab -> updateStatisticsTab(intent.tab)
            is TransactionsIntent.ChangeCurrencySymbol -> updateCurrencySymbol(intent.symbol)
            is TransactionsIntent.UpdateMonthlyBudget -> updateMonthlyBudget(intent.budget)
            is TransactionsIntent.ImportBackupData -> importBackupData(intent.json, traceId)
            is TransactionsIntent.LinkGoogleAccount -> linkGoogleAccount(intent.email, intent.displayName, intent.avatarUrl)
            is TransactionsIntent.UnlinkGoogleAccount -> unlinkGoogleAccount()
            is TransactionsIntent.TriggerCloudBackup -> triggerCloudBackup(traceId)
            is TransactionsIntent.TriggerCloudRestore -> triggerCloudRestore(traceId)
            is TransactionsIntent.SeedDemoData -> seedDemoData(traceId)
            is TransactionsIntent.ClearAllData -> clearAllData(traceId)
            is TransactionsIntent.ChangeLanguage -> changeLanguage(intent.langCode)
            is TransactionsIntent.ChangeThemeMode -> changeThemeMode(intent.mode)
            is TransactionsIntent.ChangeAccentColor -> changeAccentColor(intent.accent)
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
                val themeEnum = try { ThemeMode.valueOf(mode) } catch (e: Exception) { ThemeMode.SYSTEM }
                updateState { copy(themeMode = themeEnum) }
            }
        }
        viewModelScope.launch {
            prefManager.accentColorFlow.collectLatest { accent ->
                val accentEnum = try { AccentColor.valueOf(accent) } catch (e: Exception) { AccentColor.EMERALD }
                updateState { copy(accentColor = accentEnum) }
            }
        }
        viewModelScope.launch {
            prefManager.currencySymbolFlow.collectLatest { symbol ->
                updateState { copy(currencySymbol = symbol) }
            }
        }
        viewModelScope.launch {
            prefManager.monthlyBudgetFlow.collectLatest { budget ->
                updateState {
                    val ratio = if (budget > 0) (totalExpense / budget).toFloat() else 0f
                    copy(
                        monthlyBudget = budget,
                        remainingBudget = (budget - totalExpense).coerceAtLeast(0.0),
                        budgetUsageRatio = ratio,
                        isOverBudget = totalExpense > budget
                    )
                }
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
                updateState { copy(syncState = syncState) }
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            updateState { copy(isLoading = true) }
            dao.getAllTransactionsFlow().collectLatest { allList ->
                ApmLogger.db(tag = "RoomDB", message = "Loaded ${allList.size} transactions from SQLite")
                applyFiltersAndCalculations(allList)
            }
        }
    }

    private fun applyFiltersAndCalculations(allList: List<TransactionEntity>) {
        val calculated = TransactionCalculationEngine.filterAndCalculate(
            allList = allList,
            currentOffset = viewState.value.selectedMonthOffset,
            query = viewState.value.searchQuery,
            accountFilter = viewState.value.selectedAccountFilter,
            budget = viewState.value.monthlyBudget,
            sortOrder = viewState.value.sortOrder,
            currencySymbol = viewState.value.currencySymbol
        )

        // Update Launcher Widget
        val todayCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val todayStart = todayCal.timeInMillis
        val todayExp = allList.filter { it.type == "EXPENSE" && it.timestamp >= todayStart }.sumOf { it.amount }
        try {
            ListenExpenseAppWidgetProvider.updateAllWidgets(application, todayExp, viewState.value.currencySymbol)
        } catch (_: Exception) {}

        updateState {
            copy(
                transactions = calculated.transactions,
                filteredTransactions = calculated.filteredTransactions,
                totalExpense = calculated.totalExpense,
                totalIncome = calculated.totalIncome,
                netBalance = calculated.netBalance,
                remainingBudget = (monthlyBudget - calculated.totalExpense).coerceAtLeast(0.0),
                categoryShares = calculated.categoryShares,
                progressSegments = calculated.progressSegments,
                incomeCategoryShares = calculated.incomeCategoryShares,
                incomeProgressSegments = calculated.incomeProgressSegments,
                dailyTrendBars = calculated.dailyTrendBars,
                dailyAverageExpense = calculated.dailyAverageExpense,
                dailyAverageIncome = calculated.dailyAverageIncome,
                maxExpenseTransaction = calculated.maxExpenseTransaction,
                maxIncomeTransaction = calculated.maxIncomeTransaction,
                budgetUsageRatio = calculated.budgetUsageRatio,
                isOverBudget = calculated.isOverBudget,
                monthTitle = calculated.monthTitle,
                isLoading = false
            )
        }
    }

    private fun addTransaction(intent: TransactionsIntent.AddTransaction, traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "InsertTransaction", traceId = traceId) {
                val entity = TransactionEntity(
                    type = intent.type,
                    categoryId = intent.categoryId,
                    categoryName = intent.categoryName,
                    categoryIcon = intent.categoryIcon,
                    categoryColorHex = intent.categoryColorHex,
                    amount = intent.amount,
                    note = intent.note,
                    accountType = intent.accountType,
                    timestamp = intent.timestamp
                )
                dao.insertTransaction(entity)
            }
            emitEffect(TransactionsEffect.TransactionAddedSuccess)
            emitEffect(TransactionsEffect.ShowToast("记账成功！"))
        }
    }

    private fun updateTransaction(transaction: TransactionEntity, traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "UpdateTransaction", traceId = traceId) {
                dao.insertTransaction(transaction)
            }
            emitEffect(TransactionsEffect.ShowToast("账单已更新"))
        }
    }

    private fun deleteTransaction(id: String, traceId: String) {
        viewModelScope.launch {
            val toDelete = viewState.value.transactions.find { it.id == id }
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "DeleteTransaction", traceId = traceId) {
                dao.deleteTransactionById(id)
            }
            if (toDelete != null) {
                emitEffect(TransactionsEffect.ShowUndoSnackbar("已删除账单", toDelete))
            } else {
                emitEffect(TransactionsEffect.ShowToast("已删除账单"))
            }
        }
    }

    private fun restoreDeletedTransaction(transaction: TransactionEntity, traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "RestoreTransaction", traceId = traceId) {
                dao.insertTransaction(transaction)
            }
            emitEffect(TransactionsEffect.ShowToast("已撤销删除并恢复账单"))
        }
    }

    private fun updateSearchQuery(query: String) {
        updateState { copy(searchQuery = query) }
        applyFiltersAndCalculations(viewState.value.transactions)
    }

    private fun updateAccountFilter(accountType: String) {
        updateState { copy(selectedAccountFilter = accountType) }
        applyFiltersAndCalculations(viewState.value.transactions)
    }

    private fun updateMonthOffset(offsetDelta: Int) {
        val newOffset = viewState.value.selectedMonthOffset + offsetDelta
        updateState { copy(selectedMonthOffset = newOffset) }
        applyFiltersAndCalculations(viewState.value.transactions)
    }

    private fun updateSortOrder(order: TransactionSortOrder) {
        updateState { copy(sortOrder = order) }
        applyFiltersAndCalculations(viewState.value.transactions)
    }

    private fun updateStatisticsTab(tab: String) {
        updateState { copy(statisticsTab = tab) }
    }

    private fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            prefManager.setCurrencySymbol(symbol)
            emitEffect(TransactionsEffect.ShowToast("币种符号已更新为 $symbol"))
        }
    }

    private fun updateMonthlyBudget(budget: Double) {
        viewModelScope.launch {
            prefManager.setMonthlyBudget(budget)
            emitEffect(TransactionsEffect.ShowToast("月度预算已设定为 ${viewState.value.currencySymbol}$budget"))
        }
    }

    private fun linkGoogleAccount(email: String, displayName: String?, avatarUrl: String?) {
        viewModelScope.launch {
            prefManager.setLoggedIn(
                isLoggedIn = true,
                userEmail = email,
                displayName = displayName ?: "",
                avatarUrl = avatarUrl ?: ""
            )
            emitEffect(TransactionsEffect.ShowToast("Google 账户连携成功 ($email)"))
        }
    }

    private fun unlinkGoogleAccount() {
        viewModelScope.launch {
            prefManager.setLoggedIn(isLoggedIn = false, userEmail = "", displayName = "", avatarUrl = "")
            emitEffect(TransactionsEffect.ShowToast("已退出 Google 账户登录"))
        }
    }

    private fun triggerCloudBackup(traceId: String) {
        val email = viewState.value.googleAccountEmail
        if (email.isNullOrBlank()) {
            viewModelScope.launch {
                emitEffect(TransactionsEffect.ShowToast("请先连携 Google 账户以使用云端备份！"))
            }
            return
        }

        viewModelScope.launch {
            val res = CloudSyncManager.backupToCloud(viewState.value.transactions, email, traceId)
            res.onSuccess { count ->
                prefManager.setLastSyncTimestamp(System.currentTimeMillis())
                emitEffect(TransactionsEffect.ShowToast("云端备份成功 (已备份 $count 条账单)"))
            }.onFailure { err ->
                emitEffect(TransactionsEffect.ShowToast("云端备份失败: ${err.message}"))
            }
        }
    }

    private fun triggerCloudRestore(traceId: String) {
        val email = viewState.value.googleAccountEmail
        if (email.isNullOrBlank()) {
            viewModelScope.launch {
                emitEffect(TransactionsEffect.ShowToast("请先连携 Google 账户以从云端恢复数据！"))
            }
            return
        }

        viewModelScope.launch {
            val res = CloudSyncManager.restoreFromCloud(email, traceId)
            res.onSuccess { list ->
                dao.insertTransactions(list)
                prefManager.setLastSyncTimestamp(System.currentTimeMillis())
                emitEffect(TransactionsEffect.ShowToast("云端恢复成功 (已恢复 ${list.size} 条账单)"))
            }.onFailure { err ->
                emitEffect(TransactionsEffect.ShowToast("云端恢复失败: ${err.message}"))
            }
        }
    }

    private fun importBackupData(json: String, traceId: String) {
        viewModelScope.launch {
            try {
                val importedList = TransactionBackupManager.importFromJson(json)
                if (importedList.isNotEmpty()) {
                    TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "ImportBackupData", traceId = traceId) {
                        dao.insertTransactions(importedList)
                    }
                    emitEffect(TransactionsEffect.ShowToast("成功导入 ${importedList.size} 条账单！"))
                } else {
                    emitEffect(TransactionsEffect.ShowToast("未解析到有效账单数据"))
                }
            } catch (e: Exception) {
                ApmLogger.e(tag = "Import", message = "Import failed: ${e.message}", throwable = e)
                emitEffect(TransactionsEffect.ShowToast("导入失败: ${e.message}"))
            }
        }
    }

    private fun seedDemoData(traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "SeedDemoData", traceId = traceId) {
                val now = System.currentTimeMillis()
                val oneDay = 86400000L
                val demoList = listOf(
                    // Current Month - Today & Recent
                    TransactionEntity(type = "EXPENSE", categoryId = "c_food", categoryName = "餐饮", categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 38.0, note = "午餐老碗牛肉面", accountType = "CASH", timestamp = now - 7200000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_cafe", categoryName = "咖啡饮品", categoryIcon = "c_cafe", categoryColorHex = "#84CC16", amount = 22.0, note = "冰美式咖啡", accountType = "CASH", timestamp = now - 14400000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_transport", categoryName = "交通", categoryIcon = "c_transport", categoryColorHex = "#3B82F6", amount = 6.0, note = "地铁通勤", accountType = "BANK", timestamp = now - 28800000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_shopping", categoryName = "购物", categoryIcon = "c_shopping", categoryColorHex = "#EC4899", amount = 168.0, note = "超市采购生活用品", accountType = "BANK", timestamp = now - oneDay),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_food", categoryName = "餐饮", categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 128.0, note = "晚餐日料定食", accountType = "CASH", timestamp = now - oneDay - 7200000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_entertainment", categoryName = "娱乐", categoryIcon = "c_entertainment", categoryColorHex = "#8B5CF6", amount = 75.0, note = "IMAX 电影票", accountType = "BANK", timestamp = now - oneDay * 2),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_fitness", categoryName = "运动健身", categoryIcon = "c_fitness", categoryColorHex = "#06B6D4", amount = 200.0, note = "羽毛球馆包场", accountType = "BANK", timestamp = now - oneDay * 3),
                    TransactionEntity(type = "INCOME", categoryId = "c_investment", categoryName = "理财", categoryIcon = "c_investment", categoryColorHex = "#3B82F6", amount = 356.8, note = "指数基金定投分红", accountType = "BANK", timestamp = now - oneDay * 4),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_medical", categoryName = "医疗", categoryIcon = "c_medical", categoryColorHex = "#10B981", amount = 65.0, note = "药房维生素补剂", accountType = "CASH", timestamp = now - oneDay * 5),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_pets", categoryName = "宠物", categoryIcon = "c_pets", categoryColorHex = "#F97316", amount = 180.0, note = "猫咪进口冻干粮", accountType = "BANK", timestamp = now - oneDay * 6),
                    TransactionEntity(type = "INCOME", categoryId = "c_salary", categoryName = "工资", categoryIcon = "c_salary", categoryColorHex = "#10B981", amount = 15000.0, note = "本月薪资到账", accountType = "BANK", timestamp = now - oneDay * 7),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_housing", categoryName = "居住", categoryIcon = "c_housing", categoryColorHex = "#F59E0B", amount = 3500.0, note = "月度房租物业费", accountType = "BANK", timestamp = now - oneDay * 8),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_social", categoryName = "人情", categoryIcon = "c_social", categoryColorHex = "#6366F1", amount = 500.0, note = "朋友婚礼礼金", accountType = "BANK", timestamp = now - oneDay * 10),
                    TransactionEntity(type = "INCOME", categoryId = "c_gift", categoryName = "礼金", categoryIcon = "c_gift", categoryColorHex = "#F59E0B", amount = 888.0, note = "长辈生日红包", accountType = "CASH", timestamp = now - oneDay * 12),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_shopping", categoryName = "购物", categoryIcon = "c_shopping", categoryColorHex = "#EC4899", amount = 499.0, note = "机械键盘与鼠标", accountType = "BANK", timestamp = now - oneDay * 15),

                    // Previous Month (30-45 days ago)
                    TransactionEntity(type = "INCOME", categoryId = "c_salary", categoryName = "工资", categoryIcon = "c_salary", categoryColorHex = "#10B981", amount = 15000.0, note = "上月薪资", accountType = "BANK", timestamp = now - oneDay * 35),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_housing", categoryName = "居住", categoryIcon = "c_housing", categoryColorHex = "#F59E0B", amount = 3500.0, note = "上月房租", accountType = "BANK", timestamp = now - oneDay * 36),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_food", categoryName = "餐饮", categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 320.0, note = "周末家庭聚餐", accountType = "BANK", timestamp = now - oneDay * 38),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_shopping", categoryName = "购物", categoryIcon = "c_shopping", categoryColorHex = "#EC4899", amount = 650.0, note = "换季服饰采购", accountType = "BANK", timestamp = now - oneDay * 42)
                )
                dao.insertTransactions(demoList)
            }
            emitEffect(TransactionsEffect.ShowToast("成功填充 19 条多周期、全场景精细测试账单！"))
        }
    }

    private fun clearAllData(traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "ClearAllData", traceId = traceId) {
                dao.clearAll()
            }
            emitEffect(TransactionsEffect.ShowToast("已清空所有账单"))
        }
    }

    fun exportBackupJson(): String {
        return TransactionBackupManager.exportToJson(viewState.value.transactions)
    }

    fun exportBackupCsv(): String {
        return TransactionBackupManager.exportToCsv(viewState.value.transactions)
    }

    fun clearApmLogs() {
        ApmLogger.clear()
    }

    fun exportApmLogs(): String {
        return ApmLogger.exportPlainText()
    }

    private fun toggleHideBalance(hide: Boolean) {
        updateState { copy(hideBalance = hide) }
    }

    private fun changeLanguage(langCode: String) {
        viewModelScope.launch {
            prefManager.setLanguage(langCode)
        }
    }

    private fun changeThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            prefManager.setThemeMode(mode.name)
        }
    }

    private fun changeAccentColor(accent: AccentColor) {
        viewModelScope.launch {
            prefManager.setAccentColor(accent.name)
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TransactionsViewModel::class.java)) {
                return TransactionsViewModel(application) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
