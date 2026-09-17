package com.listen.expensetracker.core.state

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import java.util.Calendar

/**
 * 导航与跨界面路由扩展方法 (ExpenseAppStateNavigation)。
 *
 * 职责：
 * 1. 【深度链接路由】：实现从统计图表到流水明细的“数据下钻”逻辑。
 * 2. 【数据路由解耦】：只聚焦操作流水 ViewModel 与 Tab 切换，消除对统计/设置 VM 的直接依赖。
 * 3. 【解耦拆分】：将复杂的导航逻辑从 ExpenseAppState 主类中抽离，保持代码整洁。
 */

/**
 * 导航至特定分类的月度账单列表。
 * 通常用于从饼图点击某个分类时触发。
 */
fun ExpenseAppState.navigateToTransactionsCategory(categoryName: String, monthOffset: Int) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByCategory(categoryName, monthOffset))
}

/**
 * 导航至特定分类的年度账单列表。
 * 用于从年度统计图表中下钻。
 */
fun ExpenseAppState.navigateToTransactionsAnnualCategory(year: Int, categoryName: String) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByAnnualCategory(year, categoryName))
}

/**
 * 导航至特定日期的账单列表。
 */
fun ExpenseAppState.navigateToTransactionsDate(monthOffset: Int, day: Int, dateLabel: String = "") {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByDate(monthOffset, day, dateLabel))
}

/**
 * 导航并定位到单笔具体的交易。
 */
fun ExpenseAppState.navigateToTransaction(monthOffset: Int, transaction: TransactionEntity) {
    val cal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.FilterByTransaction(monthOffset, transaction.id, cal.get(Calendar.DAY_OF_MONTH), transaction.amount))
}

/**
 * 导航至流水页并直接弹出预算调整对话框。
 */
fun ExpenseAppState.navigateToBudgetAdjustment(monthOffset: Int) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.SetMonthOffset(monthOffset))
    transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget))
}

/**
 * 导航至特定月份的整月账单列表。
 */
fun ExpenseAppState.navigateToTransactionsMonth(monthOffset: Int) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.ResetAllFilters)
    transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
    transactionsViewModel.handleIntent(TransactionsIntent.SelectMonth(monthOffset))
}

/**
 * 快捷开启记账对话框。
 */
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

/**
 * 开启预算中心。
 */
fun ExpenseAppState.openBudgetCenter() {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget))
}

/**
 * 开启周期性账单管理。
 */
fun ExpenseAppState.openRecurringTransactions(recurringTag: String) {
    switchTab(NavTab.TRANSACTIONS)
    transactionsViewModel.handleIntent(TransactionsIntent.SearchQueryChange(recurringTag))
}

/**
 * 切换至设置页并触发检查更新。
 */
fun ExpenseAppState.checkForUpdates(version: String = "") {
    targetUpdateVersion = version
    switchTab(NavTab.SETTINGS)
}
