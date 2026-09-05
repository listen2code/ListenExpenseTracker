package com.listen.expensetracker.features.recurring.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.RecurringMonthlyBaseline
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen

/**
 * 周期账单中心 - 每月固定收支 Baseline 总览卡片。
 */
@Composable
fun RecurringOverviewCard(
    baseline: RecurringMonthlyBaseline,
    monthlyBudget: Double,
    currencySymbol: String,
    lang: String,
    modifier: Modifier = Modifier
) {
    val budgetRatioStr = if (monthlyBudget > 0) {
        "%.1f%%".format((baseline.totalExpense / monthlyBudget) * 100)
    } else {
        "--"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(AppDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
    ) {
        CommonText(
            text = AppStrings.RECURRING_MONTHLY_BASELINE.tr(lang),
            fontSize = AppDimens.TextSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            CommonText(
                text = "$currencySymbol%.2f".format(baseline.totalExpense),
                fontSize = AppDimens.TextDisplay,
                minFontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ExpenseRed,
                maxLines = 1,
                autoResize = true
            )

            if (baseline.totalIncome > 0) {
                CommonText(
                    text = "+$currencySymbol%.2f".format(baseline.totalIncome),
                    fontSize = AppDimens.TextBody,
                    minFontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = IncomeGreen,
                    maxLines = 1,
                    autoResize = true
                )
            }
        }

        CommonText(
            text = AppStrings.RECURRING_BASELINE_DESC.tr(lang).format(budgetRatioStr, baseline.expenseCount),
            fontSize = AppDimens.TextMicro,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
