package com.listen.expensetracker.core.notification

import android.content.Context
import android.content.SharedPreferences

/**
 * 快速同步通知与预警偏好存储 (NotificationPreferences)。
 * 负责全局通知开关、各子场景预警开关、去重防骚扰状态键与版本通知冷却时间戳的持久化。
 */
object NotificationPreferences {
    private const val PREF_NAME = "expense_notification_prefs"

    private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    private const val KEY_BUDGET_ALERTS_ENABLED = "budget_alerts_enabled"
    private const val KEY_BUDGET_WARNING_ENABLED = "budget_warning_threshold_enabled"
    private const val KEY_RECURRING_ALERTS_ENABLED = "recurring_alerts_enabled"
    private const val KEY_UPDATE_ALERTS_ENABLED = "update_alerts_enabled"
    private const val KEY_NOTIFIED_KEYS = "notified_keys"
    private const val KEY_LAST_UPDATE_TAG = "last_update_notified_tag"
    private const val KEY_LAST_UPDATE_TIME = "last_update_notified_time"

    const val DEFAULT_UPDATE_COOLDOWN_MS = 3 * 24 * 60 * 60 * 1000L // 3 days

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isBudgetAlertsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BUDGET_ALERTS_ENABLED, true)
    }

    fun setBudgetAlertsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BUDGET_ALERTS_ENABLED, enabled).apply()
    }

    fun isBudgetWarningThresholdEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_BUDGET_WARNING_ENABLED, true)
    }

    fun setBudgetWarningThresholdEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_BUDGET_WARNING_ENABLED, enabled).apply()
    }

    fun isRecurringBillsAlertsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_RECURRING_ALERTS_ENABLED, true)
    }

    fun setRecurringBillsAlertsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_RECURRING_ALERTS_ENABLED, enabled).apply()
    }

    fun isAppUpdatesAlertsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_UPDATE_ALERTS_ENABLED, true)
    }

    fun setAppUpdatesAlertsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_UPDATE_ALERTS_ENABLED, enabled).apply()
    }

    /**
     * 判断指定去重键是否已被通知过。
     */
    fun isNotified(context: Context, key: String): Boolean {
        val set = getPrefs(context).getStringSet(KEY_NOTIFIED_KEYS, emptySet()) ?: emptySet()
        return set.contains(key)
    }

    /**
     * 记录已通知去重键。
     */
    fun markNotified(context: Context, key: String) {
        val currentSet = getPrefs(context).getStringSet(KEY_NOTIFIED_KEYS, emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet().apply { add(key) }
        getPrefs(context).edit().putStringSet(KEY_NOTIFIED_KEYS, newSet).apply()
    }

    /**
     * 清除过期的去重键（保留当月和上月，清理更早的历史记录）。
     */
    fun cleanExpiredDedupKeys(context: Context, currentYear: Int, currentMonth: Int) {
        val currentSet = getPrefs(context).getStringSet(KEY_NOTIFIED_KEYS, emptySet()) ?: return
        val currentKeyPrefix = String.format("%04d_%02d", currentYear, currentMonth)
        val prevYear = if (currentMonth == 1) currentYear - 1 else currentYear
        val prevMonth = if (currentMonth == 1) 12 else currentMonth - 1
        val prevKeyPrefix = String.format("%04d_%02d", prevYear, prevMonth)

        val filteredSet = currentSet.filter { key ->
            key.startsWith(currentKeyPrefix) || key.startsWith(prevKeyPrefix)
        }.toSet()

        if (filteredSet.size != currentSet.size) {
            getPrefs(context).edit().putStringSet(KEY_NOTIFIED_KEYS, filteredSet).apply()
        }
    }

    /**
     * 判断新版本是否满足通知条件（同一版本号带 3 天冷却）。
     */
    fun shouldNotifyUpdate(
        context: Context,
        tagName: String,
        currentTime: Long = System.currentTimeMillis(),
        cooldownMs: Long = DEFAULT_UPDATE_COOLDOWN_MS
    ): Boolean {
        val prefs = getPrefs(context)
        val lastTag = prefs.getString(KEY_LAST_UPDATE_TAG, "") ?: ""
        val lastTime = prefs.getLong(KEY_LAST_UPDATE_TIME, 0L)

        if (lastTag.isBlank() || lastTag != tagName) {
            return true
        }
        return (currentTime - lastTime) >= cooldownMs
    }

    /**
     * 记录版本更新通知已触发。
     */
    fun recordUpdateNotified(
        context: Context,
        tagName: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        getPrefs(context).edit()
            .putString(KEY_LAST_UPDATE_TAG, tagName)
            .putLong(KEY_LAST_UPDATE_TIME, timestamp)
            .apply()
    }

    /**
     * 清空全部通知与去重记录（供测试与重置）。
     */
    fun clearAll(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
