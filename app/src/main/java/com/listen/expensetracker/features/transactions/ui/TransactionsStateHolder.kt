package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.AnnualCalculationEngine
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
    val monthPagerState: PagerState,
    val yearPagerState: PagerState,
    val listState: LazyListState,
    val groupedTransactions: Map<String, List<TransactionEntity>>,
    val currentMonthTitle: String,
    val currentMonthOffset: Int,
    val currentYearTitle: String,
    val currentYearOffset: Int
) {
    val pagerState: PagerState get() = monthPagerState
}

/**
 * 创建并记住 [TransactionsStateHolder] 的 Composable 辅助函数。
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

    // 4. 动态按天分组流水，利用 remember 避免不必要的集合重算
    val groupedTransactions = remember(state.filteredTransactions) {
        state.filteredTransactions.groupBy { formatDayGroupHeader(it.timestamp) }
    }

    // 5. 根据当前滑动手势或状态机实时计算顶部胶囊标题与月份/年份偏移
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

    // 6. 挂载画面专用副作用与手势监听
    TransactionsEffects(
        viewModel = viewModel,
        monthPagerState = monthPagerState,
        yearPagerState = yearPagerState,
        listState = listState,
        groupedTransactions = groupedTransactions,
        selectedMonthOffset = state.selectedMonthOffset,
        selectedYearOffset = state.selectedYearOffset,
        onIntent = onIntent
    )

    return remember(
        monthPagerState, yearPagerState, listState, groupedTransactions,
        currentMonthTitle, activeMonthOffset,
        currentYearTitle, activeYearOffset
    ) {
        TransactionsStateHolder(
            monthPagerState = monthPagerState,
            yearPagerState = yearPagerState,
            listState = listState,
            groupedTransactions = groupedTransactions,
            currentMonthTitle = currentMonthTitle,
            currentMonthOffset = activeMonthOffset,
            currentYearTitle = currentYearTitle,
            currentYearOffset = activeYearOffset
        )
    }
}
