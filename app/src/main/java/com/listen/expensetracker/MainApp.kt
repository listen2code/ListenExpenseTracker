package com.listen.expensetracker

import android.os.SystemClock.uptimeMillis
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.listen.expensetracker.core.i18n.LocalAppLanguage
import com.listen.expensetracker.core.i18n.tr
import com.listen.expensetracker.core.route.CommonRoute
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.NavTab
import com.listen.expensetracker.core.state.navigateToBudgetAdjustment
import com.listen.expensetracker.core.state.navigateToTransaction
import com.listen.expensetracker.core.state.navigateToTransactionsAnnualCategory
import com.listen.expensetracker.core.state.navigateToTransactionsCategory
import com.listen.expensetracker.core.state.navigateToTransactionsDate
import com.listen.expensetracker.core.state.navigateToTransactionsMonth
import com.listen.expensetracker.features.settings.ui.SettingsScreen
import com.listen.expensetracker.features.statistics.ui.StatisticsScreen
import com.listen.expensetracker.features.transactions.ui.TransactionsScreen

/**
 * 主界面导航架构与脚手架组件 (App)。
 * 负责底部导航栏 (NavigationBar)、Tab 切换、双击回到顶部手势及 Screen 路由调度。
 *
 * @param appState 统一应用状态持有者
 * @param modifier Composable 修饰符（可选参数）
 * @param lang 当前语言偏好（默认为全局 CompositionLocal [LocalAppLanguage]）
 */
@Composable
fun MainApp(
    appState: ExpenseAppState,
    modifier: Modifier = Modifier,
    lang: String = LocalAppLanguage.current
) {
    // 记录底部导航栏 Tab 上次被点击的时间，用于实现双击回到顶部的交互逻辑（阈值 350 毫秒内有效）
    var lastTabClickTime by remember { mutableLongStateOf(0L) }

    Scaffold(
        // 配置全局的 Snackbar 显示容器
        snackbarHost = { SnackbarHost(appState.snackbarHostState) },
        bottomBar = {
            // 渲染底部导航栏
            NavigationBar {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = appState.currentTab == tab,
                        onClick = {
                            val now = uptimeMillis()
                            if (appState.currentTab == tab) {
                                // 如果当前已在该 Tab 下，则判断是否触发双击回到顶部
                                if (now - lastTabClickTime < 350L) {
                                    appState.triggerScrollToTop(tab)
                                    lastTabClickTime = 0L
                                } else {
                                    lastTabClickTime = now
                                }
                            } else {
                                // 切换到选中的 Tab
                                lastTabClickTime = 0L
                                appState.switchTab(tab)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.labelKey.tr()) },
                        label = { Text(tab.labelKey.tr()) }
                    )
                }
            }
        },
        // 禁用默认的 WindowInsets 处理，改由内部手动控制边距，避免 UI 冲突
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        // 为子页面配置修饰符，主要通过 padding 避开底部导航栏的遮挡
        val screenModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        
        // 【状态持久化容器】
        // 为什么需要它：当在底部导航栏切换 Tab 时，原本的 Screen 会被从 Composition 树中移除。
        // 如果不使用这个 Holder，切换回来时所有 rememberSaveable 的状态（如列表滚动位置、输入框草稿等）都会丢失。
        // 它通过 key (这里是 currentTab) 将 UI 状态手动“挂起”在内存中。
        val saveableStateHolder = rememberSaveableStateHolder()

        // 使用 Provider 包裹内容，它会根据传入的 key 自动保存/恢复内部所有 rememberSaveable 的变量
        saveableStateHolder.SaveableStateProvider(appState.currentTab) {
            // 根据当前选中的导航标签动态渲染对应的业务屏幕
            when (appState.currentTab) {
                // 账单流水明细界面
                NavTab.TRANSACTIONS -> CommonRoute(appState.transactionsViewModel) { state, _ ->
                    TransactionsScreen(
                        state = state,
                        viewModel = appState.transactionsViewModel,
                        modifier = screenModifier
                    )
                }
                // 统计报表与趋势分析界面
                NavTab.STATISTICS -> CommonRoute(appState.statisticsViewModel) { state, _ ->
                    StatisticsScreen(
                        state = state,
                        viewModel = appState.statisticsViewModel,
                        // 【穿透导航逻辑】从统计图表点击后的深度跳转行为
                        // 1. 跳转至特定月份、特定分类的账单列表
                        onNavigateToTransactions = { monthOffset, categoryName ->
                            appState.navigateToTransactionsCategory(categoryName, monthOffset)
                        },
                        // 2. 跳转至年度统计中特定分类的明细列表
                        onNavigateToTransactionsAnnualCategory = { year, categoryName ->
                            appState.navigateToTransactionsAnnualCategory(year, categoryName)
                        },
                        // 3. 跳转至特定日期的账单列表（通常从日历或趋势图点击）
                        onNavigateToTransactionsDate = { monthOffset, day, dateLabel ->
                            appState.navigateToTransactionsDate(monthOffset, day, dateLabel)
                        },
                        // 4. 跳转至单笔账单的具体详情页
                        onNavigateToTransaction = { monthOffset, tx ->
                            appState.navigateToTransaction(monthOffset, tx)
                        },
                        // 5. 跳转至预算设置/调整界面
                        onNavigateToBudget = { monthOffset ->
                            appState.navigateToBudgetAdjustment(monthOffset)
                        },
                        // 6. 跳转至某一整月的账单明细列表
                        onNavigateToTransactionsMonth = { monthOffset ->
                            appState.navigateToTransactionsMonth(monthOffset)
                        },
                        modifier = screenModifier
                    )
                }
                // 系统设置与偏好配置界面
                NavTab.SETTINGS -> CommonRoute(appState.settingsViewModel) { state, _ ->
                    SettingsScreen(
                        state = state,
                        targetMonthOffset = appState.activeMonthOffset,
                        viewModel = appState.settingsViewModel,
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
