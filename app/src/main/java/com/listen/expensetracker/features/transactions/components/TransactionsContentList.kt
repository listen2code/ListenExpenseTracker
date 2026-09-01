package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonEmpty
import java.util.Calendar

/**
 * Calculates target LazyColumn index for scrolling to a specific transaction within grouped transactions.
 */
fun calculateTransactionScrollIndex(
    groupedTransactions: Map<String, List<TransactionEntity>>,
    txId: String
): Int {
    var targetIndex = 1
    for ((_, txList) in groupedTransactions) {
        val txIdx = txList.indexOfFirst { it.id == txId }
        if (txIdx != -1) return targetIndex + 1 + txIdx
        targetIndex += 1 + txList.size
    }
    return -1
}

/**
 * Calculates target LazyColumn index for scrolling to a specific day within grouped transactions.
 */
fun calculateDayScrollIndex(
    groupedTransactions: Map<String, List<TransactionEntity>>,
    day: Int
): Int {
    var targetIndex = 1
    for ((_, txList) in groupedTransactions) {
        val cal = Calendar.getInstance().apply { timeInMillis = txList.first().timestamp }
        val d = cal.get(Calendar.DAY_OF_MONTH)
        if (d <= day) return targetIndex
        targetIndex += 1 + txList.size
    }
    return -1
}

/**
 * LazyColumn Transactions Content List view for a specific month page (monthOffset).
 * Calculates and presents the Balance Overview Card and Grouped Transaction Items for that month.
 */
@Composable
fun TransactionsContentList(
    state: TransactionsUiState,
    monthOffset: Int,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberSaveable(monthOffset, saver = LazyListState.Saver) { LazyListState() }
) {
    val lang = state.language
    val sym = state.currencySymbol

    // Calculate real-time month-specific ledger statistics for this specific page
    val calc = remember(
        state.transactions,
        monthOffset,
        state.searchQuery,
        state.selectedAccountFilter,
        state.typeFilter,
        state.selectedCategories,
        state.amountPreset,
        state.customMinAmount,
        state.customMaxAmount,
        state.monthlyBudget,
        state.sortOrder,
        lang
    ) {
        TransactionCalculationEngine.filterAndCalculate(
            allList = state.transactions,
            currentOffset = monthOffset,
            query = state.searchQuery,
            accountFilter = state.selectedAccountFilter,
            budget = state.monthlyBudget,
            sortOrder = state.sortOrder,
            currencySymbol = sym,
            lang = lang,
            typeFilter = state.typeFilter,
            selectedCategories = state.selectedCategories,
            amountPreset = state.amountPreset,
            customMinAmount = state.customMinAmount,
            customMaxAmount = state.customMaxAmount
        )
    }

    val groupedTransactions = remember(calc.filteredTransactions) {
        calc.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.SpaceLarge),
        contentPadding = PaddingValues(bottom = AppDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
    ) {
        // Monthly Balance Overview Card
        item(key = "balance_overview_card") {
            BalanceOverviewCard(
                currencySymbol = sym,
                netBalance = calc.netBalance,
                totalExpense = calc.totalExpense,
                totalIncome = calc.totalIncome,
                budgetUsageRatio = calc.budgetUsageRatio,
                isOverBudget = calc.isOverBudget,
                hideBalance = state.hideBalance,
                lang = lang,
                monthlyBudget = calc.monthlyBudget,
                remainingBudget = calc.remainingBudget,
                onBudgetClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.SpaceExtraSmall)
            )
        }

        // Grouped Transactions List
        if (groupedTransactions.isEmpty()) {
            item(key = "empty_transactions_view") {
                if (state.hasActiveFilters) {
                    CommonEmpty(
                        message = "${AppStrings.EMPTY_SEARCH_TITLE.tr(lang)}\n${AppStrings.EMPTY_SEARCH_DESC.tr(lang)}",
                        action = {
                            CommonButton(
                                text = AppStrings.FILTER_CLEAR_ACTIVE.tr(lang),
                                style = CommonButtonStyle.Outlined,
                                onClick = { onIntent(TransactionsIntent.ResetAllFilters) }
                            )
                        }
                    )
                } else {
                    CommonEmpty(
                        message = AppStrings.EMPTY_TRANSACTIONS.tr(lang),
                        action = if (state.isDeveloperMode) {
                            {
                                CommonButton(
                                    text = AppStrings.SEED_MONTH_DEMO_DATA.tr(lang),
                                    style = CommonButtonStyle.Tonal,
                                    onClick = { onIntent(TransactionsIntent.SeedDemoData(monthOffset)) }
                                )
                            }
                        } else null
                    )
                }
            }
        } else {
            groupedTransactions.forEach { (dateHeader, txList) ->
                val dayExpense = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val dayIncome = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                item(key = "header_$dateHeader") {
                    DateGroupHeader(
                        dateHeader = dateHeader,
                        dayExpense = dayExpense,
                        dayIncome = dayIncome,
                        currencySymbol = sym,
                        lang = lang,
                        hideAmount = state.hideBalance
                    )
                }

                items(items = txList, key = { it.id }) { tx ->
                    TransactionItemRow(
                        transaction = tx,
                        currencySymbol = sym,
                        hideAmount = state.hideBalance,
                        onClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.EditTransaction(tx))) },
                        onLongClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.ConfirmDelete(tx))) },
                        lang = lang
                    )
                }
            }
        }
    }
}
