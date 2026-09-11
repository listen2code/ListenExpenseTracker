package com.listen.expensetracker.features.budget.engine

import android.content.Context
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.core.notification.NotificationPreferences
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.CategoryBudgetEngine
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.NotificationStrings
import java.util.Calendar
import kotlin.math.abs

enum class BudgetAlertLevel {
    WARNING,
    OVERBUDGET
}

data class BudgetAlert(
    val targetId: String,
    val categoryName: String?,
    val level: BudgetAlertLevel,
    val title: String,
    val message: String,
    val spentAmount: Double,
    val budgetAmount: Double,
    val usageRatio: Float
)

/**
 * 预算预警与超支决策引擎 (BudgetAlertGuard)。
 * 负责在流水变动时评估月度总预算与分类预算的健康度，执行防骚扰去重与单向升级状态机。
 */
object BudgetAlertGuard {

    fun evaluate(
        context: Context,
        allTransactions: List<TransactionEntity>,
        monthOffset: Int = 0,
        totalBudget: Double,
        categoryRatios: Map<String, Float>,
        currencySymbol: String = "￥",
        lang: String = "zh",
        sendNotifications: Boolean = true
    ): List<BudgetAlert> {
        if (!NotificationPreferences.isNotificationsEnabled(context) ||
            !NotificationPreferences.isBudgetAlertsEnabled(context)
        ) {
            return emptyList()
        }

        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val yearMonth = String.format("%04d_%02d", year, month)

        NotificationPreferences.cleanExpiredDedupKeys(context, year, month)

        val result = CategoryBudgetEngine.calculate(
            allTransactions = allTransactions,
            currentOffset = monthOffset,
            totalBudget = totalBudget,
            categoryRatios = categoryRatios
        )

        val alerts = mutableListOf<BudgetAlert>()

        // 1. 月度总预算校验
        if (totalBudget > 0) {
            if (result.totalSpent >= totalBudget) {
                val dedupKey = "$yearMonth:TOTAL:${BudgetAlertLevel.OVERBUDGET.name}"
                if (!NotificationPreferences.isNotified(context, dedupKey)) {
                    val overrun = result.totalSpent - totalBudget
                    val title = NotificationStrings.NOTIFY_BUDGET_OVERRUN_TOTAL_TITLE.tr(lang)
                    val body = NotificationStrings.NOTIFY_BUDGET_OVERRUN_TOTAL_BODY.tr(lang).format(
                        currencySymbol, result.totalSpent.formatAmount(),
                        currencySymbol, overrun.formatAmount()
                    )
                    val alert = BudgetAlert(
                        targetId = "TOTAL",
                        categoryName = null,
                        level = BudgetAlertLevel.OVERBUDGET,
                        title = title,
                        message = body,
                        spentAmount = result.totalSpent,
                        budgetAmount = totalBudget,
                        usageRatio = result.usageRatio
                    )
                    alerts.add(alert)
                    if (sendNotifications) {
                        LocalNotificationManager.sendBudgetAlert(
                            context = context,
                            notificationId = LocalNotificationManager.ID_BUDGET_TOTAL_ALERT,
                            title = title,
                            content = body,
                            categoryId = null
                        )
                    }
                    NotificationPreferences.markNotified(context, dedupKey)
                }
            } else if (result.usageRatio >= 0.8f && NotificationPreferences.isBudgetWarningThresholdEnabled(context)) {
                val dedupKey = "$yearMonth:TOTAL:${BudgetAlertLevel.WARNING.name}"
                if (!NotificationPreferences.isNotified(context, dedupKey)) {
                    val title = NotificationStrings.NOTIFY_BUDGET_WARNING_TOTAL_TITLE.tr(lang)
                    val body = NotificationStrings.NOTIFY_BUDGET_WARNING_TOTAL_BODY.tr(lang).format(
                        result.usageRatio * 100f,
                        currencySymbol,
                        result.remainingBudget.formatAmount()
                    )
                    val alert = BudgetAlert(
                        targetId = "TOTAL",
                        categoryName = null,
                        level = BudgetAlertLevel.WARNING,
                        title = title,
                        message = body,
                        spentAmount = result.totalSpent,
                        budgetAmount = totalBudget,
                        usageRatio = result.usageRatio
                    )
                    alerts.add(alert)
                    if (sendNotifications) {
                        LocalNotificationManager.sendBudgetAlert(
                            context = context,
                            notificationId = LocalNotificationManager.ID_BUDGET_TOTAL_ALERT,
                            title = title,
                            content = body,
                            categoryId = null
                        )
                    }
                    NotificationPreferences.markNotified(context, dedupKey)
                }
            }
        }

        // 2. 分类预算逐项校验
        for (status in result.statusList) {
            if (status.budgetAmount <= 0) continue

            val catName = status.category.getDisplayName(lang)
            val notifId = LocalNotificationManager.ID_BUDGET_CATEGORY_ALERT_BASE + abs(status.category.id.hashCode() % 1000)

            if (status.spentAmount >= status.budgetAmount) {
                val dedupKey = "$yearMonth:${status.category.id}:${BudgetAlertLevel.OVERBUDGET.name}"
                if (!NotificationPreferences.isNotified(context, dedupKey)) {
                    val overrun = status.spentAmount - status.budgetAmount
                    val title = NotificationStrings.NOTIFY_BUDGET_OVERRUN_CAT_TITLE.tr(lang).format(catName)
                    val body = NotificationStrings.NOTIFY_BUDGET_OVERRUN_CAT_BODY.tr(lang).format(
                        currencySymbol, status.spentAmount.formatAmount(),
                        currencySymbol, overrun.formatAmount()
                    )
                    val alert = BudgetAlert(
                        targetId = status.category.id,
                        categoryName = catName,
                        level = BudgetAlertLevel.OVERBUDGET,
                        title = title,
                        message = body,
                        spentAmount = status.spentAmount,
                        budgetAmount = status.budgetAmount,
                        usageRatio = status.usageRatio
                    )
                    alerts.add(alert)
                    if (sendNotifications) {
                        LocalNotificationManager.sendBudgetAlert(
                            context = context,
                            notificationId = notifId,
                            title = title,
                            content = body,
                            categoryId = status.category.id
                        )
                    }
                    NotificationPreferences.markNotified(context, dedupKey)
                }
            } else if (status.usageRatio >= 0.8f && NotificationPreferences.isBudgetWarningThresholdEnabled(context)) {
                val dedupKey = "$yearMonth:${status.category.id}:${BudgetAlertLevel.WARNING.name}"
                if (!NotificationPreferences.isNotified(context, dedupKey)) {
                    val title = NotificationStrings.NOTIFY_BUDGET_WARNING_CAT_TITLE.tr(lang).format(catName)
                    val body = NotificationStrings.NOTIFY_BUDGET_WARNING_CAT_BODY.tr(lang).format(
                        status.usageRatio * 100f
                    )
                    val alert = BudgetAlert(
                        targetId = status.category.id,
                        categoryName = catName,
                        level = BudgetAlertLevel.WARNING,
                        title = title,
                        message = body,
                        spentAmount = status.spentAmount,
                        budgetAmount = status.budgetAmount,
                        usageRatio = status.usageRatio
                    )
                    alerts.add(alert)
                    if (sendNotifications) {
                        LocalNotificationManager.sendBudgetAlert(
                            context = context,
                            notificationId = notifId,
                            title = title,
                            content = body,
                            categoryId = status.category.id
                        )
                    }
                    NotificationPreferences.markNotified(context, dedupKey)
                }
            }
        }

        return alerts
    }
}
