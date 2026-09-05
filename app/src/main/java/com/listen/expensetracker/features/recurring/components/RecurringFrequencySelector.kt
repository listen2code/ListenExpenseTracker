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
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonSegmentedControl
import com.listen.uicomponent.components.CommonText

/**
 * 周期执行频次与具体扣款日选择组件 (RecurringFrequencySelector)。
 * 支持 每日 / 每周 / 每月 / 每年 四档切换，并提供对应的水平滚动快捷药丸标签。
 */
@Composable
fun RecurringFrequencySelector(
    frequency: RecurringFrequency,
    dayOfPeriod: Int,
    lang: String,
    onFrequencyChange: (RecurringFrequency) -> Unit,
    onDayChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val freqList = listOf(
        RecurringFrequency.DAILY,
        RecurringFrequency.WEEKLY,
        RecurringFrequency.MONTHLY,
        RecurringFrequency.YEARLY
    )
    val freqLabels = listOf(
        AppStrings.RECURRING_FREQ_DAILY.tr(lang),
        AppStrings.RECURRING_FREQ_WEEKLY.tr(lang),
        AppStrings.RECURRING_FREQ_MONTHLY.tr(lang),
        AppStrings.RECURRING_FREQ_YEARLY.tr(lang)
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
    ) {
        // 1. 频次切换控件：每天、每周、每月、每年
        CommonSegmentedControl(
            items = freqLabels,
            selectedIndex = freqList.indexOf(frequency).coerceAtLeast(0),
            onIndexChange = { index ->
                val newFreq = freqList.getOrElse(index) { RecurringFrequency.MONTHLY }
                onFrequencyChange(newFreq)
                if (newFreq == RecurringFrequency.WEEKLY && dayOfPeriod !in 1..7) {
                    onDayChange(1)
                } else if (newFreq == RecurringFrequency.MONTHLY && dayOfPeriod !in 1..28) {
                    onDayChange(1)
                }
            }
        )

        // 2. 根据频次展示详细时间配置
        when (frequency) {
            RecurringFrequency.DAILY -> {
                // 每天固定执行，无需额外配置特定日期
            }
            RecurringFrequency.WEEKLY -> {
                WeeklyDayPicker(
                    selectedDay = dayOfPeriod,
                    lang = lang,
                    onDayChange = onDayChange
                )
            }
            RecurringFrequency.MONTHLY -> {
                MonthlyDaySelector(
                    dayOfPeriod = dayOfPeriod,
                    lang = lang,
                    onDayChange = onDayChange
                )
            }
            RecurringFrequency.YEARLY -> {
                // 每年按设定日期执行
            }
        }
    }
}

@Composable
private fun WeeklyDayPicker(
    selectedDay: Int,
    lang: String,
    onDayChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val weekdays = listOf(
        1 to AppStrings.WEEKDAY_MON.tr(lang),
        2 to AppStrings.WEEKDAY_TUE.tr(lang),
        3 to AppStrings.WEEKDAY_WED.tr(lang),
        4 to AppStrings.WEEKDAY_THU.tr(lang),
        5 to AppStrings.WEEKDAY_FRI.tr(lang),
        6 to AppStrings.WEEKDAY_SAT.tr(lang),
        7 to AppStrings.WEEKDAY_SUN.tr(lang)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        weekdays.forEach { (day, name) ->
            val isSel = selectedDay == day
            Surface(
                shape = RoundedCornerShape(AppDimens.CornerButton),
                color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = if (isSel) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
                    .clickable { onDayChange(day) }
            ) {
                CommonText(
                    text = name,
                    fontSize = AppDimens.TextCaption,
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
