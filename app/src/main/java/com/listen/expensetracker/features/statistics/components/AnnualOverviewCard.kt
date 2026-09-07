package com.listen.expensetracker.features.statistics.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
 * 呈现 12 个月的月度双柱收支对比与净结余健康度，支持轻触具体月份高亮并展示详情。
 */
@Composable
fun AnnualOverviewCard(
    summaries: List<AnnualMonthSummary>,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    lang: String = "zh",
    hideAmount: Boolean = false,
    currentSelectedMonth: Int? = null,
    onMonthClick: ((monthIndex: Int) -> Unit)? = null,
    onNavigateToTransactionsMonth: ((monthIndex: Int) -> Unit)? = null
) {
    if (summaries.isEmpty()) return

    val maxVal = remember(summaries) {
        summaries.maxOfOrNull { maxOf(it.totalExpense, it.totalIncome) }?.coerceAtLeast(10.0) ?: 1000.0
    }

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(summaries) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
    }

    var internalSelectedMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    val activeSelectedMonth = currentSelectedMonth ?: internalSelectedMonth
    val selectedSummary = summaries.firstOrNull { it.monthIndex == activeSelectedMonth }

    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
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

                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFFEF4444), label = AppStrings.TYPE_EXPENSE.tr(lang))
                    LegendItem(color = Color(0xFF10B981), label = AppStrings.TYPE_INCOME.tr(lang))
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpaceLarge))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                summaries.forEach { item ->
                    val isSelected = activeSelectedMonth == item.monthIndex
                    AnnualBarColumn(
                        item = item,
                        maxVal = maxVal,
                        animRatio = animProgress.value,
                        isSelected = isSelected,
                        onClick = {
                            internalSelectedMonth = if (activeSelectedMonth == item.monthIndex) null else item.monthIndex
                            onMonthClick?.invoke(item.monthIndex)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            AnimatedVisibility(
                visible = selectedSummary != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                if (selectedSummary != null) {
                    Column {
                        Spacer(modifier = Modifier.height(AppDimens.SpaceMedium))
                        AnnualOverviewMonthDetail(
                            summary = selectedSummary,
                            currencySymbol = currencySymbol,
                            hideAmount = hideAmount,
                            lang = lang,
                            onViewTransactions = onNavigateToTransactionsMonth?.let { cb ->
                                { cb(selectedSummary.monthIndex) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text = label, fontSize = AppDimens.TextMicro, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AnnualBarColumn(
    item: AnnualMonthSummary,
    maxVal: Double,
    animRatio: Float,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val trackHeight = 72.dp
    val expRatio = if (item.totalExpense > 0) ((item.totalExpense / maxVal) * animRatio).coerceIn(0.08, 1.0) else 0.0
    val incRatio = if (item.totalIncome > 0) ((item.totalIncome / maxVal) * animRatio).coerceIn(0.08, 1.0) else 0.0
    val colBg = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.CornerButton))
            .background(colBg)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 1.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.height(trackHeight).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((trackHeight.value * expRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (item.totalExpense > 0) Color(0xFFEF4444) else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((trackHeight.value * incRatio).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (item.totalIncome > 0) Color(0xFF10B981) else Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))

        Text(
            text = item.monthLabel,
            fontSize = AppDimens.TextMicro,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
