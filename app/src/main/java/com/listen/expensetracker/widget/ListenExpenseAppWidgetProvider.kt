package com.listen.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.listen.expensetracker.MainActivity
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
 * 负责桌面 5x2 智能双模看板数据流管理、闪电记账 Intent 路由与常量统一维护。
 */
class ListenExpenseAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        when (action) {
            ACTION_PREV_MONTH -> {
                val currentOffset = getWidgetMonthOffset(context, widgetId)
                setWidgetMonthOffset(context, widgetId, currentOffset - 1)
                triggerWidgetUpdate(context, widgetId)
            }
            ACTION_NEXT_MONTH -> {
                val currentOffset = getWidgetMonthOffset(context, widgetId)
                setWidgetMonthOffset(context, widgetId, currentOffset + 1)
                triggerWidgetUpdate(context, widgetId)
            }
            ACTION_TOGGLE_HIDE_AMOUNT -> {
                val currentHide = getWidgetHideAmount(context, widgetId)
                setWidgetHideAmount(context, widgetId, !currentHide)
                triggerWidgetUpdate(context, widgetId)
            }
        }
    }

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
                for (id in appWidgetIds) {
                    updateSingleWidget(context, appWidgetManager, id, allList, prefs.currencySymbol, prefs.monthlyBudget, prefs.language)
                }
            } catch (_: Exception) {
                val (_, _, defaultTitle) = TransactionCalculationEngine.getMonthRangeAndTitle(0, "zh")
                for (id in appWidgetIds) {
                    WidgetLayoutBinder.renderWidget(context, appWidgetManager, id, 0.0, 5000.0, "￥", defaultTitle, BudgetHealthStatus.NORMAL, "zh")
                }
            }
        }
    }

    companion object {
        // 快捷分类标识
        const val CAT_FOOD = "c_food"
        const val CAT_TRANSPORT = "c_transport"
        const val CAT_SHOPPING = "c_shopping"
        const val CAT_DAILY = "c_other_exp"

        // 小部件自定义广播 Action
        const val ACTION_PREV_MONTH = "com.listen.expensetracker.widget.ACTION_PREV_MONTH"
        const val ACTION_NEXT_MONTH = "com.listen.expensetracker.widget.ACTION_NEXT_MONTH"
        const val ACTION_TOGGLE_HIDE_AMOUNT = "com.listen.expensetracker.widget.ACTION_TOGGLE_HIDE_AMOUNT"

        // 偏好持久化 Key
        private const val PREFS_NAME = "listen_expense_widget_prefs"
        private const val KEY_OFFSET_PREFIX = "widget_month_offset_"
        private const val KEY_HIDE_PREFIX = "widget_hide_amount_"

        // 统一小部件与深层链接 (Deep Link) 路由常量，避免在 Activity 中硬编码 (Rule 22)
        const val EXTRA_QUICK_ADD_CATEGORY = "extra_quick_add_category"
        const val EXTRA_QUICK_ADD_TYPE = "extra_quick_add_type"
        const val URI_SCHEME = "lexpense"
        const val URI_HOST_QUICK_ADD = "quick_add"
        const val PARAM_CATEGORY = "category"
        const val PARAM_TYPE = "type"

        fun getWidgetMonthOffset(context: Context, widgetId: Int): Int {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sp.getInt("$KEY_OFFSET_PREFIX$widgetId", 0)
        }

        fun setWidgetMonthOffset(context: Context, widgetId: Int, offset: Int) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().putInt("$KEY_OFFSET_PREFIX$widgetId", offset).apply()
        }

        fun getWidgetHideAmount(context: Context, widgetId: Int): Boolean {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return sp.getBoolean("$KEY_HIDE_PREFIX$widgetId", false)
        }

        fun setWidgetHideAmount(context: Context, widgetId: Int, hide: Boolean) {
            val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sp.edit().putBoolean("$KEY_HIDE_PREFIX$widgetId", hide).apply()
        }

        private fun triggerWidgetUpdate(context: Context, widgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val allList = db.transactionDao().getAllTransactions()
                    val prefManager = ExpenseDataStoreManager(context)
                    val prefs = prefManager.preferencesFlow.first()
                    val appWidgetManager = AppWidgetManager.getInstance(context)
                    updateSingleWidget(context, appWidgetManager, widgetId, allList, prefs.currencySymbol, prefs.monthlyBudget, prefs.language)
                } catch (_: Exception) {}
            }
        }

        fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            allList: List<TransactionEntity>,
            currencySymbol: String = "￥",
            monthlyBudget: Double = 5000.0,
            lang: String = "zh"
        ) {
            val offset = getWidgetMonthOffset(context, appWidgetId)
            val hideAmount = getWidgetHideAmount(context, appWidgetId)
            val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(offset, lang)
            val totalExpense = calculateMonthlyExpense(allList, startTs, endTs)
            val health = calculateHealthStatus(totalExpense, monthlyBudget)
            WidgetLayoutBinder.renderWidget(
                context = context,
                manager = appWidgetManager,
                widgetId = appWidgetId,
                spent = totalExpense,
                budget = monthlyBudget,
                currency = currencySymbol,
                title = title,
                health = health,
                lang = lang,
                hideAmount = hideAmount,
                monthOffset = offset
            )
        }

        /**
         * 统一标准化分类别名映射，兼容外部调用与旧版分类 ID
         */
        fun normalizeCategoryId(raw: String?): String? = when (raw) {
            "cat_food", "c_food" -> "c_food"
            "cat_transport", "c_transport" -> "c_transport"
            "cat_shopping", "c_shopping" -> "c_shopping"
            "cat_daily", "cat_other", "c_other_exp" -> "c_other_exp"
            else -> raw
        }

        /**
         * 解析小部件或外部 DeepLink 触发的快速记账意图。
         * 若匹配则返回 Pair(categoryId, transactionType)，否则返回 null。
         */
        fun parseQuickAddIntent(intent: Intent?): Pair<String?, String>? {
            if (intent == null) return null
            val uri = intent.data
            val isQuickAddUri = uri != null && uri.scheme == URI_SCHEME && uri.host == URI_HOST_QUICK_ADD
            val isQuickAddExtra = intent.hasExtra(EXTRA_QUICK_ADD_CATEGORY) || intent.hasExtra(EXTRA_QUICK_ADD_TYPE)
            if (!isQuickAddUri && !isQuickAddExtra) return null

            val rawCategory = uri?.getQueryParameter(PARAM_CATEGORY) ?: intent.getStringExtra(EXTRA_QUICK_ADD_CATEGORY)
            val type = uri?.getQueryParameter(PARAM_TYPE) ?: intent.getStringExtra(EXTRA_QUICK_ADD_TYPE) ?: TransactionType.EXPENSE
            val categoryId = normalizeCategoryId(rawCategory)
            return Pair(categoryId, type)
        }

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
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ListenExpenseAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            for (appWidgetId in appWidgetIds) {
                updateSingleWidget(context, appWidgetManager, appWidgetId, allList, currencySymbol, monthlyBudget, lang)
            }
        }

        /**
         * 纯计算逻辑：过滤指定月份已发生支出
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

        internal fun createOpenAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_MAIN
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(context, 100, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }

        fun createPrevMonthPendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, ListenExpenseAppWidgetProvider::class.java).apply {
                action = ACTION_PREV_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId * 10 + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createNextMonthPendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, ListenExpenseAppWidgetProvider::class.java).apply {
                action = ACTION_NEXT_MONTH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId * 10 + 2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun createToggleEyePendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, ListenExpenseAppWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_HIDE_AMOUNT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId * 10 + 3,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
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
                    "$URI_SCHEME://$URI_HOST_QUICK_ADD?$PARAM_CATEGORY=$categoryId&$PARAM_TYPE=$type".toUri()
                } else {
                    "$URI_SCHEME://$URI_HOST_QUICK_ADD?$PARAM_TYPE=$type".toUri()
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (categoryId != null) {
                    putExtra(EXTRA_QUICK_ADD_CATEGORY, categoryId)
                }
                putExtra(EXTRA_QUICK_ADD_TYPE, type)
            }
            return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
