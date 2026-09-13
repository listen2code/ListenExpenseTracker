package com.listen.expensetracker.core.state

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import java.util.Calendar

/**
 * 导航与跨界面路由扩展方法 (ExpenseAppStateNavigation)。
 * 从主状态机中拆分，保持主文件行数 <= 250 行。
 */
fun ExpenseAppState.navigateToTransactionsCategory(categoryName: String, monthOffset: Int) {
    preserveStatisticsYearOnReturn = false
    statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
    statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByCategory(categoryName, monthOffset))
    lastTimeTab = NavTab.TRANSACTIONS
    currentTab = NavTab.TRANSACTIONS
}

fun ExpenseAppState.navigateToTransactionsAnnualCategory(year: Int, categoryName: String) {
    preserveStatisticsYearOnReturn = false
    val curYear = Calendar.getInstance().get(Calendar.YEAR)
    statisticsViewModel.handleIntent(StatisticsIntent.SetYearOffset(year - curYear))
    statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.YEAR))
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByAnnualCategory(year, categoryName))
    lastTimeTab = NavTab.TRANSACTIONS
    currentTab = NavTab.TRANSACTIONS
}

fun ExpenseAppState.navigateToTransactionsDate(monthOffset: Int, day: Int, dateLabel: String = "") {
    preserveStatisticsYearOnReturn = false
    statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
    statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByDate(monthOffset, day, dateLabel))
    lastTimeTab = NavTab.TRANSACTIONS
    currentTab = NavTab.TRANSACTIONS
}

fun ExpenseAppState.navigateToTransaction(monthOffset: Int, transaction: TransactionEntity) {
    preserveStatisticsYearOnReturn = false
    statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
    statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
    val cal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByTransaction(monthOffset, transaction.id, cal.get(Calendar.DAY_OF_MONTH), transaction.amount))
    lastTimeTab = NavTab.TRANSACTIONS
    currentTab = NavTab.TRANSACTIONS
}

fun ExpenseAppState.navigateToBudgetAdjustment(monthOffset: Int) {
    preserveStatisticsYearOnReturn = false
    statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
    statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.SetMonthOffset(monthOffset))
    transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget))
    lastTimeTab = NavTab.TRANSACTIONS
    currentTab = NavTab.TRANSACTIONS
}

fun ExpenseAppState.navigateToTransactionsMonth(monthOffset: Int) {
    preserveStatisticsYearOnReturn = true
    transactionsViewModel.handleIntent(TransactionsIntent.ResetAllFilters)
    transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.SelectMonth(monthOffset))
    lastTimeTab = NavTab.TRANSACTIONS
    currentTab = NavTab.TRANSACTIONS
}

fun ExpenseAppState.openQuickAdd(
    categoryId: String? = null,
    type: String = TransactionType.EXPENSE
) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(
        TransactionsIntent.OpenDialog(
            TransactionsDialog.AddTransaction(initialCategoryId = categoryId, initialType = type)
        )
    )
}

fun ExpenseAppState.openBudgetCenter() {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget))
}

fun ExpenseAppState.openRecurringTransactions(recurringTag: String) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.SearchQueryChange(recurringTag))
}

fun ExpenseAppState.checkForUpdates(version: String = "") {
    switchTab(NavTab.SETTINGS)
    settingsViewModel.handleIntent(SettingsIntent.CheckForUpdates(version))
}
