package com.listen.expensetracker.features.transactions.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionsIntentTest {
    @Test
    fun testIntents() {
        val addIntent = TransactionsIntent.AddTransaction(
            type = "EXPENSE",
            categoryId = "cat_1",
            categoryName = "Food",
            categoryIcon = "icon",
            categoryColorHex = "#FFF",
            amount = 100.0,
            note = "note",
            accountType = "CASH"
        )
        assertEquals("EXPENSE", addIntent.type)
        assertEquals("cat_1", addIntent.categoryId)
        assertEquals(100.0, addIntent.amount, 0.0)

        val deleteIntent = TransactionsIntent.DeleteTransaction("id_1")
        assertEquals("id_1", deleteIntent.id)

        val toggleIntent = TransactionsIntent.ToggleHideBalance(true)
        assertTrue(toggleIntent.hide)

        val searchIntent = TransactionsIntent.SearchQueryChange("test")
        assertEquals("test", searchIntent.query)

        val filterIntent = TransactionsIntent.FilterAccountChange("BANK")
        assertEquals("BANK", filterIntent.accountType)
        
        val sortIntent = TransactionsIntent.ChangeSortOrder(TransactionSortOrder.AMOUNT_DESC)
        assertEquals(TransactionSortOrder.AMOUNT_DESC, sortIntent.order)
        
        val dialogIntent = TransactionsIntent.OpenDialog(TransactionsDialog.AddTransaction(initialCategoryId = "c_food"))
        assertEquals(TransactionsDialog.AddTransaction(initialCategoryId = "c_food"), dialogIntent.dialog)

        val appearIntent = TransactionsIntent.ScreenAppear
        val disappearIntent = TransactionsIntent.ScreenDisappear
        assertTrue(appearIntent is TransactionsIntent)
        assertTrue(disappearIntent is TransactionsIntent)
    }
}
