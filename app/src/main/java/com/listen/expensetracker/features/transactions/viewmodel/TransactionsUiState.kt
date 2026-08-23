package com.listen.expensetracker.features.transactions.viewmodel

import com.listen.expensetracker.data.db.TransactionEntity
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
 * Dialog presentation state for Transactions feature.
 */
sealed interface TransactionsDialog {
    data object AddTransaction : TransactionsDialog
    data class EditTransaction(val transaction: TransactionEntity) : TransactionsDialog
    data object MonthPicker : TransactionsDialog
    data object AddAccount : TransactionsDialog
}

/**
 * Immutable UI State representing ledger transactions, month grouping, search & filters.
 */
data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val filteredTransactions: List<TransactionEntity> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val netBalance: Double = 0.0,
    val monthlyBudget: Double = 5000.0,
    val remainingBudget: Double = 5000.0,
    val budgetUsageRatio: Float = 0.0f,
    val isOverBudget: Boolean = false,
    val hideBalance: Boolean = false,
    val searchQuery: String = "",
    val selectedAccountFilter: String = "ALL",
    val selectedMonthOffset: Int = 0,
    val monthTitle: String = "本月",
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val currencySymbol: String = "￥",
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD,
    val activeDialog: TransactionsDialog? = null,
    val isLoading: Boolean = false
)

/**
 * User Intents for Transactions Feature.
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
    data class OpenDialog(val dialog: TransactionsDialog) : TransactionsIntent
    data object DismissDialog : TransactionsIntent
}
