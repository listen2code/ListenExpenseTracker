package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AnnualTransactionEngineTest {

    private val now = System.currentTimeMillis()
    private lateinit var sampleTransactions: List<TransactionEntity>

    @Before
    fun setUp() {
        sampleTransactions = listOf(
            TransactionEntity(
                id = "tx-1",
                type = "EXPENSE",
                categoryId = "c_shopping",
                categoryName = "购物",
                categoryIcon = "c_shopping",
                categoryColorHex = "#EF4444",
                amount = 200.0,
                note = "新衣服",
                accountType = "CASH",
                timestamp = now - 3000
            ),
            TransactionEntity(
                id = "tx-2",
                type = "EXPENSE",
                categoryId = "c_transport",
                categoryName = "交通",
                categoryIcon = "c_transport",
                categoryColorHex = "#3B82F6",
                amount = 50.0,
                note = "高铁票",
                accountType = "BANK",
                timestamp = now - 2000
            ),
            TransactionEntity(
                id = "tx-3",
                type = "INCOME",
                categoryId = "c_salary",
                categoryName = "工资",
                categoryIcon = "c_salary",
                categoryColorHex = "#10B981",
                amount = 10000.0,
                note = "年终奖",
                accountType = "BANK",
                timestamp = now - 1000
            )
        )
    }

    @Test
    fun testFilterAndCalculateYearTransactions_All() {
        val result = AnnualTransactionEngine.filterAndCalculateYearTransactions(
            allList = sampleTransactions,
            yearOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0
        )

        assertEquals(3, result.filteredTransactions.size)
        assertEquals(250.0, result.totalExpense, 0.01)
        assertEquals(10000.0, result.totalIncome, 0.01)
        assertEquals(9750.0, result.netBalance, 0.01)
        // Annual budget is 5000.0 * 12 = 60000.0
        assertEquals(60000.0, result.monthlyBudget, 0.01)
        assertFalse(result.isOverBudget)
    }

    @Test
    fun testFilterAndCalculateYearTransactions_CategoryFilter() {
        val result = AnnualTransactionEngine.filterAndCalculateYearTransactions(
            allList = sampleTransactions,
            yearOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            selectedCategories = setOf("购物")
        )

        assertEquals(1, result.filteredTransactions.size)
        assertEquals("tx-1", result.filteredTransactions.first().id)
        assertEquals(200.0, result.totalExpense, 0.01)
        assertEquals(0.0, result.totalIncome, 0.01)
    }

    @Test
    fun testFilterAndCalculateYearTransactions_SearchQuery() {
        val result = AnnualTransactionEngine.filterAndCalculateYearTransactions(
            allList = sampleTransactions,
            yearOffset = 0,
            query = "高铁",
            accountFilter = "ALL",
            budget = 5000.0
        )

        assertEquals(1, result.filteredTransactions.size)
        assertEquals("tx-2", result.filteredTransactions.first().id)
    }

    @Test
    fun testFilterAndCalculateYearTransactions_SortOrder() {
        val resultAsc = AnnualTransactionEngine.filterAndCalculateYearTransactions(
            allList = sampleTransactions,
            yearOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.AMOUNT_ASC
        )

        assertEquals(50.0, resultAsc.filteredTransactions.first().amount, 0.01)
        assertEquals(10000.0, resultAsc.filteredTransactions.last().amount, 0.01)
    }
}
