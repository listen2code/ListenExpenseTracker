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
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.BudgetHealthStatus
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 桌面小部件 2.0 (App Widget 2.0 - 快速记账与预算看板)。
 * 负责桌面 5x2 智能双模看板数据流管理、闪电记账 Intent 路由与常量统一维护。
 * 
 * 设计模式与生命周期说明:
 * - AppWidgetProvider 实际上是一个 BroadcastReceiver。
 * - 它的生命周期主要通过 `onReceive` (分发自定义的点击动作) 和 `onUpdate` (系统发起的冷启动或定期更新) 来驱动。
 */
class ListenExpenseAppWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        ExpenseStrings.init()
        super.onReceive(context, intent)
        // 这里实现了一个局部状态机（widget-local state machine），
        // 用于处理用户点击操作，如月份前后切换和隐藏/显示金额切换，更新状态后立即触发 UI 渲染。
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
        ExpenseStrings.init()
        // 冷启动或添加小组件时，从 Room 和 DataStore 异步提取数据并渲染。
        // 技术决策: 为什么使用 CoroutineScope(Dispatchers.IO) 而不是 viewModelScope?
        // 因为 Widget 的运行环境是一个独立的 BroadcastReceiver 上下文，没有 ViewModel 生命周期，
        // 因此必须使用独立的协程作用域来执行异步的数据库和偏好设置读取。
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
                // 异常回退机制 (try-catch fallback):
                // 如果数据库或数据存储读取失败，回退渲染默认占位数据，
                // 确保小部件在异常情况下也不会变成白板（Blank）。
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

        // 统一小部件与深层链接 (Deep Link) 路由常量，避免在 Activity 中硬编码 (Rule 22)。
        // 采用 URI_SCHEME/URI_HOST 的规范模式，使跨文件的路由配置保持一致且易于维护。
        const val EXTRA_QUICK_ADD_CATEGORY = "extra_quick_add_category"
        const val EXTRA_QUICK_ADD_TYPE = "extra_quick_add_type"
        const val URI_SCHEME = "lexpense"
        const val URI_HOST_QUICK_ADD = "quick_add"
        const val PARAM_CATEGORY = "category"
        const val PARAM_TYPE = "type"

        fun getWidgetMonthOffset(context: Context, widgetId: Int): Int =
            WidgetPreferences.getMonthOffset(context, widgetId)

        fun setWidgetMonthOffset(context: Context, widgetId: Int, offset: Int) =
            WidgetPreferences.setMonthOffset(context, widgetId, offset)

        fun getWidgetHideAmount(context: Context, widgetId: Int): Boolean =
            WidgetPreferences.getHideAmount(context, widgetId)

        fun setWidgetHideAmount(context: Context, widgetId: Int, hide: Boolean) =
            WidgetPreferences.setHideAmount(context, widgetId, hide)

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
            manager: AppWidgetManager,
            widgetId: Int,
            allList: List<TransactionEntity>,
            currency: String = "￥",
            budget: Double = 5000.0,
            lang: String = "zh"
        ) {
            ExpenseStrings.init()
            val offset = getWidgetMonthOffset(context, widgetId)
            val hideAmount = getWidgetHideAmount(context, widgetId)
            val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(offset, lang)
            val totalExpense = calculateMonthlyExpense(allList, startTs, endTs)
            val health = calculateHealthStatus(totalExpense, budget)
            WidgetLayoutBinder.renderWidget(
                context = context, manager = manager, widgetId = widgetId, spent = totalExpense,
                budget = budget, currency = currency, title = title, health = health,
                lang = lang, hideAmount = hideAmount, monthOffset = offset
            )
        }

        /**
         * 统一标准化分类别名映射，兼容外部调用与旧版分类 ID。
         * 统一映射可确保向下兼容（Backward Compatibility）。
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
            return Pair(normalizeCategoryId(rawCategory), type)
        }

        /**
         * 响应式流触发小部件数据刷新。
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
            for (id in appWidgetManager.getAppWidgetIds(componentName)) {
                updateSingleWidget(context, appWidgetManager, id, allList, currencySymbol, monthlyBudget, lang)
            }
        }

        /**
         * 纯计算逻辑：过滤指定月份已发生支出
         */
        fun calculateMonthlyExpense(allList: List<TransactionEntity>, startTs: Long, endTs: Long): Double =
            allList.filter { it.type == TransactionType.EXPENSE && it.timestamp in startTs..endTs }.sumOf { it.amount }

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

        // 统一委托至 WidgetIntentFactory 构建标准 PendingIntent (Rule 21 单文件行数约束)
        internal fun createOpenAppPendingIntent(context: Context): PendingIntent =
            WidgetIntentFactory.createOpenAppPendingIntent(context)

        fun createPrevMonthPendingIntent(context: Context, widgetId: Int): PendingIntent =
            WidgetIntentFactory.createPrevMonthPendingIntent(context, widgetId)

        fun createNextMonthPendingIntent(context: Context, widgetId: Int): PendingIntent =
            WidgetIntentFactory.createNextMonthPendingIntent(context, widgetId)

        fun createToggleEyePendingIntent(context: Context, widgetId: Int): PendingIntent =
            WidgetIntentFactory.createToggleEyePendingIntent(context, widgetId)

        fun createQuickAddPendingIntent(
            context: Context,
            categoryId: String?,
            requestCode: Int,
            type: String = TransactionType.EXPENSE
        ): PendingIntent = WidgetIntentFactory.createQuickAddPendingIntent(context, categoryId, requestCode, type)
    }
}
