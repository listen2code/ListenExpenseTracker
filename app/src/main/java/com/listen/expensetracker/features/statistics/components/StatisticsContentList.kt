package com.listen.expensetracker.features.statistics.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.listen.uicomponent.charts.PieChartItem
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsTab
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
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberSaveable(monthOffset, saver = LazyListState.Saver) { LazyListState() },
    onCategoryClick: ((categoryName: String) -> Unit)? = null,
    onDateClick: ((day: Int, dateLabel: String) -> Unit)? = null,
    onTransactionClick: ((TransactionEntity) -> Unit)? = null
) {
    val lang = state.language
    val sym = state.currencySymbol

    val isExpenseTab = state.statisticsTab == StatisticsTab.EXPENSE

    // Real-time calculation for this specific month page
    val calc = remember(state.allTransactions, monthOffset, state.monthlyBudget, lang) {
        TransactionCalculationEngine.filterAndCalculate(
            allList = state.allTransactions, currentOffset = monthOffset, query = "", accountFilter = "ALL", budget = state.monthlyBudget, sortOrder = TransactionSortOrder.DATE_DESC, currencySymbol = sym, lang = lang
        )
    }

    val activeShares = if (isExpenseTab) calc.categoryShares else calc.incomeCategoryShares

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.SpaceLarge),
        contentPadding = PaddingValues(bottom = AppDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
    ) {
        // Donut Chart & Segmented Progress Card
        item(key = "donut_chart_card") {
            SurfaceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppDimens.SpaceExtraSmall)
            ) {
                AnimatedContent(
                    targetState = isExpenseTab,
                    transitionSpec = {
                        fadeIn(
                            animationSpec = tween(300)
                        ) togetherWith fadeOut(
                            animationSpec = tween(200)
                        )
                    },
                    label = "DonutChartTabTransition"
                ) { expenseTab ->
                    val shares = if (expenseTab) calc.categoryShares else calc.incomeCategoryShares
                    val segments = if (expenseTab) calc.progressSegments else calc.incomeProgressSegments
                    val amount = if (expenseTab) calc.totalExpense else calc.totalIncome

                    var selectedDonutItem by remember(expenseTab, monthOffset) { mutableStateOf<PieChartItem?>(null) }

                    if (shares.isEmpty() || amount <= 0.0) {
                        CommonEmpty(
                            message = if (expenseTab) AppStrings.EMPTY_MONTH_EXPENSE.tr(lang) else AppStrings.EMPTY_MONTH_INCOME.tr(lang),
                            modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                        )
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            DonutChart(
                                items = shares,
                                totalValue = amount,
                                centerTitle = if (expenseTab) AppStrings.TOTAL_EXPENSE.tr(lang) else AppStrings.TOTAL_INCOME.tr(lang),
                                centerValueText = if (state.hideAmount) "••••" else "$sym${"%.2f".format(amount)}",
                                currencySymbol = sym,
                                hideAmount = state.hideAmount,
                                selectedItem = selectedDonutItem,
                                onSelectionChange = { selectedDonutItem = it },
                                onTooltipClick = onCategoryClick?.let { callback -> { item -> callback(item.label) } },
                                modifier = Modifier.padding(vertical = AppDimens.SpaceSmall)
                            )
                            SegmentedProgressBar(
                                segments = segments,
                                highlightColorHex = selectedDonutItem?.colorHex,
                                onSegmentClick = { seg ->
                                    val matchedItem = shares.find { it.colorHex.equals(seg.colorHex, ignoreCase = true) }
                                    selectedDonutItem = if (selectedDonutItem == matchedItem) null else matchedItem
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = AppDimens.SpaceSmall)
                            )
                        }
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
                            text = AppStrings.TREND_MONTH_DAILY.tr(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = AppDimens.TextTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = AppDimens.SpaceSmall)
                        )
                        val maxDailyVal = calc.dailyTrendPoints.maxOfOrNull { it.value } ?: 0.0
                        LineChart(
                            points = calc.dailyTrendPoints,
                            chartHeight = AppDimens.ChartHeightStandard,
                            currencySymbol = sym,
                            hideAmount = state.hideAmount,
                            maxLabel = AppStrings.CHART_MAX.tr(lang).format(sym, maxDailyVal),
                            totalLabel = if (state.hideAmount) "••••" else "$sym${"%.2f".format(calc.totalExpense)}",
                            onTooltipClick = onDateClick?.let { cb -> { pt -> pt.label.toIntOrNull()?.let { day -> cb(day, pt.subLabel ?: "") } } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Key Metrics Summary Card
        item(key = "metrics_card") {
            AnimatedContent(
                targetState = isExpenseTab,
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(300)
                    ) togetherWith fadeOut(
                        animationSpec = tween(200)
                    )
                },
                label = "MetricsTabTransition"
            ) { expenseTab ->
                MetricsSummaryCard(
                    isExpenseTab = expenseTab,
                    dailyAverage = if (expenseTab) calc.dailyAverageExpense else calc.dailyAverageIncome,
                    maxTransaction = if (expenseTab) calc.maxExpenseTransaction else calc.maxIncomeTransaction,
                    currencySymbol = sym,
                    lang = lang,
                    modifier = Modifier.fillMaxWidth(),
                    hideAmount = state.hideAmount,
                    onMaxTransactionClick = onTransactionClick
                )
            }
        }

        // Category Breakdown Ranking List
        if (activeShares.isNotEmpty()) {
            item(key = "ranking_header") {
                Text(
                    text = if (isExpenseTab) AppStrings.EXPENSE_RANKING.tr(lang) else AppStrings.INCOME_RANKING.tr(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = AppDimens.SpaceSmall)
                )
            }

            item(key = "ranking_card") {
                SurfaceCard(
                    cornerRadius = AppDimens.CornerCard,
                    contentPadding = AppDimens.SpaceLarge,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
                        activeShares.forEachIndexed { index, item ->
                            RankingCategoryItem(
                                rank = index + 1,
                                share = item,
                                currencySymbol = sym,
                                hideAmount = state.hideAmount,
                                lang = lang,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = onCategoryClick?.let { { it(item.label) } }
                            )
                            if (index < activeShares.lastIndex) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                    modifier = Modifier.padding(vertical = AppDimens.SpaceSmall)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
