package com.listen.expensetracker.core.security

import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppConstants
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState

/**
 * 应用安全与隐私防窥生命周期协调器 (AppSecurityCoordinator)。
 * 纯粹负责调度生物识别锁定状态机、超时锁屏判定与 Recent Apps 窗口安全标志 (FLAG_SECURE)。
 *
 * 架构设计：实现 DefaultLifecycleObserver，通过 lifecycle.addObserver(coordinator)
 * 自主监听 Activity 的生命周期回调，无需 Activity 手动重写 onStart/onResume/onPause。
 */
class AppSecurityCoordinator(
    private val settingsProvider: () -> SettingsUiState? = { null }
) : DefaultLifecycleObserver {

    /**
     * 当前应用是否被锁定（展示全屏遮罩层）
     * 作为 mutableStateOf 存在，以便 Compose 可以自动观察其变化以显示/隐藏 BiometricLockOverlay。
     */
    var isAppLocked by mutableStateOf(false)
        private set

    // 使用 SystemClock.elapsedRealtime() 单调时间，防止用户修改系统时间导致的安全绕过
    private var backgroundTimestamp = 0L
    
    // 守卫防止重复触发验证弹窗
    private var isAuthenticating = false

    /**
     * 在 Activity onCreate 阶段快速同步检测是否需要初始锁定
     */
    fun checkInitialLock(activity: FragmentActivity) {
        val enabled = SecurityPreferences.isBiometricEnabled(activity)
        val supported = BiometricSecurityManager.isBiometricOrCredentialAvailable(activity)
        if (enabled && supported) {
            isAppLocked = true
            applyRecentAppsShield(activity, true)
        }
    }

    /**
     * 响应式调度窗口安全标志 (FLAG_SECURE)：
     * 当开启 Recent Apps Shield 或处于生物识别锁定状态时，窗口常驻保持 FLAG_SECURE。
     * 从根本上杜绝 Android 系统在多任务手势上滑瞬间窃取屏幕快照 (Task Snapshot)。
     */
    fun applyRecentAppsShield(activity: FragmentActivity, recentAppsShield: Boolean) {
        if (recentAppsShield || isAppLocked) {
            activity.window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val activity = owner as? FragmentActivity ?: return
        val s = settingsProvider()
        val enabled = s?.biometricLockEnabled ?: SecurityPreferences.isBiometricEnabled(activity)
        val supported = s?.isBiometricSupported ?: BiometricSecurityManager.isBiometricOrCredentialAvailable(activity)
        val timeout = s?.lockTimeoutSeconds ?: SecurityPreferences.getLockTimeoutSeconds(activity)
        val lang = s?.language ?: AppConstants.DEFAULT_LANG
        val shield = s?.recentAppsShieldEnabled ?: true

        applyRecentAppsShield(activity, shield)
        if (enabled && supported) {
            val elapsedSeconds = if (backgroundTimestamp == 0L) {
                Long.MAX_VALUE / 1000
            } else {
                (SystemClock.elapsedRealtime() - backgroundTimestamp) / 1000
            }
            if (elapsedSeconds >= timeout) {
                isAppLocked = true
                applyRecentAppsShield(activity, shield)
                promptUnlock(activity, lang, shield)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        val activity = owner as? FragmentActivity ?: return
        val shield = settingsProvider()?.recentAppsShieldEnabled ?: true
        applyRecentAppsShield(activity, shield)
    }

    override fun onPause(owner: LifecycleOwner) {
        val activity = owner as? FragmentActivity ?: return
        val shield = settingsProvider()?.recentAppsShieldEnabled ?: true
        applyRecentAppsShield(activity, shield)
    }

    override fun onStop(owner: LifecycleOwner) {
        backgroundTimestamp = SystemClock.elapsedRealtime()
    }

    /**
     * 调起系统生物识别/设备密码验证
     */
    fun promptUnlock(activity: FragmentActivity, lang: String, recentAppsShield: Boolean = true) {
        if (isAuthenticating) return
        isAuthenticating = true
        BiometricSecurityManager.promptUnlock(
            activity = activity,
            title = AppStrings.SECURITY_UNLOCK_PROMPT_TITLE.tr(lang),
            subtitle = AppStrings.SECURITY_UNLOCK_PROMPT_SUBTITLE.tr(lang),
            onSuccess = {
                isAuthenticating = false
                isAppLocked = false
                backgroundTimestamp = SystemClock.elapsedRealtime()
                applyRecentAppsShield(activity, recentAppsShield)
            },
            onError = { _, _ ->
                isAuthenticating = false
            }
        )
    }
}
