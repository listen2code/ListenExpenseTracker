package com.listen.listenexpensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.data.db.TransactionEntity
import com.listen.arch.i18n.StringsRes
import com.listen.listenexpensetracker.data.model.CategoryRepository
import com.listen.listenexpensetracker.ui.state.TransactionSortOrder
import com.listen.listenexpensetracker.ui.state.TransactionsIntent
import com.listen.listenexpensetracker.ui.state.TransactionsUiState
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SearchBarInput
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sym = state.currencySymbol
    val lang = state.language

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(-1)) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month")
                        }
                        Text(
                            text = state.monthTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        IconButton(onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(1)) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { onIntent(TransactionsIntent.ToggleHideBalance(!state.hideBalance)) }) {
                        Icon(
                            imageVector = if (state.hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Balance"
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
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input Bar
            SearchBarInput(
                query = state.searchQuery,
                onQueryChange = { onIntent(TransactionsIntent.SearchQueryChange(it)) },
                placeholder = StringsRes.get("search_placeholder", lang),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Account Filter Chips & Sort Button Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    listOf(
                        "ALL" to StringsRes.get("filter_all", lang),
                        "WECHAT" to StringsRes.get("filter_wechat", lang),
                        "ALIPAY" to StringsRes.get("filter_alipay", lang),
                        "BANK" to StringsRes.get("filter_bank", lang),
                        "CASH" to StringsRes.get("filter_cash", lang)
                    ).forEach { (accKey, label) ->
                        FilterChip(
                            selected = state.selectedAccountFilter == accKey,
                            onClick = { onIntent(TransactionsIntent.FilterAccountChange(accKey)) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Box {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        listOf(
                            TransactionSortOrder.DATE_DESC to StringsRes.get("sort_date_desc", lang),
                            TransactionSortOrder.DATE_ASC to StringsRes.get("sort_date_asc", lang),
                            TransactionSortOrder.AMOUNT_DESC to StringsRes.get("sort_amount_desc", lang),
                            TransactionSortOrder.AMOUNT_ASC to StringsRes.get("sort_amount_asc", lang)
                        ).forEach { (order, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (state.sortOrder == order) FontWeight.Bold else FontWeight.Normal,
                                        color = if (state.sortOrder == order) MaterialTheme.colorScheme.primary else Color.Unspecified
                                    )
                                },
                                onClick = {
                                    onIntent(TransactionsIntent.ChangeSortOrder(order))
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // Overview Balance Card
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("balance_title", lang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.netBalance)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(StringsRes.get("total_expense", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.totalExpense)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ExpenseRed
                            )
                        }

                        Column {
                            Text(StringsRes.get("total_income", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.totalIncome)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = IncomeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Monthly Budget Progress Bar
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${StringsRes.get("monthly_budget", lang)} $sym${String.format("%.0f", state.monthlyBudget)} (${StringsRes.get("used_budget", lang)} ${String.format("%.1f", state.budgetUsageRatio * 100)}%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.isOverBudget) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(Icons.Default.Warning, contentDescription = "Over Budget", tint = ExpenseRed, modifier = Modifier.size(12.dp))
                                    Text(StringsRes.get("over_budget", lang), fontSize = 11.sp, color = ExpenseRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { state.budgetUsageRatio.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (state.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Donut Chart
            if (state.categoryShares.isNotEmpty()) {
                DonutChart(
                    items = state.categoryShares,
                    totalValue = state.totalExpense,
                    centerTitle = StringsRes.get("total_expense", lang),
                    centerValueText = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.totalExpense)}"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Transaction Stream List
            if (state.filteredTransactions.isEmpty()) {
                EmptyStateView(message = StringsRes.get("empty_transactions", lang))
            } else {
                Text(
                    text = "${StringsRes.get("nav_transactions", lang)} (${state.filteredTransactions.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(
                        items = state.filteredTransactions,
                        key = { it.id }
                    ) { transaction ->
                        TransactionItemRow(
                            transaction = transaction,
                            currencySymbol = sym,
                            hideAmount = state.hideBalance,
                            onClick = { editingTransaction = transaction },
                            onDelete = { onIntent(TransactionsIntent.DeleteTransaction(transaction.id)) }
                        )
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        AddTransactionSheet(
            onDismiss = { showAddSheet = false },
            onSaveTransaction = { type, catId, catName, catIcon, catColor, amount, note, accType, timestamp ->
                onIntent(
                    TransactionsIntent.AddTransaction(
                        type = type,
                        categoryId = catId,
                        categoryName = catName,
                        categoryIcon = catIcon,
                        categoryColorHex = catColor,
                        amount = amount,
                        note = note,
                        accountType = accType,
                        timestamp = timestamp
                    )
                )
            }
        )
    }

    editingTransaction?.let { tx ->
        EditTransactionSheet(
            transaction = tx,
            onDismiss = { editingTransaction = null },
            onSaveEdit = { updatedTx ->
                onIntent(TransactionsIntent.UpdateTransaction(updatedTx))
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionItemRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    hideAmount: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ExpenseRed, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White
                )
            }
        }
    ) {
        SurfaceCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val cat = CategoryRepository.getCategoryById(transaction.categoryId)
                val color = parseHexColor(transaction.categoryColorHex)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = transaction.categoryName,
                            tint = color
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = transaction.categoryName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = when (transaction.accountType) {
                                    "WECHAT" -> "微信"
                                    "ALIPAY" -> "支付宝"
                                    "BANK" -> "银行卡"
                                    "CASH" -> "现金"
                                    else -> ""
                                },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Text(
                            text = if (transaction.note.isNotBlank()) transaction.note else formatDate(transaction.timestamp),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                val amountPrefix = if (transaction.type == "EXPENSE") "-" else "+"
                val amountColor = if (transaction.type == "EXPENSE") ExpenseRed else IncomeGreen

                Text(
                    text = if (hideAmount) "****" else "$amountPrefix$currencySymbol${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = amountColor
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
