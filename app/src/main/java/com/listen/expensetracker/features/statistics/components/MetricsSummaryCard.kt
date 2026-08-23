package com.listen.expensetracker.features.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.data.db.TransactionEntity
import com.listen.arch.i18n.StringsRes
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
    modifier: Modifier = Modifier
) {
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
                    text = if (isExpenseTab) StringsRes.get("daily_average_expense", lang) else StringsRes.get("daily_average_income", lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$currencySymbol${String.format("%.2f", dailyAverage)}",
                    fontSize = AppDimens.TextTitle,
                    fontWeight = FontWeight.Bold,
                    color = if (isExpenseTab) ExpenseRed else IncomeGreen,
                    maxLines = 1
                )
            }

            // Max Single Transaction
            Column {
                Text(
                    text = if (isExpenseTab) StringsRes.get("max_expense", lang) else StringsRes.get("max_income", lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val maxDisplay = maxTransaction?.let { "$currencySymbol${String.format("%.2f", it.amount)} (${it.categoryName})" } ?: "无"
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
