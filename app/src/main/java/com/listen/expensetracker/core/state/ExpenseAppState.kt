package com.listen.expensetracker.core.state

import android.app.Application
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Calendar

/**
 * Type-safe Navigation Tab definitions for ListenExpenseTracker.
 * 使用类型安全的枚举替代魔术数字(0, 1, 2)进行 Tab 导航，提升可读性并防止越界错误。
 */
enum class NavTab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector
) {
    TRANSACTIONS("transactions", "nav_transactions", Icons.AutoMirrored.Filled.List),
    STATISTICS("statistics", "nav_statistics", Icons.Default.PieChart),
    SETTINGS("settings", "nav_settings", Icons.Default.Settings)
}

/**
 * Sealed definition of all Global App-Level Overlays (Modals, Floating Bubbles, HUDs).
 * 使用 sealed interface 确保在全局覆盖物(如悬浮窗、HUD)匹配时能够使用 exhaustive(详尽的) when 表达式，
 * 方便未来安全地扩展新的覆盖物类型。
 */
sealed interface AppOverlay {
    data object ApmInspector : AppOverlay
}

/**
 * Clean Application State Holder coordinating ViewModels, Navigation Tabs, SnackbarHostState, and Global Overlays.
 * 经典的“指挥家 (Conductor)”模式：它本身不是 ViewModel，但作为顶层状态持有者，
 * 负责统筹并协调 3 个主要的 ViewModel 和 SnackbarHostState 的交互。
 */
