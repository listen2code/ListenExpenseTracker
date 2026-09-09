package com.listen.expensetracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.listen.arch.i18n.tr
import com.listen.expensetracker.R
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.BudgetHealthStatus

/**
 * 负责桌面小部件的 RemoteViews 布局绑定与 UI 渲染。
 * 从 ListenExpenseAppWidgetProvider 中拆分以遵守单一职责并严格保证单文件行数不超过 250 行。
 */
object WidgetLayoutBinder {

    fun renderWidget(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        spent: Double,
        budget: Double,
        currency: String,
        title: String,
        health: BudgetHealthStatus,
        lang: String,
        hideAmount: Boolean = false,
        monthOffset: Int = 0
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_expense_overview)
        val formattedSpent = if (hideAmount) "••••" else "$currency${spent.formatAmount()}"
        val remaining = budget - spent
        val usageRatio = if (budget > 0) (spent / budget).toFloat() else 0f
        val progressPercent = (usageRatio * 100).toInt().coerceIn(0, 100)

        // 1. 设置当月看板标题与金额（月份精简呈现，支出移动至金额旁）
        val displayMonthTitle = if (title.contains("(")) {
            title.substringAfter("(").substringBefore(")")
        } else {
            title
        }
        views.setTextViewText(R.id.widget_month_title, displayMonthTitle)
        views.setTextViewText(R.id.widget_spent_label, AppStrings.TYPE_EXPENSE.tr(lang))
        views.setTextViewText(R.id.widget_spent_amount, formattedSpent)

        // 代码端阶梯式动态降阶字号 (兜底部分启动器不支持 XML autoSizeTextType，彻底杜绝 ...)
        val isEn = lang.equals("en", ignoreCase = true)
        val targetSpentSp = when {
            formattedSpent.length <= 6 -> if (isEn) 16.5f else 18f   // ￥0 ~ ￥999 或 ••••
            formattedSpent.length <= 8 -> if (isEn) 14.5f else 15.5f // ￥1,234
            formattedSpent.length <= 10 -> if (isEn) 12f else 13f    // ￥12,345
            formattedSpent.length <= 12 -> 10.5f                     // ￥123,456
            else -> 9.5f                                             // 百万级大金额
        }
        views.setTextViewTextSize(R.id.widget_spent_amount, TypedValue.COMPLEX_UNIT_SP, targetSpentSp)

        val remainingText = if (budget > 0) {
            if (hideAmount) {
                if (remaining >= 0) "${AppStrings.BUDGET_REMAINING_PREFIX.tr(lang)} ••••" else "${AppStrings.BUDGET_OVER_PREFIX.tr(lang)} ••••"
            } else if (remaining >= 0) {
                "${AppStrings.BUDGET_REMAINING_PREFIX.tr(lang)} $currency${remaining.formatAmount()}"
            } else {
                "${AppStrings.BUDGET_OVER_PREFIX.tr(lang)} $currency${(-remaining).formatAmount()}"
            }
        } else {
            AppStrings.BUDGET_NO_LIMIT.tr(lang)
        }
        views.setTextViewText(R.id.widget_budget_remaining, remainingText)
        val targetRemainingSp = if (remainingText.length > 11) 9.5f else 11f
        views.setTextViewTextSize(R.id.widget_budget_remaining, TypedValue.COMPLEX_UNIT_SP, targetRemainingSp)

        // 2. 小眼睛图标状态绑定
        val eyeIcon = if (hideAmount) R.drawable.widget_ic_eye_off else R.drawable.widget_ic_eye
        views.setImageViewResource(R.id.widget_btn_toggle_eye, eyeIcon)

