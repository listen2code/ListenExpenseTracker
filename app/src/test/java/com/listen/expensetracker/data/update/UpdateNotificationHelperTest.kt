package com.listen.expensetracker.data.update

import android.content.Context
import android.content.SharedPreferences
import com.listen.expensetracker.data.i18n.ExpenseStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 软件更新通知辅助器单元测试
 */
class UpdateNotificationHelperTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()

    @Before
    fun setUp() {
        ExpenseStrings.init()
        whenever(context.getSharedPreferences(eq("expense_notification_prefs"), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.apply()).then { }

        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(true)
        whenever(prefs.getBoolean(eq("update_alerts_enabled"), eq(true))).thenReturn(true)
    }

    private fun createRelease(tag: String, title: String, changelog: String): ReleaseInfo {
        return ReleaseInfo(
            tagName = tag,
            title = title,
            changelog = changelog,
            htmlUrl = "https://example.com/releases",
            apkDownloadUrl = "https://example.com/download.apk"
        )
    }

    @Test
    fun testNotifyUpdateAvailable_disabled() {
        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(false)
        val release = createRelease("v2.0.0", "Release 2.0", "- New features")

        val result = UpdateNotificationHelper.notifyUpdateAvailable(
            context = context,
            releaseInfo = release,
            sendNotification = false
        )
        assertNull(result)
    }

    @Test
    fun testNotifyUpdateAvailable_alertsDisabled() {
        whenever(prefs.getBoolean(eq("update_alerts_enabled"), eq(true))).thenReturn(false)
        val release = createRelease("v2.0.0", "Release 2.0", "- New features")

        val result = UpdateNotificationHelper.notifyUpdateAvailable(
            context = context,
            releaseInfo = release,
            sendNotification = false
        )
        assertNull(result)
    }

    @Test
    fun testNotifyUpdateAvailable_inCooldown() {
        val now = 1000000000L
        whenever(prefs.getString(eq("last_update_notified_tag"), eq(""))).thenReturn("v2.0.0")
        whenever(prefs.getLong(eq("last_update_notified_time"), eq(0L))).thenReturn(now - 3600 * 1000L)

        val release = createRelease("v2.0.0", "Release 2.0", "- New features")
        val result = UpdateNotificationHelper.notifyUpdateAvailable(
            context = context,
            releaseInfo = release,
            currentTime = now,
            sendNotification = false
        )
        assertNull(result)
    }

    @Test
    fun testNotifyUpdateAvailable_successAfterCooldown() {
        val now = 1000000000L
        whenever(prefs.getString(eq("last_update_notified_tag"), eq(""))).thenReturn("v2.0.0")
        whenever(prefs.getLong(eq("last_update_notified_time"), eq(0L))).thenReturn(now - 4 * 86400 * 1000L)

        val release = createRelease("v2.0.0", "Release 2.0", "- 优化性能\n- 修复缺陷")
        val result = UpdateNotificationHelper.notifyUpdateAvailable(
            context = context,
            releaseInfo = release,
            lang = "zh",
            currentTime = now,
            sendNotification = false
        )
        assertNotNull(result)
        val (title, body) = result!!
        assertTrue(title.contains("v2.0.0"))
        assertTrue(body.contains("优化性能"))
        verify(editor).putString(eq("last_update_notified_tag"), eq("v2.0.0"))
        verify(editor).putLong(eq("last_update_notified_time"), eq(now))
    }

    @Test
    fun testNotifyUpdateAvailable_differentNewVersion() {
        val now = 1000000000L
        whenever(prefs.getString(eq("last_update_notified_tag"), eq(""))).thenReturn("v1.9.0")
        whenever(prefs.getLong(eq("last_update_notified_time"), eq(0L))).thenReturn(now - 1000L)

        val release = createRelease("v2.0.0", "Major Release", "全新体验发布")
        val result = UpdateNotificationHelper.notifyUpdateAvailable(
            context = context,
            releaseInfo = release,
            lang = "zh",
            currentTime = now,
            sendNotification = false
        )
        assertNotNull(result)
        val (title, body) = result!!
        assertTrue(title.contains("v2.0.0"))
        assertTrue(body.contains("全新体验发布"))
    }
}
