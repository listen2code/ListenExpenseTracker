package com.listen.listenexpensetracker

import com.listen.listenexpensetracker.ui.state.TransactionSortOrder
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

        assertEquals("时间最新", TransactionSortOrder.DATE_DESC.displayName)
        assertEquals("时间最早", TransactionSortOrder.DATE_ASC.displayName)
        assertEquals("金额降序", TransactionSortOrder.AMOUNT_DESC.displayName)
        assertEquals("金额升序", TransactionSortOrder.AMOUNT_ASC.displayName)
    }
}
