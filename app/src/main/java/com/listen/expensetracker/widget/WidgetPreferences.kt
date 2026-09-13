package com.listen.expensetracker.widget

import android.content.Context
import androidx.core.content.edit

/**
 * SharedPreferences storage helper for individual App Widget instance states.
 * Manages month offsets and privacy toggle states keyed by widgetId.
 */
object WidgetPreferences {
    private const val PREFS_NAME = "listen_expense_widget_prefs"
    private const val KEY_OFFSET_PREFIX = "widget_month_offset_"
    private const val KEY_HIDE_PREFIX = "widget_hide_amount_"

    fun getMonthOffset(context: Context, widgetId: Int): Int {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getInt("$KEY_OFFSET_PREFIX$widgetId", 0)
    }

    fun setMonthOffset(context: Context, widgetId: Int, offset: Int) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit { putInt("$KEY_OFFSET_PREFIX$widgetId", offset) }
    }

    fun getHideAmount(context: Context, widgetId: Int): Boolean {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sp.getBoolean("$KEY_HIDE_PREFIX$widgetId", false)
    }

    fun setHideAmount(context: Context, widgetId: Int, hide: Boolean) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sp.edit { putBoolean("$KEY_HIDE_PREFIX$widgetId", hide) }
    }
}
