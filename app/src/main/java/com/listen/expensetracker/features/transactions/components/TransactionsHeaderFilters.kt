package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var accountKeyToDelete by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
    ) {
        // Search Input Bar & Filter Trigger Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
        ) {
            SearchBarInput(
                query = state.searchQuery,
                onQueryChange = { onIntent(TransactionsIntent.SearchQueryChange(it)) },
                placeholder = AppStrings.search_placeholder.tr(lang),
                modifier = Modifier.weight(1f)
            )

            // Compound Filter Sheet Trigger with badge count
            val hasDialogFilters = state.activeFilterCount > 0
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (hasDialogFilters) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.FilterSheet)) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = AppStrings.filter_title.tr(lang),
                        tint = if (hasDialogFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    if (state.activeFilterCount > 0) {
                        Text(
                            text = "${state.activeFilterCount}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Active Filter Tags Row (Scrollable chips with 'X' button)
        ActiveFilterTagsRow(
            state = state,
            lang = lang,
            onIntent = onIntent
        )

        // Horizontally Scrollable Account Filter Chips & Manage Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
