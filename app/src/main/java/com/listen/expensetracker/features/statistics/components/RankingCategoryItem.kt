package com.listen.expensetracker.features.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.parseHexColor

/**
 * Category Ranking Item Card displaying percentage bar, category color dot, and total amount.
 */
@Composable
fun RankingCategoryItem(
    share: PieChartItem,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false
) {
    val color = parseHexColor(share.colorHex)

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceStandard,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Category Color Dot and Name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(AppDimens.IconSizeMedium)
                        .clip(CircleShape)
                        .background(color)
                )
                Text(
                    text = share.label,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = AppDimens.TextSubtitle,
                    maxLines = 1
                )
            }

            // Progress Bar & Percentage & Amount
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (hideAmount) "••••" else "$currencySymbol${"%.2f".format(share.value)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TextSubtitle,
                        maxLines = 1
                    )
                    Text(
                        text = "${"%.1f".format(share.percentage * 100)}%",
                        fontSize = AppDimens.TextMicro,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                LinearProgressIndicator(
                    progress = { share.percentage },
                    modifier = Modifier
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
