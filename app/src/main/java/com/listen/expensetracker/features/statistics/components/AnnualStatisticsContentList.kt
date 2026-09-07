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
import com.listen.expensetracker.data.engine.AnnualCalculationEngine
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsTab
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.uicomponent.charts.LineChart
import com.listen.uicomponent.components.SurfaceCard
import java.util.Calendar

/**
 * 年度专属统计内容列表视图 (AnnualStatisticsContentList)。
 * 聚合展示年度 12 个月收支总览看板、全年分类占比环形图、年度月别走势折线图、年度关键指标与全年分类排行。
 */
@Composable
fun AnnualStatisticsContentList(
    state: StatisticsUiState,
    yearOffset: Int,
    onIntent: (StatisticsIntent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberSaveable(yearOffset, saver = LazyListState.Saver) { LazyListState() },
    onAnnualCategoryClick: ((year: Int, categoryName: String) -> Unit)? = null,
    onTransactionClick: ((TransactionEntity) -> Unit)? = null,
    onNavigateToTransactionsMonth: ((monthOffset: Int) -> Unit)? = null
) {
    val lang = state.language
    val sym = state.currencySymbol
    val isExpenseTab = state.statisticsTab == StatisticsTab.EXPENSE

    // 实时计算该指定年份的所有度量指标
    val calc = remember(state.allTransactions, yearOffset, lang) {
        AnnualCalculationEngine.filterAndCalculateYear(
            allList = state.allTransactions,
            yearOffset = yearOffset,
            lang = lang
        )
    }

    val activeShares = if (isExpenseTab) calc.categoryShares else calc.incomeCategoryShares
    val activeSegments = if (isExpenseTab) calc.progressSegments else calc.incomeProgressSegments
    val totalAmount = if (isExpenseTab) calc.totalExpense else calc.totalIncome

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = AppDimens.SpaceLarge),
        contentPadding = PaddingValues(bottom = AppDimens.SpaceLarge),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
    ) {
        // 1. 全年 12 个月收支总览卡片 (作为年别视图的核心主看板)
        item(key = "annual_overview_card") {
            AnnualOverviewCard(
                summaries = calc.annualSummaries,
                currencySymbol = sym,
                lang = lang,
                hideAmount = state.hideAmount,
                modifier = Modifier.fillMaxWidth(),
                onNavigateToTransactionsMonth = onNavigateToTransactionsMonth?.let { cb ->
                    { monthIndex ->
                        val curY = Calendar.getInstance().get(Calendar.YEAR)
                        val curM = Calendar.getInstance().get(Calendar.MONTH) + 1
                        val targetOffset = (calc.year - curY) * 12 + (monthIndex - curM)
                        cb(targetOffset)
                    }
                }
            )
        }

        // 2. 全年各大分类收支占比与比例条卡片
        item(key = "annual_donut_card") {
            StatisticsBreakdownCard(
                shares = activeShares,
                segments = activeSegments,
                totalAmount = totalAmount,
                isExpenseTab = isExpenseTab,
                currencySymbol = sym,
                lang = lang,
                hideAmount = state.hideAmount,
                key = yearOffset,
                onCategoryClick = onAnnualCategoryClick?.let { cb -> { cat -> cb(calc.year, cat) } }
            )
        }

        // 3. 年度 12 个月月度支出/收入走势折线图
        if (calc.monthlyTrendPoints.isNotEmpty()) {
            item(key = "annual_trend_chart") {
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = AppStrings.TREND_YEAR_MONTHLY.tr(lang),
                            fontWeight = FontWeight.Bold,
                            fontSize = AppDimens.TextTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = AppDimens.SpaceSmall)
                        )
                        val maxMonthlyVal = calc.monthlyTrendPoints.maxOfOrNull { it.value } ?: 0.0
                        LineChart(
                            points = calc.monthlyTrendPoints,
                            chartHeight = AppDimens.ChartHeightStandard,
                            currencySymbol = sym,
                            hideAmount = state.hideAmount,
                            maxLabel = AppStrings.CHART_MAX.tr(lang).format(sym, maxMonthlyVal),
                            totalLabel = if (state.hideAmount) "••••" else "$sym${calc.totalExpense.formatAmount()}",
                            onTooltipClick = onNavigateToTransactionsMonth?.let { cb ->
                                { pt ->
                                    val monthIdx = calc.monthlyTrendPoints.indexOf(pt) + 1
                                    if (monthIdx in 1..12) {
                                        val curY = Calendar.getInstance().get(Calendar.YEAR)
                                        val curM = Calendar.getInstance().get(Calendar.MONTH) + 1
                                        val targetOffset = (calc.year - curY) * 12 + (monthIdx - curM)
                                        cb(targetOffset)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 4. 年度关键指标卡片 (月均支出/收入、全年单笔极值)
        item(key = "annual_metrics_card") {
            AnimatedContent(
                targetState = isExpenseTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
                },
                label = "AnnualMetricsTabTransition"
            ) { expenseTab ->
                MetricsSummaryCard(
                    isExpenseTab = expenseTab,
                    dailyAverage = if (expenseTab) calc.monthlyAverageExpense else calc.monthlyAverageIncome,
                    maxTransaction = if (expenseTab) calc.maxExpenseTransaction else calc.maxIncomeTransaction,
                    currencySymbol = sym,
                    lang = lang,
                    modifier = Modifier.fillMaxWidth(),
                    averageLabel = if (expenseTab) AppStrings.STATS_MONTH_AVG_EXPENSE.tr(lang) else AppStrings.STATS_MONTH_AVG_INCOME.tr(lang),
                    hideAmount = state.hideAmount,
                    onMaxTransactionClick = onTransactionClick
                )
            }
        }

        // 5. 全年分类排行列表 (支持点击分类穿透至流水画面年度筛选)
        if (activeShares.isNotEmpty()) {
            item(key = "annual_ranking_header") {
                Text(
                    text = if (isExpenseTab) AppStrings.EXPENSE_RANKING.tr(lang) else AppStrings.INCOME_RANKING.tr(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = AppDimens.SpaceSmall)
                )
            }

            item(key = "annual_ranking_card") {
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
                                onClick = onAnnualCategoryClick?.let { cb -> { cb(calc.year, item.label) } }
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
