package com.listen.expensetracker.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.listen.arch.apm.ApmLogger

/**
 * 生物识别与设备安全凭据管理器 (BiometricSecurityManager)。
 * 统一收拢指纹、面容以及锁屏凭据（PIN/图案/密码）的设备可用性检测与验证调度。
 *
 * 采用 Object 单例设计：作为一个无状态的工具类(Stateless utility)，
 * 不持有上下文状态，使得在应用任意位置安全、便捷地调用。
 */
object BiometricSecurityManager {

    private const val TAG = "BiometricSecurity"

    /**
     * 检查当前设备是否支持生物识别或锁屏密码。
     */
    fun isBiometricOrCredentialAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            // 采用降级链(Fallback chain)策略：首选强生物识别(指纹/面容)，
            // 若不支持，则回退到备用设备凭据(PIN/图案/密码)。
            val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            val canAuthenticate = biometricManager.canAuthenticate(authenticators)
            canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            // 包裹 try-catch 防护：在部分魔改定制的 OEM ROM(厂商系统) 中，
            // 访问 BiometricManager 可能会抛出运行时异常，拦截该异常避免崩溃。
            ApmLogger.e(TAG, "Failed to check biometric availability: ${e.message}")
            false
        }
    }

    /**
     * 拉起系统生物识别/设备密码解锁弹窗。
     * 参数要求传入 FragmentActivity 而非普通 Context：
     * 这是因为 BiometricPrompt 强依赖于 Fragment 生命周期来进行安全的 Dialog 状态管理。
     */
    fun promptUnlock(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onError: (errorCode: Int, errString: String) -> Unit = { _, _ -> }
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // 接入 APM：记录所有鉴权事件，形成安全审计日志(Security audit trail)
                ApmLogger.i(TAG, "Biometric authentication succeeded")
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                ApmLogger.w(TAG, "Biometric authentication error [$errorCode]: $errString")
                onError(errorCode, errString.toString())
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                ApmLogger.w(TAG, "Biometric authentication attempt rejected")
            }
        }

        try {
            val prompt = BiometricPrompt(activity, executor, callback)
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
                .build()

            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            ApmLogger.e(TAG, "Failed to launch BiometricPrompt: ${e.message}")
            onError(BiometricPrompt.ERROR_UNABLE_TO_PROCESS, e.message ?: "Authentication failed")
        }
    }
}
