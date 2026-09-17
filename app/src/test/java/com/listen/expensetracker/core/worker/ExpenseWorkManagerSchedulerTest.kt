package com.listen.expensetracker.core.worker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseWorkManagerSchedulerTest {

    @Test
    fun constants_haveExpectedValues() {
        assertEquals("expense_periodic_worker", ExpenseWorkManagerScheduler.PERIODIC_WORK_NAME)
        assertEquals("expense_one_time_sync", ExpenseWorkManagerScheduler.ONE_TIME_WORK_NAME)
        assertEquals("tag_expense_periodic", ExpenseWorkManagerScheduler.WORK_TAG)
        assertEquals(24L, ExpenseWorkManagerScheduler.PERIODIC_INTERVAL_HOURS)
        assertEquals(2L, ExpenseWorkManagerScheduler.FLEX_INTERVAL_HOURS)
    }

    @Test
    fun createDefaultConstraints_requiresBatteryNotLow() {
        val constraints = ExpenseWorkManagerScheduler.createDefaultConstraints()
        assertTrue("Worker constraints must require battery not low to save energy", constraints.requiresBatteryNotLow())
    }

    @Test
    fun buildPeriodicWorkRequest_configuredProperly() {
        val request = ExpenseWorkManagerScheduler.buildPeriodicWorkRequest()
        assertNotNull("WorkRequest id should not be null", request.id)
        assertTrue("WorkRequest should contain tag", request.tags.contains(ExpenseWorkManagerScheduler.WORK_TAG))
        assertTrue("WorkRequest should contain worker class name tag", request.tags.any { it.contains("ExpensePeriodicWorker") })
        val workSpec = request.workSpec
        assertTrue("WorkSpec must require battery not low", workSpec.constraints.requiresBatteryNotLow())
        assertEquals("Interval should match 24 hours in millis", 24 * 60 * 60 * 1000L, workSpec.intervalDuration)
    }
}
