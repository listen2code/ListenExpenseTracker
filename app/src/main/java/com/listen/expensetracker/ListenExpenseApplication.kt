package com.listen.expensetracker

import android.app.Application
import com.listen.arch.apm.CrashHandler
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.core.shortcut.ExpenseShortcutManager
import com.listen.expensetracker.core.worker.ExpenseWorkManagerScheduler
import com.listen.expensetracker.data.i18n.ExpenseStrings

/**
 * Global Application class for ListenExpenseTracker.
 * Ensures system-wide initialization of internationalization dictionaries,
 * crash reporting handlers, notification channels, periodic WorkManager schedules,
 * and dynamic desktop shortcuts across all process entry points.
 */
class ListenExpenseApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ExpenseStrings.init()
        CrashHandler.init(this)
        LocalNotificationManager.createNotificationChannels(this)
        ExpenseWorkManagerScheduler.schedulePeriodicWorker(this)
        ExpenseShortcutManager.updateDynamicShortcuts(this)
    }
}
