package com.listen.expensetracker.features.transactions.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.theme.IncomeGreen
import java.util.Calendar

/**
 * Formats unix timestamp into standard Day Group Header format (e.g. "23 星期日 2026.08").
 *
 * @param timestamp Epoch timestamp in milliseconds
 * @return Localized formatted date header string
 */
fun formatDayGroupHeader(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val day = "%02d".format(cal.get(Calendar.DAY_OF_MONTH))
    val weekdays = arrayOf("星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    val weekday = weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
    val year = cal.get(Calendar.YEAR)
    val month = "%02d".format(cal.get(Calendar.MONTH) + 1)
    return "$day $weekday $year.$month"
}

/**
 * Sticky Date Group Header Component displaying the day date and daily net totals.
 *
 * @param dateHeader Formatted date string
 * @param dayExpense Total expense for this specific day
 * @param dayIncome Total income for this specific day
 * @param currencySymbol Active currency symbol
 * @param lang ISO language code
 * @param modifier Composable modifier
 */
@Composable
fun DateGroupHeader(
    dateHeader: String,
    dayExpense: Double,
    dayIncome: Double,
    currencySymbol: String,
    lang: String,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppDimens.SpaceMedium, bottom = 1.dp, start = AppDimens.SpaceExtraSmall, end = AppDimens.SpaceExtraSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateHeader,
            fontSize = AppDimens.TextSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            if (dayExpense > 0) {
                Text(
                    text = if (hideAmount) "${AppStrings.TYPE_EXPENSE.tr(lang)} ••••" else "${AppStrings.TYPE_EXPENSE.tr(lang)} $currencySymbol${"%.2f".format(dayExpense)}",
                    fontSize = AppDimens.TextCaption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (dayIncome > 0) {
                Text(
                    text = if (hideAmount) "${AppStrings.TYPE_INCOME.tr(lang)} ••••" else "${AppStrings.TYPE_INCOME.tr(lang)} $currencySymbol${"%.2f".format(dayIncome)}",
                    fontSize = AppDimens.TextCaption,
                    color = IncomeGreen
                )
            }
        }
    }
}
