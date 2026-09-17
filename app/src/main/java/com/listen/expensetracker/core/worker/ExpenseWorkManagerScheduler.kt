package com.listen.expensetracker.core.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WorkManager 后台任务统一调度器 (ExpenseWorkManagerScheduler)。
 * 负责注册每日周期性维护任务 (KEEP 策略)，并提供单次立即同步与取消调度能力。
 */
object ExpenseWorkManagerScheduler {

    const val PERIODIC_WORK_NAME = "expense_periodic_worker"
    const val ONE_TIME_WORK_NAME = "expense_one_time_sync"
    const val WORK_TAG = "tag_expense_periodic"

    const val PERIODIC_INTERVAL_HOURS = 24L
    const val FLEX_INTERVAL_HOURS = 2L

    /**
     * 构建带有非低电量约束的基础 Constraints。
     */
    fun createDefaultConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()
    }

    /**
     * 构建周期请求对象 (暴露给单元测试与调度器使用)。
     */
    fun buildPeriodicWorkRequest(): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<ExpensePeriodicWorker>(
            PERIODIC_INTERVAL_HOURS,
            TimeUnit.HOURS,
            FLEX_INTERVAL_HOURS,
            TimeUnit.HOURS
        )
            .setConstraints(createDefaultConstraints())
            .addTag(WORK_TAG)
            .build()
    }

    /**
     * 注册每日周期性后台任务。
     * 使用 [ExistingPeriodicWorkPolicy.KEEP] 确保重复调用时不覆盖已有调度时间表。
     */
    fun schedulePeriodicWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val request = buildPeriodicWorkRequest()
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * 取消周期性后台任务。
     */
    fun cancelPeriodicWorker(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * 触发单次即时后台任务（供设置页测试排查或外部触发）。
     */
    fun triggerImmediateWork(context: Context): UUID {
        val workManager = WorkManager.getInstance(context)
        val request = OneTimeWorkRequestBuilder<ExpensePeriodicWorker>()
            .addTag(WORK_TAG)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
        return request.id
    }
}
