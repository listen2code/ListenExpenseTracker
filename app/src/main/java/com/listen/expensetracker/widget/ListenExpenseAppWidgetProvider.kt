package com.listen.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.listen.expensetracker.MainActivity
import com.listen.expensetracker.R
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import java.util.Calendar

class ListenExpenseAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId, "￥0.00")
        }
    }

    companion object {
        fun updateFromTransactions(context: Context, allList: List<TransactionEntity>, currencySymbol: String = "￥") {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayExp = allList.filter { it.type == TransactionType.EXPENSE && it.timestamp >= todayStart }.sumOf { it.amount }
            try { updateAllWidgets(context, todayExp, currencySymbol) } catch (_: Exception) {}
        }

        fun updateAllWidgets(context: Context, todayExpense: Double, currencySymbol: String = "￥") {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ListenExpenseAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            val formatted = "$currencySymbol${"%.2f".format(todayExpense)}"

            for (appWidgetId in appWidgetIds) {
                updateWidgetView(context, appWidgetManager, appWidgetId, formatted)
            }
        }

        private fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            amountText: String
        ) {
            val intent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val views = RemoteViews(context.packageName, R.layout.widget_expense_overview).apply {
                setTextViewText(R.id.widget_amount, amountText)
                setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                setOnClickPendingIntent(R.id.widget_action_btn, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
