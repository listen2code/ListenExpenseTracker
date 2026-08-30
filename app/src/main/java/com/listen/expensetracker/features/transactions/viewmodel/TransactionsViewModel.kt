package com.listen.expensetracker.features.transactions.viewmodel

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import android.app.Application
import java.util.Calendar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.engine.DemoDataEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel managing ledger transactions list, filters, add/edit/delete mutations, and undo actions.
 */
class TransactionsViewModel(
    private val application: Application
) : BaseViewModel<TransactionsUiState, TransactionsIntent, CommonUiEffect>(TransactionsUiState()) {

    private val _screenEffect = MutableSharedFlow<TransactionsEffect>(replay = 0, extraBufferCapacity = 64)
    val screenEffect = _screenEffect.asSharedFlow()

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = ExpenseDataStoreManager(application)

    init {
        ApmLogger.i(tag = "VM", message = "TransactionsViewModel initialized")
        AccountRepository.onAccountsChangedListener = { json ->
            viewModelScope.launch { prefManager.setCustomAccountsJson(json) }
        }
        observeSettings()
        observeTransactions()
    }

    override fun handleIntent(intent: TransactionsIntent) {
        val traceId = TraceManager.newTraceId()
        when (intent) {
            is TransactionsIntent.LoadData -> observeTransactions()
            is TransactionsIntent.AddTransaction -> addTransaction(intent, traceId)
            is TransactionsIntent.UpdateTransaction -> updateTransaction(intent.transaction, traceId)
            is TransactionsIntent.DeleteTransaction -> deleteTransaction(intent.id, traceId)
            is TransactionsIntent.RestoreDeletedTransaction -> restoreDeletedTransaction(intent.transaction, traceId)
            is TransactionsIntent.ToggleHideBalance -> { viewModelScope.launch { prefManager.setHideBalance(intent.hide) } }
            is TransactionsIntent.SearchQueryChange -> { updateState { copy(searchQuery = intent.query) }; recalculate() }
            is TransactionsIntent.FilterAccountChange -> { updateState { copy(selectedAccountFilter = intent.accountType) }; recalculate() }
            is TransactionsIntent.ChangeMonthOffset -> { updateState { copy(selectedMonthOffset = currentState.selectedMonthOffset + intent.offsetDelta) }; recalculate() }
            is TransactionsIntent.SetMonthOffset -> { updateState { copy(selectedMonthOffset = intent.offset) }; recalculate() }
            is TransactionsIntent.SelectMonth -> {
                updateState { copy(selectedMonthOffset = intent.offset) }; recalculate()
                _screenEffect.tryEmit(TransactionsEffect.ScrollToMonth(intent.offset))
            }
            is TransactionsIntent.ScrollToTop -> { _screenEffect.tryEmit(TransactionsEffect.ScrollToTop) }
            is TransactionsIntent.ChangeSortOrder -> { updateState { copy(sortOrder = intent.order) }; recalculate() }
            is TransactionsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is TransactionsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
            is TransactionsIntent.SeedDemoData -> seedDemoData(intent.monthOffset)
            is TransactionsIntent.FilterByCategory -> {
                val cat = CategoryRepository.allCategories.find {
                    it.id.equals(intent.categoryName, true) || it.nameKey.equals(intent.categoryName, true) ||
                    it.customName.equals(intent.categoryName, true) || it.getDisplayName("zh").equals(intent.categoryName, true) ||
                    it.getDisplayName("en").equals(intent.categoryName, true) || it.getDisplayName("ja").equals(intent.categoryName, true)
                }
                updateState {
                    copy(
                        selectedMonthOffset = intent.monthOffset, searchQuery = "", selectedAccountFilter = "ALL",
                        typeFilter = cat?.type ?: "ALL", selectedCategories = setOf(cat?.id ?: intent.categoryName),
                        amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null,
                        sortOrder = TransactionSortOrder.DATE_DESC
                    )
                }
                recalculate()
                _screenEffect.tryEmit(TransactionsEffect.ScrollToMonth(intent.monthOffset))
            }
            is TransactionsIntent.FilterByDate -> {
                updateState {
                    copy(
                        selectedMonthOffset = intent.monthOffset, searchQuery = intent.dateLabel ?: "",
                        selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(),
                        amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null,
                        sortOrder = TransactionSortOrder.DATE_DESC
                    )
                }
                recalculate()
                _screenEffect.tryEmit(TransactionsEffect.ScrollToMonth(intent.monthOffset))
                _screenEffect.tryEmit(TransactionsEffect.ScrollToDay(intent.day))
            }
            is TransactionsIntent.FilterByTransaction -> {
                val amtStr = if (intent.amount != null) {
                    if (intent.amount % 1.0 == 0.0) "%.0f".format(intent.amount) else "%.2f".format(intent.amount)
                } else ""
                updateState {
                    copy(
                        selectedMonthOffset = intent.monthOffset, searchQuery = amtStr,
                        selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(),
                        amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null,
                        sortOrder = TransactionSortOrder.DATE_DESC
                    )
                }
                recalculate()
                _screenEffect.tryEmit(TransactionsEffect.ScrollToMonth(intent.monthOffset))
                _screenEffect.tryEmit(TransactionsEffect.ScrollToTransaction(intent.transactionId))
            }
            is TransactionsIntent.ChangeTypeFilter -> { updateState { copy(typeFilter = intent.type) }; recalculate() }
            is TransactionsIntent.ApplyCompoundFilter -> {
                updateState { copy(typeFilter = intent.type, selectedCategories = intent.categories, amountPreset = intent.preset, customMinAmount = intent.min, customMaxAmount = intent.max, sortOrder = intent.sortOrder) }
                recalculate()
            }
            is TransactionsIntent.ResetAllFilters -> {
                updateState { copy(searchQuery = "", selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(), amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null, sortOrder = TransactionSortOrder.DATE_DESC) }
                recalculate()
            }
            is TransactionsIntent.ClearTypeFilter -> { updateState { copy(typeFilter = "ALL") }; recalculate() }
            is TransactionsIntent.ClearCategoryFilter -> { updateState { copy(selectedCategories = emptySet()) }; recalculate() }
            is TransactionsIntent.RemoveCategoryFilter -> { updateState { copy(selectedCategories = selectedCategories - intent.categoryId) }; recalculate() }
            is TransactionsIntent.ClearAmountFilter -> { updateState { copy(amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null) }; recalculate() }
            is TransactionsIntent.ClearSortOrder -> { updateState { copy(sortOrder = TransactionSortOrder.DATE_DESC) }; recalculate() }
            is TransactionsIntent.UpdateMonthlyBudget -> { viewModelScope.launch { prefManager.setMonthlyBudget(intent.budget) } }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch { prefManager.languageFlow.collectLatest { updateState { copy(language = it) }; recalculate() } }
        viewModelScope.launch { prefManager.themeModeFlow.collectLatest { mode -> updateState { copy(themeMode = try { ThemeMode.valueOf(mode) } catch (_: Exception) { ThemeMode.SYSTEM }) } } }
        viewModelScope.launch { prefManager.accentColorFlow.collectLatest { a -> updateState { copy(accentColor = try { AccentColor.valueOf(a) } catch (_: Exception) { AccentColor.EMERALD }) } } }
        viewModelScope.launch { prefManager.currencySymbolFlow.collectLatest { sym -> updateState { copy(currencySymbol = sym) } } }
        viewModelScope.launch { prefManager.monthlyBudgetFlow.collectLatest { updateState { copy(monthlyBudget = it) }; recalculate() } }
        viewModelScope.launch { prefManager.customAccountsFlow.collectLatest { AccountRepository.deserializeCustomAccounts(it); recalculate() } }
        viewModelScope.launch { prefManager.hideBalanceFlow.collectLatest { updateState { copy(hideBalance = it) } } }
        viewModelScope.launch { prefManager.isDeveloperModeFlow.collectLatest { updateState { copy(isDeveloperMode = it) } } }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            dao.getAllTransactionsFlow().collectLatest { allList ->
                applyCalculations(allList); updateWidgets(allList)
                GoogleDriveAutoBackupManager.scheduleAutoBackup(application)
            }
        }
    }

    private fun updateWidgets(allList: List<TransactionEntity>) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayExp = allList.filter { it.type == TransactionType.EXPENSE && it.timestamp >= todayStart }.sumOf { it.amount }
        try { ListenExpenseAppWidgetProvider.updateAllWidgets(application, todayExp, currentState.currencySymbol) } catch (_: Exception) {}
    }

    private fun recalculate() {
        viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
    }

    private fun applyCalculations(allList: List<TransactionEntity>) {
        val calculated = TransactionCalculationEngine.filterAndCalculate(
            allList = allList, currentOffset = currentState.selectedMonthOffset, query = currentState.searchQuery,
            accountFilter = currentState.selectedAccountFilter, budget = currentState.monthlyBudget,
            sortOrder = currentState.sortOrder, currencySymbol = currentState.currencySymbol, lang = currentState.language,
            typeFilter = currentState.typeFilter, selectedCategories = currentState.selectedCategories,
            amountPreset = currentState.amountPreset,
            customMinAmount = currentState.customMinAmount, customMaxAmount = currentState.customMaxAmount
        )

        updateState {
            copy(
                transactions = allList, filteredTransactions = calculated.filteredTransactions,
                totalExpense = calculated.totalExpense, totalIncome = calculated.totalIncome,
                netBalance = calculated.netBalance, monthlyBudget = calculated.monthlyBudget,
                remainingBudget = calculated.remainingBudget, budgetUsageRatio = calculated.budgetUsageRatio,
                isOverBudget = calculated.isOverBudget, monthTitle = calculated.monthTitle, isLoading = false
            )
        }
    }

    private fun addTransaction(intent: TransactionsIntent.AddTransaction, traceId: String) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                type = intent.type, categoryId = intent.categoryId, categoryName = intent.categoryName,
                categoryIcon = intent.categoryIcon, categoryColorHex = intent.categoryColorHex,
                amount = intent.amount, note = intent.note, accountType = intent.accountType, timestamp = intent.timestamp
            )
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "InsertTransaction", traceId = traceId) {
                dao.insertTransaction(entity)
            }
        }
    }

    private fun updateTransaction(transaction: TransactionEntity, traceId: String) = viewModelScope.launch {
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "UpdateTransaction", traceId = traceId) {
            dao.updateTransaction(transaction)
        }
    }

    private fun deleteTransaction(id: String, traceId: String) = viewModelScope.launch {
        val entity = dao.getTransactionById(id) ?: return@launch
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "DeleteTransaction", traceId = traceId) {
            dao.deleteTransaction(entity)
        }
        val lang = currentState.language
        emitEffect(CommonUiEffect.ShowSnackbar(
            message = AppStrings.UNDO_DELETE_TOAST.tr(lang),
            actionLabel = AppStrings.UNDO_ACTION_LABEL.tr(lang),
            onAction = { handleIntent(TransactionsIntent.RestoreDeletedTransaction(entity)) }
        ))
    }

    private fun restoreDeletedTransaction(tx: TransactionEntity, traceId: String) = viewModelScope.launch {
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "RestoreTransaction", traceId = traceId) {
            dao.insertTransaction(tx)
        }
        emitEffect(CommonUiEffect.ShowToast(AppStrings.UNDO_SUCCESS_TOAST.tr(currentState.language)))
    }

    private fun seedDemoData(monthOffset: Int) = viewModelScope.launch {
        val accounts = AccountRepository.getAllAccounts().map { it.key }.ifEmpty { listOf("CASH", "BANK", "CREDIT") }
        val generated = DemoDataEngine.generate(monthOffset, currentState.language, accounts)
        dao.insertTransactions(generated)
        val (_, _, title) = TransactionCalculationEngine.getMonthRangeAndTitle(monthOffset, currentState.language)
        emitEffect(CommonUiEffect.ShowToast(AppStrings.SEED_MONTH_SUCCESS_TOAST.tr(currentState.language).format(title, generated.size)))
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TransactionsViewModel(application) as T
    }
}
