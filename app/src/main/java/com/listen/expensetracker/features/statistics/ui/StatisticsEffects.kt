package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsEffect
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * 统计分析画面专用副作用集中调度器 (StatisticsEffects)。
 * 统一承接按月与按年模式下的 Pager 联动、单次副作用监听与手势快照同步。
 */
@Composable
fun StatisticsEffects(
    viewModel: StatisticsViewModel?,
    monthPagerState: PagerState,
    yearPagerState: PagerState,
    listState: LazyListState,
    period: StatisticsPeriod,
    selectedMonthOffset: Int,
    selectedYearOffset: Int,
    onIntent: (StatisticsIntent) -> Unit
) {
    val currentPeriod by rememberUpdatedState(period)
    val currentMonthOffset by rememberUpdatedState(selectedMonthOffset)
    val currentYearOffset by rememberUpdatedState(selectedYearOffset)

    LaunchedEffect(viewModel, monthPagerState, yearPagerState) {
        // 任务 1：监听外部月份变更并同步对齐月度 Pager
        launch {
            snapshotFlow { currentMonthOffset }.collectLatest { offset ->
                val targetPage = PAGER_BASE_INDEX + offset
                if (monthPagerState.currentPage != targetPage) {
                    if (kotlin.math.abs(monthPagerState.currentPage - targetPage) <= 3) {
                        monthPagerState.animateScrollToPage(targetPage)
                    } else {
                        monthPagerState.scrollToPage(targetPage)
                    }
                }
            }
        }

        // 任务 2：监听外部年份变更并同步对齐年度 Pager
        launch {
            snapshotFlow { currentYearOffset }.collectLatest { offset ->
                val targetPage = PAGER_BASE_INDEX + offset
                if (yearPagerState.currentPage != targetPage) {
                    if (kotlin.math.abs(yearPagerState.currentPage - targetPage) <= 3) {
                        yearPagerState.animateScrollToPage(targetPage)
                    } else {
                        yearPagerState.scrollToPage(targetPage)
                    }
                }
            }
        }

        if (viewModel != null) {
            // 任务 3：监听单次事件副作用（月份/年份跳转、一键置顶等）
            launch {
                viewModel.viewEffect.filterIsInstance<StatisticsEffect>().collectLatest { effect ->
                    when (effect) {
                        is StatisticsEffect.ScrollToMonth -> {
                            val targetPage = PAGER_BASE_INDEX + effect.offset
                            if (monthPagerState.currentPage != targetPage) {
                                if (kotlin.math.abs(monthPagerState.currentPage - targetPage) <= 3) {
                                    monthPagerState.animateScrollToPage(targetPage)
                                } else {
                                    monthPagerState.scrollToPage(targetPage)
                                }
                            }
                        }
                        is StatisticsEffect.ScrollToYear -> {
                            val targetPage = PAGER_BASE_INDEX + effect.offset
                            if (yearPagerState.currentPage != targetPage) {
                                if (kotlin.math.abs(yearPagerState.currentPage - targetPage) <= 3) {
                                    yearPagerState.animateScrollToPage(targetPage)
                                } else {
                                    yearPagerState.scrollToPage(targetPage)
                                }
                            }
                        }
                        is StatisticsEffect.ScrollToTop -> {
                            val isAtTop = !listState.isScrollInProgress &&
                                listState.firstVisibleItemIndex == 0 &&
                                listState.firstVisibleItemScrollOffset == 0
                            if (isAtTop) {
                                if (currentPeriod == StatisticsPeriod.MONTH) {
                                    if (currentMonthOffset != 0 || monthPagerState.currentPage != PAGER_BASE_INDEX) {
                                        onIntent(StatisticsIntent.SelectMonth(0))
                                    }
                                } else {
                                    if (currentYearOffset != 0 || yearPagerState.currentPage != PAGER_BASE_INDEX) {
                                        onIntent(StatisticsIntent.SelectYear(0))
                                    }
                                }
                            } else {
                                listState.animateScrollToItem(0)
                            }
                        }
                    }
                }
            }
        }

        // 任务 4：监听月度 Pager 手势翻页完成并同步更新状态机中的月份偏移
        launch {
            snapshotFlow { monthPagerState.settledPage }
                .drop(1)
                .collect { page ->
                    val offset = page - PAGER_BASE_INDEX
                    if (offset != currentMonthOffset) {
                        onIntent(StatisticsIntent.SetMonthOffset(offset))
                    }
                }
        }

        // 任务 5：监听年度 Pager 手势翻页完成并同步更新状态机中的年份偏移
        launch {
            snapshotFlow { yearPagerState.settledPage }
                .drop(1)
                .collect { page ->
                    val offset = page - PAGER_BASE_INDEX
                    if (offset != currentYearOffset) {
                        onIntent(StatisticsIntent.SetYearOffset(offset))
                    }
                }
        }
    }
}
