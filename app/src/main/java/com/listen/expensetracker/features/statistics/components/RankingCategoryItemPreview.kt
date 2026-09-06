package com.listen.expensetracker.features.statistics.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.theme.ListenTheme

@Preview(showBackground = true)
@Composable
fun RankingCategoryItemPreview() {
    ExpenseStrings.init()
    val sampleShare = PieChartItem(
        label = "Dining",
        value = 1250.0,
        percentage = 0.45f,
        colorHex = "#EF4444"
    )
    ListenTheme {
        RankingCategoryItem(
            rank = 1,
            share = sampleShare,
            currencySymbol = "$",
            lang = "en",
            onClick = {}
        )
    }
}
