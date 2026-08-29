package com.listen.expensetracker.features.statistics.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen

/**
 * Key Metrics Summary Card Component displaying daily averages and peak transactions.
 */
@Composable
fun MetricsSummaryCard(
    isExpenseTab: Boolean,
    dailyAverage: Double,
    maxTransaction: TransactionEntity?,
    currencySymbol: String,
    lang: String,
    modifier: Modifier = Modifier,
    hideAmount: Boolean = false
) {
    val noneText = AppStrings.common_none.tr(lang)

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceStandard,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Daily Average
            Column {
                Text(
                    text = if (isExpenseTab) AppStrings.daily_average_expense.tr(lang) else AppStrings.daily_average_income.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (hideAmount) "••••" else "$currencySymbol${"%.2f".format(dailyAverage)}",
                        fontSize = AppDimens.TextTitle,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpenseTab) ExpenseRed else IncomeGreen,
                        maxLines = 1
                    )
                }
            }

            // Max Single Transaction
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = if (isExpenseTab) AppStrings.max_expense.tr(lang) else AppStrings.max_income.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val maxDisplay = if (hideAmount) {
                    "••••"
                } else {
                    maxTransaction?.let { "$currencySymbol${"%.2f".format(it.amount)} (${it.categoryName})" } ?: noneText
                }
                Box(
                    modifier = Modifier.height(24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = maxDisplay,
                        fontSize = AppDimens.TextTitle,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpenseTab) ExpenseRed else IncomeGreen,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
