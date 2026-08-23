package com.listen.expensetracker

import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionSortOrderTest {

    @Test
    fun testAllSortOrderValues() {
        val orders = TransactionSortOrder.entries
        assertEquals(4, orders.size)
        assertTrue(orders.contains(TransactionSortOrder.DATE_DESC))
        assertTrue(orders.contains(TransactionSortOrder.DATE_ASC))
        assertTrue(orders.contains(TransactionSortOrder.AMOUNT_DESC))
        assertTrue(orders.contains(TransactionSortOrder.AMOUNT_ASC))

        assertEquals("sort_date_desc", TransactionSortOrder.DATE_DESC.displayNameKey)
        assertEquals("sort_date_asc", TransactionSortOrder.DATE_ASC.displayNameKey)
        assertEquals("sort_amount_desc", TransactionSortOrder.AMOUNT_DESC.displayNameKey)
        assertEquals("sort_amount_asc", TransactionSortOrder.AMOUNT_ASC.displayNameKey)
    }
}
