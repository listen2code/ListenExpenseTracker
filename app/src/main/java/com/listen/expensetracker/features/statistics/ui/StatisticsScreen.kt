package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.listen.expensetracker.core.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.statistics.components.AnnualStatisticsContentList
import com.listen.expensetracker.features.statistics.components.StatisticsContentList
import com.listen.expensetracker.features.statistics.components.StatisticsDialogHost
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsTab
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.uicomponent.components.BaseScreenScaffold
import com.listen.uicomponent.components.CommonSegmentedControl
import java.util.Calendar
import com.listen.expensetracker.data.model.AppConstants

import com.listen.expensetracker.core.state.NavTab
import kotlinx.coroutines.flow.SharedFlow

/**
 * 纯无状态统计分析主画面 (StatisticsScreen)。
 * 完美支持按月 (Month) 与按年 (Year) 双维度无缝切换展示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel? = null,
    scrollToTopFlow: SharedFlow<NavTab>? = null,
    onNavigateToTransactions: ((monthOffset: Int, categoryName: String) -> Unit)? = null,
    onNavigateToTransactionsAnnualCategory: ((year: Int, categoryName: String) -> Unit)? = null,
    onNavigateToTransactionsDate: ((monthOffset: Int, day: Int, dateLabel: String) -> Unit)? = null,
    onNavigateToTransactionsMonth: ((monthOffset: Int) -> Unit)? = null,
    onNavigateToTransaction: ((monthOffset: Int, transaction: TransactionEntity) -> Unit)? = null,
    onNavigateToBudget: ((monthOffset: Int) -> Unit)? = null
) {
    // 🌟 UI 交互状态（PagerState、LazyListState、标题计算）
    val holder = rememberStatisticsStateHolder(state)

    // 🌟 独立挂载画面专用副作用监听与手势协同 (区分于 UI 状态持有者 StatisticsStateHolder)
    StatisticsEffects(
        viewModel = viewModel,
        monthPagerState = holder.monthPagerState,
        yearPagerState = holder.yearPagerState,
        listState = holder.listState,
        period = state.period,
        selectedMonthOffset = state.selectedMonthOffset,
        selectedYearOffset = state.selectedYearOffset,
        scrollToTopFlow = scrollToTopFlow
    )

    val isExpenseTab = state.statisticsTab == StatisticsTab.EXPENSE

    BaseScreenScaffold(
        titleSlot = {
            MonthNavigationCapsule(
                monthTitle = if (state.period == StatisticsPeriod.MONTH) holder.currentMonthTitle else holder.currentYearTitle,
                onPreviousMonth = {
                    if (state.period == StatisticsPeriod.MONTH) {
                        viewModel?.handleIntent(StatisticsIntent.SelectMonth(holder.currentMonthOffset - 1))
                    } else {
                        viewModel?.handleIntent(StatisticsIntent.SelectYear(holder.currentYearOffset - 1))
                    }
                },
                onNextMonth = {
                    if (state.period == StatisticsPeriod.MONTH) {
                        viewModel?.handleIntent(StatisticsIntent.SelectMonth(holder.currentMonthOffset + 1))
                    } else {
                        viewModel?.handleIntent(StatisticsIntent.SelectYear(holder.currentYearOffset + 1))
                    }
                },
                onTitleClick = { viewModel?.handleIntent(StatisticsIntent.OpenMonthPicker) }
            )
        },
        actions = {
            IconButton(onClick = { viewModel?.handleIntent(StatisticsIntent.ToggleHideAmount(!state.hideAmount)) }) {
                Icon(
                    imageVector = if (state.hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = AppConstants.ContentDescription.TOGGLE_AMOUNT,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // 1. 顶部支出/收入分析切换栏
            val tabs = listOf(AppStrings.TAB_EXPENSE_ANALYSIS.tr(), AppStrings.TAB_INCOME_ANALYSIS.tr())
            CommonSegmentedControl(
                items = tabs,
                selectedIndex = if (isExpenseTab) 0 else 1,
                onIndexChange = { index ->
                    viewModel?.handleIntent(StatisticsIntent.ChangeStatisticsTab(if (index == 0) StatisticsTab.EXPENSE else StatisticsTab.INCOME))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppDimens.SpaceLarge)
                    .padding(bottom = AppDimens.SpaceExtraSmall)
            )

            // 2. 根据周期模式切换 HorizontalPager
            if (state.period == StatisticsPeriod.MONTH) {
                HorizontalPager(
                    state = holder.monthPagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val pageOffset = page - PAGER_BASE_INDEX
                    StatisticsContentList(
                        state = state,
                        monthOffset = pageOffset,
                        listState = if (page == holder.monthPagerState.currentPage) holder.listState else rememberLazyListState(),
                        onCategoryClick = onNavigateToTransactions?.let { callback ->
                            { categoryName -> callback(pageOffset, categoryName) }
                        },
                        onDateClick = onNavigateToTransactionsDate?.let { callback ->
                            { day, dateLabel -> callback(pageOffset, day, dateLabel) }
                        },
                        onTransactionClick = onNavigateToTransaction?.let { callback ->
                            { tx -> callback(pageOffset, tx) }
                        },
                        onBudgetClick = onNavigateToBudget?.let { callback ->
                            { callback(pageOffset) }
                        }
                    )
                }
            } else {
                HorizontalPager(
                    state = holder.yearPagerState,
                    modifier = Modifier.weight(1f)
                ) { page ->
                    val pageOffset = page - PAGER_BASE_INDEX
                    AnnualStatisticsContentList(
                        state = state,
                        yearOffset = pageOffset,
                        listState = if (page == holder.yearPagerState.currentPage) holder.listState else rememberLazyListState(),
                        onAnnualCategoryClick = onNavigateToTransactionsAnnualCategory,
                        onTransactionClick = onNavigateToTransaction?.let { callback ->
                            { tx -> callback(0, tx) }
                        },
                        onNavigateToTransactionsMonth = onNavigateToTransactionsMonth
                    )
                }
            }
        }
    }

    // Feature-Level Dialog Host
    StatisticsDialogHost(state = state, viewModel = viewModel)
}
