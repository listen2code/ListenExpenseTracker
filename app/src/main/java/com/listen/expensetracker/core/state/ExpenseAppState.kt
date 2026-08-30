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
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Calendar

/**
 * Type-safe Navigation Tab definitions for ListenExpenseTracker.
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
 */
sealed interface AppOverlay {
    data object ApmInspector : AppOverlay
}

/**
 * Clean Application State Holder coordinating ViewModels, Navigation Tabs, SnackbarHostState, and Global Overlays.
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
        get() = if (currentTab == NavTab.STATISTICS) {
            statisticsViewModel.viewState.value.selectedMonthOffset
        } else {
            transactionsViewModel.viewState.value.selectedMonthOffset
        }

    fun switchTab(tab: NavTab) {
        if (tab == NavTab.STATISTICS && currentTab == NavTab.TRANSACTIONS) {
            val offset = transactionsViewModel.viewState.value.selectedMonthOffset
            if (statisticsViewModel.viewState.value.selectedMonthOffset != offset) {
                statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(offset))
            }
        } else if (tab == NavTab.TRANSACTIONS && currentTab == NavTab.STATISTICS) {
            val offset = statisticsViewModel.viewState.value.selectedMonthOffset
            if (transactionsViewModel.viewState.value.selectedMonthOffset != offset) {
                transactionsViewModel.handleIntent(TransactionsIntent.SetMonthOffset(offset))
            }
        }
        currentTab = tab
    }

    /**
     * Navigates directly from Statistics to Transactions filtered by month and category.
     */
    fun navigateToTransactionsCategory(categoryName: String, monthOffset: Int) {
        if (statisticsViewModel.viewState.value.selectedMonthOffset != monthOffset) {
            statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        }
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByCategory(categoryName, monthOffset))
        currentTab = NavTab.TRANSACTIONS
    }

    /**
     * Navigates directly from Statistics to Transactions focused on a specific date in a month.
     */
    fun navigateToTransactionsDate(monthOffset: Int, day: Int, dateLabel: String = "") {
        if (statisticsViewModel.viewState.value.selectedMonthOffset != monthOffset) {
            statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        }
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByDate(monthOffset, day, dateLabel))
        currentTab = NavTab.TRANSACTIONS
    }

    /**
     * Navigates directly from Statistics to Transactions focused on a specific transaction in a month.
     */
    fun navigateToTransaction(monthOffset: Int, transaction: TransactionEntity) {
        if (statisticsViewModel.viewState.value.selectedMonthOffset != monthOffset) {
            statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(monthOffset))
        }
        val cal = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
        val day = cal.get(Calendar.DAY_OF_MONTH)
        transactionsViewModel.handleIntent(TransactionsIntent.FilterByTransaction(monthOffset, transaction.id, day, transaction.amount))
        currentTab = NavTab.TRANSACTIONS
    }

    /**
     * Top-level active overlay state. Controlled entirely via openOverlay / dismissOverlay.
     */
    var activeOverlay by mutableStateOf<AppOverlay?>(null)
        private set

    fun openOverlay(overlay: AppOverlay) {
        activeOverlay = overlay
    }

    fun dismissOverlay() {
        activeOverlay = null
    }

    /**
     * One-time event flow for scrolling a specific tab's list to top on double-tap.
     * replay = 0 ensures no replay occurs when re-entering tabs.
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
}

/**
 * Remembers and provisions all feature ViewModels, UI state holders, and Overlay manager.
 */
@Composable
fun rememberExpenseAppState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): ExpenseAppState {
    val transactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
    val statisticsViewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(LocalContext.current.applicationContext as Application)
    )

    return remember(transactionsViewModel, statisticsViewModel, settingsViewModel, snackbarHostState) {
        ExpenseAppState(
            transactionsViewModel = transactionsViewModel,
            statisticsViewModel = statisticsViewModel,
            settingsViewModel = settingsViewModel,
            snackbarHostState = snackbarHostState
        )
    }
}
