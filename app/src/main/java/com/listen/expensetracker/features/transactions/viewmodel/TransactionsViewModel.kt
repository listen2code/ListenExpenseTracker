package com.listen.expensetracker.features.transactions.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.i18n.tr
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import com.listen.arch.mvi.LifecycleEvent
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.engine.DemoDataEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.pref.observeExpensePreferences
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel managing ledger transactions list, filters, add/edit/delete mutations, and undo actions.
 */
class TransactionsViewModel(
    private val application: Application
) : BaseViewModel<TransactionsUiState, TransactionsIntent>(TransactionsUiState()) {

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
        checkDueRecurringRules()
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
                emitEffect(TransactionsEffect.ScrollToMonth(intent.offset))
            }
            is TransactionsIntent.ScrollToTop -> { emitEffect(TransactionsEffect.ScrollToTop) }
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
                emitEffect(TransactionsEffect.ScrollToMonth(intent.monthOffset))
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
                emitEffect(TransactionsEffect.ScrollToMonth(intent.monthOffset))
                emitEffect(TransactionsEffect.ScrollToDay(intent.day))
            }
            is TransactionsIntent.FilterByTransaction -> {
                val amtStr = intent.amount?.formatAmount() ?: ""
                updateState {
                    copy(
                        selectedMonthOffset = intent.monthOffset, searchQuery = amtStr,
                        selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(),
                        amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null,
                        sortOrder = TransactionSortOrder.DATE_DESC
                    )
                }
                recalculate()
                emitEffect(TransactionsEffect.ScrollToMonth(intent.monthOffset))
                emitEffect(TransactionsEffect.ScrollToTransaction(intent.transactionId))
            }
            is TransactionsIntent.ChangeTypeFilter -> { updateState { copy(typeFilter = intent.type) }; recalculate() }
            is TransactionsIntent.ApplyCompoundFilter -> {
                updateState { copy(typeFilter = intent.type, selectedCategories = intent.categories, amountPreset = intent.preset, customMinAmount = intent.min, customMaxAmount = intent.max, sortOrder = intent.sortOrder) }; recalculate()
            }
            is TransactionsIntent.ResetAllFilters -> {
                updateState { copy(searchQuery = "", selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(), amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null, sortOrder = TransactionSortOrder.DATE_DESC) }; recalculate()
            }
            is TransactionsIntent.ClearTypeFilter -> { updateState { copy(typeFilter = "ALL") }; recalculate() }
            is TransactionsIntent.ClearCategoryFilter -> { updateState { copy(selectedCategories = emptySet()) }; recalculate() }
            is TransactionsIntent.RemoveCategoryFilter -> { updateState { copy(selectedCategories = selectedCategories - intent.categoryId) }; recalculate() }
            is TransactionsIntent.ClearAmountFilter -> { updateState { copy(amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null) }; recalculate() }
            is TransactionsIntent.ClearSortOrder -> { updateState { copy(sortOrder = TransactionSortOrder.DATE_DESC) }; recalculate() }
            is TransactionsIntent.UpdateMonthlyBudget -> { viewModelScope.launch { prefManager.setMonthlyBudget(intent.budget) } }
            is TransactionsIntent.UpdateCategoryBudgets -> {
                viewModelScope.launch {
                    prefManager.setMonthlyBudget(intent.totalBudget)
                    prefManager.setCategoryBudgetRatios(intent.ratios)
                }
            }
            is TransactionsIntent.ScreenAppear -> { checkDueRecurringRules(); recalculate() }
            is TransactionsIntent.ScreenDisappear -> Unit
        }
    }

    private fun checkDueRecurringRules() {
        viewModelScope.launch {
            com.listen.expensetracker.data.engine.RecurringTransactionEngine.processDueRules(db.recurringRuleDao(), dao)
        }
    }

    override fun toLifecycleIntent(event: LifecycleEvent): TransactionsIntent? = when (event) {
        LifecycleEvent.ON_APPEAR -> TransactionsIntent.ScreenAppear
        LifecycleEvent.ON_DISAPPEAR -> TransactionsIntent.ScreenDisappear
    }

    private fun observeSettings() {
        observeExpensePreferences(prefManager) { prefs ->
            AccountRepository.deserializeCustomAccounts(prefs.customAccounts)
            updateState {
                copy(
                    language = prefs.language, themeMode = prefs.themeMode, accentColor = prefs.accentColor,
                    currencySymbol = prefs.currencySymbol, monthlyBudget = prefs.monthlyBudget,
                    categoryBudgetRatios = prefs.categoryBudgetRatios,
                    hideBalance = prefs.hideBalance, isDeveloperMode = prefs.isDeveloperMode
                )
            }
            recalculate()
            ListenExpenseAppWidgetProvider.updateFromTransactions(
                application, currentState.transactions, prefs.currencySymbol, prefs.monthlyBudget, prefs.language
            )
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            dao.getAllTransactionsFlow().collectLatest { allList ->
                applyCalculations(allList)
                ListenExpenseAppWidgetProvider.updateFromTransactions(
                    application, allList, currentState.currencySymbol, currentState.monthlyBudget, currentState.language
                )
                GoogleDriveAutoBackupManager.scheduleAutoBackup(application)
            }
        }
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

    private fun addTransaction(intent: TransactionsIntent.AddTransaction, traceId: String) = viewModelScope.launch {
        val entity = TransactionEntity(
            type = intent.type, categoryId = intent.categoryId, categoryName = intent.categoryName,
            categoryIcon = intent.categoryIcon, categoryColorHex = intent.categoryColorHex,
            amount = intent.amount, note = intent.note, accountType = intent.accountType, timestamp = intent.timestamp
        )
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "InsertTransaction", traceId = traceId) { dao.insertTransaction(entity) }
    }

    private fun updateTransaction(transaction: TransactionEntity, traceId: String) = viewModelScope.launch {
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "UpdateTransaction", traceId = traceId) { dao.updateTransaction(transaction) }
    }

    private fun deleteTransaction(id: String, traceId: String) = viewModelScope.launch {
        val entity = dao.getTransactionById(id) ?: return@launch
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "DeleteTransaction", traceId = traceId) { dao.deleteTransaction(entity) }
        val lang = currentState.language
        emitEffect(CommonUiEffect.ShowSnackbar(
            message = AppStrings.UNDO_DELETE_TOAST.tr(lang), actionLabel = AppStrings.UNDO_ACTION_LABEL.tr(lang),
            onAction = { handleIntent(TransactionsIntent.RestoreDeletedTransaction(entity)) }
        ))
    }

    private fun restoreDeletedTransaction(tx: TransactionEntity, traceId: String) = viewModelScope.launch {
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "RestoreTransaction", traceId = traceId) { dao.insertTransaction(tx) }
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
