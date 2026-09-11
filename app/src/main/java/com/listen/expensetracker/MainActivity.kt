package com.listen.expensetracker

// 移除冗余的导航及UI组件引用（已收敛至 MainApp.kt），遵守单文件 250 行架构规则
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.listen.arch.apm.CrashHandler
import com.listen.expensetracker.core.effect.CollectCommonUiEffects
import com.listen.expensetracker.core.overlay.AppOverlayHost
import com.listen.expensetracker.core.security.AppSecurityCoordinator
import com.listen.expensetracker.core.security.BiometricLockOverlay
import com.listen.expensetracker.core.security.BiometricSecurityManager
import com.listen.expensetracker.core.security.SecurityPreferences
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.NavTab
import com.listen.expensetracker.core.state.rememberExpenseAppState
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.uicomponent.theme.ListenTheme

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider

class MainActivity : FragmentActivity() {

    private val pendingQuickAddIntent = mutableStateOf<Intent?>(null)
    private var activeAppState: ExpenseAppState? = null
    private val securityCoordinator = AppSecurityCoordinator(
        onShakeTriggered = {
            val app = activeAppState ?: return@AppSecurityCoordinator
            val currentHide = app.transactionsViewModel.viewState.value.hideBalance
            app.transactionsViewModel.handleIntent(TransactionsIntent.ToggleHideBalance(!currentHide))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ExpenseStrings.init()
        CrashHandler.init(this)
        LocalNotificationManager.createNotificationChannels(this)
        securityCoordinator.checkInitialLock(this)
        pendingQuickAddIntent.value = intent

        setContent {
            // One-line registration for all ViewModels, Navigation Tabs, and State
            val appState = rememberExpenseAppState()
            activeAppState = appState
            val settingsState by appState.settingsViewModel.viewState.collectAsState()
            val transactionsState by appState.transactionsViewModel.viewState.collectAsState()

            // [Security] 实时响应多任务防窥设置变更，常驻注入或按需解除 FLAG_SECURE (Rule 22)
            LaunchedEffect(settingsState.recentAppsShieldEnabled) {
                securityCoordinator.applyRecentAppsShield(this@MainActivity, settingsState.recentAppsShieldEnabled)
            }

            val currentIntent = pendingQuickAddIntent.value
            val isLocked = securityCoordinator.isAppLocked
            LaunchedEffect(currentIntent, isLocked) {
                if (currentIntent != null && !isLocked) {
                    handleDeepLinkIntent(currentIntent, appState)
                    pendingQuickAddIntent.value = null
                }
            }

            splashScreen.setKeepOnScreenCondition { transactionsState.isLoading }

            // Centralized CommonUiEffect collector across all ViewModels (Toast, Undo Snackbar, Share, Navigation)
            CollectCommonUiEffects(
                appState.transactionsViewModel,
                appState.statisticsViewModel,
                appState.settingsViewModel,
                snackbarHostState = appState.snackbarHostState
            )

            ListenTheme(
                themeMode = settingsState.themeMode,
                accentColor = settingsState.accentColor,
                pureBlackDark = settingsState.isPureBlackDark
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // 生物识别全屏锁屏遮罩层（锁定状态下完全隔离主界面与所有子窗口弹窗）
                    if (securityCoordinator.isAppLocked) {
                        BiometricLockOverlay(
                            onUnlockRequest = {
                                securityCoordinator.promptUnlock(
                                    this@MainActivity,
                                    settingsState.language,
                                    settingsState.recentAppsShieldEnabled
                                )
                            },
                            lang = settingsState.language
                        )
                    } else {
                        App(appState = appState)

                        // Top-level Declarative Overlay Host (0 boolean flags, 0 raw ifs)
                        AppOverlayHost(appState = appState)
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val s = activeAppState?.settingsViewModel?.viewState?.value
        val enabled = s?.biometricLockEnabled ?: SecurityPreferences.isBiometricEnabled(this)
        val supported = s?.isBiometricSupported ?: BiometricSecurityManager.isBiometricOrCredentialAvailable(this)
        val timeout = s?.lockTimeoutSeconds ?: SecurityPreferences.getLockTimeoutSeconds(this)
        val lang = s?.language ?: "zh"
        val shield = s?.recentAppsShieldEnabled ?: true

        securityCoordinator.onStart(this, enabled, supported, timeout, lang, shield)
    }

    override fun onResume() {
        super.onResume()
        val s = activeAppState?.settingsViewModel?.viewState?.value
        if (s != null) {
            securityCoordinator.onResume(this, s.recentAppsShieldEnabled, s.shakeToHideBalanceEnabled)
        }
    }

    override fun onPause() {
        super.onPause()
        val shield = activeAppState?.settingsViewModel?.viewState?.value?.recentAppsShieldEnabled ?: true
        securityCoordinator.onPause(this, shield)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingQuickAddIntent.value = intent
    }

    override fun onStop() {
        super.onStop()
        securityCoordinator.onStop()
        if (securityCoordinator.isAppLocked) {
            pendingQuickAddIntent.value = null
        }
        GoogleDriveAutoBackupManager.scheduleAutoBackup(this, delayMs = 500L)
    }

    private fun handleDeepLinkIntent(intent: Intent, appState: ExpenseAppState) {
        val data = intent.data
        if (data != null && data.scheme == "lexpense") {
            when (data.host) {
                "quick_add" -> {
                    val (categoryId, type) = ListenExpenseAppWidgetProvider.parseQuickAddIntent(intent) ?: return
                    appState.openQuickAdd(categoryId, type)
                }
                "budget_center" -> {
                    appState.switchTab(NavTab.TRANSACTIONS)
                    appState.transactionsViewModel.handleIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthlyBudget))
                }
                "transactions" -> {
                    appState.switchTab(NavTab.TRANSACTIONS)
                    if (data.getQueryParameter("filter") == "recurring") {
                        appState.transactionsViewModel.handleIntent(TransactionsIntent.SearchQueryChange("[周期]"))
                    }
                }
                "update" -> {
                    appState.switchTab(NavTab.SETTINGS)
                    val version = data.getQueryParameter("version") ?: ""
                    appState.settingsViewModel.handleIntent(SettingsIntent.CheckForUpdates(version))
                }
            }
            return
        }
        val (categoryId, type) = ListenExpenseAppWidgetProvider.parseQuickAddIntent(intent) ?: return
        appState.openQuickAdd(categoryId, type)
    }

    companion object {
        fun normalizeCategoryId(raw: String?): String? = ListenExpenseAppWidgetProvider.normalizeCategoryId(raw)
    }
}
