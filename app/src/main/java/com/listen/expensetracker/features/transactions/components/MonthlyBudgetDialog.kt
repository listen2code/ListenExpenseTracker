package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonText

/**
 * Monthly Budget Dialog matching the standardized visual style of
 * AccountManageDialog and CategoryManageDialog (CommonDialog, buttons, typography, sizes).
 */
@Composable
fun MonthlyBudgetDialog(
    currentBudget: Double,
    currencySymbol: String,
    lang: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
    modifier: Modifier = Modifier,
    spentAmount: Double? = null
) {
    var budgetInput by remember {
        mutableStateOf(
            if (currentBudget > 0) {
                if (currentBudget % 1.0 == 0.0) "%.0f".format(currentBudget) else "%.2f".format(currentBudget)
            } else "5000"
        )
    }
    val presets = remember { listOf(3000.0, 5000.0, 8000.0, 10000.0, 15000.0, 20000.0) }
    val isValid = budgetInput.isNotBlank() && (budgetInput.toDoubleOrNull() ?: 0.0) > 0

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.BUDGET_DIALOG_TITLE.tr(lang),
        modifier = modifier,
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Savings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        confirmButton = {
            CommonButton(
                text = AppStrings.COMMON_SAVE.tr(lang),
                onClick = {
                    val newBudget = budgetInput.toDoubleOrNull()?.coerceAtLeast(1.0) ?: currentBudget
                    onConfirm(newBudget)
                },
                enabled = isValid,
                style = CommonButtonStyle.Primary
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.COMMON_CANCEL.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Text
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // 1. Budget Amount Input Field (默认支持2位小数)
            CommonEditText(
                value = budgetInput,
                onValueChange = { budgetInput = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                placeholder = "5000",
                leadingIcon = {
                    CommonText(
                        text = currencySymbol,
                        fontSize = AppDimens.TextBody,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // 2. Quick Presets Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presets.forEach { preset ->
                    val presetStr = "%.0f".format(preset)
                    val isSelected = budgetInput == presetStr
                    FilterChip(
                        selected = isSelected,
                        onClick = { budgetInput = presetStr },
                        label = {
                            Text(
                                text = "$currencySymbol$presetStr",
                                fontSize = AppDimens.TextCaption,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // 3. Current Spending Context (if available)
            if (spentAmount != null && spentAmount > 0) {
                val inputVal = budgetInput.toDoubleOrNull() ?: 0.0
                val isOver = inputVal > 0 && spentAmount > inputVal
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceExtraSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    CommonText(
                        text = "${AppStrings.TOTAL_EXPENSE.tr(lang)}: $currencySymbol${"%.2f".format(spentAmount)}",
                        fontSize = AppDimens.TextCaption,
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
