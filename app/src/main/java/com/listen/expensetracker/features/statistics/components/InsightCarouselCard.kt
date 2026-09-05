package com.listen.expensetracker.features.statistics.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.FinancialInsightItem
import com.listen.expensetracker.data.engine.InsightSeverity
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard

/**
 * 智能财务洞察卡片轮播 (InsightCarouselCard)。
 * 顶部以轻卡片轮播呈现财务诊断建议，支持手势滑动、分页点指示与分类下钻交互。
 */
@Composable
fun InsightCarouselCard(
    insights: List<FinancialInsightItem>,
    modifier: Modifier = Modifier,
    lang: String = "zh",
    onInsightClick: ((FinancialInsightItem) -> Unit)? = null
) {
    if (insights.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { insights.size }
    )

    SurfaceCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. 标题行与分页指示器
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
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                    Text(
                        text = AppStrings.INSIGHT_SECTION_TITLE.tr(lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TextTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (insights.size > 1) {
                    // 分页胶囊
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.CornerPill))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(horizontal = AppDimens.SpaceSmall, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1}/${insights.size}",
                            fontSize = AppDimens.TextMicro,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))

            // 2. 轮播内容区域
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                val item = insights[page]
                SingleInsightCard(
                    item = item,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.CornerButton))
                        .clickable(enabled = onInsightClick != null) {
                            onInsightClick?.invoke(item)
                        }
                )
            }

            // 3. 底部微型圆点指示条（仅多页时展示）
            if (insights.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppDimens.SpaceSmall),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(insights.size) { idx ->
                        val isSelected = pagerState.currentPage == idx
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            label = "dotColor"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .size(if (isSelected) 5.dp else 4.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 单条洞察内容卡片，依据严重程度渲染主题色背景与图标。
 */
@Composable
private fun SingleInsightCard(
    item: FinancialInsightItem,
    modifier: Modifier = Modifier
) {
    val (bgColor, iconColor, icon) = when (item.severity) {
        InsightSeverity.POSITIVE -> Triple(
            Color(0xFF10B981).copy(alpha = 0.12f),
            Color(0xFF059669),
            Icons.AutoMirrored.Filled.TrendingDown
        )
        InsightSeverity.WARNING -> Triple(
            Color(0xFFF59E0B).copy(alpha = 0.14f),
            Color(0xFFD97706),
            Icons.AutoMirrored.Filled.TrendingUp
        )
        InsightSeverity.DANGER -> Triple(
            Color(0xFFEF4444).copy(alpha = 0.14f),
            Color(0xFFDC2626),
            Icons.Default.Warning
        )
        InsightSeverity.INFO -> Triple(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.primary,
            Icons.Default.Info
        )
    }

    Row(
        modifier = modifier
            .background(bgColor)
            .padding(AppDimens.SpaceStandard),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(AppDimens.IconSizeMedium)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TextSubtitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = item.description,
                fontSize = AppDimens.TextBody,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = AppDimens.TextHeader
            )
        }
    }
}
