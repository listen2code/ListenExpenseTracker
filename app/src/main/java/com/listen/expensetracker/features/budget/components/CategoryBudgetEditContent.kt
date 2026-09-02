package com.listen.expensetracker.features.budget.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.CategoryBudgetConfig
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard
import kotlin.math.roundToInt

/**
 * 分类预算与比例编辑面板内容 (CategoryBudgetEditContent)。
 */
@Composable
fun CategoryBudgetEditContent(
    budgetInput: String,
    onBudgetInputChange: (String) -> Unit,
    ratios: Map<String, Float>,
    onRatiosChange: (Map<String, Float>) -> Unit,
    currencySymbol: String,
    lang: String,
    modifier: Modifier = Modifier
) {
    val categories = remember { CategoryRepository.expenseCategories }
    val presets = remember { listOf(3000.0, 5000.0, 8000.0, 10000.0, 15000.0, 20000.0) }
    val totalBudget = budgetInput.toDoubleOrNull() ?: 0.0
    val totalAllocatedPercent = ratios.values.sumOf { (it * 100).roundToInt() }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. 总预算输入框
        CommonEditText(
            value = budgetInput,
            onValueChange = { input -> onBudgetInputChange(input.filter { it.isDigit() }.take(8)) },
            label = AppStrings.MONTHLY_BUDGET.tr(lang),
            placeholder = "5000",
            leadingIcon = { CommonText(text = currencySymbol, fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        // 快捷金额预设 Chips
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = totalBudget == preset
                FilterChip(
                    selected = isSelected,
                    onClick = { onBudgetInputChange("%.0f".format(preset)) },
                    label = { Text("$currencySymbol${"%.0f".format(preset)}", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        // 2. 分配状态概览卡片 (整数均分分区法，确保恒等 100%)
        val allocColor = when {
            totalAllocatedPercent == 100 -> Color(0xFF10B981)
            totalAllocatedPercent > 100 -> Color(0xFFEF4444)
            else -> Color(0xFFF59E0B)
        }
        SurfaceCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 10.dp,
            contentPadding = 8.dp
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "已分配: $totalAllocatedPercent%",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = allocColor
                    )
                    val hintText = when {
                        totalAllocatedPercent == 100 -> "已完全分配"
                        totalAllocatedPercent > 100 -> "超出 ${totalAllocatedPercent - 100}%"
                        else -> "剩余 ${100 - totalAllocatedPercent}% 可分配"
                    }
                    Text(text = hintText, fontSize = 11.sp, color = allocColor, fontWeight = FontWeight.Medium)
                }

                LinearProgressIndicator(
                    progress = { (totalAllocatedPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = allocColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 均分剩余 与 恢复默认比例 操作按钮行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                val currentIntMap = categories.associate { cat ->
                                    cat.id to ((ratios[cat.id] ?: 0f) * 100).roundToInt()
                                }.toMutableMap()
                                val currentTotal = currentIntMap.values.sum()
                                val diff = 100 - currentTotal
                                if (diff > 0 && categories.isNotEmpty()) {
                                    val base = diff / categories.size
                                    val rem = diff % categories.size
                                    categories.forEachIndexed { i, cat ->
                                        val extra = if (i < rem) 1 else 0
                                        currentIntMap[cat.id] = (currentIntMap[cat.id] ?: 0) + base + extra
                                    }
                                    onRatiosChange(currentIntMap.mapValues { it.value / 100f })
                                }
                            },
                        cornerRadius = 6.dp,
                        contentPadding = 6.dp
                    ) {
                        Text(
                            text = AppStrings.BUDGET_AUTO_EQUALIZE.tr(lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SurfaceCard(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRatiosChange(CategoryBudgetConfig.defaultRatios) },
                        cornerRadius = 6.dp,
                        contentPadding = 6.dp
                    ) {
                        Text(
                            text = AppStrings.BUDGET_RESET_DEFAULT.tr(lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            softWrap = false,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. 分类预算比例调节列表 (自适应填充剩余高度)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items = categories, key = { it.id }) { cat ->
                CategoryRatioRow(
                    category = cat,
                    ratio = ratios[cat.id] ?: 0f,
                    totalBudget = totalBudget,
                    currencySymbol = currencySymbol,
                    lang = lang,
                    onRatioChange = { newRatio ->
                        onRatiosChange(ratios + (cat.id to newRatio.coerceIn(0f, 1f)))
                    }
                )
            }
        }
    }
}
