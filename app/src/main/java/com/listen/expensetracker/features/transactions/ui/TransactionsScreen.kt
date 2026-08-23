package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.data.db.TransactionEntity
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.settings.components.MonthPickerDialog
import com.listen.expensetracker.features.transactions.components.BalanceOverviewCard
import com.listen.expensetracker.features.transactions.components.DateGroupHeader
import com.listen.expensetracker.features.transactions.components.TransactionItemRow
import com.listen.expensetracker.features.transactions.components.formatDayGroupHeader
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SearchBarInput

/**
 * Main Transactions Screen orchestrating Month Navigation, Account Filters,
 * Balance Overview, and Grouped Transaction Items.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val groupedTransactions = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.CornerPill))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = AppDimens.SpaceSmall, vertical = AppDimens.SpaceExtraSmall)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceExtraSmall)
                        ) {
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(-1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimens.IconSizeMedium))
                            }
                            Text(
                                text = state.monthTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimens.TextTitle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showMonthPickerDialog = true }
                                    .padding(horizontal = AppDimens.SpaceSmall, vertical = AppDimens.SpaceExtraSmall)
                            )
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimens.IconSizeMedium))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(TransactionsIntent.ToggleHideBalance(!state.hideBalance)) }) {
                        Icon(
                            imageVector = if (state.hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Balance",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = StringsRes.get("btn_add_transaction", lang))
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
                    placeholder = StringsRes.get("search_placeholder", lang),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Account Filter Chips & Sort Order Dropdown
            item(key = "account_filters") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        listOf(
                            "ALL" to StringsRes.get("filter_all", lang),
                            "WECHAT" to StringsRes.get("filter_wechat", lang),
                            "ALIPAY" to StringsRes.get("filter_alipay", lang),
                            "BANK" to StringsRes.get("filter_bank", lang),
                            "CASH" to StringsRes.get("filter_cash", lang)
                        ).forEach { (type, label) ->
                            val isSelected = state.selectedAccountFilter == type
                            FilterChip(
                                selected = isSelected,
                                onClick = { onIntent(TransactionsIntent.FilterAccountChange(type)) },
                                label = { Text(label, fontSize = AppDimens.TextSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }

                    // Sort Order Trigger
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            TransactionSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(StringsRes.get(order.displayNameKey, lang), fontSize = AppDimens.TextBody) },
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

            // Stabilized Balance Overview Card
            item(key = "balance_card") {
                BalanceOverviewCard(
                    currencySymbol = sym,
                    netBalance = state.netBalance,
                    totalExpense = state.totalExpense,
                    totalIncome = state.totalIncome,
                    budgetUsageRatio = state.budgetUsageRatio,
                    isOverBudget = state.isOverBudget,
                    hideBalance = state.hideBalance,
                    lang = lang
                )
            }

            // Empty State or Grouped Transaction Items
            if (state.filteredTransactions.isEmpty()) {
                item(key = "empty_state") {
                    EmptyStateView(
                        message = StringsRes.get("empty_transactions", lang),
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
                            onClick = { editingTransaction = tx },
                            onDelete = { onIntent(TransactionsIntent.DeleteTransaction(tx.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showMonthPickerDialog) {
        MonthPickerDialog(
            currentOffset = state.selectedMonthOffset,
            onOffsetSelected = { onIntent(TransactionsIntent.ChangeMonthOffset(it - state.selectedMonthOffset)) },
            onDismiss = { showMonthPickerDialog = false },
            lang = lang
        )
    }

    if (showAddSheet) {
        AddTransactionSheet(
            currencySymbol = sym,
            onDismiss = { showAddSheet = false },
            onSave = { type, catId, catName, catIcon, catColor, amt, note, acct, ts ->
                onIntent(TransactionsIntent.AddTransaction(type, catId, catName, catIcon, catColor, amt, note, acct, ts))
                showAddSheet = false
            },
            lang = lang
        )
    }

    editingTransaction?.let { tx ->
        EditTransactionSheet(
            transaction = tx,
            currencySymbol = sym,
            onDismiss = { editingTransaction = null },
            onSave = { updated ->
                onIntent(TransactionsIntent.UpdateTransaction(updated))
                editingTransaction = null
            },
            onDelete = {
                onIntent(TransactionsIntent.DeleteTransaction(tx.id))
                editingTransaction = null
            },
            lang = lang
        )
    }
}
