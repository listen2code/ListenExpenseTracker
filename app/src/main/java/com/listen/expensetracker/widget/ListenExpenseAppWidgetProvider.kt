package com.listen.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.listen.expensetracker.MainActivity
import com.listen.expensetracker.R
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
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
                val currency = prefManager.currencySymbolFlow.first()
                val budget = prefManager.monthlyBudgetFlow.first()
                updateFromTransactions(context, allList, currency, budget)
            } catch (_: Exception) {
                for (id in appWidgetIds) {
                    renderWidget(context, appWidgetManager, id, 0.0, 5000.0, "￥", "本月支出", BudgetHealthStatus.NORMAL)
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
            monthlyBudget: Double = 5000.0
        ) {
            val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(0, "zh")
            val totalExpense = calculateMonthlyExpense(allList, startTs, endTs)
            val health = calculateHealthStatus(totalExpense, monthlyBudget)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ListenExpenseAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                renderWidget(context, appWidgetManager, appWidgetId, totalExpense, monthlyBudget, currencySymbol, title, health)
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
            health: BudgetHealthStatus
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_expense_overview)
            val formattedSpent = "$currency${"%.2f".format(spent)}"
            val remaining = budget - spent
            val usageRatio = if (budget > 0) (spent / budget).toFloat() else 0f
            val progressPercent = (usageRatio * 100).toInt().coerceIn(0, 100)

            // 1. 设置当月看板标题与金额
            views.setTextViewText(R.id.widget_month_title, "$title · 支出")
            views.setTextViewText(R.id.widget_spent_amount, formattedSpent)

            val remainingText = if (budget > 0) {
                if (remaining >= 0) "剩余 $currency${"%.2f".format(remaining)}" else "已超支 $currency${"%.2f".format(-remaining)}"
            } else {
                "未设置总预算"
            }
            views.setTextViewText(R.id.widget_budget_remaining, remainingText)
            views.setProgressBar(R.id.widget_budget_progress, 100, progressPercent, false)

            // 2. 健康状态徽章渲染
            val (badgeText, badgeBg, badgeColor) = when (health) {
                BudgetHealthStatus.NORMAL -> Triple("正常", R.drawable.widget_badge_normal, R.color.widget_health_normal)
                BudgetHealthStatus.WARNING -> Triple("预警", R.drawable.widget_badge_warning, R.color.widget_health_warning)
                BudgetHealthStatus.OVERBUDGET -> Triple("超支", R.drawable.widget_badge_over, R.color.widget_health_over)
            }
            views.setTextViewText(R.id.widget_health_badge, badgeText)
            views.setInt(R.id.widget_health_badge, "setBackgroundResource", badgeBg)
            views.setTextColor(R.id.widget_health_badge, ContextCompat.getColor(context, badgeColor))

            // 3. 意图路由绑定
            val openAppPendingIntent = createOpenAppPendingIntent(context)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_budget_card, openAppPendingIntent)

            views.setOnClickPendingIntent(R.id.widget_btn_food, createQuickAddPendingIntent(context, CAT_FOOD, 201))
            views.setOnClickPendingIntent(R.id.widget_btn_transport, createQuickAddPendingIntent(context, CAT_TRANSPORT, 202))
            views.setOnClickPendingIntent(R.id.widget_btn_shopping, createQuickAddPendingIntent(context, CAT_SHOPPING, 203))
            views.setOnClickPendingIntent(R.id.widget_btn_daily, createQuickAddPendingIntent(context, CAT_DAILY, 204))
            views.setOnClickPendingIntent(R.id.widget_btn_general, createQuickAddPendingIntent(context, null, 205))

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
