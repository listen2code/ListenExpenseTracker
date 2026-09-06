package com.listen.expensetracker.features.statistics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.AnnualMonthSummary
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard

/**
 * 年度 12 个月收支总览卡片 (AnnualOverviewCard)。
 * 呈现 12 个月的月度双柱收支对比与净结余健康度，支持轻触具体月份快速联动下钻。
 */
@Composable
fun AnnualOverviewCard(
    summaries: List<AnnualMonthSummary>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    lang: String = "zh",
    hideAmount: Boolean = false,
    currentSelectedMonth: Int? = null,
    onMonthClick: ((monthIndex: Int) -> Unit)? = null
) {
    if (summaries.isEmpty()) return

    val maxVal = remember(summaries) {
        summaries.maxOfOrNull { maxOf(it.totalExpense, it.totalIncome) }?.coerceAtLeast(10.0) ?: 1000.0
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(summaries) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
        )
    }

    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. 标题行与图例说明
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                    Text(
                        text = AppStrings.ANNUAL_OVERVIEW_TITLE.tr(lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TextTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // 图例: 支出(红) vs 收入(绿)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444))
                        )
                        Text(
                            text = AppStrings.TYPE_EXPENSE.tr(lang),
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = AppStrings.TYPE_INCOME.tr(lang),
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpaceLarge))

            // 2. 12 个月收支双柱图轨道
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                summaries.forEach { item ->
                    val isCurrent = currentSelectedMonth == item.monthIndex
                    AnnualBarColumn(
                        item = item,
                        maxVal = maxVal,
                        animRatio = animProgress.value,
                        isCurrent = isCurrent,
                        onClick = onMonthClick?.let { { it(item.monthIndex) } },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 单月双轨柱状图列
 */
@Composable
private fun AnnualBarColumn(
    item: AnnualMonthSummary,
    maxVal: Double,
    animRatio: Float,
    isCurrent: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val trackHeight = 72.dp
    val expRatio = if (item.totalExpense > 0) ((item.totalExpense / maxVal) * animRatio).coerceIn(0.08, 1.0) else 0.0
    val incRatio = if (item.totalIncome > 0) ((item.totalIncome / maxVal) * animRatio).coerceIn(0.08, 1.0) else 0.0

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.CornerButton))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 柱状容器
        Row(
            modifier = Modifier
                .height(trackHeight)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            // 支出柱 (红)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((trackHeight.value * expRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (item.totalExpense > 0) Color(0xFFEF4444) else Color.Transparent)
            )

            Spacer(modifier = Modifier.width(2.dp))

            // 收入柱 (绿)
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((trackHeight.value * incRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (item.totalIncome > 0) Color(0xFF10B981) else Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))

        // 月份标签
        Text(
            text = item.monthLabel,
            fontSize = AppDimens.TextMicro,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
