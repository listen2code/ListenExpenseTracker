package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleDao
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionDao
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar

class RecurringTransactionEngineTest {

    @Test
    fun calculateNextExecutionDate_daily_advancesByOneDay() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 15, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromDate = cal.timeInMillis

        val nextDate = RecurringTransactionEngine.calculateNextExecutionDate(
            frequency = RecurringFrequency.DAILY,
            dayOfPeriod = 1,
            fromDate = fromDate
        )

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.AUGUST, nextCal.get(Calendar.MONTH))
        assertEquals(16, nextCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun calculateNextExecutionDate_monthlyBoundary_capsAtEndOfMonth() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.JANUARY, 31, 9, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val fromDate = cal.timeInMillis

        val nextDate = RecurringTransactionEngine.calculateNextExecutionDate(
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 31,
            fromDate = fromDate
        )

        val nextCal = Calendar.getInstance().apply { timeInMillis = nextDate }
        assertEquals(2026, nextCal.get(Calendar.YEAR))
        assertEquals(Calendar.FEBRUARY, nextCal.get(Calendar.MONTH))
        assertEquals(28, nextCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun calculateMonthlyBaseline_calculatesWeightedAveragesCorrectly() {
        val rules = listOf(
            RecurringRuleEntity(
                title = "Coffee", categoryId = "food", categoryName = "Food",
                categoryIcon = "food", categoryColorHex = "#F44336",
                amount = 10.0, frequency = RecurringFrequency.DAILY, isEnabled = true
            ), // 10 * 30 = 300
            RecurringRuleEntity(
                title = "Rent", categoryId = "housing", categoryName = "Housing",
                categoryIcon = "housing", categoryColorHex = "#2196F3",
                amount = 3000.0, frequency = RecurringFrequency.MONTHLY, isEnabled = true
            ), // 3000
            RecurringRuleEntity(
                title = "Car Insurance", categoryId = "transport", categoryName = "Transport",
                categoryIcon = "transport", categoryColorHex = "#FF9800",
                amount = 1200.0, frequency = RecurringFrequency.YEARLY, isEnabled = true
            ), // 1200 / 12 = 100
            RecurringRuleEntity(
                title = "Salary", type = TransactionType.INCOME, categoryId = "salary", categoryName = "Salary",
                categoryIcon = "salary", categoryColorHex = "#4CAF50",
                amount = 10000.0, frequency = RecurringFrequency.MONTHLY, isEnabled = true
            ),
            RecurringRuleEntity(
                title = "Old Sub", categoryId = "other", categoryName = "Other",
                categoryIcon = "other", categoryColorHex = "#9E9E9E",
                amount = 500.0, frequency = RecurringFrequency.MONTHLY, isEnabled = false
            ) // Disabled, should be ignored
        )

        val baseline = RecurringTransactionEngine.calculateMonthlyBaseline(rules)
        assertEquals(3400.0, baseline.totalExpense, 0.01)
        assertEquals(10000.0, baseline.totalIncome, 0.01)
        assertEquals(6600.0, baseline.netMonthly, 0.01)
        assertEquals(3, baseline.expenseCount)
        assertEquals(1, baseline.incomeCount)
    }

    @Test
    fun processDueRules_executesAutoInsertAndUpdatesNextDate() = runBlocking {
        val recurringDao: RecurringRuleDao = mock()
        val txDao: TransactionDao = mock()

        val dueRule = RecurringRuleEntity(
            id = "rule-1",
            title = "Spotify",
            categoryId = "entertainment",
            categoryName = "Entertainment",
            categoryIcon = "entertainment",
            categoryColorHex = "#9C27B0",
            amount = 18.0,
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 1,
            nextExecutionDate = 1000L,
            executionType = ExecutionType.AUTO_INSERT,
            isEnabled = true
        )

        whenever(recurringDao.getDueRules(any())).thenReturn(listOf(dueRule))

        val processed = RecurringTransactionEngine.processDueRules(recurringDao, txDao, currentTime = 5000L)

        assertEquals(1, processed)
        verify(txDao).insertTransaction(any<TransactionEntity>())
        verify(recurringDao).updateRule(any<RecurringRuleEntity>())
    }

    @Test
    fun processDueRules_ignoresNotifyConfirmRules() = runBlocking {
        val recurringDao: RecurringRuleDao = mock()
        val txDao: TransactionDao = mock()

        val notifyRule = RecurringRuleEntity(
            id = "rule-2",
            title = "Credit Card Payment",
            categoryId = "credit",
            categoryName = "Credit",
            categoryIcon = "credit",
            categoryColorHex = "#E91E63",
            amount = 5000.0,
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 10,
            nextExecutionDate = 1000L,
            executionType = ExecutionType.NOTIFY_CONFIRM,
            isEnabled = true
        )

        whenever(recurringDao.getDueRules(any())).thenReturn(listOf(notifyRule))

        val processed = RecurringTransactionEngine.processDueRules(recurringDao, txDao, currentTime = 5000L)

        assertEquals(0, processed)
        verify(txDao, never()).insertTransaction(any())
        verify(recurringDao, never()).updateRule(any())
    }
}
