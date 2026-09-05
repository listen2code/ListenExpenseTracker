package com.listen.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.listen.arch.i18n.tr
import com.listen.expensetracker.MainActivity
import com.listen.expensetracker.R
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.BudgetHealthStatus
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 桌面小部件 2.0 (App Widget 2.0 - 快速记账与预算看板)。
 * 负责桌面 4x2 智能双模看板渲染、4 大高频分类闪电记账快捷直达与数据实时联动。
 */
class ListenExpenseAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // 冷启动或添加小组件时，从 Room 和 DataStore 异步提取数据并渲染
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val allList = db.transactionDao().getAllTransactions()
                val prefManager = ExpenseDataStoreManager(context)
                val prefs = prefManager.preferencesFlow.first()
                updateFromTransactions(context, allList, prefs.currencySymbol, prefs.monthlyBudget, prefs.language)
            } catch (_: Exception) {
                val (_, _, defaultTitle) = TransactionCalculationEngine.getMonthRangeAndTitle(0, "zh")
                for (id in appWidgetIds) {
                    renderWidget(context, appWidgetManager, id, 0.0, 5000.0, "￥", defaultTitle, BudgetHealthStatus.NORMAL, "zh")
                }
            }
        }
    }

    companion object {
        const val CAT_FOOD = "c_food"
        const val CAT_TRANSPORT = "c_transport"
        const val CAT_SHOPPING = "c_shopping"
        const val CAT_DAILY = "c_other_exp"

        /**
         * 响应式流触发小部件数据刷新
         */
        fun updateFromTransactions(
            context: Context,
            allList: List<TransactionEntity>,
            currencySymbol: String = "￥",
            monthlyBudget: Double = 5000.0,
            lang: String = "zh"
        ) {
            val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(0, lang)
            val totalExpense = calculateMonthlyExpense(allList, startTs, endTs)
            val health = calculateHealthStatus(totalExpense, monthlyBudget)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ListenExpenseAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                renderWidget(context, appWidgetManager, appWidgetId, totalExpense, monthlyBudget, currencySymbol, title, health, lang)
            }
        }

        /**
         * 纯计算逻辑：过滤当月已发生支出
         */
        fun calculateMonthlyExpense(allList: List<TransactionEntity>, startTs: Long, endTs: Long): Double {
            return allList.filter { it.type == TransactionType.EXPENSE && it.timestamp in startTs..endTs }.sumOf { it.amount }
        }

        /**
         * 纯计算逻辑：根据月度支出与总预算判定健康状况
         */
        fun calculateHealthStatus(spent: Double, budget: Double): BudgetHealthStatus {
            if (budget <= 0.0) return BudgetHealthStatus.NORMAL
            val ratio = (spent / budget).toFloat()
            return when {
                spent >= budget -> BudgetHealthStatus.OVERBUDGET
                ratio >= 0.8f -> BudgetHealthStatus.WARNING
                else -> BudgetHealthStatus.NORMAL
            }
        }

        private fun renderWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            spent: Double,
            budget: Double,
            currency: String,
            title: String,
            health: BudgetHealthStatus,
            lang: String
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_expense_overview)
            val formattedSpent = "$currency${spent.formatAmount()}"
            val remaining = budget - spent
            val usageRatio = if (budget > 0) (spent / budget).toFloat() else 0f
            val progressPercent = (usageRatio * 100).toInt().coerceIn(0, 100)

            // 1. 设置当月看板标题与金额（精炼月份标签，多语言收口）
            val displayMonthTitle = if (title.contains("(")) {
                val monthPure = title.substringAfter("(").substringBefore(")")
                AppStrings.WIDGET_MONTH_EXPENSE.tr(lang).format(monthPure)
            } else {
                "$title · ${AppStrings.TYPE_EXPENSE.tr(lang)}"
            }
            views.setTextViewText(R.id.widget_month_title, displayMonthTitle)
            views.setTextViewText(R.id.widget_spent_amount, formattedSpent)

            // 代码端阶梯式动态降阶字号 (兜底部分启动器不支持 XML autoSizeTextType，彻底杜绝 ...)
            val targetSpentSp = when {
                formattedSpent.length <= 6 -> 19f   // ￥0 ~ ￥999
                formattedSpent.length <= 8 -> 16f   // ￥1,234
                formattedSpent.length <= 10 -> 13.5f // ￥12,345
                formattedSpent.length <= 12 -> 11.5f // ￥123,456
                else -> 9.5f                        // 百万级大金额
            }
            views.setTextViewTextSize(R.id.widget_spent_amount, TypedValue.COMPLEX_UNIT_SP, targetSpentSp)

            val remainingText = if (budget > 0) {
                if (remaining >= 0) {
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

            // 2. 健康状态徽章与三态彩色进度条显隐联动
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

            // 3. 动态配置 4 大高频快捷分类按钮文案（支持多语言国际化切换）
            views.setTextViewText(R.id.widget_btn_food, "🍔 ${AppStrings.CAT_FOOD.tr(lang)}")
            views.setTextViewText(R.id.widget_btn_transport, "🚗 ${AppStrings.CAT_TRANSPORT.tr(lang)}")
            views.setTextViewText(R.id.widget_btn_shopping, "🛍️ ${AppStrings.CAT_SHOPPING.tr(lang)}")
            views.setTextViewText(R.id.widget_btn_daily, "📦 ${AppStrings.CAT_OTHER_EXP.tr(lang)}")

            // 4. 意图路由绑定
            val openAppPendingIntent = createOpenAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_budget_card, openAppPendingIntent)

            views.setOnClickPendingIntent(R.id.widget_btn_food, createQuickAddPendingIntent(context, CAT_FOOD, 201))
            views.setOnClickPendingIntent(R.id.widget_btn_transport, createQuickAddPendingIntent(context, CAT_TRANSPORT, 202))
            views.setOnClickPendingIntent(R.id.widget_btn_shopping, createQuickAddPendingIntent(context, CAT_SHOPPING, 203))
            views.setOnClickPendingIntent(R.id.widget_btn_daily, createQuickAddPendingIntent(context, CAT_DAILY, 204))

            manager.updateAppWidget(widgetId, views)
        }

        private fun createOpenAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun createQuickAddPendingIntent(
            context: Context,
            categoryId: String?,
            requestCode: Int,
            type: String = TransactionType.EXPENSE
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = if (categoryId != null) {
                    Uri.parse("lexpense://quick_add?category=$categoryId&type=$type")
                } else {
                    Uri.parse("lexpense://quick_add?type=$type")
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (categoryId != null) {
                    putExtra(MainActivity.EXTRA_QUICK_ADD_CATEGORY, categoryId)
                }
                putExtra(MainActivity.EXTRA_QUICK_ADD_TYPE, type)
            }
            return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
