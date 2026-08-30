package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.transactions.components.calculateDayScrollIndex
import com.listen.expensetracker.features.transactions.components.calculateTransactionScrollIndex
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsEffect
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * 流水画面专用副作用集中调度器 (TransactionsEffects)。
 *
 * 【教学重点 - Compose 副作用与协程协同】：
 * 1. [rememberUpdatedState]：解决协程闭包捕获旧值问题。由于内部 `LaunchedEffect` 的 key 仅为 `viewModel` 和 `pagerState`，
 *    当外部重组传入新的 `groupedTransactions` 或 `selectedMonthOffset` 时，长协程不会被销毁重启，
 *    但通过 `rememberUpdatedState` 保证了在协程中始终能读取到最新的状态值。
 * 2. [LaunchedEffect] + [launch]：统一作用域管理。通过一个 LaunchedEffect 启动父协程，
 *    内部使用 `launch` 并发执行“副作用消费”与“滑动监听”，生命周期天然绑定、避免多次重启。
 * 3. [snapshotFlow]：Compose 核心桥接 API。负责将 Compose 的响应式快照状态（如 `pagerState.settledPage`）转换为标准的 Cold Flow。
 * 4. [drop(1)]：忽略初始默认发射值，防止进入页面时触发非用户操作的冗余 Intent。
 * 5. [collectLatest]：新事件到达时自动取消未完成的上一轮挂起滚动操作，保证高频快速点击时的操作平滑度。
 */
@Composable
fun TransactionsEffects(
    viewModel: TransactionsViewModel?,
    pagerState: PagerState,
    listState: LazyListState,
    groupedTransactions: Map<String, List<TransactionEntity>>,
    selectedMonthOffset: Int,
    onIntent: (TransactionsIntent) -> Unit
) {
    // 使用 rememberUpdatedState 保持长协程引用最新状态，避免 LaunchedEffect 因参数变动频繁重启
    val currentGroupedTransactions by rememberUpdatedState(groupedTransactions)
    val currentMonthOffset by rememberUpdatedState(selectedMonthOffset)

    LaunchedEffect(viewModel, pagerState) {
        if (viewModel != null) {
            // 任务 1：监听 ViewModel 发射的单次 UI 副作用（滚动定位等）
            launch {
                viewModel.screenEffect.collectLatest { effect ->
                    when (effect) {
                        is TransactionsEffect.ScrollToMonth -> {
                            val targetPage = PAGER_BASE_INDEX + effect.offset
                            if (pagerState.currentPage != targetPage) {
                                pagerState.scrollToPage(targetPage)
                            }
                        }
                        is TransactionsEffect.ScrollToTop -> {
                            listState.animateScrollToItem(0)
                        }
                        is TransactionsEffect.ScrollToTransaction -> {
                            val targetIndex = calculateTransactionScrollIndex(currentGroupedTransactions, effect.txId)
                            if (targetIndex != -1) {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                        is TransactionsEffect.ScrollToDay -> {
                            val targetIndex = calculateDayScrollIndex(currentGroupedTransactions, effect.day)
                            if (targetIndex != -1) {
                                listState.animateScrollToItem(targetIndex)
                            }
                        }
                    }
                }
            }
        }

        // 任务 2：将 Pager 滑动停止事件转化为 MVI Intent 同步至 ViewModel
        launch {
            snapshotFlow { pagerState.settledPage }
                .drop(1) // 忽略初次创建时的第 1 次发射
                .collect { page ->
                    val offset = page - PAGER_BASE_INDEX
                    if (offset != currentMonthOffset) {
                        onIntent(TransactionsIntent.SetMonthOffset(offset))
                    }
                }
        }
    }
}
