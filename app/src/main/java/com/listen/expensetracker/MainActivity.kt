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
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.rememberExpenseAppState
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.uicomponent.theme.ListenTheme

import android.content.Intent
import androidx.compose.runtime.LaunchedEffect
import com.listen.expensetracker.data.db.TransactionType

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
        pendingQuickAddIntent.value = intent

        setContent {
            // One-line registration for all ViewModels, Navigation Tabs, and State
            val appState = rememberExpenseAppState()
            activeAppState = appState
            val settingsState by appState.settingsViewModel.viewState.collectAsState()
            val transactionsState by appState.transactionsViewModel.viewState.collectAsState()

            val currentIntent = pendingQuickAddIntent.value
            LaunchedEffect(currentIntent) {
                currentIntent?.let { targetIntent ->
                    handleQuickAddIntent(targetIntent, appState)
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
                accentColor = settingsState.accentColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App(appState = appState)

                    // Top-level Declarative Overlay Host (0 boolean flags, 0 raw ifs)
                    AppOverlayHost(appState = appState)

                    // 生物识别全屏锁屏遮罩层
                    if (securityCoordinator.isAppLocked) {
                        BiometricLockOverlay(
                            onUnlockRequest = { securityCoordinator.promptUnlock(this@MainActivity, settingsState.language) },
                            lang = settingsState.language
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val s = activeAppState?.settingsViewModel?.viewState?.value
        if (s != null) {
            securityCoordinator.onStart(this, s.biometricLockEnabled, s.isBiometricSupported, s.lockTimeoutSeconds, s.language)
        }
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
        GoogleDriveAutoBackupManager.scheduleAutoBackup(this, delayMs = 500L)
    }

    private fun handleQuickAddIntent(intent: Intent, appState: ExpenseAppState) {
        val uri = intent.data
        val isQuickAddUri = uri != null && uri.scheme == "lexpense" && uri.host == "quick_add"
        val isQuickAddExtra = intent.hasExtra(EXTRA_QUICK_ADD_CATEGORY) || intent.hasExtra(EXTRA_QUICK_ADD_TYPE)
        if (isQuickAddUri || isQuickAddExtra) {
            val rawCategory = uri?.getQueryParameter("category") ?: intent.getStringExtra(EXTRA_QUICK_ADD_CATEGORY)
            val type = uri?.getQueryParameter("type") ?: intent.getStringExtra(EXTRA_QUICK_ADD_TYPE) ?: TransactionType.EXPENSE
            val categoryId = normalizeCategoryId(rawCategory)
            appState.openQuickAdd(categoryId, type)
        }
    }

    companion object {
        const val EXTRA_QUICK_ADD_CATEGORY = "extra_quick_add_category"
        const val EXTRA_QUICK_ADD_TYPE = "extra_quick_add_type"

        fun normalizeCategoryId(raw: String?): String? = when (raw) {
            "cat_food", "c_food" -> "c_food"
            "cat_transport", "c_transport" -> "c_transport"
            "cat_shopping", "c_shopping" -> "c_shopping"
            "cat_daily", "cat_other", "c_other_exp" -> "c_other_exp"
            else -> raw
        }
    }
}
