package com.listen.expensetracker.features.recurring.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonText

/**
 * 每月扣款日选择器（支持 1~31 日任意步进与高频日期预设快捷选择）。
 */
@Composable
fun MonthlyDaySelector(
    dayOfPeriod: Int,
    lang: String,
    onDayChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CommonText(text = AppStrings.RECURRING_DUE_DAY_LABEL.tr(lang), fontSize = AppDimens.TextSmall)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onDayChange(if (dayOfPeriod > 1) dayOfPeriod - 1 else 28) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Prev", modifier = Modifier.size(16.dp))
                }
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    val dayLabel = if (dayOfPeriod == 28) {
                        AppStrings.RECURRING_MONTH_END_DETAIL.tr(lang)
                    } else {
                        AppStrings.RECURRING_DAY_SUFFIX.tr(lang).format(dayOfPeriod)
                    }
                    CommonText(
                        text = dayLabel,
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = { onDayChange(if (dayOfPeriod < 28) dayOfPeriod + 1 else 1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Next", modifier = Modifier.size(16.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val quickDays = listOf(1, 5, 10, 15, 20, 25, 28)
            quickDays.forEach { day ->
                val isSel = dayOfPeriod == day
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = if (isSel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.CornerButton))
                        .clickable { onDayChange(day) }
                ) {
                    val quickLabel = if (day == 28) {
                        AppStrings.RECURRING_MONTH_END.tr(lang)
                    } else {
                        AppStrings.RECURRING_DAY_SUFFIX.tr(lang).format(day)
                    }
                    CommonText(
                        text = quickLabel,
                        fontSize = AppDimens.TextCaption,
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
