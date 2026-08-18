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
import com.listen.listenexpensetracker.ui.state.TransactionSortOrder
import com.listen.listenexpensetracker.ui.state.TransactionsEffect
import com.listen.listenexpensetracker.ui.state.TransactionsIntent
import com.listen.listenexpensetracker.ui.state.TransactionsUiState
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.charts.BarChartItem
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionsViewModel(
    application: Application
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
            is TransactionsIntent.ToggleHideBalance -> toggleHideBalance(intent.hide)
            is TransactionsIntent.SearchQueryChange -> updateSearchQuery(intent.query)
            is TransactionsIntent.FilterAccountChange -> updateAccountFilter(intent.accountType)
            is TransactionsIntent.ChangeMonthOffset -> updateMonthOffset(intent.offsetDelta)
            is TransactionsIntent.ChangeSortOrder -> updateSortOrder(intent.order)
            is TransactionsIntent.ChangeStatisticsTab -> updateStatisticsTab(intent.tab)
            is TransactionsIntent.ChangeCurrencySymbol -> updateCurrencySymbol(intent.symbol)
            is TransactionsIntent.UpdateMonthlyBudget -> updateMonthlyBudget(intent.budget)
            is TransactionsIntent.ImportBackupData -> importBackupData(intent.json, traceId)
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
                        budgetUsageRatio = ratio,
                        isOverBudget = totalExpense > budget
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
        val currentOffset = viewState.value.selectedMonthOffset
        val query = viewState.value.searchQuery.trim().lowercase()
        val accountFilter = viewState.value.selectedAccountFilter
        val budget = viewState.value.monthlyBudget
        val sortOrder = viewState.value.sortOrder

        // Calculate Month Range
        val (startTs, endTs, title) = getMonthRangeAndTitle(currentOffset)

        val monthFilteredList = if (currentOffset == 0 && query.isBlank() && accountFilter == "ALL") {
            allList
        } else {
            allList.filter { it.timestamp in startTs..endTs }
        }

        // Apply Search & Account Filters
        val matchedFiltered = monthFilteredList.filter { item ->
            val matchesQuery = query.isEmpty() ||
                    item.categoryName.lowercase().contains(query) ||
                    item.note.lowercase().contains(query)
            val matchesAccount = accountFilter == "ALL" || item.accountType == accountFilter
            matchesQuery && matchesAccount
        }

        // Apply Sorting
        val finalSorted = when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> matchedFiltered.sortedByDescending { it.timestamp }
            TransactionSortOrder.DATE_ASC -> matchedFiltered.sortedBy { it.timestamp }
            TransactionSortOrder.AMOUNT_DESC -> matchedFiltered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> matchedFiltered.sortedBy { it.amount }
        }

        val totalExp = finalSorted.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalInc = finalSorted.filter { it.type == "INCOME" }.sumOf { it.amount }

        val expenseShares = calculateCategoryShares(finalSorted.filter { it.type == "EXPENSE" }, totalExp)
        val expenseSegments = expenseShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val incomeShares = calculateCategoryShares(finalSorted.filter { it.type == "INCOME" }, totalInc)
        val incomeSegments = incomeShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val maxExpenseTx = finalSorted.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
        val maxIncomeTx = finalSorted.filter { it.type == "INCOME" }.maxByOrNull { it.amount }

        val daysInMonth = getDaysInMonth(currentOffset)
        val dailyAvgExp = if (daysInMonth > 0) totalExp / daysInMonth else 0.0
        val dailyAvgInc = if (daysInMonth > 0) totalInc / daysInMonth else 0.0

        val trendBars = calculateRecentDaysTrend(finalSorted.filter { it.type == "EXPENSE" })

        val ratio = if (budget > 0) (totalExp / budget).toFloat() else 0f

        updateState {
            copy(
                transactions = allList,
                filteredTransactions = finalSorted,
                totalExpense = totalExp,
                totalIncome = totalInc,
                netBalance = totalInc - totalExp,
                categoryShares = expenseShares,
                progressSegments = expenseSegments,
                incomeCategoryShares = incomeShares,
                incomeProgressSegments = incomeSegments,
                dailyTrendBars = trendBars,
                dailyAverageExpense = dailyAvgExp,
                dailyAverageIncome = dailyAvgInc,
                maxExpenseTransaction = maxExpenseTx,
                maxIncomeTransaction = maxIncomeTx,
                budgetUsageRatio = ratio,
                isOverBudget = totalExp > budget,
                monthTitle = title,
                isLoading = false
            )
        }
    }

    private fun calculateRecentDaysTrend(expenses: List<TransactionEntity>): List<BarChartItem> {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val dayGroups = expenses.groupBy {
            sdf.format(Date(it.timestamp))
        }

        // Generate last 7 days
        val result = mutableListOf<BarChartItem>()
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -i) }
            val dayKey = sdf.format(c.time)
            val sum = dayGroups[dayKey]?.sumOf { it.amount } ?: 0.0
            result.add(
                BarChartItem(
                    label = dayKey,
                    value = sum,
                    colorHex = "#3B82F6"
                )
            )
        }
        return result
    }

    private fun getMonthRangeAndTitle(offset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTs = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val endTs = cal.timeInMillis

        val sdf = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        val title = if (offset == 0) "本月 (${sdf.format(Date(startTs))})" else sdf.format(Date(startTs))

        return Triple(startTs, endTs, title)
    }

    private fun getDaysInMonth(offset: Int): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun calculateCategoryShares(items: List<TransactionEntity>, total: Double): List<PieChartItem> {
        if (total <= 0) return emptyList()
        return items
            .groupBy { it.categoryName to it.categoryColorHex }
            .map { (key, txs) ->
                val amount = txs.sumOf { it.amount }
                val percentage = (amount / total).toFloat()
                PieChartItem(
                    label = key.first,
                    colorHex = key.second,
                    value = amount,
                    percentage = percentage
                )
            }
            .sortedByDescending { it.value }
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
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "DeleteTransaction", traceId = traceId) {
                dao.deleteTransactionById(id)
            }
            emitEffect(TransactionsEffect.ShowToast("已删除账单"))
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

    private fun triggerCloudBackup(traceId: String) {
        viewModelScope.launch {
            val res = CloudSyncManager.backupToCloud(viewState.value.transactions, traceId)
            res.onSuccess { count ->
                emitEffect(TransactionsEffect.ShowToast("云端备份成功 (已备份 $count 条账单)"))
            }.onFailure { err ->
                emitEffect(TransactionsEffect.ShowToast("云端备份失败: ${err.message}"))
            }
        }
    }

    private fun triggerCloudRestore(traceId: String) {
        viewModelScope.launch {
            val res = CloudSyncManager.restoreFromCloud(traceId)
            res.onSuccess { list ->
                dao.insertTransactions(list)
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
                val demoList = listOf(
                    TransactionEntity(type = "EXPENSE", categoryId = "c_food", categoryName = "餐饮", categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 68.5, note = "朋友聚餐日料", accountType = "WECHAT", timestamp = System.currentTimeMillis() - 3600000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_transport", categoryName = "交通", categoryIcon = "c_transport", categoryColorHex = "#3B82F6", amount = 25.0, note = "打车回家", accountType = "ALIPAY", timestamp = System.currentTimeMillis() - 14400000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_shopping", categoryName = "购物", categoryIcon = "c_shopping", categoryColorHex = "#EC4899", amount = 299.0, note = "买衣服", accountType = "BANK", timestamp = System.currentTimeMillis() - 86400000),
                    TransactionEntity(type = "INCOME", categoryId = "c_salary", categoryName = "工资", categoryIcon = "c_salary", categoryColorHex = "#10B981", amount = 12500.0, note = "8月发薪", accountType = "BANK", timestamp = System.currentTimeMillis() - 172800000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_housing", categoryName = "居住", categoryIcon = "c_housing", categoryColorHex = "#F59E0B", amount = 3200.0, note = "房屋房租", accountType = "WECHAT", timestamp = System.currentTimeMillis() - 259200000),
                    TransactionEntity(type = "EXPENSE", categoryId = "c_entertainment", categoryName = "娱乐", categoryIcon = "c_entertainment", categoryColorHex = "#8B5CF6", amount = 128.0, note = "电影票+零食", accountType = "ALIPAY", timestamp = System.currentTimeMillis() - 345600000)
                )
                dao.insertTransactions(demoList)
            }
            emitEffect(TransactionsEffect.ShowToast("成功填充 6 条测试演示账单！"))
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
