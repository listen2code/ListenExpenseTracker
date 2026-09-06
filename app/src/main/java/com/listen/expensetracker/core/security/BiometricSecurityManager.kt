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
 */
object BiometricSecurityManager {

    private const val TAG = "BiometricSecurity"

    /**
     * 检查当前设备是否支持生物识别或锁屏密码。
     */
    fun isBiometricOrCredentialAvailable(context: Context): Boolean {
        return try {
            val biometricManager = BiometricManager.from(context)
            val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL
            val canAuthenticate = biometricManager.canAuthenticate(authenticators)
            canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS
        } catch (e: Exception) {
            ApmLogger.e(TAG, "Failed to check biometric availability: ${e.message}")
            false
        }
    }

    /**
     * 拉起系统生物识别/设备密码解锁弹窗。
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
