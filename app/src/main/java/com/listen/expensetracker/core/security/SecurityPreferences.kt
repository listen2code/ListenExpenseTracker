package com.listen.expensetracker.core.security

import android.content.Context
import android.content.SharedPreferences

/**
 * 快速同步安全凭据偏好存储 (SecurityPreferences)。
 * 用于在 Activity 启动（冷启动、热启动）的第一时间以纳秒级速度同步读取生物识别锁定状态，
 * 避免 DataStore 异步读取在首帧导致的界面泄露与未锁屏竞态。
 */
object SecurityPreferences {
    private const val PREF_NAME = "expense_security_prefs"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_lock_enabled"
    private const val KEY_LOCK_TIMEOUT = "lock_timeout_seconds"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun getLockTimeoutSeconds(context: Context): Int {
        return getPrefs(context).getInt(KEY_LOCK_TIMEOUT, 0)
    }

    fun setLockTimeoutSeconds(context: Context, seconds: Int) {
        getPrefs(context).edit().putInt(KEY_LOCK_TIMEOUT, seconds).apply()
    }
}
