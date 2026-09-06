package com.listen.expensetracker.core.security

import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings

/**
 * 应用安全与隐私防窥生命周期协调器 (AppSecurityCoordinator)。
 * 负责调度生物识别锁定状态机、Recent Apps 窗口安全标志 (FLAG_SECURE) 以及摇一摇传感器手势。
 */
class AppSecurityCoordinator(
    private val onShakeTriggered: () -> Unit
) {
    /**
     * 当前应用是否被锁定（展示全屏遮罩层）
     */
    var isAppLocked by mutableStateOf(false)
        private set

    private var backgroundTimestamp = SystemClock.elapsedRealtime()
    private val shakeDetector = ShakeDetector(onShake = onShakeTriggered)

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

    /**
     * Activity onPause 生命周期：确保安全标志生效并暂停摇一摇传感器
     */
    fun onPause(activity: FragmentActivity, recentAppsShield: Boolean) {
        applyRecentAppsShield(activity, recentAppsShield)
        shakeDetector.stop()
    }

    /**
     * Activity onResume 生命周期：安全恢复窗口标志并按需激活摇一摇传感器
     */
    fun onResume(activity: FragmentActivity, recentAppsShield: Boolean, shakeEnabled: Boolean) {
        applyRecentAppsShield(activity, recentAppsShield)
        if (shakeEnabled) {
            shakeDetector.start(activity)
        }
    }

    /**
     * Activity onStop 生命周期：记录单调时间戳
     */
    fun onStop() {
        backgroundTimestamp = SystemClock.elapsedRealtime()
    }

    /**
     * Activity onStart 生命周期：比对离开时长，超时则触发锁定并拉起验证
     */
    fun onStart(
        activity: FragmentActivity,
        biometricEnabled: Boolean,
        isBioSupported: Boolean,
        timeoutSeconds: Int,
        lang: String,
        recentAppsShield: Boolean
    ) {
        applyRecentAppsShield(activity, recentAppsShield)
        if (biometricEnabled && isBioSupported) {
            val elapsedSeconds = (SystemClock.elapsedRealtime() - backgroundTimestamp) / 1000
            if (elapsedSeconds >= timeoutSeconds) {
                isAppLocked = true
                applyRecentAppsShield(activity, recentAppsShield)
                promptUnlock(activity, lang, recentAppsShield)
            }
        }
    }

    /**
     * 调起系统生物识别/设备密码验证
     */
    fun promptUnlock(activity: FragmentActivity, lang: String, recentAppsShield: Boolean = true) {
        BiometricSecurityManager.promptUnlock(
            activity = activity,
            title = AppStrings.SECURITY_UNLOCK_PROMPT_TITLE.tr(lang),
            subtitle = AppStrings.SECURITY_UNLOCK_PROMPT_SUBTITLE.tr(lang),
            onSuccess = {
                isAppLocked = false
                applyRecentAppsShield(activity, recentAppsShield)
            }
        )
    }
}
