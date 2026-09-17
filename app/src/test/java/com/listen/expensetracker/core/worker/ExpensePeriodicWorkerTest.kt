package com.listen.expensetracker.core.worker

import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleDao
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionDao
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.RecurringTransactionEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExpensePeriodicWorkerTest {

    @Test
    fun workerTag_isExpected() {
        assertEquals("expense_periodic_worker", ExpensePeriodicWorker.TAG)
    }

    @Test
    fun processDueRules_withNoDueRules_executesZeroInserts() = runTest {
        val mockRecurringDao: RecurringRuleDao = mock()
        val mockTxDao: TransactionDao = mock()
        val testTime = 1000000L

        whenever(mockRecurringDao.getDueRules(testTime)).thenReturn(emptyList())

        val result = RecurringTransactionEngine.processDueRulesWithResult(
            recurringDao = mockRecurringDao,
            txDao = mockTxDao,
            currentTime = testTime
        )

        assertEquals(0, result.processedCount)
        assertEquals(0, result.executedRules.size)
        verify(mockTxDao, never()).insertTransaction(any())
        verify(mockRecurringDao, never()).updateRule(any())
    }

    @Test
    fun processDueRules_withDueRule_insertsTransactionAndUpdatesRule() = runTest {
        val mockRecurringDao: RecurringRuleDao = mock()
        val mockTxDao: TransactionDao = mock()
        val testTime = 1000000L

        val dueRule = RecurringRuleEntity(
            id = "rule_sub_1",
            title = "云音乐订阅",
            amount = 15.0,
            type = TransactionType.EXPENSE,
            categoryId = "c_entertainment",
            categoryName = "娱乐",
            categoryIcon = "ic_music",
            categoryColorHex = "#9C27B0",
            accountType = "CASH",
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 1,
            startDate = 1000L,
            nextExecutionDate = 500000L,
            executionType = ExecutionType.AUTO_INSERT
        )

        whenever(mockRecurringDao.getDueRules(testTime)).thenReturn(listOf(dueRule))

        val result = RecurringTransactionEngine.processDueRulesWithResult(
            recurringDao = mockRecurringDao,
            txDao = mockTxDao,
            currentTime = testTime
        )

        assertEquals(1, result.processedCount)
        assertEquals(1, result.executedRules.size)
        assertEquals("rule_sub_1", result.executedRules[0].id)
        verify(mockTxDao).insertTransaction(any())
        verify(mockRecurringDao).updateRule(any())
    }
}
