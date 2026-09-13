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
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Sealed definition of all Global App-Level Overlays (Modals, Floating Bubbles, HUDs).
 * 使用 sealed interface 确保在全局覆盖物(如悬浮窗、HUD)匹配时能够使用 exhaustive(详尽的) when 表达式，
 * 方便未来安全地扩展新的覆盖物类型。
 */
sealed interface AppOverlay {
    data object ApmInspector : AppOverlay
}

/**
 * 全局应用状态持有者 (ExpenseAppState)。
 * 这是一个典型的“指挥家 (Conductor)”或“状态编排器 (State Orchestrator)”模式。
 *
 * 核心职责：
 * 1. 【ViewModel 托管】：统一持有应用中的核心 ViewModel (流水、统计、设置)。
 * 2. 【状态同步】：协调不同页面间的状态同步（例如：切换 Tab 时保持月份一致）。
 * 3. 【全局导航】：管理底部导航栏的 Tab 切换逻辑。
 * 4. 【交互反馈】：持有 SnackbarHostState 供各界面发送通知。
 * 5. 【覆盖物控制】：管理全屏遮罩、悬浮窗等全局 UI 元素。
 */
class ExpenseAppState(
    val transactionsViewModel: TransactionsViewModel,
    val statisticsViewModel: StatisticsViewModel,
    val settingsViewModel: SettingsViewModel,
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
     * 职责：UI 渲染的直接驱动力。它决定了 MainApp 的 Scaffold 中当前显示哪一个业务屏幕。
     */
    var currentTab by mutableStateOf(NavTab.TRANSACTIONS)
        internal set

    /**
     * 【最后的业务上下文 Tab】
     * 职责：状态同步的“锚点”。
     * 为什么需要它：因为“设置 (SETTINGS)”是一个中立 Tab，它不持有月份、年份等金融偏移量状态。
     * 如果用户从 流水 -> 设置 -> 统计，在从设置切到统计的那一刻，同步逻辑需要知道“用户刚才在哪个业务页面”，
     * 从而找到对应的偏移量。此时 lastTimeTab 就指向了“流水”，确保时间同步链条不断裂。
     */
    internal var lastTimeTab: NavTab = NavTab.TRANSACTIONS

    /**
     * 当前活跃的月份偏移量（跨界面同步的主轴）。
     */
    val activeMonthOffset: Int
        get() = if (currentTab == NavTab.STATISTICS) statisticsViewModel.viewState.value.selectedMonthOffset
        else transactionsViewModel.viewState.value.selectedMonthOffset

    /**
     * 当前活跃的年份偏移量。
     */
    val activeYearOffset: Int
        get() = if (currentTab == NavTab.STATISTICS) statisticsViewModel.viewState.value.selectedYearOffset
        else transactionsViewModel.viewState.value.selectedYearOffset

    // 用于保护统计页面的年度视图，防止在特定跳转流程中被重置。
    internal var preserveStatisticsYearOnReturn = false

    /**
     * 【跨页面状态同步核心逻辑】
     * 场景：用户在“流水”页看了 3 月，切到“统计”页也应该看到 3 月。
     * 挑战：不同页面的 Period（月/年）可能不同，需要智能判断。
     *
     * @param fromTab 切出的页面
     * @param toTab 切入的页面
     */
    private fun syncTimeState(fromTab: NavTab, toTab: NavTab) {
        if (fromTab == NavTab.TRANSACTIONS && toTab == NavTab.STATISTICS) {
            val stats = statisticsViewModel.viewState.value
            val tx = transactionsViewModel.viewState.value
            // 特殊逻辑：如果是从统计年度视图下钻到流水月度视图后再回来，保持年度状态。
            if (preserveStatisticsYearOnReturn && stats.period == StatisticsPeriod.YEAR && tx.period == TransactionPeriod.MONTH) {
                preserveStatisticsYearOnReturn = false
                return
            }
            preserveStatisticsYearOnReturn = false
            // 自动同步周期类型（年/月）
            val targetPeriod = if (tx.period == TransactionPeriod.YEAR) StatisticsPeriod.YEAR else StatisticsPeriod.MONTH
            if (stats.period != targetPeriod) statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(targetPeriod))
            
            // 同步偏移量
            if (targetPeriod == StatisticsPeriod.MONTH && stats.selectedMonthOffset != tx.selectedMonthOffset) {
                statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(tx.selectedMonthOffset))
            } else if (targetPeriod == StatisticsPeriod.YEAR && stats.selectedYearOffset != tx.selectedYearOffset) {
                statisticsViewModel.handleIntent(StatisticsIntent.SetYearOffset(tx.selectedYearOffset))
            }
        } else if (fromTab == NavTab.STATISTICS && toTab == NavTab.TRANSACTIONS) {
            preserveStatisticsYearOnReturn = false
            val stats = statisticsViewModel.viewState.value
            val tx = transactionsViewModel.viewState.value
            val targetPeriod = if (stats.period == StatisticsPeriod.YEAR) TransactionPeriod.YEAR else TransactionPeriod.MONTH
            if (tx.period != targetPeriod) transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(targetPeriod))
            
            if (targetPeriod == TransactionPeriod.MONTH && tx.selectedMonthOffset != stats.selectedMonthOffset) {
                transactionsViewModel.handleIntent(TransactionsIntent.SetMonthOffset(stats.selectedMonthOffset))
            } else if (targetPeriod == TransactionPeriod.YEAR && tx.selectedYearOffset != stats.selectedYearOffset) {
                transactionsViewModel.handleIntent(TransactionsIntent.SetYearOffset(stats.selectedYearOffset))
            }
        }
    }

    /**
     * 切换底部导航 Tab。
     * 在切换前执行 [syncTimeState] 确保用户在不同功能间流转时，关注的时间跨度保持连贯。
     */
    fun switchTab(tab: NavTab) {
        if (tab != currentTab) {
            val sourceTab = if (currentTab == NavTab.SETTINGS) lastTimeTab else currentTab
            syncTimeState(fromTab = sourceTab, toTab = tab)
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
     * 当用户双击底部 Tab 时触发。
     */
    private val _scrollToTopEvents = MutableSharedFlow<NavTab>(replay = 0, extraBufferCapacity = 1)
    val scrollToTopEvents = _scrollToTopEvents.asSharedFlow()

    fun triggerScrollToTop(tab: NavTab) {
        _scrollToTopEvents.tryEmit(tab)
        // 同时向对应的 ViewModel 发送 ScrollToTop 意图，确保即使在非 UI 场景也能响应
        when (tab) {
            NavTab.TRANSACTIONS -> transactionsViewModel.handleIntent(TransactionsIntent.ScrollToTop)
            NavTab.STATISTICS -> statisticsViewModel.handleIntent(StatisticsIntent.ScrollToTop)
            NavTab.SETTINGS -> settingsViewModel.handleIntent(SettingsIntent.ScrollToTop)
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
    val statisticsViewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory(app))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(app))

    return remember(transactionsViewModel, statisticsViewModel, settingsViewModel, snackbarHostState) {
        ExpenseAppState(transactionsViewModel, statisticsViewModel, settingsViewModel, snackbarHostState)
    }
}
