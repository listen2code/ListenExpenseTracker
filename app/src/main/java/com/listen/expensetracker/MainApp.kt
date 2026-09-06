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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.route.CommonRoute
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.NavTab
import com.listen.expensetracker.features.settings.ui.SettingsScreen
import com.listen.expensetracker.features.statistics.ui.StatisticsScreen
import com.listen.expensetracker.features.transactions.ui.TransactionsScreen

/**
 * 主界面导航架构与脚手架组件 (App)。
 * 负责底部导航栏 (NavigationBar)、Tab 切换、双击回到顶部手势及 Screen 路由调度。
 *
 * @param appState 统一应用状态持有者
 * @param modifier Composable 修饰符（首个可选参数）
 */
@Composable
fun App(
    appState: ExpenseAppState,
    modifier: Modifier = Modifier
) {
    val settingsState by appState.settingsViewModel.viewState.collectAsState()
    val lang = settingsState.language

    // Double-tap tracking on active navigation tab (threshold: 350ms)
    var lastTabClickTime by remember { mutableLongStateOf(0L) }

    Scaffold(
        snackbarHost = { SnackbarHost(appState.snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = appState.currentTab == tab,
                        onClick = {
                            val now = uptimeMillis()
                            if (appState.currentTab == tab) {
                                if (now - lastTabClickTime < 350L) {
                                    appState.triggerScrollToTop(tab)
                                    lastTabClickTime = 0L
                                } else {
                                    lastTabClickTime = now
                                }
                            } else {
                                lastTabClickTime = 0L
                                appState.switchTab(tab)
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.labelKey.tr(lang)) },
                        label = { Text(tab.labelKey.tr(lang)) }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        val screenModifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        val saveableStateHolder = rememberSaveableStateHolder()

        saveableStateHolder.SaveableStateProvider(appState.currentTab) {
            when (appState.currentTab) {
                NavTab.TRANSACTIONS -> CommonRoute(appState.transactionsViewModel) { state, onIntent ->
                    TransactionsScreen(
                        state = state,
                        onIntent = onIntent,
                        viewModel = appState.transactionsViewModel,
                        modifier = screenModifier
                    )
                }
                NavTab.STATISTICS -> CommonRoute(appState.statisticsViewModel) { state, onIntent ->
                    StatisticsScreen(
                        state = state,
                        onIntent = onIntent,
                        viewModel = appState.statisticsViewModel,
                        onNavigateToTransactions = { monthOffset, categoryName ->
                            appState.navigateToTransactionsCategory(categoryName, monthOffset)
                        },
                        onNavigateToTransactionsDate = { monthOffset, day, dateLabel ->
                            appState.navigateToTransactionsDate(monthOffset, day, dateLabel)
                        },
                        onNavigateToTransaction = { monthOffset, tx ->
                            appState.navigateToTransaction(monthOffset, tx)
                        },
                        onNavigateToBudget = { monthOffset ->
                            appState.navigateToBudgetAdjustment(monthOffset)
                        },
                        modifier = screenModifier
                    )
                }
                NavTab.SETTINGS -> CommonRoute(appState.settingsViewModel) { state, onIntent ->
                    SettingsScreen(
                        state = state,
                        onIntent = onIntent,
                        targetMonthOffset = appState.activeMonthOffset,
                        viewModel = appState.settingsViewModel,
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