class ExpenseAppState(
    val transactionsViewModel: TransactionsViewModel,
    val statisticsViewModel: StatisticsViewModel,
    val settingsViewModel: SettingsViewModel,
    val snackbarHostState: SnackbarHostState
) {
    /**
     * Active navigation tab state.
     */
    var currentTab by mutableStateOf(NavTab.TRANSACTIONS)
        private set

    /**
     * Currently active month offset synchronized across Transactions and Statistics screens.
     */
    val activeMonthOffset: Int
        get() = if (currentTab == NavTab.STATISTICS) statisticsViewModel.viewState.value.selectedMonthOffset
        else transactionsViewModel.viewState.value.selectedMonthOffset

    val activeYearOffset: Int
        get() = if (currentTab == NavTab.STATISTICS) statisticsViewModel.viewState.value.selectedYearOffset
        else transactionsViewModel.viewState.value.selectedYearOffset

    // 追踪上一个“非设置”的 Tab。因为“设置(Settings)”是一个中立的 Tab，
    // 不具备时间流属性，因此不应该参与时间状态的同步。
    private var lastTimeTab: NavTab = NavTab.TRANSACTIONS
    private var preserveStatisticsYearOnReturn = false

    /**
     * 核心复杂逻辑：在“流水(Transactions)”和“统计(Statistics)”之间切换时，执行双向的月份/年份偏移量同步。
     * [preserveStatisticsYearOnReturn] 标志位的作用：
     * 当用户在“统计”页面的年度视图中，下钻点击某个月份进入“流水”页面查看细节时，
     * 我们必须保护“统计”页面的年度视图状态，确保当他们返回时，不会被错误地同步为月度视图。
     */
    private fun syncTimeState(fromTab: NavTab, toTab: NavTab) {
        if (fromTab == NavTab.TRANSACTIONS && toTab == NavTab.STATISTICS) {
            val stats = statisticsViewModel.viewState.value
            val tx = transactionsViewModel.viewState.value
            if (preserveStatisticsYearOnReturn && stats.period == StatisticsPeriod.YEAR && tx.period == TransactionPeriod.MONTH) {
                preserveStatisticsYearOnReturn = false
                return
            }
            preserveStatisticsYearOnReturn = false
            val targetPeriod = if (tx.period == TransactionPeriod.YEAR) StatisticsPeriod.YEAR else StatisticsPeriod.MONTH
            if (stats.period != targetPeriod) statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(targetPeriod))
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

    fun switchTab(tab: NavTab) {
        if (tab != currentTab) {
            // 当当前 Tab 是中立的“设置”时，使用 lastTimeTab 作为同步起点，保证时间同步链条不断裂
            val sourceTab = if (currentTab == NavTab.SETTINGS) lastTimeTab else currentTab
            syncTimeState(fromTab = sourceTab, toTab = tab)
            if (tab != NavTab.SETTINGS) lastTimeTab = tab
            currentTab = tab
        }
    }

    fun navigateToTransactionsCategory(categoryName: String, monthOffset: Int) {
        preserveStatisticsYearOnReturn = false
        statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByCategory(categoryName, monthOffset))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }

    fun navigateToTransactionsAnnualCategory(year: Int, categoryName: String) {
        preserveStatisticsYearOnReturn = false
        val curYear = Calendar.getInstance().get(Calendar.YEAR)
        statisticsViewModel.handleIntent(StatisticsIntent.SetYearOffset(year - curYear))
        statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.YEAR))
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByAnnualCategory(year, categoryName))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }

    fun navigateToTransactionsDate(monthOffset: Int, day: Int, dateLabel: String = "") {
        preserveStatisticsYearOnReturn = false
        statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByDate(monthOffset, day, dateLabel))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }

    fun navigateToTransaction(monthOffset: Int, transaction: TransactionEntity) {
        preserveStatisticsYearOnReturn = false
        statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        val cal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByTransaction(monthOffset, transaction.id, cal.get(Calendar.DAY_OF_MONTH), transaction.amount))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }

    fun navigateToBudgetAdjustment(monthOffset: Int) {
        preserveStatisticsYearOnReturn = false
        statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        transactionsViewModel.handleIntent(TransactionsIntent.SetMonthOffset(monthOffset))
        transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
        transactionsViewModel.handleIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }

    fun navigateToTransactionsMonth(monthOffset: Int) {
        // 这是整个应用中唯一将该标志位置为 true 的地方，用于标识发生了“从年度下钻到月度”的行为
        preserveStatisticsYearOnReturn = true
        // [Feature] 从年度收支总览/各月走势穿透到流水画面时，清除即存的筛选条件（搜索词、分类、账户、类型、金额等），确保完整展示该月份全量流水
        transactionsViewModel.handleIntent(TransactionsIntent.ResetAllFilters)
        transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
        transactionsViewModel.handleIntent(TransactionsIntent.SelectMonth(monthOffset))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }

    /**
     * Top-level active overlay state. Controlled entirely via openOverlay / dismissOverlay.
     */
    var activeOverlay by mutableStateOf<AppOverlay?>(null)
        private set

    fun openOverlay(overlay: AppOverlay) { activeOverlay = overlay }
    fun dismissOverlay() { activeOverlay = null }

    /**
     * One-time event flow for scrolling a specific tab's list to top on double-tap.
     * replay = 0 ensures no replay occurs when re-entering tabs.
     * 使用 replay=0 防止在重新订阅(如重新进入 Tab)时收到过期的旧事件；
     * 设置 extraBufferCapacity=1 允许 tryEmit 在非挂起上下文中成功发射事件。
     */
    private val _scrollToTopEvents = MutableSharedFlow<NavTab>(replay = 0, extraBufferCapacity = 1)
    val scrollToTopEvents = _scrollToTopEvents.asSharedFlow()

    fun triggerScrollToTop(tab: NavTab) {
        _scrollToTopEvents.tryEmit(tab)
        when (tab) {
            NavTab.TRANSACTIONS -> transactionsViewModel.handleIntent(TransactionsIntent.ScrollToTop)
            NavTab.STATISTICS -> statisticsViewModel.handleIntent(StatisticsIntent.ScrollToTop)
            NavTab.SETTINGS -> settingsViewModel.handleIntent(SettingsIntent.ScrollToTop)
        }
    }

    /**
     * 快捷拉起记账弹窗，预选指定分类与收支类型
     */
    fun openQuickAdd(
        categoryId: String? = null,
        type: String = TransactionType.EXPENSE
    ) {
        switchTab(NavTab.TRANSACTIONS)
        transactionsViewModel.handleIntent(
            TransactionsIntent.OpenDialog(
                TransactionsDialog.AddTransaction(initialCategoryId = categoryId, initialType = type)
            )
        )
    }
}

/**
 * Remembers and provisions all feature ViewModels, UI state holders, and Overlay manager.
 * 通过带有 Factory 的 viewModel() 函数集中创建所有 3 个 ViewModel，
 * 并确保它们在应用的生命周期内保持单一实例(Single-instance lifecycle)。
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
