package com.listen.expensetracker.core.worker

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.engine.RecurringExecutionResult
import com.listen.expensetracker.data.engine.RecurringTransactionEngine
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.update.UpdateCheckerService
import com.listen.expensetracker.data.update.UpdateNotificationHelper
import com.listen.expensetracker.data.update.UpdateResult
import com.listen.expensetracker.features.recurring.engine.RecurringNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * 每日后台定时调度执行器 (ExpensePeriodicWorker)。
 * 核心职责：
 * 1. 离线履约：在后台唤醒时检查已到期的周期账单，自动写入账本并派发本地状态栏通知；
 * 2. 静默更新：若网络可用，顺带静默请求最新发布版本，满足 3 天频控时通知用户升级。
 */
class ExpensePeriodicWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        ExpenseStrings.init()
        try {
            executeDailyTasks(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val TAG = "expense_periodic_worker"

        /**
         * 调度任务总编排（提取为无状态静态方法，便于单元测试与单次调试触发）。
         */
        suspend fun executeDailyTasks(
            context: Context,
            currentTime: Long = System.currentTimeMillis()
        ): Pair<RecurringExecutionResult, UpdateResult?> {
            val db = AppDatabase.getInstance(context)
            val recurringResult = processRecurringBills(context, db, currentTime)
            val updateResult = checkAppUpdatesSilently(context)
            return Pair(recurringResult, updateResult)
        }

        /**
         * 1. 履约周期账单并派发通知（纯本地数据库操作，完全支持无网离线）。
         */
        internal suspend fun processRecurringBills(
            context: Context,
            db: AppDatabase,
            currentTime: Long = System.currentTimeMillis()
        ): RecurringExecutionResult {
            val result = RecurringTransactionEngine.processDueRulesWithResult(
                recurringDao = db.recurringRuleDao(),
                txDao = db.transactionDao(),
                currentTime = currentTime
            )

            if (result.executedRules.isNotEmpty()) {
                val prefManager = ExpenseDataStoreManager.getInstance(context)
                val prefs = prefManager.preferencesFlow.first()
                RecurringNotificationHelper.notifyRecurringBillsExecuted(
                    context = context,
                    executedRules = result.executedRules,
                    currencySymbol = prefs.currencySymbol,
                    lang = prefs.language
                )
            }
            return result
        }

        /**
         * 2. 静默比对版本更新（带 3 天频控，遇网络异常静默忽略，不阻碍主流程）。
         */
        internal suspend fun checkAppUpdatesSilently(
            context: Context
        ): UpdateResult? {
            return try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val currentVersion = pInfo.versionName ?: "0.0.1"
                val currentBuildNumber = PackageInfoCompat.getLongVersionCode(pInfo)

                val prefManager = ExpenseDataStoreManager.getInstance(context)
                val prefs = prefManager.preferencesFlow.first()

                val result = UpdateCheckerService.checkLatestRelease(
                    currentVersion = currentVersion,
                    currentBuildNumber = currentBuildNumber,
                    lang = prefs.language
                )

                if (result is UpdateResult.NewVersionAvailable) {
                    UpdateNotificationHelper.notifyUpdateAvailable(
                        context = context,
                        releaseInfo = result.releaseInfo,
                        lang = prefs.language
                    )
                }
                result
            } catch (_: Exception) {
                null
            }
        }
    }
}
