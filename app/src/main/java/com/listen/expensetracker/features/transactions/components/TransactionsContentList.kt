package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.components.CommonEmpty

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
    scrollToTopTrigger: Long = 0L
) {
    val lang = state.language
    val sym = state.currencySymbol

    // Calculate real-time month-specific ledger statistics for this specific page
    val calc = remember(
        state.transactions,
        monthOffset,
        state.searchQuery,
        state.selectedAccountFilter,
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
            lang = lang
        )
    }

    val groupedTransactions = remember(calc.filteredTransactions) {
        calc.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    val listState = rememberSaveable(monthOffset, saver = LazyListState.Saver) {
        LazyListState()
    }

    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0L) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.SpaceLarge),
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.SpaceExtraSmall)
            )
        }

        // Grouped Transactions List
        if (groupedTransactions.isEmpty()) {
            item(key = "empty_transactions_view") {
                CommonEmpty(
                    message = AppStrings.empty_transactions.tr(lang),
                    modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                )
            }
        } else {
            groupedTransactions.forEach { (dateHeader, txList) ->
                val dayExpense = txList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val dayIncome = txList.filter { it.type == "INCOME" }.sumOf { it.amount }

                item(key = "header_$dateHeader") {
                    DateGroupHeader(
                        dateHeader = dateHeader,
                        dayExpense = dayExpense,
                        dayIncome = dayIncome,
                        currencySymbol = sym,
                        lang = lang
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
