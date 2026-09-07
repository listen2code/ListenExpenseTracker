package com.listen.expensetracker.features.transactions.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.LifecycleEvent
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.engine.AnnualTransactionEngine
import com.listen.expensetracker.data.engine.RecurringTransactionEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.pref.observeExpensePreferences
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import java.util.Calendar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel managing ledger transactions list, filters, mutations, and undo actions.
 */
class TransactionsViewModel(
    private val application: Application
) : BaseViewModel<TransactionsUiState, TransactionsIntent>(TransactionsUiState()) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = ExpenseDataStoreManager(application)
    private val mutationHandler = TransactionMutationHandler(
        application = application, dao = dao, scope = viewModelScope,
        emitEffect = { emitEffect(it) },
        onRestore = { handleIntent(TransactionsIntent.RestoreDeletedTransaction(it)) }
    )

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
            is TransactionsIntent.AddTransaction -> mutationHandler.addTransaction(intent, traceId)
            is TransactionsIntent.UpdateTransaction -> mutationHandler.updateTransaction(intent.transaction, traceId)
            is TransactionsIntent.DeleteTransaction -> mutationHandler.deleteTransaction(intent.id, traceId, currentState.language)
            is TransactionsIntent.RestoreDeletedTransaction -> mutationHandler.restoreDeletedTransaction(intent.transaction, traceId, currentState.language)
            is TransactionsIntent.ToggleHideBalance -> if (currentState.shakeToHideBalanceEnabled || !intent.hide) {
                viewModelScope.launch { prefManager.setHideBalance(intent.hide) }
            }
            is TransactionsIntent.SearchQueryChange -> { updateState { copy(searchQuery = intent.query) }; recalculate() }
            is TransactionsIntent.FilterAccountChange -> { updateState { copy(selectedAccountFilter = intent.accountType) }; recalculate() }
            is TransactionsIntent.ChangePeriod -> { updateState { copy(period = intent.period) }; recalculate() }
            is TransactionsIntent.ChangeMonthOffset -> { updateState { copy(selectedMonthOffset = currentState.selectedMonthOffset + intent.offsetDelta, activeAnnualFilter = null) }; recalculate() }
            is TransactionsIntent.SetMonthOffset -> { updateState { copy(selectedMonthOffset = intent.offset, activeAnnualFilter = null) }; recalculate() }
            is TransactionsIntent.SelectMonth -> {
                updateState { copy(selectedMonthOffset = intent.offset, activeAnnualFilter = null) }; recalculate()
                emitEffect(TransactionsEffect.ScrollToMonth(intent.offset))
            }
            is TransactionsIntent.ChangeYearOffset -> { updateState { copy(selectedYearOffset = currentState.selectedYearOffset + intent.offsetDelta) }; recalculate() }
            is TransactionsIntent.SetYearOffset -> { updateState { copy(selectedYearOffset = intent.offset) }; recalculate() }
            is TransactionsIntent.SelectYear -> {
                updateState { copy(selectedYearOffset = intent.offset) }; recalculate()
                emitEffect(TransactionsEffect.ScrollToYear(intent.offset))
            }
            is TransactionsIntent.ScrollToTop -> emitEffect(TransactionsEffect.ScrollToTop)
            is TransactionsIntent.ChangeSortOrder -> { updateState { copy(sortOrder = intent.order) }; recalculate() }
            is TransactionsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is TransactionsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
            is TransactionsIntent.SeedDemoData -> mutationHandler.seedDemoData(intent.monthOffset, currentState.language)
            is TransactionsIntent.FilterByCategory -> {
                val cat = findCategory(intent.categoryName)
                updateState {
                    copy(
                        period = TransactionPeriod.MONTH, selectedMonthOffset = intent.monthOffset,
                        searchQuery = "", selectedAccountFilter = "ALL", typeFilter = cat?.type ?: "ALL",
                        selectedCategories = setOf(cat?.id ?: intent.categoryName), amountPreset = AmountFilterPreset.ALL,
                        customMinAmount = null, customMaxAmount = null, sortOrder = TransactionSortOrder.DATE_DESC,
                        activeAnnualFilter = null
                    )
                }
                recalculate(); emitEffect(TransactionsEffect.ScrollToMonth(intent.monthOffset))
            }
            is TransactionsIntent.FilterByAnnualCategory -> {
                val curYear = Calendar.getInstance().get(Calendar.YEAR)
                val cat = findCategory(intent.categoryName)
                updateState {
                    copy(
                        period = TransactionPeriod.YEAR, selectedYearOffset = intent.year - curYear,
                        searchQuery = "", selectedAccountFilter = "ALL", typeFilter = cat?.type ?: "ALL",
                        selectedCategories = setOf(cat?.id ?: intent.categoryName), amountPreset = AmountFilterPreset.ALL,
                        customMinAmount = null, customMaxAmount = null, sortOrder = TransactionSortOrder.DATE_DESC,
                        activeAnnualFilter = null
                    )
                }
                recalculate(); emitEffect(TransactionsEffect.ScrollToYear(intent.year - curYear))
            }
            is TransactionsIntent.ClearAnnualFilter -> {
                updateState { copy(activeAnnualFilter = null, selectedCategories = emptySet(), typeFilter = "ALL") }; recalculate()
            }
            is TransactionsIntent.FilterByDate -> {
                updateState {
                    copy(
                        selectedMonthOffset = intent.monthOffset, searchQuery = intent.dateLabel ?: "",
                        selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(),
                        amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null,
                        sortOrder = TransactionSortOrder.DATE_DESC, activeAnnualFilter = null
                    )
                }
                recalculate(); emitEffect(TransactionsEffect.ScrollToMonth(intent.monthOffset)); emitEffect(TransactionsEffect.ScrollToDay(intent.day))
            }
            is TransactionsIntent.FilterByTransaction -> {
                val amtStr = intent.amount?.formatAmount() ?: ""
                updateState {
                    copy(
                        selectedMonthOffset = intent.monthOffset, searchQuery = amtStr,
                        selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(),
                        amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null,
                        sortOrder = TransactionSortOrder.DATE_DESC, activeAnnualFilter = null
                    )
                }
                recalculate(); emitEffect(TransactionsEffect.ScrollToMonth(intent.monthOffset)); emitEffect(TransactionsEffect.ScrollToTransaction(intent.transactionId))
            }
            is TransactionsIntent.ChangeTypeFilter -> { updateState { copy(typeFilter = intent.type) }; recalculate() }
            is TransactionsIntent.ApplyCompoundFilter -> {
                updateState { copy(typeFilter = intent.type, selectedCategories = intent.categories, amountPreset = intent.preset, customMinAmount = intent.min, customMaxAmount = intent.max, sortOrder = intent.sortOrder) }; recalculate()
            }
            is TransactionsIntent.ResetAllFilters -> {
                updateState { copy(searchQuery = "", selectedAccountFilter = "ALL", typeFilter = "ALL", selectedCategories = emptySet(), amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null, sortOrder = TransactionSortOrder.DATE_DESC, activeAnnualFilter = null) }; recalculate()
            }
            is TransactionsIntent.ClearTypeFilter -> { updateState { copy(typeFilter = "ALL") }; recalculate() }
            is TransactionsIntent.ClearCategoryFilter -> { updateState { copy(selectedCategories = emptySet()) }; recalculate() }
            is TransactionsIntent.RemoveCategoryFilter -> { updateState { copy(selectedCategories = selectedCategories - intent.categoryId) }; recalculate() }
            is TransactionsIntent.ClearAmountFilter -> { updateState { copy(amountPreset = AmountFilterPreset.ALL, customMinAmount = null, customMaxAmount = null) }; recalculate() }
            is TransactionsIntent.ClearSortOrder -> { updateState { copy(sortOrder = TransactionSortOrder.DATE_DESC) }; recalculate() }
            is TransactionsIntent.UpdateMonthlyBudget -> { viewModelScope.launch { prefManager.setMonthlyBudget(intent.budget) } }
            is TransactionsIntent.UpdateCategoryBudgets -> viewModelScope.launch {
                prefManager.setMonthlyBudget(intent.totalBudget)
                prefManager.setCategoryBudgetRatios(intent.ratios)
            }
            is TransactionsIntent.ScreenAppear -> { checkDueRecurringRules(); recalculate() }
            is TransactionsIntent.ScreenDisappear -> Unit
        }
    }

    private fun findCategory(name: String) = CategoryRepository.allCategories.find {
        it.id.equals(name, true) || it.nameKey.equals(name, true) || it.customName.equals(name, true) ||
        listOf("zh", "en", "ja").any { lang -> it.getDisplayName(lang).equals(name, true) }
    }

    private fun checkDueRecurringRules() = viewModelScope.launch {
        RecurringTransactionEngine.processDueRules(db.recurringRuleDao(), dao)
    }

    override fun toLifecycleIntent(event: LifecycleEvent): TransactionsIntent? = when (event) {
        LifecycleEvent.ON_APPEAR -> TransactionsIntent.ScreenAppear
        LifecycleEvent.ON_DISAPPEAR -> TransactionsIntent.ScreenDisappear
    }

    private fun observeSettings() = observeExpensePreferences(prefManager) { prefs ->
        AccountRepository.deserializeCustomAccounts(prefs.customAccounts)
        updateState {
            copy(
                language = prefs.language, themeMode = prefs.themeMode, accentColor = prefs.accentColor,
                currencySymbol = prefs.currencySymbol, monthlyBudget = prefs.monthlyBudget,
                categoryBudgetRatios = prefs.categoryBudgetRatios, hideBalance = prefs.hideBalance,
                isDeveloperMode = prefs.isDeveloperMode, shakeToHideBalanceEnabled = prefs.shakeToHideBalanceEnabled
            )
        }
        recalculate()
        ListenExpenseAppWidgetProvider.updateFromTransactions(
            application, currentState.transactions, prefs.currencySymbol, prefs.monthlyBudget, prefs.language
        )
    }

    private fun observeTransactions() = viewModelScope.launch {
        dao.getAllTransactionsFlow().collectLatest { allList ->
            applyCalculations(allList)
            ListenExpenseAppWidgetProvider.updateFromTransactions(
                application, allList, currentState.currencySymbol, currentState.monthlyBudget, currentState.language
            )
            GoogleDriveAutoBackupManager.scheduleAutoBackup(application)
        }
    }

    private fun recalculate() = viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }

    private fun applyCalculations(allList: List<TransactionEntity>) {
        if (currentState.period == TransactionPeriod.YEAR) {
            val calc = AnnualTransactionEngine.filterAndCalculateYearTransactions(
                allList = allList, yearOffset = currentState.selectedYearOffset, query = currentState.searchQuery,
                accountFilter = currentState.selectedAccountFilter, budget = currentState.monthlyBudget,
                sortOrder = currentState.sortOrder, currencySymbol = currentState.currencySymbol, lang = currentState.language,
                typeFilter = currentState.typeFilter, selectedCategories = currentState.selectedCategories,
                amountPreset = currentState.amountPreset,
                customMinAmount = currentState.customMinAmount, customMaxAmount = currentState.customMaxAmount
            )
            updateState {
                copy(
                    transactions = allList, filteredTransactions = calc.filteredTransactions,
                    totalExpense = calc.totalExpense, totalIncome = calc.totalIncome,
                    netBalance = calc.netBalance, remainingBudget = calc.remainingBudget,
                    budgetUsageRatio = calc.budgetUsageRatio, isOverBudget = calc.isOverBudget,
                    yearTitle = calc.monthTitle, isLoading = false
                )
            }
            return
        }

        val calc = TransactionCalculationEngine.filterAndCalculate(
            allList = allList, currentOffset = currentState.selectedMonthOffset, query = currentState.searchQuery,
            accountFilter = currentState.selectedAccountFilter, budget = currentState.monthlyBudget,
            sortOrder = currentState.sortOrder, currencySymbol = currentState.currencySymbol, lang = currentState.language,
            typeFilter = currentState.typeFilter, selectedCategories = currentState.selectedCategories,
            amountPreset = currentState.amountPreset,
            customMinAmount = currentState.customMinAmount, customMaxAmount = currentState.customMaxAmount
        )

        updateState {
            copy(
                transactions = allList, filteredTransactions = calc.filteredTransactions,
                totalExpense = calc.totalExpense, totalIncome = calc.totalIncome, netBalance = calc.netBalance,
                monthlyBudget = calc.monthlyBudget, remainingBudget = calc.remainingBudget,
                budgetUsageRatio = calc.budgetUsageRatio, isOverBudget = calc.isOverBudget,
                monthTitle = calc.monthTitle, isLoading = false
            )
        }
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TransactionsViewModel(app) as T
    }
}
