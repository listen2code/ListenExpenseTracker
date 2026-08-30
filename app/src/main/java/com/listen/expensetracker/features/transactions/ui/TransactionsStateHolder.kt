package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.common.components.PAGER_PAGE_COUNT
import com.listen.expensetracker.features.transactions.components.formatDayGroupHeader
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel

/**
 * 流水画面专用 UI 状态持有者 (TransactionsStateHolder)。
 *
 * Google 官方 UI State Holder 设计模式说明：
 * 1. 【职责解耦】：
 *    - [TransactionsUiState]：存放业务领域只读数据（账单、预算、搜索关键字等），由 ViewModel 管理，支持 JVM 纯净单测；
 *    - [TransactionsStateHolder]：存放 Compose 控件状态与动画协调（PagerState, LazyListState, 滚动计算, 副作用触发），生命周期跟随 Compose 树。
 * 2. 【杜绝内存泄漏】：严禁将 PagerState/LazyListState 等持有 Compose 布局引用的对象放入 ViewModel；
 * 3. 【极致纯净的 Screen】：将状态初始化与效应调度全部收拢在此，使 Screen Composable 开门见山只写 UI 布局。
 */
class TransactionsStateHolder(
    val pagerState: PagerState,
    val listState: LazyListState,
    val groupedTransactions: Map<String, List<TransactionEntity>>,
    val currentMonthTitle: String,
    val currentMonthOffset: Int
)

/**
 * 创建并记住 [TransactionsStateHolder] 的 Composable 辅助函数。
 *
 * 
 * - 使用 [rememberPagerState] 与虚拟基准页 [PAGER_BASE_INDEX] 支撑双向无限滑动；
 * - 使用 [rememberSaveable] 配合 [LazyListState.Saver] 实现进程死亡或配置变更后的列表位置恢复；
 * - 在状态容器内部安全挂载 [TransactionsEffects]，避免向 Screen 暴露杂乱的效应监听逻辑。
 */
@Composable
fun rememberTransactionsStateHolder(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    viewModel: TransactionsViewModel? = null
): TransactionsStateHolder {
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

    // 3. 动态按天分组流水，利用 remember 避免不必要的集合重算
    val groupedTransactions = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    // 4. 根据当前滑动手势或状态机实时计算顶部胶囊标题与月份偏移（避免跨 Tab 切换时的闪烁）
    val activeOffset = if (pagerState.isScrollInProgress) {
        pagerState.currentPage - PAGER_BASE_INDEX
    } else {
        state.selectedMonthOffset
    }
    val (_, _, currentMonthTitle) = remember(activeOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(activeOffset, lang)
    }

    // 5. 挂载画面专用副作用与手势监听
    TransactionsEffects(
        viewModel = viewModel,
        pagerState = pagerState,
        listState = listState,
        groupedTransactions = groupedTransactions,
        selectedMonthOffset = state.selectedMonthOffset,
        onIntent = onIntent
    )

    return remember(pagerState, listState, groupedTransactions, currentMonthTitle, activeOffset) {
        TransactionsStateHolder(
            pagerState = pagerState,
            listState = listState,
            groupedTransactions = groupedTransactions,
            currentMonthTitle = currentMonthTitle,
            currentMonthOffset = activeOffset
        )
    }
}
