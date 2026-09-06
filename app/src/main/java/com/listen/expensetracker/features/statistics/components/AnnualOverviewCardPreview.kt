package com.listen.expensetracker.features.statistics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.engine.AnnualMonthSummary
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

@Preview(showBackground = true)
@Composable
fun AnnualOverviewCardPreview() {
    ExpenseStrings.init()
    val sampleSummaries = listOf(
        AnnualMonthSummary(1, "Jan", 2100.0, 5000.0, 2900.0),
        AnnualMonthSummary(2, "Feb", 1850.0, 5000.0, 3150.0),
        AnnualMonthSummary(3, "Mar", 4200.0, 5000.0, 800.0),
        AnnualMonthSummary(4, "Apr", 2500.0, 5000.0, 2500.0),
        AnnualMonthSummary(5, "May", 2800.0, 5000.0, 2200.0),
        AnnualMonthSummary(6, "Jun", 3100.0, 8000.0, 4900.0),
        AnnualMonthSummary(7, "Jul", 2400.0, 5000.0, 2600.0),
        AnnualMonthSummary(8, "Aug", 2950.0, 5000.0, 2050.0),
        AnnualMonthSummary(9, "Sep", 2200.0, 5000.0, 2800.0),
        AnnualMonthSummary(10, "Oct", 1900.0, 5000.0, 3100.0),
        AnnualMonthSummary(11, "Nov", 3500.0, 5000.0, 1500.0),
        AnnualMonthSummary(12, "Dec", 5800.0, 10000.0, 4200.0)
    )
    ListenTheme {
        AnnualOverviewCard(
            summaries = sampleSummaries,
            currencySymbol = "$",
            lang = "en",
            currentSelectedMonth = 8
        )
    }
}
