package com.listen.expensetracker.features.statistics.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.charts.LineChart
import com.listen.uicomponent.components.CommonEmpty
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard

/**
 * LazyColumn Statistics Content List view for a specific month page (monthOffset).
 * Contains Donut Chart, Trend Line Chart, Key Metrics, and Category Rankings computed for that month.
 */
@Composable
fun StatisticsContentList(
    state: StatisticsUiState,
    monthOffset: Int,
    onIntent: (StatisticsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol

    val isExpenseTab = state.statisticsTab == "EXPENSE"

    // Real-time calculation for this specific month page
    val calc = remember(state.allTransactions, monthOffset, state.monthlyBudget, lang) {
        TransactionCalculationEngine.filterAndCalculate(
            allList = state.allTransactions,
            currentOffset = monthOffset,
            query = "",
            accountFilter = "ALL",
            budget = state.monthlyBudget,
            sortOrder = TransactionSortOrder.DATE_DESC,
            currencySymbol = sym,
            lang = lang
        )
    }

    val activeShares = if (isExpenseTab) calc.categoryShares else calc.incomeCategoryShares
    val activeSegments = if (isExpenseTab) calc.progressSegments else calc.incomeProgressSegments
    val totalAmount = if (isExpenseTab) calc.totalExpense else calc.totalIncome

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
    ) {
        // Donut Chart & Segmented Progress Card
        item(key = "donut_chart_card") {
            SurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.SpaceExtraSmall)
            ) {
                if (activeShares.isEmpty() || totalAmount <= 0.0) {
                    CommonEmpty(
                        message = if (isExpenseTab) AppStrings.empty_month_expense.tr(lang) else AppStrings.empty_month_income.tr(lang),
                        modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        DonutChart(
                            items = activeShares,
                            totalValue = totalAmount,
                            centerTitle = if (isExpenseTab) AppStrings.total_expense.tr(lang) else AppStrings.total_income.tr(lang),
                            centerValueText = if (state.hideAmount) "••••" else "$sym${"%.2f".format(totalAmount)}",
                            modifier = Modifier.padding(vertical = AppDimens.SpaceSmall)
                        )
                        SegmentedProgressBar(
                            segments = activeSegments,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppDimens.SpaceSmall)
                        )
                    }
                }
            }
        }

        // Month Daily Trend Line Chart (Expense Only)
        if (isExpenseTab && calc.dailyTrendPoints.isNotEmpty()) {
            item(key = "trend_chart_card") {
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = AppStrings.trend_month_daily.tr(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = AppDimens.TextTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = AppDimens.SpaceSmall)
                        )
                        LineChart(
                            points = calc.dailyTrendPoints,
                            chartHeight = AppDimens.ChartHeightStandard,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Key Metrics Summary Card
        item(key = "metrics_card") {
            MetricsSummaryCard(
                isExpenseTab = isExpenseTab,
                dailyAverage = if (isExpenseTab) calc.dailyAverageExpense else calc.dailyAverageIncome,
                maxTransaction = if (isExpenseTab) calc.maxExpenseTransaction else calc.maxIncomeTransaction,
                currencySymbol = sym,
                lang = lang,
                modifier = Modifier.fillMaxWidth(),
                hideAmount = state.hideAmount
            )
        }

        // Category Breakdown Ranking List
        if (activeShares.isNotEmpty()) {
            item(key = "ranking_header") {
                Text(
                    text = if (isExpenseTab) AppStrings.expense_ranking.tr(lang) else AppStrings.income_ranking.tr(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = AppDimens.SpaceSmall)
                )
            }

            items(activeShares, key = { it.label }) { item ->
                RankingCategoryItem(
                    share = item,
                    currencySymbol = sym,
                    modifier = Modifier.fillMaxWidth(),
                    hideAmount = state.hideAmount
                )
            }
        }
    }
}
