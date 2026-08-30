package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.statistics.components.StatisticsContentList
import com.listen.expensetracker.features.statistics.components.StatisticsDialogHost
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsTab
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.uicomponent.components.BaseScreenScaffold
import com.listen.uicomponent.components.CommonSegmentedControl

/**
 * 纯无状态统计分析主画面 (StatisticsScreen)。
 *
 * Google 官方 UI State Holder 架构规范：
 * 1. 业务只读数据由 [state] ([StatisticsUiState]) 纯数据类驱动；
 * 2. 交互状态与动画控制器（PagerState、LazyListState、副作用监听）统一由 [rememberStatisticsStateHolder] 承接；
 * 3. 顶部收支切换栏（Expense / Income Toggle）与底部图表卡片彻底解耦。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onIntent: (StatisticsIntent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel? = null,
    onNavigateToTransactions: ((monthOffset: Int, categoryName: String) -> Unit)? = null,
    onNavigateToTransactionsDate: ((monthOffset: Int, day: Int, dateLabel: String) -> Unit)? = null,
    onNavigateToTransaction: ((monthOffset: Int, transaction: TransactionEntity) -> Unit)? = null
) {
    // 🌟 一行收口所有 Pager、ListState 与副作用协同逻辑
    val holder = rememberStatisticsStateHolder(state, onIntent, viewModel)
    val lang = state.language
    val isExpenseTab = state.statisticsTab == StatisticsTab.EXPENSE

    BaseScreenScaffold(
        titleSlot = {
            MonthNavigationCapsule(
                monthTitle = holder.currentMonthTitle,
                onPreviousMonth = {
                    onIntent(StatisticsIntent.SelectMonth(holder.currentMonthOffset - 1))
                },
                onNextMonth = {
                    onIntent(StatisticsIntent.SelectMonth(holder.currentMonthOffset + 1))
                },
                onTitleClick = { onIntent(StatisticsIntent.OpenMonthPicker) }
            )
        },
        actions = {
            IconButton(onClick = { onIntent(StatisticsIntent.ToggleHideAmount(!state.hideAmount)) }) {
                Icon(
                    imageVector = if (state.hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Amount",
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
            // 1. Pinned Top Expense vs Income Segmented Toggle (Stationary)
            val tabs = listOf(AppStrings.TAB_EXPENSE_ANALYSIS.tr(lang), AppStrings.TAB_INCOME_ANALYSIS.tr(lang))
            CommonSegmentedControl(
                items = tabs,
                selectedIndex = if (isExpenseTab) 0 else 1,
                onIndexChange = { index ->
                    onIntent(StatisticsIntent.ChangeStatisticsTab(if (index == 0) StatisticsTab.EXPENSE else StatisticsTab.INCOME))
                },
                modifier = Modifier
                    .padding(horizontal = AppDimens.SpaceLarge)
                    .padding(bottom = AppDimens.SpaceExtraSmall)
            )

            // 2. Horizontal PageView Slider with month-specific analytics calculation
            HorizontalPager(
                state = holder.pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset = page - PAGER_BASE_INDEX
                StatisticsContentList(
                    state = state,
                    monthOffset = pageOffset,
                    onIntent = onIntent,
                    listState = if (page == holder.pagerState.currentPage) holder.listState else rememberLazyListState(),
                    onCategoryClick = onNavigateToTransactions?.let { callback ->
                        { categoryName -> callback(pageOffset, categoryName) }
                    },
                    onDateClick = onNavigateToTransactionsDate?.let { callback ->
                        { day, dateLabel -> callback(pageOffset, day, dateLabel) }
                    },
                    onTransactionClick = onNavigateToTransaction?.let { callback ->
                        { tx -> callback(pageOffset, tx) }
                    }
                )
            }
        }
    }

    // Feature-Level Dialog Host
    StatisticsDialogHost(state = state, onIntent = onIntent)
}
