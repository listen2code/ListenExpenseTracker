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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.CategoryBudgetCalculationResult
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.components.CommonBadge
import com.listen.uicomponent.components.CommonBadgeSize
import com.listen.uicomponent.components.CommonBadgeStyle
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ListenTheme
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
                    val spentStr = if (hideAmount) "••••" else "$currencySymbol${result.totalSpent.formatAmount()}"
                    val budgetStr = if (hideAmount) "••••" else "$currencySymbol${result.totalBudget.formatAmount()}"
                    CommonText(
                        text = spentStr,
                        fontSize = 17.sp,
                        minFontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        autoResize = true
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
                    val remStr = if (hideAmount) "••••" else "$currencySymbol${abs(result.remainingBudget).formatAmount()}"
                    val prefix = if (result.remainingBudget >= 0) AppStrings.BUDGET_REMAINING_PREFIX.tr(lang) else AppStrings.BUDGET_OVER_PREFIX.tr(lang)
                    val remText = "$prefix $remStr"
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

            // 3. 底部状态汇总标签（统一接入 ListenUiComponent 的 CommonBadge，使用 Error/Warning/Success 语义风格）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (result.overBudgetCount > 0) {
                    CommonBadge(
                        text = AppStrings.BUDGET_COUNT_OVER.tr(lang).format(result.overBudgetCount),
                        style = CommonBadgeStyle.Error,
                        showDot = true,
                        size = CommonBadgeSize.Small
                    )
                }
                if (result.warningCount > 0) {
                    CommonBadge(
                        text = AppStrings.BUDGET_COUNT_WARNING.tr(lang).format(result.warningCount),
                        style = CommonBadgeStyle.Warning,
                        showDot = true,
                        size = CommonBadgeSize.Small
                    )
                }
                CommonBadge(
                    text = AppStrings.BUDGET_COUNT_NORMAL.tr(lang).format(result.normalCount),
                    style = CommonBadgeStyle.Success,
                    showDot = true,
                    size = CommonBadgeSize.Small
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BudgetOverviewHeaderCardPreview() {
    ExpenseStrings.init()
    val sampleResult = CategoryBudgetCalculationResult(
        totalBudget = 5000.0,
        totalSpent = 3800.0,
        remainingBudget = 1200.0,
        usageRatio = 0.76f,
        statusList = emptyList(),
        overBudgetCount = 1,
        warningCount = 2,
        normalCount = 8
    )
    ListenTheme {
        BudgetOverviewHeaderCard(
            result = sampleResult,
            currencySymbol = "$",
            lang = "en",
            hideAmount = false
        )
    }
}
