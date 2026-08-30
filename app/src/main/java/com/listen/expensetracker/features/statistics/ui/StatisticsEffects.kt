package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsEffect
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * 统计分析画面专用副作用集中调度器 (StatisticsEffects)。
 *
 * 【教学重点 - Compose 副作用与数据流桥接】：
 * 1. [rememberUpdatedState]：将外部动态变化的 `selectedMonthOffset` 转为长协程安全的状态读取引用，
 *    确保协程生命周期只跟随 `viewModel` 与 `pagerState`，避免页面滑动或重组时协程意外重启。
 * 2. [LaunchedEffect] + [launch]：统一父级协程生命周期，利用两组并发协程分别处理“副作用事件流监听”与“手势快照流监听”。
 * 3. [snapshotFlow]：将 Compose 响应式对象转换为标准 Flow，实现手势页面落定（settledPage）后的 MVI Intent 同步。
 * 4. [drop(1)]：过滤组件挂载时的首个默认值，避免初次加载时触发不必要的状态回写。
 */
@Composable
fun StatisticsEffects(
    viewModel: StatisticsViewModel?,
    pagerState: PagerState,
    listState: LazyListState,
    selectedMonthOffset: Int,
    onIntent: (StatisticsIntent) -> Unit
) {
    // 捕获最新月份偏移量，防止协程闭包持有陈旧值
    val currentMonthOffset by rememberUpdatedState(selectedMonthOffset)

    LaunchedEffect(viewModel, pagerState) {
        if (viewModel != null) {
            // 任务 1：监听单次事件副作用（月份跳转、一键置顶等）
            launch {
                viewModel.screenEffect.collectLatest { effect ->
                    when (effect) {
                        is StatisticsEffect.ScrollToMonth -> {
                            val targetPage = PAGER_BASE_INDEX + effect.offset
                            if (pagerState.currentPage != targetPage) {
                                pagerState.scrollToPage(targetPage)
                            }
                        }
                        is StatisticsEffect.ScrollToTop -> {
                            listState.animateScrollToItem(0)
                        }
                    }
                }
            }
        }

        // 任务 2：监听 Pager 手势翻页完成并同步更新状态机中的月份偏移
        launch {
            snapshotFlow { pagerState.settledPage }
                .drop(1)
                .collect { page ->
                    val offset = page - PAGER_BASE_INDEX
                    if (offset != currentMonthOffset) {
                        onIntent(StatisticsIntent.SetMonthOffset(offset))
                    }
                }
        }
    }
}
