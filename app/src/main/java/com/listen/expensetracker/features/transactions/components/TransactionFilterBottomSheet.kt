package com.listen.expensetracker.features.transactions.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.components.CommonBottomSheet
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle

/**
 * Compound Filter Bottom Sheet for Transactions with compact custom amount input.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFilterBottomSheet(
    currentType: String,
    currentCategories: Set<String>,
    currentPreset: AmountFilterPreset,
    currentSortOrder: TransactionSortOrder,
    currentMin: Double?,
    currentMax: Double?,
    currencySymbol: String,
    lang: String,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: (type: String, categories: Set<String>, preset: AmountFilterPreset, min: Double?, max: Double?, sort: TransactionSortOrder) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedCategories by remember { mutableStateOf(currentCategories) }
    var selectedPreset by remember { mutableStateOf(currentPreset) }
    var selectedSortOrder by remember { mutableStateOf(currentSortOrder) }
    var minAmountText by remember { mutableStateOf(currentMin?.toString() ?: "") }
    var maxAmountText by remember { mutableStateOf(currentMax?.toString() ?: "") }

    val categories = when (selectedType) {
        TransactionType.EXPENSE -> CategoryRepository.expenseCategories
        TransactionType.INCOME -> CategoryRepository.incomeCategories
        else -> CategoryRepository.allCategories
    }

    CommonBottomSheet(onDismissRequest = onDismiss, title = null, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.SpaceSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // 1. Transaction Type Section
            FilterSectionHeader(AppStrings.FILTER_TYPE.tr(lang))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                listOf(
                    TransactionType.ALL to AppStrings.FILTER_TYPE_ALL.tr(lang),
                    TransactionType.EXPENSE to AppStrings.TYPE_EXPENSE.tr(lang),
                    TransactionType.INCOME to AppStrings.TYPE_INCOME.tr(lang)
                ).forEach { (typeKey, label) ->
                    FilterChip(
                        selected = selectedType == typeKey,
                        onClick = {
                            selectedType = typeKey
                            selectedCategories = emptySet()
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            // 2. Category Section with Icon and Text (Multi-Selectable)
            FilterSectionHeader(AppStrings.SETTINGS_CATEGORY_MANAGE.tr(lang))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedCategories.isEmpty(), onClick = { selectedCategories = emptySet() },
                    leadingIcon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(AppStrings.FILTER_ALL.tr(lang), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                )
                categories.forEach { cat ->
                    val catColor = try { Color(AndroidColor.parseColor(cat.colorHex)) } catch (_: Exception) { MaterialTheme.colorScheme.primary }
                    val isSelected = selectedCategories.contains(cat.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategories = if (isSelected) selectedCategories - cat.id else selectedCategories + cat.id },
                        leadingIcon = { Icon(imageVector = cat.icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor, modifier = Modifier.size(16.dp)) },
                        label = { Text(cat.getDisplayName(lang), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            // 3. Amount Range Section
            FilterSectionHeader(AppStrings.FILTER_AMOUNT.tr(lang))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall), verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                AmountFilterPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset, onClick = { selectedPreset = preset },
                        label = { Text(preset.labelKey.tr(lang), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            if (selectedPreset == AmountFilterPreset.CUSTOM) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall), verticalAlignment = Alignment.CenterVertically) {
                    CompactFilterAmountInput(
                        value = minAmountText, onValueChange = { minAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        placeholder = AppStrings.FILTER_MIN_AMOUNT.tr(lang), currencySymbol = currencySymbol, modifier = Modifier.weight(1f)
                    )
                    Text("—", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    CompactFilterAmountInput(
                        value = maxAmountText, onValueChange = { maxAmountText = it.filter { ch -> ch.isDigit() || ch == '.' } },
                        placeholder = AppStrings.FILTER_MAX_AMOUNT.tr(lang), currencySymbol = currencySymbol, modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Sort Order Section
            FilterSectionHeader(AppStrings.FILTER_SORT.tr(lang))
            FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall), verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                TransactionSortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = selectedSortOrder == order, onClick = { selectedSortOrder = order },
                        label = { Text(order.displayNameKey.tr(lang), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primaryContainer)
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))

            // 5. Action Buttons (Reset & Done)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
                CommonButton(
                    text = AppStrings.FILTER_RESET.tr(lang),
                    onClick = {
                        selectedType = "ALL"
                        selectedCategories = emptySet()
                        selectedPreset = AmountFilterPreset.ALL
                        selectedSortOrder = TransactionSortOrder.DATE_DESC
                        minAmountText = ""
                        maxAmountText = ""
                        onReset()
                        onDismiss()
                    },
                    style = CommonButtonStyle.Outlined, modifier = Modifier.weight(1f)
                )
                CommonButton(
                    text = AppStrings.FILTER_APPLY.tr(lang),
                    onClick = {
                        onApply(selectedType, selectedCategories, selectedPreset, minAmountText.toDoubleOrNull(), maxAmountText.toDoubleOrNull(), selectedSortOrder)
                        onDismiss()
                    },
                    style = CommonButtonStyle.Primary, modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CompactFilterAmountInput(value: String, onValueChange: (String) -> Unit, placeholder: String, currencySymbol: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)), modifier = modifier.height(38.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(currencySymbol, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            BasicTextField(
                value = value, onValueChange = onValueChange, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true, textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) Text(placeholder, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    inner()
                }
            )
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
}
