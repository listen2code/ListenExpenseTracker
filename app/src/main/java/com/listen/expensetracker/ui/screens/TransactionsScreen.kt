package com.listen.expensetracker.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.ui.components.MonthPickerDialog
import com.listen.expensetracker.ui.state.TransactionSortOrder
import com.listen.expensetracker.ui.state.TransactionsIntent
import com.listen.expensetracker.ui.state.TransactionsUiState
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SearchBarInput
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor
import java.util.Calendar

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
    var showAccountManageDialog by remember { mutableStateOf(false) }
    var showMonthPickerDialog by remember { mutableStateOf(false) }
    var accountListVersion by remember { mutableStateOf(0) }

    val sym = state.currencySymbol
    val lang = state.language

    val accounts = remember(accountListVersion) { AccountRepository.getAllAccounts() }

    val groupedTransactions = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    // Sleek Compact Month Navigation Pill (Centered, tight width)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(-1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = state.monthTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showMonthPickerDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Search Input Bar
            item(key = "search_bar") {
                SearchBarInput(
                    query = state.searchQuery,
                    onQueryChange = { onIntent(TransactionsIntent.SearchQueryChange(it)) },
                    placeholder = StringsRes.get("search_placeholder", lang),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. Account Filter Chips with '+' Manage Button & Sort Dropdown
            item(key = "filters_row") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedAccountFilter == "ALL",
                                onClick = { onIntent(TransactionsIntent.FilterAccountChange("ALL")) },
                                label = { Text(StringsRes.get("filter_all", lang), fontSize = 11.sp, fontWeight = if (state.selectedAccountFilter == "ALL") FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        items(accounts, key = { it.key }) { acc ->
                            FilterChip(
                                selected = state.selectedAccountFilter == acc.key,
                                onClick = { onIntent(TransactionsIntent.FilterAccountChange(acc.key)) },
                                label = { Text(acc.nameZh, fontSize = 11.sp, fontWeight = if (state.selectedAccountFilter == acc.key) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // '+' Manage Account Types Button
                        item {
                            IconButton(
                                onClick = { showAccountManageDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Manage Accounts",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.primary)
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
            }

            // 3. Small-Screen Responsive Ultra-Compact Balance Card
            item(key = "balance_card") {
                SurfaceCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = StringsRes.get("balance_title", lang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.netBalance)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(StringsRes.get("total_expense", lang), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.totalExpense)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ExpenseRed
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(StringsRes.get("total_income", lang), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (state.hideBalance) "****" else "$sym${String.format("%.2f", state.totalIncome)}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeGreen
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LinearProgressIndicator(
                                progress = { state.budgetUsageRatio.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (state.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                text = "${String.format("%.0f", state.budgetUsageRatio * 100)}%",
                                fontSize = 9.sp,
                                color = if (state.isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Date-Grouped Transaction Stream Items
            if (state.filteredTransactions.isEmpty()) {
                item(key = "empty_state") {
                    EmptyStateView(message = StringsRes.get("empty_transactions", lang))
                }
            } else {
                groupedTransactions.forEach { (dateHeader, dayList) ->
                    val dayExpense = dayList.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val dayIncome = dayList.filter { it.type == "INCOME" }.sumOf { it.amount }

                    // Date Group Header
                    item(key = "header_$dateHeader") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 1.dp, start = 2.dp, end = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dateHeader,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (dayExpense > 0) {
                                    Text(
                                        text = "${StringsRes.get("type_expense", lang)} $sym${String.format("%.2f", dayExpense)}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (dayIncome > 0) {
                                    Text(
                                        text = "${StringsRes.get("type_income", lang)} $sym${String.format("%.2f", dayIncome)}",
                                        fontSize = 10.sp,
                                        color = IncomeGreen
                                    )
                                }
                            }
                        }
                    }

                    // Ultra-Compact Transaction Items with perfectly matching 10.dp corner radius & 0 corner bleed
                    items(items = dayList, key = { it.id }) { transaction ->
                        UltraCompactTransactionItemRow(
                            transaction = transaction,
                            currencySymbol = sym,
                            hideAmount = state.hideBalance,
                            onClick = { editingTransaction = transaction },
                            onDelete = { onIntent(TransactionsIntent.DeleteTransaction(transaction.id)) }
                        )
                    }
                }
            }

            // Bottom Spacing for FloatingActionButton
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
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

    // Account Management Dialog (Unconstrained height, zero hint clipping)
    if (showAccountManageDialog) {
        AccountManageDialog(
            accounts = accounts,
            onAddAccount = { name ->
                AccountRepository.addAccount(name)
                accountListVersion++
            },
            onDeleteAccount = { key ->
                AccountRepository.deleteAccount(key)
                accountListVersion++
            },
            onDismiss = { showAccountManageDialog = false },
            lang = lang
        )
    }

    // Dedicated Year-Month Picker Dialog
    if (showMonthPickerDialog) {
        val nowCal = Calendar.getInstance()
        val currentSelectedCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, state.selectedMonthOffset)
        }
        MonthPickerDialog(
            initialYear = currentSelectedCal.get(Calendar.YEAR),
            initialMonth = currentSelectedCal.get(Calendar.MONTH),
            onMonthSelected = { selectedYear, selectedMonth ->
                val targetCal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
                val diffMonths = (targetCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)) * 12 +
                        (targetCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH))
                val delta = diffMonths - state.selectedMonthOffset
                onIntent(TransactionsIntent.ChangeMonthOffset(delta))
            },
            onDismiss = { showMonthPickerDialog = false },
            lang = lang
        )
    }
}

@Composable
private fun AccountManageDialog(
    accounts: List<com.listen.expensetracker.data.model.AccountTypeItem>,
    onAddAccount: (String) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onDismiss: () -> Unit,
    lang: String
) {
    var newAccountName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(StringsRes.get("manage_accounts_title", lang), fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        placeholder = { Text(StringsRes.get("account_name_input", lang), fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (newAccountName.isNotBlank()) {
                                onAddAccount(newAccountName.trim())
                                newAccountName = ""
                            }
                        }
                    ) {
                        Text(StringsRes.get("btn_save", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Text("已有账户：", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    accounts.forEach { acc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(acc.nameZh, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (!acc.isSystem) {
                                IconButton(
                                    onClick = { onDeleteAccount(acc.key) },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Text("系统默认", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("btn_done", lang))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UltraCompactTransactionItemRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    hideAmount: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val exactCornerRadius = 10.dp
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.70f },
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
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isSwiping = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart || dismissState.progress > 0.08f
            if (isSwiping) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(exactCornerRadius))
                        .background(ExpenseRed)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }
        }
    ) {
        SurfaceCard(
            cornerRadius = exactCornerRadius,
            contentPadding = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val cat = CategoryRepository.getCategoryById(transaction.categoryId)
                val color = parseHexColor(transaction.categoryColorHex)
                val accountDisplay = AccountRepository.getAccountName(transaction.accountType)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    // 24dp Ultra-Compact Category Icon
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = cat.icon,
                            contentDescription = transaction.categoryName,
                            tint = color,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = transaction.categoryName,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "· $accountDisplay",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (transaction.note.isNotBlank()) {
                            Text(
                                text = transaction.note,
                                fontSize = 9.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                val amountPrefix = if (transaction.type == "EXPENSE") "-" else "+"
                val amountColor = if (transaction.type == "EXPENSE") ExpenseRed else IncomeGreen

                Text(
                    text = if (hideAmount) "****" else "$amountPrefix$currencySymbol${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = amountColor
                )
            }
        }
    }
}

private fun formatDayGroupHeader(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
    val weekdays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    val weekday = weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val year = cal.get(Calendar.YEAR)
    val month = String.format("%02d", cal.get(Calendar.MONTH) + 1)
    return "$day $weekday $year.$month"
}
