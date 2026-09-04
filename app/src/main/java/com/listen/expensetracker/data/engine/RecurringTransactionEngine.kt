package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleDao
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionDao
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import java.util.Calendar

data class RecurringMonthlyBaseline(
    val totalExpense: Double,
    val totalIncome: Double,
    val netMonthly: Double,
    val expenseCount: Int,
    val incomeCount: Int
)

object RecurringTransactionEngine {

    /**
     * 计算下次执行时间戳。
     * 自动处理月末大小月及闰年边界防溢出，保证计算得到的时间严格晚于 [fromDate]。
     */
    fun calculateNextExecutionDate(frequency: RecurringFrequency, dayOfPeriod: Int, fromDate: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = fromDate
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        do {
            when (frequency) {
                RecurringFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RecurringFrequency.WEEKLY -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    val targetCalendarDay = when (dayOfPeriod) {
                        1 -> Calendar.MONDAY
                        2 -> Calendar.TUESDAY
                        3 -> Calendar.WEDNESDAY
                        4 -> Calendar.THURSDAY
                        5 -> Calendar.FRIDAY
                        6 -> Calendar.SATURDAY
                        else -> Calendar.SUNDAY
                    }
                    cal.set(Calendar.DAY_OF_WEEK, targetCalendarDay)
                }
                RecurringFrequency.MONTHLY -> {
                    cal.add(Calendar.MONTH, 1)
                    val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    cal.set(Calendar.DAY_OF_MONTH, dayOfPeriod.coerceIn(1, maxDay))
                }
                RecurringFrequency.YEARLY -> {
                    cal.add(Calendar.YEAR, 1)
                }
            }
        } while (cal.timeInMillis <= fromDate)

        return cal.timeInMillis
    }

    /**
     * 计算折算到每月的固定生活成本基准 (Baseline)。
     */
    fun calculateMonthlyBaseline(rules: List<RecurringRuleEntity>): RecurringMonthlyBaseline {
        val enabledRules = rules.filter { it.isEnabled }
        var totalExp = 0.0
        var totalInc = 0.0
        var expCount = 0
        var incCount = 0

        for (rule in enabledRules) {
            val monthlyAmt = when (rule.frequency) {
                RecurringFrequency.DAILY -> rule.amount * 30.0
                RecurringFrequency.WEEKLY -> rule.amount * (52.0 / 12.0)
                RecurringFrequency.MONTHLY -> rule.amount
                RecurringFrequency.YEARLY -> rule.amount / 12.0
            }
            if (rule.type == TransactionType.EXPENSE) {
                totalExp += monthlyAmt
                expCount++
            } else {
                totalInc += monthlyAmt
                incCount++
            }
        }

        return RecurringMonthlyBaseline(
            totalExpense = totalExp,
            totalIncome = totalInc,
            netMonthly = totalInc - totalExp,
            expenseCount = expCount,
            incomeCount = incCount
        )
    }

    /**
     * 履约待执行的周期规则。
     * 自动插入账单记录并递增下一次执行时间戳。
     */
    suspend fun processDueRules(
        recurringDao: RecurringRuleDao,
        txDao: TransactionDao,
        currentTime: Long = System.currentTimeMillis()
    ): Int {
        val dueRules = recurringDao.getDueRules(currentTime)
        var processedCount = 0

        for (rule in dueRules) {
            if (rule.executionType == ExecutionType.AUTO_INSERT) {
                val baseNote = rule.note.ifEmpty { rule.title }
                val recurringNote = if (baseNote.startsWith("[周期]")) baseNote else "[周期] $baseNote"
                val tx = TransactionEntity(
                    type = rule.type,
                    categoryId = rule.categoryId,
                    categoryName = rule.categoryName,
                    categoryIcon = rule.categoryIcon,
                    categoryColorHex = rule.categoryColorHex,
                    amount = rule.amount,
                    note = recurringNote,
                    accountType = rule.accountType,
                    timestamp = rule.nextExecutionDate.coerceAtMost(currentTime)
                )
                txDao.insertTransaction(tx)

                val nextDate = calculateNextExecutionDate(rule.frequency, rule.dayOfPeriod, rule.nextExecutionDate)
                val isStillEnabled = rule.endDate == null || nextDate <= rule.endDate
                val updatedRule = rule.copy(
                    lastExecutionDate = currentTime,
                    nextExecutionDate = nextDate,
                    isEnabled = isStillEnabled
                )
                recurringDao.updateRule(updatedRule)
                processedCount++
            }
        }

        return processedCount
    }
}
