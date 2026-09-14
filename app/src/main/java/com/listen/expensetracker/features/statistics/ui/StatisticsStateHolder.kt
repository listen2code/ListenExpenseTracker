package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.listen.expensetracker.data.engine.AnnualCalculationEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.common.components.PAGER_PAGE_COUNT
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
/**
 * 统计分析画面专用 UI 状态持有者 (StatisticsStateHolder)。
 *
 * 【架构设计】：
 * 遵循 Google 官方推荐的 "Plain State Holder" 模式。
 * 核心职责：仅负责持有 UI 自身的交互状态（Month/Year PagerState, LazyListState, 标题计算）。
 * 副作用与手势协同监听在 [StatisticsEffects] 中由 Screen 独立挂载，与本状态容器职责严格区分。
 */
class StatisticsStateHolder(
    val monthPagerState: PagerState,
    val yearPagerState: PagerState,
    val listState: LazyListState,
    val currentMonthTitle: String,
    val currentMonthOffset: Int,
    val currentYearTitle: String,
    val currentYearOffset: Int
)

/**
 * 创建并记住 [StatisticsStateHolder] 的 Composable 辅助函数。
 */
@Composable
fun rememberStatisticsStateHolder(
    state: StatisticsUiState
): StatisticsStateHolder {
    val lang = state.language

    // 1. 初始化月度 Pager 状态
    val monthPagerState = rememberPagerState(
        initialPage = PAGER_BASE_INDEX + state.selectedMonthOffset,
        pageCount = { PAGER_PAGE_COUNT }
    )
    val targetMonthPage = PAGER_BASE_INDEX + state.selectedMonthOffset
    if (monthPagerState.currentPage != targetMonthPage && !monthPagerState.isScrollInProgress) {
        monthPagerState.requestScrollToPage(targetMonthPage)
    }

    // 2. 初始化年度 Pager 状态
    val yearPagerState = rememberPagerState(
        initialPage = PAGER_BASE_INDEX + state.selectedYearOffset,
        pageCount = { PAGER_PAGE_COUNT }
    )
    val targetYearPage = PAGER_BASE_INDEX + state.selectedYearOffset
    if (yearPagerState.currentPage != targetYearPage && !yearPagerState.isScrollInProgress) {
        yearPagerState.requestScrollToPage(targetYearPage)
    }

    // 3. 初始化列表滚动状态并绑定 Saver
    val listState = rememberSaveable(monthPagerState.currentPage, yearPagerState.currentPage, saver = LazyListState.Saver) {
        LazyListState()
    }

    // 4. 根据当前滑动手势或状态机实时计算顶部胶囊标题与月份/年份偏移
    val activeMonthOffset = if (monthPagerState.isScrollInProgress) {
        monthPagerState.currentPage - PAGER_BASE_INDEX
    } else {
        state.selectedMonthOffset
    }
    val (_, _, currentMonthTitle) = remember(activeMonthOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(activeMonthOffset, lang)
    }

    val activeYearOffset = if (yearPagerState.isScrollInProgress) {
        yearPagerState.currentPage - PAGER_BASE_INDEX
    } else {
        state.selectedYearOffset
    }
    val (_, _, currentYearTitle) = remember(activeYearOffset, lang) {
        AnnualCalculationEngine.getYearRangeAndTitle(activeYearOffset, lang)
    }

    return remember(
        monthPagerState, yearPagerState, listState,
        currentMonthTitle, activeMonthOffset,
        currentYearTitle, activeYearOffset
    ) {
        StatisticsStateHolder(
            monthPagerState = monthPagerState,
            yearPagerState = yearPagerState,
            listState = listState,
            currentMonthTitle = currentMonthTitle,
            currentMonthOffset = activeMonthOffset,
            currentYearTitle = currentYearTitle,
            currentYearOffset = activeYearOffset
        )
    }
}
