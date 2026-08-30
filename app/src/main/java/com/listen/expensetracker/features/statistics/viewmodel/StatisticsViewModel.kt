package com.listen.expensetracker.features.statistics.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogger
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel dedicated exclusively to financial analysis, category breakdown, and multi-dimensional statistics.
 */
class StatisticsViewModel(
    application: Application
) : BaseViewModel<StatisticsUiState, StatisticsIntent, CommonUiEffect>(StatisticsUiState()) {

    private val _screenEffect = MutableSharedFlow<StatisticsEffect>(replay = 0, extraBufferCapacity = 64)
    val screenEffect = _screenEffect.asSharedFlow()

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = ExpenseDataStoreManager(application)

    init {
        ApmLogger.i(tag = "VM", message = "StatisticsViewModel initialized")
        observeSettings()
        observeTransactions()
    }

    override fun handleIntent(intent: StatisticsIntent) {
        when (intent) {
            is StatisticsIntent.ChangeMonthOffset -> {
                val newOffset = currentState.selectedMonthOffset + intent.offsetDelta
                updateState { copy(selectedMonthOffset = newOffset) }
                viewModelScope.launch {
                    val allList = dao.getAllTransactions()
                    applyCalculations(allList)
                }
            }
            is StatisticsIntent.SetMonthOffset -> {
                updateState { copy(selectedMonthOffset = intent.offset) }
                viewModelScope.launch {
                    val allList = dao.getAllTransactions()
                    applyCalculations(allList)
                }
            }
            is StatisticsIntent.ChangeStatisticsTab -> {
                updateState { copy(statisticsTab = intent.tab) }
            }
            is StatisticsIntent.ToggleHideAmount -> {
                viewModelScope.launch { prefManager.setHideBalance(intent.hide) }
            }
            is StatisticsIntent.OpenMonthPicker -> updateState { copy(showMonthPicker = true) }
            is StatisticsIntent.DismissMonthPicker -> updateState { copy(showMonthPicker = false) }
            is StatisticsIntent.ScrollToTop -> { _screenEffect.tryEmit(StatisticsEffect.ScrollToTop) }
            is StatisticsIntent.SelectMonth -> {
                updateState { copy(selectedMonthOffset = intent.offset) }
                viewModelScope.launch {
                    val allList = dao.getAllTransactions()
                    applyCalculations(allList)
                }
                _screenEffect.tryEmit(StatisticsEffect.ScrollToMonth(intent.offset))
            }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            prefManager.languageFlow.collectLatest { lang ->
                updateState { copy(language = lang) }
                val allList = dao.getAllTransactions()
                applyCalculations(allList)
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
        viewModelScope.launch {
            prefManager.hideBalanceFlow.collectLatest { hide ->
                updateState { copy(hideAmount = hide) }
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            dao.getAllTransactionsFlow().collectLatest { allList ->
                applyCalculations(allList)
            }
        }
    }

    private fun applyCalculations(allList: List<TransactionEntity>) {
        val calculated = TransactionCalculationEngine.filterAndCalculate(
            allList = allList,
            currentOffset = currentState.selectedMonthOffset,
            query = "",
            accountFilter = "ALL",
            budget = currentState.monthlyBudget,
            sortOrder = TransactionSortOrder.DATE_DESC,
            currencySymbol = currentState.currencySymbol,
            lang = currentState.language
        )

        updateState {
            copy(
                allTransactions = allList,
                categoryShares = calculated.categoryShares,
                progressSegments = calculated.progressSegments,
                incomeCategoryShares = calculated.incomeCategoryShares,
                incomeProgressSegments = calculated.incomeProgressSegments,
                dailyTrendBars = calculated.dailyTrendBars,
                dailyTrendPoints = calculated.dailyTrendPoints,
                totalExpense = calculated.totalExpense,
                totalIncome = calculated.totalIncome,
                netBalance = calculated.netBalance,
                monthlyBudget = calculated.monthlyBudget,
                remainingBudget = calculated.remainingBudget,
                budgetUsageRatio = calculated.budgetUsageRatio,
                isOverBudget = calculated.isOverBudget,
                dailyAverageExpense = calculated.dailyAverageExpense,
                dailyAverageIncome = calculated.dailyAverageIncome,
                maxExpenseTransaction = calculated.maxExpenseTransaction,
                maxIncomeTransaction = calculated.maxIncomeTransaction,
                monthTitle = calculated.monthTitle,
                isLoading = false
            )
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(application) as T
        }
    }
}
