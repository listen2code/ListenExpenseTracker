package com.listen.expensetracker.features.transactions.viewmodel

import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode

sealed interface TransactionsEffect : CommonUiEffect {
    data class ScrollToMonth(val offset: Int) : TransactionsEffect
    data object ScrollToTop : TransactionsEffect
    data class ScrollToTransaction(val txId: String) : TransactionsEffect
    data class ScrollToDay(val day: Int) : TransactionsEffect
}


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
    data class ConfirmDelete(val transaction: TransactionEntity) : TransactionsDialog
    data object MonthPicker : TransactionsDialog
    data object ManageAccount : TransactionsDialog
    data object FilterSheet : TransactionsDialog
    data object MonthlyBudget : TransactionsDialog
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
    val typeFilter: String = "ALL",
    val selectedCategories: Set<String> = emptySet(),
    val amountPreset: AmountFilterPreset = AmountFilterPreset.ALL,
    val customMinAmount: Double? = null,
    val customMaxAmount: Double? = null,
    val selectedMonthOffset: Int = 0,
    val monthTitle: String = "本月",
    val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
    val currencySymbol: String = "￥",
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD,
    val activeDialog: TransactionsDialog? = null,
    val isLoading: Boolean = false,
    val isDeveloperMode: Boolean = false
) {
    val categoryFilter: String
        get() = if (selectedCategories.isEmpty()) "ALL" else selectedCategories.first()

    val activeFilterCount: Int
        get() {
            var count = 0
            if (typeFilter != "ALL") count++
            if (selectedCategories.isNotEmpty()) count++
            if (amountPreset != AmountFilterPreset.ALL) count++
            if (sortOrder != TransactionSortOrder.DATE_DESC) count++
            return count
        }

    val hasActiveFilters: Boolean
        get() = activeFilterCount > 0 || searchQuery.isNotBlank() || selectedAccountFilter != "ALL"
}

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
    data class SetMonthOffset(val offset: Int) : TransactionsIntent
    data class ChangeSortOrder(val order: TransactionSortOrder) : TransactionsIntent
    data class OpenDialog(val dialog: TransactionsDialog) : TransactionsIntent
    data object DismissDialog : TransactionsIntent
    data class SeedDemoData(val monthOffset: Int) : TransactionsIntent
    data class FilterByCategory(val categoryName: String, val monthOffset: Int) : TransactionsIntent
    data class FilterByDate(val monthOffset: Int, val day: Int, val dateLabel: String? = null) : TransactionsIntent
    data class FilterByTransaction(val monthOffset: Int, val transactionId: String, val day: Int, val amount: Double? = null) : TransactionsIntent
    data object ScrollToTop : TransactionsIntent
    data class SelectMonth(val offset: Int) : TransactionsIntent
    data class ChangeTypeFilter(val type: String) : TransactionsIntent
    data class ApplyCompoundFilter(
        val type: String,
        val categories: Set<String> = emptySet(),
        val preset: AmountFilterPreset,
        val min: Double? = null,
        val max: Double? = null,
        val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC
    ) : TransactionsIntent
    data object ResetAllFilters : TransactionsIntent
    data object ClearTypeFilter : TransactionsIntent
    data object ClearCategoryFilter : TransactionsIntent
    data class RemoveCategoryFilter(val categoryId: String) : TransactionsIntent
    data object ClearAmountFilter : TransactionsIntent
    data object ClearSortOrder : TransactionsIntent
    data class UpdateMonthlyBudget(val budget: Double) : TransactionsIntent
    data object ScreenAppear : TransactionsIntent
    data object ScreenDisappear : TransactionsIntent
}

