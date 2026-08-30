package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.common.components.PAGER_PAGE_COUNT
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel

/**
 * 统计分析画面专用 UI 状态持有者 (StatisticsStateHolder)。
 *
 * Google 官方 UI State Holder 设计模式说明：
 * 1. 【职责解耦】：
 *    - [StatisticsUiState]：存放业务领域只读数据（分类占比图表数据、收支总计、预算进度、趋势图点位），由 ViewModel 状态机持有；
 *    - [StatisticsStateHolder]：承载界面控件与动画调度状态（PagerState, LazyListState, 滚动计算, 副作用触发），生命周期与 Compose 树绑定。
 * 2. 【杜绝内存泄漏】：严禁将 PagerState/LazyListState 放入 ViewModel；
 * 3. 【极致纯净的 Screen】：将状态初始化与效应调度全部收拢在此，使 Screen 函数专注于纯声明式视图渲染。
 */
class StatisticsStateHolder(
    val pagerState: PagerState,
    val listState: LazyListState,
    val currentMonthTitle: String,
    val currentMonthOffset: Int
)

/**
 * 创建并记住 [StatisticsStateHolder] 的 Composable 辅助函数。
 *
 * 
 * - 使用 [rememberPagerState] 支撑统计页横向月份滑动切换；
 * - 使用 [rememberSaveable] 配合 [LazyListState.Saver] 实现列表位置记忆与恢复；
 * - 内部挂载 [StatisticsEffects]，避免向 Screen 暴露杂乱的效应监听逻辑。
 */
@Composable
fun rememberStatisticsStateHolder(
    state: StatisticsUiState,
    onIntent: (StatisticsIntent) -> Unit,
    viewModel: StatisticsViewModel? = null
): StatisticsStateHolder {
    val lang = state.language

    // 1. 初始化 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = PAGER_BASE_INDEX + state.selectedMonthOffset,
        pageCount = { PAGER_PAGE_COUNT }
    )

    // 2. 初始化列表滚动状态并绑定 Saver
    val listState = rememberSaveable(pagerState.currentPage, saver = LazyListState.Saver) {
        LazyListState()
    }

    // 3. 根据当前滑动手势或状态机实时计算顶部胶囊标题与月份偏移（避免跨 Tab 切换时的闪烁）
    val activeOffset = if (pagerState.isScrollInProgress) {
        pagerState.currentPage - PAGER_BASE_INDEX
    } else {
        state.selectedMonthOffset
    }
    val (_, _, currentMonthTitle) = remember(activeOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(activeOffset, lang)
    }

    // 4. 挂载画面专用副作用与手势监听
    StatisticsEffects(
        viewModel = viewModel,
        pagerState = pagerState,
        listState = listState,
        selectedMonthOffset = state.selectedMonthOffset,
        onIntent = onIntent
    )

    return remember(pagerState, listState, currentMonthTitle, activeOffset) {
        StatisticsStateHolder(
            pagerState = pagerState,
            listState = listState,
            currentMonthTitle = currentMonthTitle,
            currentMonthOffset = activeOffset
        )
    }
}
