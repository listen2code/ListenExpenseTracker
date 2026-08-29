package com.listen.expensetracker.features.transactions.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen

/**
 * Ultra-Compact Balance Overview Card Component.
 * Displays Net Balance, Monthly Expense, Income, and Budget Consumption Progress.
 * Optimized with fixed text metrics and maxLines to eliminate visual jitter when toggling privacy.
 *
 * @param currencySymbol Active currency symbol (e.g., "$", "¥")
 * @param netBalance Calculated net balance (Income - Expense)
 * @param totalExpense Total expense for the selected month
 * @param totalIncome Total income for the selected month
 * @param budgetUsageRatio Progress ratio of budget spent (0.0 to 1.0)
 * @param isOverBudget True if expense exceeds configured monthly budget
 * @param hideBalance True if privacy masking is active
 * @param lang ISO language code for internationalization
 * @param modifier Composable modifier
 */
@Composable
fun BalanceOverviewCard(
    currencySymbol: String,
    netBalance: Double,
    totalExpense: Double,
    totalIncome: Double,
    budgetUsageRatio: Float,
    isOverBudget: Boolean,
    hideBalance: Boolean,
    lang: String,
    modifier: Modifier = Modifier
) {
    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceStandard,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
            // Top Row: Balance Title & Primary Net Balance Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.balance_title.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = androidx.compose.ui.Modifier.height(30.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = if (hideBalance) "••••" else "$currencySymbol${"%.2f".format(netBalance)}",
                        fontSize = AppDimens.TextDisplay,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
            }

            // Middle Row: Total Expense & Total Income breakdown (Always visible)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                ) {
                    Text(
                        text = AppStrings.total_expense.tr(lang),
                        fontSize = AppDimens.TextCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${"%.2f".format(totalExpense)}",
                        fontSize = AppDimens.TextBody,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                ) {
                    Text(
                        text = AppStrings.total_income.tr(lang),
                        fontSize = AppDimens.TextCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$currencySymbol${"%.2f".format(totalIncome)}",
                        fontSize = AppDimens.TextBody,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                        maxLines = 1
                    )
                }
            }

            // Bottom Row: Budget Progress Indicator & Percentage
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = budgetUsageRatio.coerceIn(0f, 1f),
                animationSpec = androidx.compose.animation.core.tween(
                    durationMillis = 500,
                    easing = androidx.compose.animation.core.FastOutSlowInEasing
                ),
                label = "BudgetUsageProgress"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "${"%.0f".format(budgetUsageRatio * 100)}%",
                    fontSize = AppDimens.TextMicro,
                    color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
