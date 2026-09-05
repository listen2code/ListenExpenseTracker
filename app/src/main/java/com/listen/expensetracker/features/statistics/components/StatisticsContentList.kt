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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.FinancialInsightEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsTab
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.charts.LineChart
import com.listen.uicomponent.components.SurfaceCard

/**
 * LazyColumn Statistics Content List view for a specific month page (monthOffset).
 * Aggregates Financial Insights, Annual Overview, Donut Breakdown, Trend Line Chart, Key Metrics, and Rankings.
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

    // 智能财务诊断与环比分析洞察列表
    val insights = remember(state.allTransactions, monthOffset, state.monthlyBudget, sym, lang) {
        FinancialInsightEngine.generateInsights(
            allTransactions = state.allTransactions,
            currentOffset = monthOffset,
            monthlyBudget = state.monthlyBudget,
            currencySymbol = sym,
            lang = lang
        )
    }

    // 全年 12 个月收支总览数据
    val annualSummaries = remember(state.allTransactions, monthOffset, lang) {
        FinancialInsightEngine.calculateAnnualOverview(
            allTransactions = state.allTransactions,
            currentOffset = monthOffset,
            lang = lang
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
        // 1. 顶部智能财务洞察卡片轮播
        if (insights.isNotEmpty()) {
            item(key = "financial_insights_card") {
                InsightCarouselCard(
                    insights = insights,
                    lang = lang,
                    modifier = Modifier.padding(top = AppDimens.SpaceExtraSmall),
                    onInsightClick = { item ->
                        if (item.isCategoryAction && item.categoryId != null) {
                            val catName = state.allTransactions.firstOrNull { it.categoryId == item.categoryId }?.categoryName ?: item.categoryId
                            onCategoryClick?.invoke(catName)
                        } else if (item.targetDay != null) {
                            onDateClick?.invoke(item.targetDay, "${item.targetDay}")
                        }
                    }
                )
            }
        }

        // 2. 分类占比与比例条卡片 (Donut & Segmented Progress)
        item(key = "donut_chart_card") {
            StatisticsBreakdownCard(
                calc = calc,
                isExpenseTab = isExpenseTab,
                monthOffset = monthOffset,
                currencySymbol = sym,
                lang = lang,
                hideAmount = state.hideAmount,
                onCategoryClick = onCategoryClick
            )
        }

        // 3. Month Daily Trend Line Chart (Expense Only)
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
                            totalLabel = if (state.hideAmount) "••••" else "$sym${calc.totalExpense.formatAmount()}",
                            onTooltipClick = onDateClick?.let { cb -> { pt -> pt.label.toIntOrNull()?.let { day -> cb(day, pt.subLabel ?: "") } } },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. Key Metrics Summary Card
        item(key = "metrics_card") {
            AnimatedContent(
                targetState = isExpenseTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
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

        // 5. 年度 12 个月收支总览卡片 (Annual Overview)
        item(key = "annual_overview_card") {
            AnnualOverviewCard(
                summaries = annualSummaries,
                currencySymbol = sym,
                lang = lang,
                hideAmount = state.hideAmount,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // 6. Category Breakdown Ranking List
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
