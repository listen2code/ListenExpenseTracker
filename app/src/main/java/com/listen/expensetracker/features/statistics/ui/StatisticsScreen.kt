package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.statistics.components.MetricsSummaryCard
import com.listen.expensetracker.features.statistics.components.RankingCategoryItem
import com.listen.expensetracker.features.statistics.components.StatisticsDialogHost
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.uicomponent.charts.BarChart
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.components.BaseScreenScaffold
import com.listen.uicomponent.components.EmptyStateView
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard

/**
 * Stateful entry route for Statistics Screen.
 */
@Composable
fun StatisticsRoute(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(),
) {
    val state by viewModel.viewState.collectAsState()
    StatisticsScreen(
        state = state,
        onIntent = viewModel::handleIntent,
        modifier = modifier
    )
}

/**
 * Pure Stateless Statistics Screen displaying Donut Chart, Segmented Progress,
 * 7-Day Trend Bar Chart, Key Metrics, and Category Rankings.
 */
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onIntent: (StatisticsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol

    val isExpenseTab = state.statisticsTab == "EXPENSE"
    val activeShares = if (isExpenseTab) state.categoryShares else state.incomeCategoryShares
    val activeSegments = if (isExpenseTab) state.progressSegments else state.incomeProgressSegments
    val totalAmount = if (isExpenseTab) state.totalExpense else state.totalIncome

    BaseScreenScaffold(
        titleSlot = {
            MonthNavigationCapsule(
                monthTitle = state.monthTitle,
                onPreviousMonth = { onIntent(StatisticsIntent.ChangeMonthOffset(-1)) },
                onNextMonth = { onIntent(StatisticsIntent.ChangeMonthOffset(1)) },
                onTitleClick = { onIntent(StatisticsIntent.OpenMonthPicker) }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.SpaceLarge),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
        ) {
            // Expense vs Income Segmented Toggle
            item(key = "tab_toggle") {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = AppDimens.SpaceExtraSmall)
                ) {
                    SegmentedButton(
                        selected = isExpenseTab,
                        onClick = { onIntent(StatisticsIntent.ChangeStatisticsTab("EXPENSE")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(
                            StringsRes.get("tab_expense_analysis", lang),
                            fontSize = AppDimens.TextBody,
                            fontWeight = if (isExpenseTab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    SegmentedButton(
                        selected = !isExpenseTab,
                        onClick = { onIntent(StatisticsIntent.ChangeStatisticsTab("INCOME")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(
                            StringsRes.get("tab_income_analysis", lang),
                            fontSize = AppDimens.TextBody,
                            fontWeight = if (!isExpenseTab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Donut Chart & Segmented Progress Card
            item(key = "donut_chart_card") {
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    if (activeShares.isEmpty() || totalAmount <= 0.0) {
                        EmptyStateView(
                            message = if (isExpenseTab) StringsRes.get("empty_month_expense", lang) else StringsRes.get("empty_month_income", lang),
                            modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                        )
                    } else {
                        DonutChart(
                            items = activeShares,
                            totalValue = totalAmount,
                            centerTitle = if (isExpenseTab) StringsRes.get("total_expense", lang) else StringsRes.get("total_income", lang),
                            centerValueText = "$sym${"%.2f".format(totalAmount)}",
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

            // 7-Day Trend Bar Chart (Expense Only)
            if (isExpenseTab && state.dailyTrendBars.isNotEmpty()) {
                item(key = "trend_chart_card") {
                    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = StringsRes.get("trend_7days", lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = AppDimens.TextTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = AppDimens.SpaceSmall)
                        )
                        BarChart(
                            items = state.dailyTrendBars,
                            height = AppDimens.ChartHeightStandard,
                            barWidth = AppDimens.ChartBarWidth,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Key Metrics Summary Card
            item(key = "metrics_card") {
                MetricsSummaryCard(
                    isExpenseTab = isExpenseTab,
                    dailyAverage = if (isExpenseTab) state.dailyAverageExpense else state.dailyAverageIncome,
                    maxTransaction = if (isExpenseTab) state.maxExpenseTransaction else state.maxIncomeTransaction,
                    currencySymbol = sym,
                    lang = lang,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Breakdown Ranking List
            if (activeShares.isNotEmpty()) {
                item(key = "ranking_header") {
                    Text(
                        text = if (isExpenseTab) StringsRes.get("expense_ranking", lang) else StringsRes.get("income_ranking", lang),
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Feature-Level Dialog Host
    StatisticsDialogHost(state = state, onIntent = onIntent)
}
