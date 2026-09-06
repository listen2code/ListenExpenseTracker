package com.listen.expensetracker.features.statistics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.engine.FinancialInsightItem
import com.listen.expensetracker.data.engine.InsightSeverity
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 智能财务洞察轮播卡片预览 (InsightCarouselCardPreview)。
 */
@Preview(showBackground = true)
@Composable
fun InsightCarouselCardPreview() {
    ExpenseStrings.init()
    val sampleInsights = listOf(
        FinancialInsightItem(
            id = "1",
            title = "Higher Food Spending",
            description = "Your food spending this week is 25% higher than usual. Consider cooking at home more.",
            severity = InsightSeverity.WARNING
        ),
        FinancialInsightItem(
            id = "2",
            title = "Savings Goal Reached",
            description = "Great job! You've met your savings goal for this month.",
            severity = InsightSeverity.POSITIVE
        ),
        FinancialInsightItem(
            id = "3",
            title = "Budget Tip",
            description = "Try the 50/30/20 rule to manage your income more effectively.",
            severity = InsightSeverity.INFO
        )
    )
    ListenTheme {
        InsightCarouselCard(
            insights = sampleInsights,
            lang = "en"
        )
    }
}
