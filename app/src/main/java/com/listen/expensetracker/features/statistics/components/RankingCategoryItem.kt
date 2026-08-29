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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.Category
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.theme.parseHexColor

/**
 * Modern Category Ranking Item displaying podium badges (#1 🥇, #2 🥈, #3 🥉),
 * category icon with soft tinted aura, percentage chip, amount, and full-width animated progress bar.
 */
@Composable
fun RankingCategoryItem(
    rank: Int,
    share: PieChartItem,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false,
    lang: String = "zh",
    onClick: (() -> Unit)? = null
) {
    val color = parseHexColor(share.colorHex)
    val category: Category? = remember(share.label, lang) {
        CategoryRepository.allCategories.find {
            it.getDisplayName(lang) == share.label ||
                it.id == share.label ||
                it.nameKey == share.label ||
                it.colorHex.equals(share.colorHex, ignoreCase = true)
        }
    }

    // Unique data fingerprint for this specific category and its percentage
    val dataSignature = remember(share.label, share.percentage) {
        "${share.label}_${share.percentage}"
    }
    // Preserves the last animated signature across LazyColumn scroll recycling
    var animatedSignature by rememberSaveable { mutableStateOf("") }
    // Initialize directly to target percentage if already animated to avoid scroll re-trigger
    val animProgress = remember {
        Animatable(if (animatedSignature == dataSignature) share.percentage else 0f)
    }

    // Only trigger bar progress animation when category share percentage actually changes
    LaunchedEffect(dataSignature) {
        if (animatedSignature != dataSignature) {
            animatedSignature = dataSignature
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = share.percentage,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    val (badgeBg, badgeTextColor) = when (rank) {
        1 -> Color(0xFFF59E0B) to Color.White
        2 -> Color(0xFF94A3B8) to Color.White
        3 -> Color(0xFFD97706) to Color.White
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val itemModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerButton))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    } else {
        modifier.fillMaxWidth()
    }

    Column(
        modifier = itemModifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Group: Rank Badge, Category Icon Aura, Name, Percentage Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = rank.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }

                // Category Icon Bubble
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (category != null) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = share.label,
                            tint = color,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Category Name & Percentage Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = share.label,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = AppDimens.TextSubtitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.12f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${"%.1f".format(share.percentage * 100)}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }

            // Right Group: Formatted Amount & Optional Navigation Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = if (hideAmount) "••••" else "$currencySymbol${"%.2f".format(share.value)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextSubtitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View in Transactions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Full-width Smooth Progress Bar
        LinearProgressIndicator(
            progress = { animProgress.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    }
}
