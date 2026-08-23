package com.listen.expensetracker.features.transactions.viewmodel

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.i18n.StringsRes
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
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
) : BaseViewModel<TransactionsUiState, TransactionsIntent, CommonUiEffect>(TransactionsUiState()) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val prefManager = ExpenseDataStoreManager(application)

    init {
        ApmLogger.i(tag = "VM", message = "TransactionsViewModel initialized")
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
            is TransactionsIntent.ToggleHideBalance -> updateState { copy(hideBalance = intent.hide) }
            is TransactionsIntent.SearchQueryChange -> {
                updateState { copy(searchQuery = intent.query) }
                recalculate()
            }
            is TransactionsIntent.FilterAccountChange -> {
                updateState { copy(selectedAccountFilter = intent.accountType) }
                recalculate()
            }
            is TransactionsIntent.ChangeMonthOffset -> {
                val newOffset = currentState.selectedMonthOffset + intent.offsetDelta
                updateState { copy(selectedMonthOffset = newOffset) }
                recalculate()
            }
            is TransactionsIntent.SetMonthOffset -> {
                updateState { copy(selectedMonthOffset = intent.offset) }
                recalculate()
            }
            is TransactionsIntent.ChangeSortOrder -> {
                updateState { copy(sortOrder = intent.order) }
                recalculate()
            }
            is TransactionsIntent.OpenDialog -> updateState { copy(activeDialog = intent.dialog) }
            is TransactionsIntent.DismissDialog -> updateState { copy(activeDialog = null) }
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            prefManager.languageFlow.collectLatest { lang ->
                updateState { copy(language = lang) }
                recalculate()
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
                recalculate()
            }
        }
    }

    private fun observeTransactions() {
        viewModelScope.launch {
            dao.getAllTransactionsFlow().collectLatest { allList ->
                applyCalculations(allList)
                updateWidgets(allList)
            }
        }
    }

    private fun updateWidgets(allList: List<TransactionEntity>) {
        val todayCal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val todayStart = todayCal.timeInMillis
        val todayExp = allList.filter { it.type == "EXPENSE" && it.timestamp >= todayStart }.sumOf { it.amount }
        try {
            ListenExpenseAppWidgetProvider.updateAllWidgets(application, todayExp, currentState.currencySymbol)
        } catch (_: Exception) {}
    }

    private fun recalculate() {
        viewModelScope.launch {
            val allList = dao.getAllTransactions()
            applyCalculations(allList)
        }
    }

    private fun applyCalculations(allList: List<TransactionEntity>) {
        val calculated = TransactionCalculationEngine.filterAndCalculate(
            allList = allList,
            currentOffset = currentState.selectedMonthOffset,
            query = currentState.searchQuery,
            accountFilter = currentState.selectedAccountFilter,
            budget = currentState.monthlyBudget,
            sortOrder = currentState.sortOrder,
            currencySymbol = currentState.currencySymbol,
            lang = currentState.language
        )

        updateState {
            copy(
                transactions = allList,
                filteredTransactions = calculated.filteredTransactions,
                totalExpense = calculated.totalExpense,
                totalIncome = calculated.totalIncome,
                netBalance = calculated.netBalance,
                monthlyBudget = calculated.monthlyBudget,
                remainingBudget = calculated.remainingBudget,
                budgetUsageRatio = calculated.budgetUsageRatio,
                isOverBudget = calculated.isOverBudget,
                monthTitle = calculated.monthTitle,
                isLoading = false
            )
        }
    }

    private fun addTransaction(
        intent: TransactionsIntent.AddTransaction,
        traceId: String
    ) {
        viewModelScope.launch {
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
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "InsertTransaction", traceId = traceId) {
                dao.insertTransaction(entity)
            }
        }
    }

    private fun updateTransaction(transaction: TransactionEntity, traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "UpdateTransaction", traceId = traceId) {
                dao.updateTransaction(transaction)
            }
        }
    }

    private fun deleteTransaction(id: String, traceId: String) {
        viewModelScope.launch {
            val entity = dao.getTransactionById(id)
            if (entity != null) {
                TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "DeleteTransaction", traceId = traceId) {
                    dao.deleteTransaction(entity)
                }
                val lang = currentState.language
                emitEffect(CommonUiEffect.ShowSnackbar(
                    message = AppStrings.undo_delete_toast.tr(lang),
                    actionLabel = AppStrings.undo_action_label.tr(lang),
                    onAction = {
                        handleIntent(TransactionsIntent.RestoreDeletedTransaction(entity))
                    }
                ))
            }
        }
    }

    private fun restoreDeletedTransaction(tx: TransactionEntity, traceId: String) {
        viewModelScope.launch {
            TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "RestoreTransaction", traceId = traceId) {
                dao.insertTransaction(tx)
            }
            emitEffect(CommonUiEffect.ShowToast(AppStrings.undo_success_toast.tr(currentState.language)))
        }
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TransactionsViewModel(application) as T
        }
    }
}
