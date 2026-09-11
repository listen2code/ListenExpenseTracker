package com.listen.expensetracker.data.update

import android.content.Context
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.core.notification.NotificationPreferences
import com.listen.expensetracker.data.i18n.NotificationStrings

/**
 * 版本更新通知决策与频控助手 (UpdateNotificationHelper)。
 * 负责在检测到可用新版本时核验 3 天频控冷却，构建通知摘要并派发提醒。
 */
object UpdateNotificationHelper {

    fun notifyUpdateAvailable(
        context: Context,
        releaseInfo: ReleaseInfo,
        lang: String = "zh",
        currentTime: Long = System.currentTimeMillis(),
        sendNotification: Boolean = true
    ): Pair<String, String>? {
        if (!NotificationPreferences.isNotificationsEnabled(context) ||
            !NotificationPreferences.isAppUpdatesAlertsEnabled(context)
        ) {
            return null
        }

        if (!NotificationPreferences.shouldNotifyUpdate(context, releaseInfo.tagName, currentTime)) {
            return null
        }

        val title = NotificationStrings.NOTIFY_UPDATE_TITLE.tr(lang).format(releaseInfo.tagName)
        val changelogPreview = releaseInfo.changelog.lines()
            .map { it.trim().removePrefix("-").removePrefix("*").trim() }
            .firstOrNull { it.isNotBlank() }
            ?: releaseInfo.title

        val body = NotificationStrings.NOTIFY_UPDATE_BODY.tr(lang).format(changelogPreview)

        if (sendNotification) {
            LocalNotificationManager.sendAppUpdateAlert(
                context = context,
                notificationId = LocalNotificationManager.ID_UPDATE_ALERT,
                title = title,
                content = body,
                versionName = releaseInfo.tagName
            )
        }

        NotificationPreferences.recordUpdateNotified(context, releaseInfo.tagName, currentTime)
        return Pair(title, body)
    }
}
