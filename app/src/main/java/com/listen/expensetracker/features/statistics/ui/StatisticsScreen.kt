package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.settings.components.MonthPickerDialog
import com.listen.expensetracker.features.statistics.components.MetricsSummaryCard
import com.listen.expensetracker.features.statistics.components.RankingCategoryItem
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.charts.BarChart
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard

/**
 * Statistics Screen displaying Donut Chart, Segmented Progress,
 * 7-Day Trend Bar Chart, Key Metrics, and Category Rankings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = state.language
    val sym = state.currencySymbol
    var showMonthPickerDialog by remember { mutableStateOf(false) }

    val isExpenseTab = state.statisticsTab == "EXPENSE"
    val activeShares = if (isExpenseTab) state.categoryShares else state.incomeCategoryShares
    val activeSegments = if (isExpenseTab) state.progressSegments else state.incomeProgressSegments
    val totalAmount = if (isExpenseTab) state.totalExpense else state.totalIncome

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                title = {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.CornerPill))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = AppDimens.SpaceSmall, vertical = AppDimens.SpaceExtraSmall)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceExtraSmall)
                        ) {
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(-1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimens.IconSizeMedium))
                            }
                            Text(
                                text = state.monthTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimens.TextTitle,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showMonthPickerDialog = true }
                                    .padding(horizontal = AppDimens.SpaceSmall, vertical = AppDimens.SpaceExtraSmall)
                            )
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(AppDimens.IconSizeMedium))
                            }
                        }
                    }
                }
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
                        onClick = { onIntent(TransactionsIntent.ChangeStatisticsTab("EXPENSE")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(StringsRes.get("tab_expense_analysis", lang), fontSize = AppDimens.TextBody, fontWeight = if (isExpenseTab) FontWeight.Bold else FontWeight.Normal)
                    }
                    SegmentedButton(
                        selected = !isExpenseTab,
                        onClick = { onIntent(TransactionsIntent.ChangeStatisticsTab("INCOME")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(StringsRes.get("tab_income_analysis", lang), fontSize = AppDimens.TextBody, fontWeight = if (!isExpenseTab) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            if (activeShares.isEmpty()) {
                item(key = "empty_state") {
                    EmptyStateView(
                        message = if (isExpenseTab) StringsRes.get("empty_month_expense", lang) else StringsRes.get("empty_month_income", lang),
                        modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                    )
                }
            } else {
                // Segmented Progress Ratio Bar
                item(key = "progress_bar") {
                    SegmentedProgressBar(
                        segments = activeSegments,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category Donut Chart
                item(key = "donut_chart") {
                    DonutChart(
                        items = activeShares,
                        totalValue = totalAmount,
                        centerTitle = if (isExpenseTab) StringsRes.get("total_expense", lang) else StringsRes.get("total_income", lang),
                        centerValueText = "$sym${String.format("%.2f", totalAmount)}"
                    )
                }

                // Core Key Metrics Card
                item(key = "metrics_card") {
                    MetricsSummaryCard(
                        isExpenseTab = isExpenseTab,
                        dailyAverage = if (isExpenseTab) state.dailyAverageExpense else state.dailyAverageIncome,
                        maxTransaction = if (isExpenseTab) state.maxExpenseTransaction else state.maxIncomeTransaction,
                        currencySymbol = sym,
                        lang = lang
                    )
                }

                // 7-Day Trend Bar Chart (Expense tab)
                if (isExpenseTab && state.dailyTrendBars.isNotEmpty()) {
                    item(key = "7day_chart") {
                        SurfaceCard(
                            cornerRadius = AppDimens.CornerCard,
                            contentPadding = AppDimens.SpaceStandard,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = StringsRes.get("trend_7days", lang),
                                    fontSize = AppDimens.TextBody,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))
                                BarChart(
                                    items = state.dailyTrendBars,
                                    height = AppDimens.ChartHeightStandard,
                                    barWidth = AppDimens.ChartBarWidth
                                )
                            }
                        }
                    }
                }

                // Ranking Header
                item(key = "ranking_header") {
                    Text(
                        text = if (isExpenseTab) StringsRes.get("expense_ranking", lang) else StringsRes.get("income_ranking", lang),
                        fontSize = AppDimens.TextBody,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = AppDimens.SpaceSmall, bottom = AppDimens.SpaceExtraSmall)
                    )
                }

                // Category Ranking Items
                items(items = activeShares, key = { it.label }) { share ->
                    RankingCategoryItem(
                        share = share,
                        currencySymbol = sym
                    )
                }
            }
        }
    }

    if (showMonthPickerDialog) {
        MonthPickerDialog(
            currentOffset = state.selectedMonthOffset,
            onOffsetSelected = { onIntent(TransactionsIntent.ChangeMonthOffset(it - state.selectedMonthOffset)) },
            onDismiss = { showMonthPickerDialog = false },
            lang = lang
        )
    }
}
