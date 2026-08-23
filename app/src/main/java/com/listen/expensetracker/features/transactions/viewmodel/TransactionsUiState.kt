package com.listen.expensetracker.features.transactions.viewmodel

import com.listen.arch.sync.SyncState
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.uicomponent.charts.BarChartItem
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

/**
 * Sorting order enumeration for transactions list.
 */
enum class TransactionSortOrder(val displayNameKey: String) {
    DATE_DESC("sort_date_desc"),
    DATE_ASC("sort_date_asc"),
    AMOUNT_DESC("sort_amount_desc"),
    AMOUNT_ASC("sort_amount_asc")
}

/**
 * Immutable UI State representing the complete presentation state of the application.
 * Follows Unidirectional Data Flow (UDF) / MVI architecture.
 */
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
    val remainingBudget: Double = 5000.0,
    val budgetUsageRatio: Float = 0.0f,
    val isOverBudget: Boolean = false,
    val dailyAverageExpense: Double = 0.0,
    val dailyAverageIncome: Double = 0.0,
    val maxExpenseTransaction: TransactionEntity? = null,
    val maxIncomeTransaction: TransactionEntity? = null,
    val hideBalance: Boolean = false,
    val isLoading: Boolean = false,
    val syncState: SyncState = SyncState(),
    val googleAccountEmail: String? = null,
    val googleDisplayName: String? = null,
    val googleAvatarUrl: String? = null,
    val searchQuery: String = "",
    val selectedAccountFilter: String = "ALL",
    val selectedMonthOffset: Int = 0,
    val monthTitle: String = "本月",
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val statisticsTab: String = "EXPENSE",
    val currencySymbol: String = "￥",
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD
)

/**
 * User Intents triggering state mutations in MVI architecture.
 */
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
    data class RestoreDeletedTransaction(val transaction: TransactionEntity) : TransactionsIntent
    data class ToggleHideBalance(val hide: Boolean) : TransactionsIntent
    data class SearchQueryChange(val query: String) : TransactionsIntent
    data class FilterAccountChange(val accountType: String) : TransactionsIntent
    data class ChangeMonthOffset(val offsetDelta: Int) : TransactionsIntent
    data class ChangeSortOrder(val order: TransactionSortOrder) : TransactionsIntent
    data class ChangeStatisticsTab(val tab: String) : TransactionsIntent
    data class ChangeCurrencySymbol(val symbol: String) : TransactionsIntent
    data class UpdateMonthlyBudget(val budget: Double) : TransactionsIntent
    data class ImportBackupData(val json: String) : TransactionsIntent
    data class LinkGoogleAccount(
        val email: String,
        val displayName: String? = null,
        val avatarUrl: String? = null
    ) : TransactionsIntent
    data object UnlinkGoogleAccount : TransactionsIntent
    data object TriggerCloudBackup : TransactionsIntent
    data object TriggerCloudRestore : TransactionsIntent
    data object SeedDemoData : TransactionsIntent
    data object ClearAllData : TransactionsIntent
    data class ChangeLanguage(val langCode: String) : TransactionsIntent
    data class ChangeThemeMode(val mode: ThemeMode) : TransactionsIntent
    data class ChangeAccentColor(val accent: AccentColor) : TransactionsIntent
}

/**
 * Single-event side effects emitted by the ViewModel for UI presentation.
 */
sealed interface TransactionsEffect {
    data class ShowToast(val message: String) : TransactionsEffect
    data class ShowUndoSnackbar(val message: String, val transaction: TransactionEntity) : TransactionsEffect
    data object TransactionAddedSuccess : TransactionsEffect
}
