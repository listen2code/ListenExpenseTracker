package com.listen.expensetracker.features.transactions.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonText
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
    monthlyBudget: Double = 0.0,
    remainingBudget: Double = 0.0,
    onBudgetClick: () -> Unit = {},
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
                    text = AppStrings.BALANCE_TITLE.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier.height(30.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    CommonText(
                        text = if (hideBalance) "••••" else "$currencySymbol${"%.2f".format(netBalance)}",
                        fontSize = AppDimens.TextDisplay,
                        minFontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        autoResize = true
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
                        text = AppStrings.TOTAL_EXPENSE.tr(lang),
                        fontSize = AppDimens.TextCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CommonText(
                        text = if (hideBalance) "••••" else "$currencySymbol${"%.2f".format(totalExpense)}",
                        fontSize = AppDimens.TextBody,
                        minFontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExpenseRed,
                        maxLines = 1,
                        autoResize = true
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                ) {
                    Text(
                        text = AppStrings.TOTAL_INCOME.tr(lang),
                        fontSize = AppDimens.TextCaption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    CommonText(
                        text = if (hideBalance) "••••" else "$currencySymbol${"%.2f".format(totalIncome)}",
                        fontSize = AppDimens.TextBody,
                        minFontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = IncomeGreen,
                        maxLines = 1,
                        autoResize = true
                    )
                }
            }

            // Bottom Section: Prominent Monthly Budget Progress Container
            val animatedProgress by animateFloatAsState(
                targetValue = budgetUsageRatio.coerceIn(0f, 1f),
                animationSpec = tween(
                    durationMillis = 500,
                    easing = FastOutSlowInEasing
                ),
                label = "BudgetUsageProgress"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .clickable(onClick = onBudgetClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = AppStrings.MONTHLY_BUDGET.tr(lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (monthlyBudget > 0.0) {
                            Text(
                                text = if (hideBalance) "(••••)" else "($currencySymbol${"%.0f".format(monthlyBudget)})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Budget",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(11.dp)
                        )
                    }

                    Text(
                        text = if (hideBalance) {
                            if (isOverBudget) "${AppStrings.OVER_BUDGET.tr(lang)} ••••" else "${AppStrings.USED_BUDGET.tr(lang)} ••••"
                        } else if (isOverBudget) {
                            "${AppStrings.OVER_BUDGET.tr(lang)} $currencySymbol${"%.0f".format(totalExpense - monthlyBudget)}"
                        } else {
                            "${AppStrings.USED_BUDGET.tr(lang)} ${"%.0f".format(budgetUsageRatio * 100)}%"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary
                    )
                }

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (isOverBudget) ExpenseRed else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}
