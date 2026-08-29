package com.listen.expensetracker

import com.listen.arch.i18n.tr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.listen.arch.apm.CrashHandler
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.core.effect.CollectCommonUiEffects
import com.listen.expensetracker.core.overlay.AppOverlayHost
import com.listen.expensetracker.core.route.CommonRoute
import com.listen.expensetracker.core.state.AppOverlay
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.NavTab
import com.listen.expensetracker.core.state.rememberExpenseAppState
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.settings.ui.SettingsScreen
import com.listen.expensetracker.features.statistics.ui.StatisticsScreen
import com.listen.expensetracker.features.transactions.ui.TransactionsScreen
import com.listen.uicomponent.theme.ListenTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ExpenseStrings.init()
        CrashHandler.init(this)

        setContent {
            // One-line registration for all ViewModels, Navigation Tabs, and State
            val appState = rememberExpenseAppState()
            val settingsState by appState.settingsViewModel.viewState.collectAsState()
            val transactionsState by appState.transactionsViewModel.viewState.collectAsState()

            splashScreen.setKeepOnScreenCondition { transactionsState.isLoading }

            // Centralized CommonUiEffect collector across all ViewModels (Toast, Undo Snackbar, Share, APM, Google Login)
            CollectCommonUiEffects(
                appState.transactionsViewModel,
                appState.statisticsViewModel,
                appState.settingsViewModel,
                snackbarHostState = appState.snackbarHostState,
                onOpenApm = { appState.openOverlay(AppOverlay.ApmInspector) },
                onLaunchGoogleSignIn = {
                    lifecycleScope.launch {
                        appState.settingsViewModel.launchGoogleAccountPicker(this@MainActivity)
                    }
                }
            )

            ListenTheme(
                themeMode = settingsState.themeMode,
                accentColor = settingsState.accentColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(appState = appState)

                    // Top-level Declarative Overlay Host (0 boolean flags, 0 raw ifs)
                    AppOverlayHost(appState = appState)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager.scheduleAutoBackup(this, delayMs = 500L)
    }
}

@Composable
fun App(
    appState: ExpenseAppState,
    modifier: Modifier = Modifier
) {
    val settingsState by appState.settingsViewModel.viewState.collectAsState()
    val lang = settingsState.language

    Scaffold(
        snackbarHost = { SnackbarHost(appState.snackbarHostState) },
        bottomBar = {
            NavigationBar {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = appState.currentTab == tab,
                        onClick = { appState.switchTab(tab) },
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
        val saveableStateHolder = androidx.compose.runtime.saveable.rememberSaveableStateHolder()

        saveableStateHolder.SaveableStateProvider(appState.currentTab) {
            when (appState.currentTab) {
                NavTab.TRANSACTIONS -> CommonRoute(appState.transactionsViewModel) { state, onIntent ->
                    TransactionsScreen(
                        state = state,
                        onIntent = onIntent,
                        modifier = screenModifier
                    )
                }
                NavTab.STATISTICS -> CommonRoute(appState.statisticsViewModel) { state, onIntent ->
                    StatisticsScreen(
                        state = state,
                        onIntent = onIntent,
                        modifier = screenModifier
                    )
                }
                NavTab.SETTINGS -> CommonRoute(appState.settingsViewModel) { state, onIntent ->
                    SettingsScreen(
                        state = state,
                        onIntent = onIntent,
                        targetMonthOffset = appState.activeMonthOffset,
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
