package com.listen.expensetracker.core.effect

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.listen.expensetracker.core.security.AppSecurityCoordinator
import com.listen.expensetracker.core.security.ShakeDetector
import com.listen.expensetracker.core.state.ExpenseAppState
import com.listen.expensetracker.core.state.NavTab
import com.listen.expensetracker.data.model.AppConstants
import com.listen.expensetracker.data.model.AppConstants.DeepLink
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * 集中管理应用级的“副作用”任务 (System Side Effects)。
 * 对称于 CommonUiEffectHandler，将 App 与系统交互的逻辑（Intent、防窥、摇一摇手势、启动页）从 Activity 中解耦。
 */
@Composable
fun AppSideEffectHandler(
    appState: ExpenseAppState,
    securityCoordinator: AppSecurityCoordinator
) {
    val context = LocalContext.current as FragmentActivity
    val settingsState by appState.settingsViewModel.viewState.collectAsState()
    val transactionsState by appState.transactionsViewModel.viewState.collectAsState()

    // 【DisposableEffect 使用说明】：
    // 1. 适用场景：用于需要“成对”操作的副作用任务（例如：注册/注销、开启/停止、订阅/取消）。
    // 2. 核心机制：当 context 或开关变化时，会先执行上一次的 onDispose，再重新执行块内逻辑。
    // 3. 强制要求：必须以 onDispose { ... } 结尾，确保资源在组件销毁或 Key 变化时被干净地释放，防止内存泄漏。
    
    // 摇一摇手势副作用：仅当开关开启且界面位于前台活跃状态时注册加速度传感器
    DisposableEffect(context, settingsState.shakeToHideBalanceEnabled) {
        if (!settingsState.shakeToHideBalanceEnabled) {
            // 如果开关关闭，直接返回一个空的清理块
            return@DisposableEffect onDispose {}
        }

        // 初始化传感器探测器
        val shakeDetector = ShakeDetector(
            onShake = {
                val currentHide = appState.transactionsViewModel.viewState.value.hideBalance
                appState.transactionsViewModel.handleIntent(TransactionsIntent.ToggleHideBalance(!currentHide))
            }
        )

        // 观察 Lifecycle 状态，确保只有在 App 位于前台（Resume）时才激活传感器，节省电量
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> shakeDetector.start(context)
                Lifecycle.Event.ON_PAUSE -> shakeDetector.stop()
                else -> Unit
            }
        }

        // 注册观察者
        context.lifecycle.addObserver(lifecycleObserver)
        
        // 如果注册时已经处于 Resume 状态，立即启动
        if (context.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            shakeDetector.start(context)
        }

        // 【关键清理块】：当用户离开主界面、关闭 Activity 或在设置中关闭此功能时，此块会被强制调用
        onDispose {
            context.lifecycle.removeObserver(lifecycleObserver)
            shakeDetector.stop()
        }
    }

    LaunchedEffect(appState) {
        // 冷启动：初次构筑时，将 Activity 原始启动 Intent 注入单向数据流管道
        context.intent?.let { appState.sendIntent(it) }

        // 子任务 1：响应式观察者 - 多任务防窥设置 (FLAG_SECURE)
        launch {
            snapshotFlow { settingsState.recentAppsShieldEnabled }.collect { enabled ->
                securityCoordinator.applyRecentAppsShield(context, enabled)
            }
        }

        // 子任务 2：单向数据流 (UDF) 管道 - 处理 Intent 跳转
        launch {
            appState.intentChannel.receiveAsFlow().collect { incomingIntent ->
                // 等待直到 App 安全解锁
                snapshotFlow { securityCoordinator.isAppLocked }.first { isLocked -> !isLocked }
                handleDeepLinkIntent(incomingIntent, appState)
            }
        }

        // 子任务 3：首次加载成功后，永久标记 App 为就绪状态
        launch {
            snapshotFlow { transactionsState.isLoading }.first { !it }
            appState.markReady()
        }
    }
}

/**
 * 内部路由处理器：解析并分发深度链接意图
 */
private fun handleDeepLinkIntent(intent: Intent, appState: ExpenseAppState) {
    val data = intent.data
    if (data != null && data.scheme == DeepLink.SCHEME) {
        when (data.host) {
            DeepLink.HOST_QUICK_ADD -> {
                val (categoryId, type) = ListenExpenseAppWidgetProvider.parseQuickAddIntent(intent) ?: return
                appState.openQuickAdd(categoryId, type)
            }
            DeepLink.HOST_BUDGET_CENTER -> {
                appState.openBudgetCenter()
            }
            DeepLink.HOST_TRANSACTIONS -> {
                if (data.getQueryParameter(DeepLink.PARAM_FILTER) == DeepLink.VALUE_RECURRING) {
                    appState.openRecurringTransactions(AppConstants.RECURRING_TAG)
                } else {
                    appState.switchTab(NavTab.TRANSACTIONS)
                }
            }
            DeepLink.HOST_UPDATE -> {
                val version = data.getQueryParameter(DeepLink.PARAM_VERSION) ?: ""
                appState.checkForUpdates(version)
            }
        }
        return
    }
    // 处理来自小组件的直接隐式 Intent (非 URI格式)
    val (categoryId, type) = ListenExpenseAppWidgetProvider.parseQuickAddIntent(intent) ?: return
    appState.openQuickAdd(categoryId, type)
}
