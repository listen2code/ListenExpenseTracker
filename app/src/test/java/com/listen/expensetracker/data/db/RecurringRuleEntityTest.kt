package com.listen.expensetracker.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringRuleEntityTest {

    @Test
    fun defaultValues_areInitializedProperly() {
        val rule = RecurringRuleEntity(
            title = "Netflix",
            categoryId = "entertainment",
            categoryName = "Entertainment",
            categoryIcon = "entertainment",
            categoryColorHex = "#9C27B0",
            amount = 68.0
        )

        assertNotNull(rule.id)
        assertEquals("Netflix", rule.title)
        assertEquals(TransactionType.EXPENSE, rule.type)
        assertEquals(68.0, rule.amount, 0.001)
        assertEquals(RecurringFrequency.MONTHLY, rule.frequency)
        assertEquals(ExecutionType.AUTO_INSERT, rule.executionType)
        assertTrue(rule.isEnabled)
    }

    @Test
    fun copyAndMutate_worksAccurately() {
        val rule = RecurringRuleEntity(
            title = "Gym Membership",
            categoryId = "fitness",
            categoryName = "Fitness",
            categoryIcon = "fitness",
            categoryColorHex = "#4CAF50",
            amount = 300.0,
            frequency = RecurringFrequency.YEARLY,
            dayOfPeriod = 15
        )

        val updated = rule.copy(isEnabled = false, amount = 350.0)
        assertEquals(350.0, updated.amount, 0.001)
        assertEquals(false, updated.isEnabled)
        assertEquals(RecurringFrequency.YEARLY, updated.frequency)
        assertEquals(15, updated.dayOfPeriod)
    }
}
