package com.listen.expensetracker.features.recurring.engine

import android.content.Context
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.core.notification.NotificationPreferences
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.NotificationStrings
import com.listen.expensetracker.data.model.AccountRepository

/**
 * 周期账单自动履约入账通知助手 (RecurringNotificationHelper)。
 * 负责在周期引擎自动入库后构建单笔/多笔聚合通知文案并派发通知。
 */
object RecurringNotificationHelper {

    fun notifyRecurringBillsExecuted(
        context: Context,
        executedRules: List<RecurringRuleEntity>,
        currencySymbol: String = "￥",
        lang: String = "zh",
        sendNotification: Boolean = true
    ): Pair<String, String>? {
        if (executedRules.isEmpty()) return null
        if (!NotificationPreferences.isNotificationsEnabled(context) ||
            !NotificationPreferences.isRecurringBillsAlertsEnabled(context)
        ) {
            return null
        }

        return if (executedRules.size == 1) {
            val rule = executedRules.first()
            val accountName = AccountRepository.getAccountDisplayName(rule.accountType, lang)
            val title = NotificationStrings.NOTIFY_RECURRING_SINGLE_TITLE.tr(lang)
            val body = NotificationStrings.NOTIFY_RECURRING_SINGLE_BODY.tr(lang).format(
                rule.title,
                currencySymbol,
                rule.amount.formatAmount(),
                accountName
            )
            if (sendNotification) {
                LocalNotificationManager.sendRecurringBillAlert(
                    context = context,
                    notificationId = LocalNotificationManager.ID_RECURRING_ALERT,
                    title = title,
                    content = body
                )
            }
            Pair(title, body)
        } else {
            val totalExpense = executedRules.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val count = executedRules.size
            val title = NotificationStrings.NOTIFY_RECURRING_MULTI_TITLE.tr(lang).format(count)
            val body = NotificationStrings.NOTIFY_RECURRING_MULTI_BODY.tr(lang).format(
                count,
                currencySymbol,
                totalExpense.formatAmount()
            )
            val details = executedRules.map {
                "• ${it.title}: $currencySymbol${it.amount.formatAmount()}"
            }
            if (sendNotification) {
                LocalNotificationManager.sendRecurringBillAlert(
                    context = context,
                    notificationId = LocalNotificationManager.ID_RECURRING_ALERT,
                    title = title,
                    content = body,
                    details = details
                )
            }
            Pair(title, body)
        }
    }
}
