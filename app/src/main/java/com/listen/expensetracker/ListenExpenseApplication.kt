package com.listen.expensetracker

import android.app.Application
import com.listen.arch.apm.CrashHandler
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.data.i18n.ExpenseStrings

/**
 * Global Application class for ListenExpenseTracker.
 * Ensures system-wide initialization of internationalization dictionaries,
 * crash reporting handlers, and notification channels across all process entry points
 * (Activities, BroadcastReceivers, Services, and App Widgets).
 */
class ListenExpenseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ExpenseStrings.init()
        CrashHandler.init(this)
        LocalNotificationManager.createNotificationChannels(this)
    }
}
