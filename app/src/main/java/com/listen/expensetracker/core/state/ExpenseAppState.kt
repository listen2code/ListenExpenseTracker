package com.listen.expensetracker.core.state

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel

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

    fun switchTab(tab: NavTab) {
        currentTab = tab
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
}

/**
 * Remembers and provisions all feature ViewModels, UI state holders, and Overlay manager.
 */
@Composable
fun rememberExpenseAppState(
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
): ExpenseAppState {
    val transactionsViewModel: TransactionsViewModel = viewModel(
        factory = TransactionsViewModel.Factory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application)
    )
    val statisticsViewModel: StatisticsViewModel = viewModel(
        factory = StatisticsViewModel.Factory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application)
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
