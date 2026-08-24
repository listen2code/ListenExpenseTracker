package com.listen.expensetracker.features.transactions.ui

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.transactions.components.BalanceOverviewCard
import com.listen.expensetracker.features.transactions.components.DateGroupHeader
import com.listen.expensetracker.features.transactions.components.TransactionItemRow
import com.listen.expensetracker.features.transactions.components.TransactionsDialogHost
import com.listen.expensetracker.features.transactions.components.formatDayGroupHeader
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.uicomponent.components.BaseScreenScaffold
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SearchBarInput

/**
 * Pure Stateless Transactions Screen orchestrating Month Navigation, Account Filters,
 * Balance Overview, and Grouped Transaction Items.
 */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol
    var showSortMenu by remember { mutableStateOf(false) }

    val groupedTransactions = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    BaseScreenScaffold(
        titleSlot = {
            MonthNavigationCapsule(
                monthTitle = state.monthTitle,
                onPreviousMonth = { onIntent(TransactionsIntent.ChangeMonthOffset(-1)) },
                onNextMonth = { onIntent(TransactionsIntent.ChangeMonthOffset(1)) },
                onTitleClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthPicker)) }
            )
        },
        actions = {
            IconButton(onClick = { onIntent(TransactionsIntent.ToggleHideBalance(!state.hideBalance)) }) {
                Icon(
                    imageVector = if (state.hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Balance",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.AddTransaction)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.btn_add_transaction.tr(lang))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // Search Input Bar
            item(key = "search_bar") {
                SearchBarInput(
                    query = state.searchQuery,
                    onQueryChange = { onIntent(TransactionsIntent.SearchQueryChange(it)) },
                    placeholder = AppStrings.search_placeholder.tr(lang),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Horizontally Scrollable Account Filter Chips & Fixed Sort Order Trigger
            item(key = "filters_and_sort") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterKeys = AccountRepository.getFilterKeys()
                        filterKeys.forEach { acctKey ->
                            val isSelected = state.selectedAccountFilter == acctKey
                            val label = AccountRepository.getAccountDisplayName(acctKey, lang)
                            FilterChip(
                                selected = isSelected,
                                onClick = { onIntent(TransactionsIntent.FilterAccountChange(acctKey)) },
                                label = { Text(label, fontSize = AppDimens.TextMicro) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }

                        // Manage Custom Accounts Button
                        IconButton(
                            onClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.ManageAccount)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = AppStrings.manage_accounts_title.tr(lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Sort Order Menu Trigger (Fixed on the right)
                    Box(modifier = Modifier.padding(start = AppDimens.SpaceSmall)) {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TransactionSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.displayNameKey.tr(lang)) },
                                    onClick = {
                                        onIntent(TransactionsIntent.ChangeSortOrder(order))
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Balance Overview Card
            item(key = "balance_overview_card") {
                BalanceOverviewCard(
                    currencySymbol = sym,
                    netBalance = state.netBalance,
                    totalExpense = state.totalExpense,
                    totalIncome = state.totalIncome,
                    budgetUsageRatio = state.budgetUsageRatio,
                    isOverBudget = state.isOverBudget,
                    hideBalance = state.hideBalance,
                    lang = lang,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Grouped Transactions List
            if (groupedTransactions.isEmpty()) {
                item(key = "empty_transactions_view") {
                    EmptyStateView(
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

    // Feature-Level Dialog Host
    TransactionsDialogHost(state = state, onIntent = onIntent)
}
