package com.listen.expensetracker.features.statistics.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogger
import com.listen.arch.mvi.BaseViewModel
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.AnnualCalculationEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.pref.observeExpensePreferences
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * ViewModel dedicated exclusively to financial analysis, category breakdown, and multi-dimensional statistics.
 */
class StatisticsViewModel(
    application: Application
) : BaseViewModel<StatisticsUiState, StatisticsIntent>(StatisticsUiState()) {

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
            is StatisticsIntent.ChangePeriod -> {
                updateState { copy(period = intent.period) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
            }
            is StatisticsIntent.ChangeMonthOffset -> {
                val newOffset = currentState.selectedMonthOffset + intent.offsetDelta
                updateState { copy(selectedMonthOffset = newOffset) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
            }
            is StatisticsIntent.SetMonthOffset -> {
                updateState { copy(selectedMonthOffset = intent.offset) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
            }
            is StatisticsIntent.SelectMonth -> {
                updateState { copy(selectedMonthOffset = intent.offset) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
                emitEffect(StatisticsEffect.ScrollToMonth(intent.offset))
            }
            is StatisticsIntent.ChangeYearOffset -> {
                val newOffset = currentState.selectedYearOffset + intent.offsetDelta
                updateState { copy(selectedYearOffset = newOffset) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
            }
            is StatisticsIntent.SetYearOffset -> {
                updateState { copy(selectedYearOffset = intent.offset) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
            }
            is StatisticsIntent.SelectYear -> {
                updateState { copy(selectedYearOffset = intent.offset) }
                viewModelScope.launch { applyCalculations(dao.getAllTransactions()) }
                emitEffect(StatisticsEffect.ScrollToYear(intent.offset))
            }
            is StatisticsIntent.ChangeStatisticsTab -> {
                updateState { copy(statisticsTab = intent.tab) }
            }
            is StatisticsIntent.ToggleHideAmount -> {
                viewModelScope.launch { prefManager.setHideBalance(intent.hide) }
            }
            is StatisticsIntent.OpenMonthPicker -> updateState { copy(showMonthPicker = true) }
            is StatisticsIntent.DismissMonthPicker -> updateState { copy(showMonthPicker = false) }
            is StatisticsIntent.ScrollToTop -> { emitEffect(StatisticsEffect.ScrollToTop) }
        }
    }

    private fun observeSettings() {
        observeExpensePreferences(prefManager) { prefs ->
            val budget = prefs.monthlyBudget
            val ratio = if (budget > 0) (currentState.totalExpense / budget).toFloat() else 0f
            updateState {
                copy(
                    language = prefs.language,
                    themeMode = prefs.themeMode,
                    accentColor = prefs.accentColor,
                    currencySymbol = prefs.currencySymbol,
                    monthlyBudget = budget,
                    remainingBudget = (budget - totalExpense).coerceAtLeast(0.0),
                    budgetUsageRatio = ratio,
                    isOverBudget = totalExpense > budget,
                    hideAmount = prefs.hideBalance
                )
            }
            applyCalculations(dao.getAllTransactions())
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
        if (currentState.period == StatisticsPeriod.YEAR) {
            val annualResult = AnnualCalculationEngine.filterAndCalculateYear(
                allList = allList,
                yearOffset = currentState.selectedYearOffset,
                lang = currentState.language
            )
            val annualBudget = currentState.monthlyBudget * 12
            val annualRatio = if (annualBudget > 0) (annualResult.totalExpense / annualBudget).toFloat() else 0f
            updateState {
                copy(
                    allTransactions = allList,
                    categoryShares = annualResult.categoryShares,
                    progressSegments = annualResult.progressSegments,
                    incomeCategoryShares = annualResult.incomeCategoryShares,
                    incomeProgressSegments = annualResult.incomeProgressSegments,
                    dailyTrendBars = emptyList(),
                    dailyTrendPoints = annualResult.monthlyTrendPoints,
                    totalExpense = annualResult.totalExpense,
                    totalIncome = annualResult.totalIncome,
                    netBalance = annualResult.netBalance,
                    remainingBudget = (annualBudget - annualResult.totalExpense).coerceAtLeast(0.0),
                    budgetUsageRatio = annualRatio,
                    isOverBudget = annualResult.totalExpense > annualBudget,
                    dailyAverageExpense = annualResult.monthlyAverageExpense,
                    dailyAverageIncome = annualResult.monthlyAverageIncome,
                    maxExpenseTransaction = annualResult.maxExpenseTransaction,
                    maxIncomeTransaction = annualResult.maxIncomeTransaction,
                    yearTitle = annualResult.yearTitle,
                    isLoading = false
                )
            }
        } else {
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
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatisticsViewModel(application) as T
        }
    }
}
