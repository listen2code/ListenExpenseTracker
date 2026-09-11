package com.listen.expensetracker.core.notification

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NotificationPreferencesTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(eq("expense_notification_prefs"), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putStringSet(any(), any())).thenReturn(editor)
        whenever(editor.clear()).thenReturn(editor)
    }

    @Test
    fun testNotificationsEnabled_defaultAndSetter() {
        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(true)
        assertTrue(NotificationPreferences.isNotificationsEnabled(context))

        NotificationPreferences.setNotificationsEnabled(context, false)
        verify(editor).putBoolean("notifications_enabled", false)
        verify(editor).apply()
    }

    @Test
    fun testBudgetAlertsEnabled_defaultAndSetter() {
        whenever(prefs.getBoolean(eq("budget_alerts_enabled"), eq(true))).thenReturn(true)
        assertTrue(NotificationPreferences.isBudgetAlertsEnabled(context))

        NotificationPreferences.setBudgetAlertsEnabled(context, false)
        verify(editor).putBoolean("budget_alerts_enabled", false)
    }

    @Test
    fun testBudgetWarningThresholdEnabled_defaultAndSetter() {
        whenever(prefs.getBoolean(eq("budget_warning_threshold_enabled"), eq(true))).thenReturn(true)
        assertTrue(NotificationPreferences.isBudgetWarningThresholdEnabled(context))

        NotificationPreferences.setBudgetWarningThresholdEnabled(context, false)
        verify(editor).putBoolean("budget_warning_threshold_enabled", false)
    }

    @Test
    fun testRecurringBillsAlertsEnabled_defaultAndSetter() {
        whenever(prefs.getBoolean(eq("recurring_alerts_enabled"), eq(true))).thenReturn(true)
        assertTrue(NotificationPreferences.isRecurringBillsAlertsEnabled(context))

        NotificationPreferences.setRecurringBillsAlertsEnabled(context, false)
        verify(editor).putBoolean("recurring_alerts_enabled", false)
    }

    @Test
    fun testAppUpdatesAlertsEnabled_defaultAndSetter() {
        whenever(prefs.getBoolean(eq("update_alerts_enabled"), eq(true))).thenReturn(true)
        assertTrue(NotificationPreferences.isAppUpdatesAlertsEnabled(context))

        NotificationPreferences.setAppUpdatesAlertsEnabled(context, false)
        verify(editor).putBoolean("update_alerts_enabled", false)
    }

    @Test
    fun testDedupState_isNotifiedAndMarkNotified() {
        whenever(prefs.getStringSet(eq("notified_keys"), any())).thenReturn(setOf("2026_09:TOTAL:WARNING"))
        assertTrue(NotificationPreferences.isNotified(context, "2026_09:TOTAL:WARNING"))
        assertFalse(NotificationPreferences.isNotified(context, "2026_09:TOTAL:OVERBUDGET"))

        NotificationPreferences.markNotified(context, "2026_09:TOTAL:OVERBUDGET")
        verify(editor).putStringSet(eq("notified_keys"), eq(setOf("2026_09:TOTAL:WARNING", "2026_09:TOTAL:OVERBUDGET")))
    }

    @Test
    fun testCleanExpiredDedupKeys_preservesCurrentAndPreviousMonth() {
        val oldKeys = setOf(
            "2026_09:TOTAL:WARNING",
            "2026_08:c_food:OVERBUDGET",
            "2026_07:TOTAL:WARNING",
            "2026_06:c_trans:OVERBUDGET"
        )
        whenever(prefs.getStringSet(eq("notified_keys"), any())).thenReturn(oldKeys)

        NotificationPreferences.cleanExpiredDedupKeys(context, currentYear = 2026, currentMonth = 9)

        // Should keep 2026_09 and 2026_08 only
        val expectedKept = setOf("2026_09:TOTAL:WARNING", "2026_08:c_food:OVERBUDGET")
        verify(editor).putStringSet(eq("notified_keys"), eq(expectedKept))
    }

    @Test
    fun testShouldNotifyUpdate_handlesFirstTimeAndCooldown() {
        whenever(prefs.getString(eq("last_update_notified_tag"), any())).thenReturn(null)
        whenever(prefs.getLong(eq("last_update_notified_time"), eq(0L))).thenReturn(0L)

        // First time check should return true
        assertTrue(NotificationPreferences.shouldNotifyUpdate(context, "v1.2.0", currentTime = 1000000L))

        // Same version within 3 days cooldown should return false
        whenever(prefs.getString(eq("last_update_notified_tag"), any())).thenReturn("v1.2.0")
        whenever(prefs.getLong(eq("last_update_notified_time"), eq(0L))).thenReturn(1000000L)
        assertFalse(NotificationPreferences.shouldNotifyUpdate(context, "v1.2.0", currentTime = 1000000L + 1000L))

        // Different version should return true immediately
        assertTrue(NotificationPreferences.shouldNotifyUpdate(context, "v1.3.0", currentTime = 1000000L + 1000L))

        // Same version after 3 days (259200000 ms) should return true
        val afterCooldown = 1000000L + 3 * 24 * 60 * 60 * 1000L + 1L
        assertTrue(NotificationPreferences.shouldNotifyUpdate(context, "v1.2.0", currentTime = afterCooldown))
    }

    @Test
    fun testRecordUpdateNotified_storesTagAndTimestamp() {
        NotificationPreferences.recordUpdateNotified(context, "v1.2.0", timestamp = 55555L)
        verify(editor).putString("last_update_notified_tag", "v1.2.0")
        verify(editor).putLong("last_update_notified_time", 55555L)
    }

    @Test
    fun testClearAll_clearsPreferences() {
        NotificationPreferences.clearAll(context)
        verify(editor).clear()
        verify(editor).apply()
    }
}
