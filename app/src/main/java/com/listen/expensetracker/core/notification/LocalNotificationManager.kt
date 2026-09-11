package com.listen.expensetracker.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.listen.arch.i18n.tr
import com.listen.expensetracker.MainActivity
import com.listen.expensetracker.R
import com.listen.expensetracker.data.i18n.NotificationStrings

/**
 * 统一本地通知调度管理中枢 (LocalNotificationManager)。
 * 统筹渠道注册、通知构造、权限拦截与系统状态栏安全派发。
 */
object LocalNotificationManager {

    const val CHANNEL_BUDGET_ALERTS = "channel_budget_alerts"
    const val CHANNEL_RECURRING_BILLS = "channel_recurring_bills"
    const val CHANNEL_APP_UPDATES = "channel_app_updates"

    // 固定通知 ID 分段，避免冲突
    const val ID_BUDGET_TOTAL_ALERT = 1001
    const val ID_BUDGET_CATEGORY_ALERT_BASE = 2000
    const val ID_RECURRING_ALERT = 3001
    const val ID_UPDATE_ALERT = 4001

    /**
     * 注册 Android 8.0+ 系统通知渠道矩阵。
     */
    fun createNotificationChannels(context: Context, lang: String = "zh") {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. 预算超支与警戒预警渠道 (高优先级，带震动与横幅)
        val budgetChannel = NotificationChannel(
            CHANNEL_BUDGET_ALERTS,
            NotificationStrings.CHANNEL_BUDGET_ALERTS_NAME.tr(lang),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = NotificationStrings.CHANNEL_BUDGET_ALERTS_DESC.tr(lang)
            enableVibration(true)
            enableLights(true)
        }

        // 2. 周期账单自动履约入账渠道 (默认优先级，温和提示音)
        val recurringChannel = NotificationChannel(
            CHANNEL_RECURRING_BILLS,
            NotificationStrings.CHANNEL_RECURRING_BILLS_NAME.tr(lang),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = NotificationStrings.CHANNEL_RECURRING_BILLS_DESC.tr(lang)
            enableVibration(true)
        }

        // 3. 应用版本更新提醒渠道 (默认优先级)
        val updateChannel = NotificationChannel(
            CHANNEL_APP_UPDATES,
            NotificationStrings.CHANNEL_APP_UPDATES_NAME.tr(lang),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = NotificationStrings.CHANNEL_APP_UPDATES_DESC.tr(lang)
            enableVibration(false)
        }

        manager.createNotificationChannels(listOf(budgetChannel, recurringChannel, updateChannel))
    }

    /**
     * 发送预算超支或预警通知。
     */
    fun sendBudgetAlert(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        categoryId: String? = null
    ): Boolean {
        if (!shouldSendNotification(context) || !NotificationPreferences.isBudgetAlertsEnabled(context)) {
            return false
        }

        val deepLink = if (categoryId != null) {
            "lexpense://budget_center?categoryId=$categoryId"
        } else {
            "lexpense://budget_center"
        }
        val pendingIntent = createDeepLinkPendingIntent(context, notificationId, deepLink)

        val notification = NotificationCompat.Builder(context, CHANNEL_BUDGET_ALERTS)
            .setSmallIcon(R.drawable.widget_app_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return dispatchNotification(context, notificationId, notification)
    }

    /**
     * 发送周期账单自动入账通知（单笔或多笔聚合）。
     */
    fun sendRecurringBillAlert(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        details: List<String> = emptyList()
    ): Boolean {
        if (!shouldSendNotification(context) || !NotificationPreferences.isRecurringBillsAlertsEnabled(context)) {
            return false
        }

        val pendingIntent = createDeepLinkPendingIntent(context, notificationId, "lexpense://transactions?filter=recurring")

        val builder = NotificationCompat.Builder(context, CHANNEL_RECURRING_BILLS)
            .setSmallIcon(R.drawable.widget_app_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (details.isNotEmpty()) {
            val inboxStyle = NotificationCompat.InboxStyle().setBigContentTitle(title)
            details.take(5).forEach { inboxStyle.addLine(it) }
            if (details.size > 5) {
                inboxStyle.setSummaryText("+${details.size - 5} 条更多账单")
            }
            builder.setStyle(inboxStyle)
        } else {
            builder.setStyle(NotificationCompat.BigTextStyle().bigText(content))
        }

        return dispatchNotification(context, notificationId, builder.build())
    }

    /**
     * 发送新版本发布更新提醒通知。
     */
    fun sendAppUpdateAlert(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        versionName: String
    ): Boolean {
        if (!shouldSendNotification(context) || !NotificationPreferences.isAppUpdatesAlertsEnabled(context)) {
            return false
        }

        val pendingIntent = createDeepLinkPendingIntent(context, notificationId, "lexpense://update?version=$versionName")

        val notification = NotificationCompat.Builder(context, CHANNEL_APP_UPDATES)
            .setSmallIcon(R.drawable.widget_app_icon)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        return dispatchNotification(context, notificationId, notification)
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    private fun shouldSendNotification(context: Context): Boolean {
        return NotificationPreferences.isNotificationsEnabled(context) &&
               NotificationPermissionHelper.hasNotificationPermission(context)
    }

    private fun createDeepLinkPendingIntent(context: Context, requestCode: Int, deepLinkUri: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(deepLinkUri)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun dispatchNotification(context: Context, notificationId: Int, notification: android.app.Notification): Boolean {
        return try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
