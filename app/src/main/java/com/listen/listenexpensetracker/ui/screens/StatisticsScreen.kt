package com.listen.listenexpensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.listenexpensetracker.ui.state.TransactionsIntent
import com.listen.listenexpensetracker.ui.state.TransactionsUiState
import com.listen.uicomponent.charts.BarChart
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val sym = state.currencySymbol
    val lang = state.language
    val isExpenseTab = state.statisticsTab == "EXPENSE"

    val activeShares = if (isExpenseTab) state.categoryShares else state.incomeCategoryShares
    val activeSegments = if (isExpenseTab) state.progressSegments else state.incomeProgressSegments
    val totalAmount = if (isExpenseTab) state.totalExpense else state.totalIncome

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringsRes.get("stats_title", lang),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Expense vs Income Segmented Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                SegmentedButton(
                    selected = isExpenseTab,
                    onClick = { onIntent(TransactionsIntent.ChangeStatisticsTab("EXPENSE")) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("${StringsRes.get("tab_expense_analysis", lang)} ($sym${String.format("%.0f", state.totalExpense)})", fontSize = 12.sp)
                }
                SegmentedButton(
                    selected = !isExpenseTab,
                    onClick = { onIntent(TransactionsIntent.ChangeStatisticsTab("INCOME")) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("${StringsRes.get("tab_income_analysis", lang)} ($sym${String.format("%.0f", state.totalIncome)})", fontSize = 12.sp)
                }
            }

            if (activeShares.isEmpty()) {
                EmptyStateView(message = if (isExpenseTab) "暂无支出数据，去记一笔账吧！" else "暂无收入数据，快记录一笔入账吧！")
            } else {
                // Segmented Progress Ratio Bar
                SegmentedProgressBar(
                    segments = activeSegments,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Category Donut Chart
                DonutChart(
                    items = activeShares,
                    totalValue = totalAmount,
                    centerTitle = if (isExpenseTab) StringsRes.get("total_expense", lang) else StringsRes.get("total_income", lang),
                    centerValueText = "$sym${String.format("%.2f", totalAmount)}"
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Core Key Metrics Card
                SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = if (isExpenseTab) StringsRes.get("daily_average_expense", lang) else StringsRes.get("daily_average_income", lang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$sym${String.format("%.2f", if (isExpenseTab) state.dailyAverageExpense else state.dailyAverageIncome)}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpenseTab) ExpenseRed else IncomeGreen
                            )
                        }

                        Column {
                            Text(
                                text = if (isExpenseTab) StringsRes.get("max_expense", lang) else StringsRes.get("max_income", lang),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val maxTx = if (isExpenseTab) state.maxExpenseTransaction else state.maxIncomeTransaction
                            Text(
                                text = maxTx?.let { "$sym${String.format("%.2f", it.amount)} (${it.categoryName})" } ?: "无",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isExpenseTab) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 7-day spending bar chart (when in expense tab)
                if (isExpenseTab && state.dailyTrendBars.isNotEmpty()) {
                    SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text(
                                text = StringsRes.get("trend_7days", lang),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            BarChart(
                                items = state.dailyTrendBars,
                                height = 130.dp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Text(
                    text = if (isExpenseTab) StringsRes.get("expense_ranking", lang) else StringsRes.get("income_ranking", lang),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(activeShares) { share ->
                        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = share.label,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = parseHexColor(share.colorHex)
                                    )
                                    Text(
                                        text = "${String.format("%.1f", share.percentage * 100)}%",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = "$sym${String.format("%.2f", share.value)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (isExpenseTab) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
