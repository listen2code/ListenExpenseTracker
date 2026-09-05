package com.listen.expensetracker.features.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.BudgetHealthStatus
import com.listen.expensetracker.data.model.CategoryBudgetStatus
import com.listen.uicomponent.components.SurfaceCard
import kotlin.math.abs

/**
 * 紧凑现代风格的分类预算执行卡片 (CategoryBudgetItemCard)。
 */
@Composable
fun CategoryBudgetItemCard(
    status: CategoryBudgetStatus,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean = false,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status.status) {
        BudgetHealthStatus.NORMAL -> Color(0xFF10B981)
        BudgetHealthStatus.WARNING -> Color(0xFFF59E0B)
        BudgetHealthStatus.OVERBUDGET -> Color(0xFFEF4444)
    }

    val catColor = try {
        Color(android.graphics.Color.parseColor(status.category.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        contentPadding = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. 分类图标
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(catColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = status.category.icon,
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 2. 信息与进度条
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = status.category.getDisplayName(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "${(status.ratio * 100).toInt()}%",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // 右侧状态文本
                    val badgeText = when (status.status) {
                        BudgetHealthStatus.NORMAL -> {
                            val remStr = if (hideAmount) "••••" else "$currencySymbol${status.remainingAmount.formatAmount()}"
                            "${AppStrings.BUDGET_REMAINING_PREFIX.tr(lang)} $remStr"
                        }
                        BudgetHealthStatus.WARNING -> {
                            val remStr = if (hideAmount) "••••" else "$currencySymbol${status.remainingAmount.formatAmount()}"
                            "${AppStrings.BUDGET_ONLY_REMAINING_PREFIX.tr(lang)} $remStr"
                        }
                        BudgetHealthStatus.OVERBUDGET -> {
                            val overStr = if (hideAmount) "••••" else "$currencySymbol${abs(status.spentAmount - status.budgetAmount).formatAmount()}"
                            "${AppStrings.BUDGET_OVER_PREFIX.tr(lang)} $overStr"
                        }
                    }
                    Text(
                        text = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                // 进度条
                val progress = status.usageRatio.coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                )

                // 底部已用与预算、百分比
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val spentStr = if (hideAmount) "••••" else "$currencySymbol${status.spentAmount.formatAmount()}"
                    val budgetStr = if (hideAmount) "••••" else "$currencySymbol${status.budgetAmount.formatAmount()}"
                    Text(
                        text = "$spentStr / $budgetStr",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(status.usageRatio * 100).toInt()}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor
                    )
                }
            }
        }
    }
}
