package com.listen.expensetracker.data.db

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionEntityTest {
    @Test
    fun testDefaultValues() {
        val entity = TransactionEntity(
            type = "EXPENSE",
            categoryId = "cat_1",
            categoryName = "Food",
            categoryIcon = "icon_food",
            categoryColorHex = "#FFFFFF",
            amount = 100.0
        )
        assertNotNull(entity.id)
        assertTrue(entity.timestamp > 0L)
        assertEquals("", entity.note)
        assertEquals("CASH", entity.accountType)
        assertEquals("EXPENSE", entity.type)
        assertEquals(100.0, entity.amount, 0.0)
    }

    @Test
    fun testCopyBehavior() {
        val original = TransactionEntity(
            type = "INCOME",
            categoryId = "cat_2",
            categoryName = "Salary",
            categoryIcon = "icon_salary",
            categoryColorHex = "#000000",
            amount = 5000.0,
            note = "Monthly salary",
            accountType = "BANK"
        )
        
        val copied = original.copy(amount = 5500.0)
        
        assertEquals(original.id, copied.id)
        assertEquals(original.type, copied.type)
        assertEquals(5500.0, copied.amount, 0.0)
        assertEquals("Monthly salary", copied.note)
        assertNotEquals(original.amount, copied.amount)
    }
}