        // 3. 健康状态徽章与三态彩色进度条显隐联动
        val (badgeText, badgeBg, badgeColor) = when (health) {
            BudgetHealthStatus.NORMAL -> Triple(AppStrings.BUDGET_STATUS_NORMAL.tr(lang), R.drawable.widget_badge_normal, R.color.widget_health_normal)
            BudgetHealthStatus.WARNING -> Triple(AppStrings.BUDGET_STATUS_WARNING.tr(lang), R.drawable.widget_badge_warning, R.color.widget_health_warning)
            BudgetHealthStatus.OVERBUDGET -> Triple(AppStrings.BUDGET_STATUS_OVER.tr(lang), R.drawable.widget_badge_over, R.color.widget_health_over)
        }
        views.setTextViewText(R.id.widget_health_badge, badgeText)
        views.setInt(R.id.widget_health_badge, "setBackgroundResource", badgeBg)
        views.setTextColor(R.id.widget_health_badge, ContextCompat.getColor(context, badgeColor))

        // 联动三态进度条显隐并设置进度
        views.setViewVisibility(R.id.widget_budget_progress_normal, if (health == BudgetHealthStatus.NORMAL) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_budget_progress_warning, if (health == BudgetHealthStatus.WARNING) View.VISIBLE else View.GONE)
        views.setViewVisibility(R.id.widget_budget_progress_over, if (health == BudgetHealthStatus.OVERBUDGET) View.VISIBLE else View.GONE)

        val activeProgressBarId = when (health) {
            BudgetHealthStatus.NORMAL -> R.id.widget_budget_progress_normal
            BudgetHealthStatus.WARNING -> R.id.widget_budget_progress_warning
            BudgetHealthStatus.OVERBUDGET -> R.id.widget_budget_progress_over
        }
        views.setProgressBar(activeProgressBarId, 100, progressPercent, false)

        // 4. 动态配置 4 大高频快捷分类按钮文案（上下布局，仅需更新文本子控件）
        views.setTextViewText(R.id.widget_btn_food_text, AppStrings.CAT_FOOD.tr(lang))
        views.setTextViewText(R.id.widget_btn_transport_text, AppStrings.CAT_TRANSPORT.tr(lang))
        views.setTextViewText(R.id.widget_btn_shopping_text, AppStrings.CAT_SHOPPING.tr(lang))
        views.setTextViewText(R.id.widget_btn_daily_text, AppStrings.CAT_OTHER_EXP.tr(lang))

        // 5. 意图路由绑定 (月份左右切换、眼睛显隐切换、打开 App、4个快捷记账)
        views.setOnClickPendingIntent(R.id.widget_btn_prev_month, ListenExpenseAppWidgetProvider.createPrevMonthPendingIntent(context, widgetId))
        views.setOnClickPendingIntent(R.id.widget_btn_next_month, ListenExpenseAppWidgetProvider.createNextMonthPendingIntent(context, widgetId))
        views.setOnClickPendingIntent(R.id.widget_btn_toggle_eye, ListenExpenseAppWidgetProvider.createToggleEyePendingIntent(context, widgetId))

        val openAppPendingIntent = ListenExpenseAppWidgetProvider.createOpenAppPendingIntent(context)
        // 防误触优化：移除外层 card 与 root 的全局兜底绑定，将应用拉起精准约束于金额、预算及标题等核心内容区域
        views.setOnClickPendingIntent(R.id.widget_spent_container, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_spent_amount, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_budget_remaining, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_month_title, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_app_icon, openAppPendingIntent)
        views.setOnClickPendingIntent(R.id.widget_health_badge, openAppPendingIntent)

        views.setOnClickPendingIntent(R.id.widget_btn_food, ListenExpenseAppWidgetProvider.createQuickAddPendingIntent(context, ListenExpenseAppWidgetProvider.CAT_FOOD, 201))
        views.setOnClickPendingIntent(R.id.widget_btn_transport, ListenExpenseAppWidgetProvider.createQuickAddPendingIntent(context, ListenExpenseAppWidgetProvider.CAT_TRANSPORT, 202))
        views.setOnClickPendingIntent(R.id.widget_btn_shopping, ListenExpenseAppWidgetProvider.createQuickAddPendingIntent(context, ListenExpenseAppWidgetProvider.CAT_SHOPPING, 203))
        views.setOnClickPendingIntent(R.id.widget_btn_daily, ListenExpenseAppWidgetProvider.createQuickAddPendingIntent(context, ListenExpenseAppWidgetProvider.CAT_DAILY, 204))

        manager.updateAppWidget(widgetId, views)
    }
}
