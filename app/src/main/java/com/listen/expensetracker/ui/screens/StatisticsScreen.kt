package com.listen.expensetracker.ui.screens

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
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.ui.components.MonthPickerDialog
import com.listen.expensetracker.ui.state.TransactionsIntent
import com.listen.expensetracker.ui.state.TransactionsUiState
import com.listen.uicomponent.charts.BarChart
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.components.EmptyStateView
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor
import java.util.Calendar

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
    var showMonthPickerDialog by remember { mutableStateOf(false) }

    val activeShares = if (isExpenseTab) state.categoryShares else state.incomeCategoryShares
    val activeSegments = if (isExpenseTab) state.progressSegments else state.incomeProgressSegments
    val totalAmount = if (isExpenseTab) state.totalExpense else state.totalIncome

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                title = {
                    // Sleek Compact Month Navigation Pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(-1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                text = state.monthTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showMonthPickerDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                            IconButton(
                                onClick = { onIntent(TransactionsIntent.ChangeMonthOffset(1)) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
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
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Expense vs Income Segmented Toggle
            item(key = "tab_toggle") {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 2.dp)
                ) {
                    SegmentedButton(
                        selected = isExpenseTab,
                        onClick = { onIntent(TransactionsIntent.ChangeStatisticsTab("EXPENSE")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text(StringsRes.get("tab_expense_analysis", lang), fontSize = 12.sp, fontWeight = if (isExpenseTab) FontWeight.Bold else FontWeight.Normal)
                    }
                    SegmentedButton(
                        selected = !isExpenseTab,
                        onClick = { onIntent(TransactionsIntent.ChangeStatisticsTab("INCOME")) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text(StringsRes.get("tab_income_analysis", lang), fontSize = 12.sp, fontWeight = if (!isExpenseTab) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            if (activeShares.isEmpty()) {
                item(key = "empty_state") {
                    EmptyStateView(message = if (isExpenseTab) "该月份暂无支出数据" else "该月份暂无收入数据")
                }
            } else {
                // 2. Segmented Progress Ratio Bar
                item(key = "progress_bar") {
                    SegmentedProgressBar(
                        segments = activeSegments,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Category Donut Chart
                item(key = "donut_chart") {
                    DonutChart(
                        items = activeShares,
                        totalValue = totalAmount,
                        centerTitle = if (isExpenseTab) StringsRes.get("total_expense", lang) else StringsRes.get("total_income", lang),
                        centerValueText = "$sym${String.format("%.2f", totalAmount)}"
                    )
                }

                // 4. Core Key Metrics Card
                item(key = "metrics_card") {
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
                                    fontSize = 14.sp,
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
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isExpenseTab) ExpenseRed else IncomeGreen
                                )
                            }
                        }
                    }
                }

                // 5. 7-day spending bar chart (when in expense tab)
                if (isExpenseTab && state.dailyTrendBars.isNotEmpty()) {
                    item(key = "7day_chart") {
                        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                Text(
                                    text = StringsRes.get("trend_7days", lang),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                BarChart(
                                    items = state.dailyTrendBars,
                                    height = 136.dp
                                )
                            }
                        }
                    }
                }

                // 6. Ranking Header
                item(key = "ranking_header") {
                    Text(
                        text = if (isExpenseTab) StringsRes.get("expense_ranking", lang) else StringsRes.get("income_ranking", lang),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                // 7. Category Ranking Items
                items(
                    items = activeShares,
                    key = { it.label }
                ) { share ->
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
                                    fontSize = 13.sp,
                                    color = parseHexColor(share.colorHex)
                                )
                                Text(
                                    text = "${String.format("%.1f", share.percentage * 100)}%",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "$sym${String.format("%.2f", share.value)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isExpenseTab) ExpenseRed else IncomeGreen
                            )
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (showMonthPickerDialog) {
        val nowCal = Calendar.getInstance()
        val currentSelectedCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, state.selectedMonthOffset)
        }
        MonthPickerDialog(
            initialYear = currentSelectedCal.get(Calendar.YEAR),
            initialMonth = currentSelectedCal.get(Calendar.MONTH),
            onMonthSelected = { selectedYear, selectedMonth ->
                val targetCal = Calendar.getInstance().apply { set(selectedYear, selectedMonth, 1) }
                val diffMonths = (targetCal.get(Calendar.YEAR) - nowCal.get(Calendar.YEAR)) * 12 +
                        (targetCal.get(Calendar.MONTH) - nowCal.get(Calendar.MONTH))
                val delta = diffMonths - state.selectedMonthOffset
                onIntent(TransactionsIntent.ChangeMonthOffset(delta))
            },
            onDismiss = { showMonthPickerDialog = false },
            lang = lang
        )
    }
}
