package com.listen.expensetracker.core.state

import android.app.Application
import android.content.Intent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sealed definition of all Global App-Level Overlays (Modals, Floating Bubbles, HUDs).
 */
sealed interface AppOverlay {
    data object ApmInspector : AppOverlay
}

/**
 * 全局应用 UI 状态与导航持有者 (ExpenseAppState)。
 * 纯粹负责 UI 导航状态机 (当前 Tab、SnackbarHostState、意图管道与置顶事件流)。
 * 不再充当全量 ViewModel 容器，实现 Screen 与 ViewModel 生命周期对齐。
 */
class ExpenseAppState(
    val transactionsViewModel: TransactionsViewModel,
    val snackbarHostState: SnackbarHostState
) {
    /**
     * 系统级意图管道 (Intent Channel)。
     * 用于捕获外部触发的行为（如：小组件点击、DeepLink 唤起）。
     */
    private val _intentChannel = Channel<Intent>(capacity = Channel.CONFLATED)
    val intentChannel = _intentChannel

    fun sendIntent(intent: Intent) {
        _intentChannel.trySend(intent)
    }

    /**
     * 首屏就绪标记。
     * 主要用于控制 Android 系统启动页 (SplashScreen) 的保持时间。
     */
    var isInitialReady by mutableStateOf(false)
        private set

    fun markReady() {
        if (!isInitialReady) isInitialReady = true
    }

    /**
     * 【当前活跃 Tab】
     * 驱动 MainApp 渲染对应界面的单一事实来源。
     */
    var currentTab by mutableStateOf(NavTab.TRANSACTIONS)
        internal set

    /**
     * 最后的业务上下文 Tab（用于保持导航连贯性）。
     */
    internal var lastTimeTab: NavTab = NavTab.TRANSACTIONS

    /**
     * 待触发检查更新的版本号（从 DeepLink 传入后在进入设置页时消费）。
     */
    var targetUpdateVersion by mutableStateOf<String?>(null)

    /**
     * 当前活跃的月份偏移量（由主流水数据中心直接提供）。
     */
    val activeMonthOffset: Int
        get() = transactionsViewModel.viewState.value.selectedMonthOffset

    /**
     * 当前活跃的年份偏移量。
     */
    val activeYearOffset: Int
        get() = transactionsViewModel.viewState.value.selectedYearOffset

    /**
     * 切换底部导航 Tab。
     */
    fun switchTab(tab: NavTab) {
        if (tab != currentTab) {
            if (tab != NavTab.SETTINGS) lastTimeTab = tab
            currentTab = tab
        }
    }

    /**
     * 全局覆盖物状态。
     */
    var activeOverlay by mutableStateOf<AppOverlay?>(null)
        private set

    fun openOverlay(overlay: AppOverlay) { activeOverlay = overlay }
    fun dismissOverlay() { activeOverlay = null }

    /**
     * 回到顶部事件流。
     * 当用户双击底部 Tab 时触发，各 Screen 的 Effects 独立消费。
     */
    private val _scrollToTopEvents = MutableSharedFlow<NavTab>(replay = 0, extraBufferCapacity = 1)
    val scrollToTopEvents = _scrollToTopEvents.asSharedFlow()

    fun triggerScrollToTop(tab: NavTab) {
        _scrollToTopEvents.tryEmit(tab)
        if (tab == NavTab.TRANSACTIONS) {
            transactionsViewModel.handleIntent(TransactionsIntent.ScrollToTop)
        }
    }
}

/**
 * 在 Composable 函数中创建并记住整个应用的全局状态实例。
 */
@Composable
fun rememberExpenseAppState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): ExpenseAppState {
    val app = LocalContext.current.applicationContext as Application
    val transactionsViewModel: TransactionsViewModel = viewModel(factory = TransactionsViewModel.Factory(app))

    return remember(transactionsViewModel, snackbarHostState) {
        ExpenseAppState(transactionsViewModel, snackbarHostState)
    }
}
