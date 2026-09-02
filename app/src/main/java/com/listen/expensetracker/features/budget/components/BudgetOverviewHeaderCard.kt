package com.listen.expensetracker.features.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.listen.expensetracker.data.engine.CategoryBudgetCalculationResult
import com.listen.uicomponent.components.SurfaceCard
import kotlin.math.abs

/**
 * 分类预算中心顶部总览卡片 (BudgetOverviewHeaderCard)。
 */
@Composable
fun BudgetOverviewHeaderCard(
    result: CategoryBudgetCalculationResult,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean,
    modifier: Modifier = Modifier
) {
    val healthColor = when {
        result.totalSpent > result.totalBudget -> Color(0xFFEF4444)
        result.usageRatio >= 0.8f -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        contentPadding = 8.dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    val spentStr = if (hideAmount) "••••" else "$currencySymbol${"%.0f".format(result.totalSpent)}"
                    val budgetStr = if (hideAmount) "••••" else "$currencySymbol${"%.0f".format(result.totalBudget)}"
                    Text(
                        text = spentStr,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = " / $budgetStr",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 1.dp, start = 2.dp)
                    )
                }

                // 右侧健康胶囊
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(healthColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val remStr = if (hideAmount) "••••" else "$currencySymbol${"%.0f".format(abs(result.remainingBudget))}"
                    val remText = if (result.remainingBudget >= 0) "余 $remStr" else "超 $remStr"
                    Text(
                        text = "${(result.usageRatio * 100).toInt()}% · $remText",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = healthColor
                    )
                }
            }

            // 进度条
            val progress = result.usageRatio.coerceIn(0f, 1f)
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp)),
                color = healthColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )

            // 3. 底部状态汇总标签 (优雅内嵌在总览卡片中)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.overBudgetCount > 0) {
                    CompactStatusDot(text = "${result.overBudgetCount}超支", color = Color(0xFFEF4444))
                }
                if (result.warningCount > 0) {
                    CompactStatusDot(text = "${result.warningCount}预警", color = Color(0xFFF59E0B))
                }
                CompactStatusDot(text = "${result.normalCount}正常", color = Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun CompactStatusDot(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text = "● $text", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
