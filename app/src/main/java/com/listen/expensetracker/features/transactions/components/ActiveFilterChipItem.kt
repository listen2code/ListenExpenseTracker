package com.listen.expensetracker.features.transactions.components

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
import com.listen.expensetracker.data.engine.AmountFilterPreset
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
    val containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurface
    val closeIconTint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier.padding(start = 8.dp, end = 4.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = closeIconTint,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
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
            state.sortOrder != TransactionSortOrder.DATE_DESC

    if (!hasDialogFilters) return

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Transaction Type Tag
        if (state.typeFilter != "ALL") {
            ActiveFilterChipItem(
                label = if (state.typeFilter == "EXPENSE") AppStrings.type_expense.tr(lang) else AppStrings.type_income.tr(lang),
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
                cat?.colorHex?.let { Color(android.graphics.Color.parseColor(it)) }
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
                val minStr = state.customMinAmount?.let { "${state.currencySymbol}${"%.0f".format(it)}" } ?: "0"
                val maxStr = state.customMaxAmount?.let { "${state.currencySymbol}${"%.0f".format(it)}" } ?: "∞"
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
            text = AppStrings.filter_clear_active.tr(lang),
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
