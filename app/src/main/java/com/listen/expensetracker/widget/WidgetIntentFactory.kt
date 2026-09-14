package com.listen.expensetracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.listen.expensetracker.MainActivity
import com.listen.expensetracker.data.db.TransactionType

/**
 * Factory responsible for creating standard PendingIntents for App Widget interactions.
 * Isolates Intent construction, request codes, and flags to maintain separation of concerns.
 */
object WidgetIntentFactory {

    fun createOpenAppPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            100,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createPrevMonthPendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, ListenExpenseAppWidgetProvider::class.java).apply {
            action = ListenExpenseAppWidgetProvider.ACTION_PREV_MONTH
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
            action = ListenExpenseAppWidgetProvider.ACTION_NEXT_MONTH
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
            action = ListenExpenseAppWidgetProvider.ACTION_TOGGLE_HIDE_AMOUNT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId * 10 + 3,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createResetMonthPendingIntent(context: Context, widgetId: Int): PendingIntent {
        val intent = Intent(context, ListenExpenseAppWidgetProvider::class.java).apply {
            action = ListenExpenseAppWidgetProvider.ACTION_RESET_MONTH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            widgetId * 10 + 4,
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
                "${ListenExpenseAppWidgetProvider.URI_SCHEME}://${ListenExpenseAppWidgetProvider.URI_HOST_QUICK_ADD}?${ListenExpenseAppWidgetProvider.PARAM_CATEGORY}=$categoryId&${ListenExpenseAppWidgetProvider.PARAM_TYPE}=$type".toUri()
            } else {
                "${ListenExpenseAppWidgetProvider.URI_SCHEME}://${ListenExpenseAppWidgetProvider.URI_HOST_QUICK_ADD}?${ListenExpenseAppWidgetProvider.PARAM_TYPE}=$type".toUri()
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (categoryId != null) {
                putExtra(ListenExpenseAppWidgetProvider.EXTRA_QUICK_ADD_CATEGORY, categoryId)
            }
            putExtra(ListenExpenseAppWidgetProvider.EXTRA_QUICK_ADD_TYPE, type)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
