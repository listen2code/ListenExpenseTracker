package com.listen.expensetracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.listen.expensetracker.core.i18n.LocalAppLanguage
import com.listen.expensetracker.core.effect.AppSideEffectHandler
import com.listen.expensetracker.core.effect.CommonUiEffectHandler
import com.listen.expensetracker.core.overlay.AppOverlayHost
import com.listen.expensetracker.core.security.AppSecurityCoordinator
import com.listen.expensetracker.core.security.BiometricLockOverlay
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.rememberExpenseAppState
import com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import com.listen.uicomponent.theme.ListenTheme

/**
 * 应用主入口 Activity。
 * 采用 FragmentActivity 以支持 androidx.biometric 库。
 * 核心职责：生命周期分发、系统级副作用协调（安全、Intent 路由）、根 UI 声明。
 */
class MainActivity : FragmentActivity() {

    // 缓存当前的 AppState 引用，用于在非 Composable 的生命周期回调中访问 ViewModel 状态
    private var activeAppState: ExpenseAppState? = null

    /**
     * 安全协调器：统一管理生物识别锁、超时验证、多任务防窥等核心安全逻辑。
     * 架构：实现 DefaultLifecycleObserver，通过 settingsProvider 动态获取最新偏好，自动响应生命周期事件。
     */
    private val securityCoordinator = AppSecurityCoordinator(
        settingsProvider = { activeAppState?.settingsViewModel?.viewState?.value }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 初始化系统启动页 API，必须在 super.onCreate 之前调用
        val splashScreen = installSplashScreen()
        
        // 2. 启用全屏边到边体验 (Edge-to-Edge)
        enableEdgeToEdge()
        
        super.onCreate(savedInstanceState)
        
        // 3. 在 UI 挂载前立即执行初次锁定状态检查，并注册生命周期自感知观察者
        securityCoordinator.checkInitialLock(this)
        lifecycle.addObserver(securityCoordinator)

        setContent {
            // 4. 创建并记住全局唯一的 AppState。它是整个 UI 树的单一事实来源。
            val appState = rememberExpenseAppState()
            activeAppState = appState

            val settingsState by appState.settingsViewModel.viewState.collectAsState()

            // 5. 联动控制启动页：只要 AppState 标记为“未就绪”，SplashScreen 就会一直遮盖 Activity。
            // 它是通过 AppSideEffectHandler 监听首屏加载成功后触发 isInitialReady 的。
            splashScreen.setKeepOnScreenCondition { !appState.isInitialReady }

            /**
             * 系统级副作用处理器 (System-Level Side Effects)
             * 职责：处理冷/热启动 Intent 路由、多任务预览防窥设置、首屏就绪监控。
             */
            AppSideEffectHandler(appState, securityCoordinator)

            // 业务级副作用处理器 (Global UI Event Collector)
            // 职责：跨 ViewModel 统一收集并消费 Toast、Snackbar 撤销、分享、导航跳转等瞬时事件。
            CommonUiEffectHandler(
                appState.transactionsViewModel,
                appState.statisticsViewModel,
                appState.settingsViewModel,
                snackbarHostState = appState.snackbarHostState
            )

            // 6. 注入全局语言环境 (CompositionLocal) 与主题 (ListenTheme)
            CompositionLocalProvider(
                LocalAppLanguage provides settingsState.language
            ) {
                ListenTheme(
                    themeMode = settingsState.themeMode,
                    accentColor = settingsState.accentColor,
                    pureBlackDark = settingsState.isPureBlackDark
                ) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        /**
                         * 物理隔离层级逻辑：
                         * - 当 App 处于锁定时：只渲染 BiometricLockOverlay 遮罩，业务组件 (App) 物理卸载，确保隐私安全。
                         * - 当 App 已解锁：正常渲染 App 主体及全局覆盖物宿主 (AppOverlayHost)。
                         */
                        if (securityCoordinator.isAppLocked) {
                            BiometricLockOverlay(
                                onUnlockRequest = {
                                    securityCoordinator.promptUnlock(
                                        this@MainActivity,
                                        settingsState.language,
                                        settingsState.recentAppsShieldEnabled
                                    )
                                }
                            )
                        } else {
                            // 渲染主功能导航架构（无需层层传 lang，由 LocalAppLanguage 自动提供）
                            MainApp(appState = appState)

                            // 全局声明式覆盖物宿主 (处理全屏加载 HUD、检查器等)
                            AppOverlayHost(appState = appState)
                        }
                    }
                }
            }
        }
    }

    /**
     * 热启动意图捕获：当 App 已经在后台，通过 DeepLink 或小组件再次唤起时，
     * 将新 Intent 投递到 UDF 管道进行单向分发处理。
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        activeAppState?.sendIntent(intent)
    }

    /**
     * 退出前台清理与云同步：
     * 静默触发 Google Drive 云端自动备份检查（自动防抖与脏检查）。
     */
    override fun onStop() {
        super.onStop()
        GoogleDriveAutoBackupManager.scheduleAutoBackup(this, delayMs = 500L)
    }

    companion object {
        /**
         * 分类 ID 规范化桥接：
         * 将来自小组件或外部 Intent 的分类 ID 转换为标准格式，确保路由精准匹配。
         */
        fun normalizeCategoryId(raw: String?): String? = ListenExpenseAppWidgetProvider.normalizeCategoryId(raw)
    }
}
