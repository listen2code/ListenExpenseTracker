package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.components.SearchBarInput

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight

/**
 * Pinned Top Search Bar and Account Filter Row for Transactions Screen.
 * Stays stationary while the balance card and transaction list glide underneath in HorizontalPager.
 */
@Composable
fun TransactionsHeaderFilters(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    showSortMenu: Boolean,
    onShowSortMenuChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var accountKeyToDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
    ) {
        // Search Input Bar
        SearchBarInput(
            query = state.searchQuery,
            onQueryChange = { onIntent(TransactionsIntent.SearchQueryChange(it)) },
            placeholder = AppStrings.search_placeholder.tr(lang),
            modifier = Modifier.fillMaxWidth()
        )

        // Horizontally Scrollable Account Filter Chips & Fixed Sort Order Trigger
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
                    val isCustom = acctKey != "ALL" && acctKey != "CASH" && acctKey != "BANK" && acctKey != "CREDIT"

                    AccountFilterChipItem(
                        selected = isSelected,
                        label = label,
                        isCustom = isCustom,
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onIntent(TransactionsIntent.FilterAccountChange(acctKey))
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            accountKeyToDelete = acctKey
                        }
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
                IconButton(onClick = { onShowSortMenuChange(true) }) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Sort",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { onShowSortMenuChange(false) }
                ) {
                    TransactionSortOrder.entries.forEach { order ->
                        DropdownMenuItem(
                            text = { Text(order.displayNameKey.tr(lang)) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                                onIntent(TransactionsIntent.ChangeSortOrder(order))
                                onShowSortMenuChange(false)
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Account Confirmation Dialog on Long Press
    accountKeyToDelete?.let { keyToDelete ->
        AccountDeleteConfirmDialog(
            accountName = AccountRepository.getAccountDisplayName(keyToDelete, lang),
            onDismiss = { accountKeyToDelete = null },
            onConfirm = {
                AccountRepository.deleteAccount(keyToDelete)
                if (state.selectedAccountFilter == keyToDelete) {
                    onIntent(TransactionsIntent.FilterAccountChange("ALL"))
                }
                accountKeyToDelete = null
            },
            lang = lang
        )
    }
}

/**
 * Account filter chip supporting normal click and long-press for custom accounts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AccountFilterChipItem(
    selected: Boolean,
    label: String,
    isCustom: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val labelColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    Surface(
        shape = RoundedCornerShape(AppDimens.CornerButton),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .clip(RoundedCornerShape(AppDimens.CornerButton))
            .combinedClickable(
                onClick = onClick,
                onLongClick = if (isCustom) onLongClick else null
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = AppDimens.TextMicro,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = labelColor
            )
        }
    }
}
