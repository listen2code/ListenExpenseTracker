package com.listen.listenexpensetracker.ui.state

import com.listen.arch.data.db.TransactionEntity
import com.listen.arch.sync.SyncState
import com.listen.uicomponent.charts.BarChartItem
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

enum class TransactionSortOrder(val displayName: String) {
    DATE_DESC("时间最新"),
    DATE_ASC("时间最早"),
    AMOUNT_DESC("金额降序"),
    AMOUNT_ASC("金额升序")
}

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val categoryShares: List<PieChartItem> = emptyList(),
    val progressSegments: List<ProgressSegment> = emptyList(),
    val incomeCategoryShares: List<PieChartItem> = emptyList(),
    val incomeProgressSegments: List<ProgressSegment> = emptyList(),
    val dailyTrendBars: List<BarChartItem> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val monthlyBudget: Double = 5000.0,
    val budgetUsageRatio: Float = 0.0f,
    val isOverBudget: Boolean = false,
    val dailyAverageExpense: Double = 0.0,
    val dailyAverageIncome: Double = 0.0,
    val maxExpenseTransaction: TransactionEntity? = null,
    val maxIncomeTransaction: TransactionEntity? = null,
    val hideBalance: Boolean = false,
    val isLoading: Boolean = false,
    val syncState: SyncState = SyncState(),
    val searchQuery: String = "",
    val selectedAccountFilter: String = "ALL", // "ALL", "WECHAT", "ALIPAY", "BANK", "CASH"
    val selectedMonthOffset: Int = 0, // 0 = current month, -1 = last month, etc.
    val monthTitle: String = "本月",
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val statisticsTab: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val currencySymbol: String = "￥",
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD
)

sealed interface TransactionsIntent {
    data object LoadData : TransactionsIntent
    data class AddTransaction(
        val type: String,
        val categoryId: String,
        val categoryName: String,
        val categoryIcon: String,
        val categoryColorHex: String,
        val amount: Double,
        val note: String,
        val accountType: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : TransactionsIntent
    data class UpdateTransaction(val transaction: TransactionEntity) : TransactionsIntent
    data class DeleteTransaction(val id: String) : TransactionsIntent
    data class ToggleHideBalance(val hide: Boolean) : TransactionsIntent
    data class SearchQueryChange(val query: String) : TransactionsIntent
    data class FilterAccountChange(val accountType: String) : TransactionsIntent
    data class ChangeMonthOffset(val offsetDelta: Int) : TransactionsIntent
    data class ChangeSortOrder(val order: TransactionSortOrder) : TransactionsIntent
    data class ChangeStatisticsTab(val tab: String) : TransactionsIntent
    data class ChangeCurrencySymbol(val symbol: String) : TransactionsIntent
    data class UpdateMonthlyBudget(val budget: Double) : TransactionsIntent
    data class ImportBackupData(val json: String) : TransactionsIntent
    data object TriggerCloudBackup : TransactionsIntent
    data object TriggerCloudRestore : TransactionsIntent
    data object SeedDemoData : TransactionsIntent
    data object ClearAllData : TransactionsIntent
    data class ChangeLanguage(val langCode: String) : TransactionsIntent
    data class ChangeThemeMode(val mode: ThemeMode) : TransactionsIntent
    data class ChangeAccentColor(val accent: AccentColor) : TransactionsIntent
}

sealed interface TransactionsEffect {
    data class ShowToast(val message: String) : TransactionsEffect
    data object TransactionAddedSuccess : TransactionsEffect
}
