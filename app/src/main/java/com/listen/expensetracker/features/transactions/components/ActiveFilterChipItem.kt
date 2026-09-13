package com.listen.expensetracker.features.transactions.components

import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.components.CommonFilterChip
import com.listen.uicomponent.theme.ListenTheme

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState

/**
 * High-contrast removable active filter tag chip with an icon, label, and 'X' button.
 */
@Composable
fun ActiveFilterChipItem(
    label: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color? = null
) {
    // [ListenUiComponent] 统一采用 CommonFilterChip 可移除胶囊标签组件 (Rule 25)
    CommonFilterChip(
        label = label,
        removable = true,
        onRemove = onRemove,
        icon = icon,
        iconTint = iconTint,
        modifier = modifier
    )
}

/**
 * Horizontally scrollable row displaying active filter tags with quick-delete 'X' buttons.
 */
@Composable
fun ActiveFilterTagsRow(
    state: TransactionsUiState,
    lang: String,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDialogFilters = state.typeFilter != "ALL" ||
            state.selectedCategories.isNotEmpty() ||
            state.amountPreset != AmountFilterPreset.ALL ||
            state.sortOrder != TransactionSortOrder.DATE_DESC ||
            state.activeAnnualFilter != null

    if (!hasDialogFilters) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 0. Annual Category Filter Tag (Scheme B)
        state.activeAnnualFilter?.let { annual ->
            val tagText = AppStrings.FILTER_ANNUAL_TAG.tr(lang).format(annual.year, annual.categoryName)
            ActiveFilterChipItem(
                label = tagText,
                onRemove = { onIntent(TransactionsIntent.ClearAnnualFilter) }
            )
        }

        // 1. Transaction Type Tag
        if (state.typeFilter != TransactionType.ALL && state.activeAnnualFilter == null) {
            ActiveFilterChipItem(
                label = if (state.typeFilter == TransactionType.EXPENSE) AppStrings.TYPE_EXPENSE.tr(lang) else AppStrings.TYPE_INCOME.tr(lang),
                onRemove = { onIntent(TransactionsIntent.ClearTypeFilter) }
            )
        }

        // 2. Category Tags (Multi-Selectable)
        state.selectedCategories.forEach { catId ->
            val cat = remember(catId) {
                CategoryRepository.allCategories.find {
                    it.id == catId || it.nameKey == catId || it.customName == catId
                }
            }
            val catColor = try {
                cat?.colorHex?.let { Color(AndroidColor.parseColor(it)) }
            } catch (_: Exception) { null }

            ActiveFilterChipItem(
                label = cat?.getDisplayName(lang) ?: catId,
                icon = cat?.icon,
                iconTint = catColor,
                onRemove = { onIntent(TransactionsIntent.RemoveCategoryFilter(catId)) }
            )
        }

        // 3. Amount Range Tag
        if (state.amountPreset != AmountFilterPreset.ALL) {
            val amountLabel = if (state.amountPreset == AmountFilterPreset.CUSTOM) {
                val minStr = state.customMinAmount?.let { "${state.currencySymbol}${it.formatAmount()}" } ?: "0"
                val maxStr = state.customMaxAmount?.let { "${state.currencySymbol}${it.formatAmount()}" } ?: "∞"
                "$minStr ~ $maxStr"
            } else {
                state.amountPreset.labelKey.tr(lang)
            }
            ActiveFilterChipItem(
                label = amountLabel,
                onRemove = { onIntent(TransactionsIntent.ClearAmountFilter) }
            )
        }

        // 4. Sort Order Tag (if non-default)
        if (state.sortOrder != TransactionSortOrder.DATE_DESC) {
            ActiveFilterChipItem(
                label = state.sortOrder.displayNameKey.tr(lang),
                onRemove = { onIntent(TransactionsIntent.ClearSortOrder) }
            )
        }

        // Clear All Link
        Text(
            text = AppStrings.FILTER_CLEAR_ACTIVE.tr(lang),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onIntent(TransactionsIntent.ResetAllFilters) }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}
@Preview(showBackground = true)
@Composable
fun ActiveFilterChipItemPreview() {
    ExpenseStrings.init()
    ListenTheme {
        ActiveFilterChipItem(
            label = "Food",
            onRemove = {}
        )
    }
}
