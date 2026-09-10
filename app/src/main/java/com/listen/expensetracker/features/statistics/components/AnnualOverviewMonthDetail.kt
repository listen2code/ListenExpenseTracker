package com.listen.expensetracker.features.statistics.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.AnnualMonthSummary
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.theme.ListenTheme

/**
 * 年度收支总览卡片 - 选中月份详情条 (AnnualOverviewMonthDetail)。
 * 展示选中月份的支出、收入与净结余，并支持点击右侧小箭头直达流水画面对应月份。
 */
@Composable
fun AnnualOverviewMonthDetail(
    summary: AnnualMonthSummary,
    currencySymbol: String,
    hideAmount: Boolean,
    lang: String,
    modifier: Modifier = Modifier,
    onViewTransactions: (() -> Unit)? = null
) {
    val expStr = if (hideAmount) "••••" else "$currencySymbol${summary.totalExpense.formatAmount()}"
    val incStr = if (hideAmount) "••••" else "$currencySymbol${summary.totalIncome.formatAmount()}"
    val balSign = if (summary.netBalance > 0) "+" else ""
    val balStr = if (hideAmount) "••••" else "$balSign$currencySymbol${summary.netBalance.formatAmount()}"
    val balColor = when {
        summary.netBalance > 0 -> Color(0xFF10B981)
        summary.netBalance < 0 -> Color(0xFFEF4444)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .then(
                if (onViewTransactions != null) {
                    Modifier.clickable(onClick = onViewTransactions)
                } else Modifier
            )
            .padding(horizontal = AppDimens.SpaceMedium, vertical = AppDimens.SpaceSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                ) {
                    Text(
                        text = summary.monthLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TextSubtitle,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${AppStrings.BALANCE_TITLE.tr(lang)}: $balStr",
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = balColor
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${AppStrings.TYPE_EXPENSE.tr(lang)}: $expStr",
                        fontSize = AppDimens.TextMicro,
                        color = Color(0xFFEF4444)
                    )
                    Text(
                        text = "${AppStrings.TYPE_INCOME.tr(lang)}: $incStr",
                        fontSize = AppDimens.TextMicro,
                        color = Color(0xFF10B981)
                    )
                }
            }

            if (onViewTransactions != null) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View in Transactions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AnnualOverviewMonthDetailPreview() {
    ExpenseStrings.init()
    val sampleSummary = AnnualMonthSummary(
        monthIndex = 8,
        monthLabel = "8月",
        totalExpense = 4520.0,
        totalIncome = 8000.0,
        netBalance = 3480.0
    )
    ListenTheme {
        AnnualOverviewMonthDetail(
            summary = sampleSummary,
            currencySymbol = "¥",
            hideAmount = false,
            lang = "zh",
            onViewTransactions = {}
        )
    }
}
