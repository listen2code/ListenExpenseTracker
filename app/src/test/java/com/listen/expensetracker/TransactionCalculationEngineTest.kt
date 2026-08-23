package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TransactionCalculationEngineTest {

    private val now = System.currentTimeMillis()
    private lateinit var sampleTransactions: List<TransactionEntity>

    @Before
    fun setUp() {
        sampleTransactions = listOf(
            TransactionEntity(
                id = "tx-1",
                type = "EXPENSE",
                categoryId = "c_food",
                categoryName = "餐饮",
                categoryIcon = "c_food",
                categoryColorHex = "#EF4444",
                amount = 120.0,
                note = "烤肉聚餐",
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
                amount = 30.0,
                note = "地铁通勤",
                accountType = "CASH",
                timestamp = now - 2000
            ),
            TransactionEntity(
                id = "tx-3",
                type = "EXPENSE",
                categoryId = "c_food",
                categoryName = "餐饮",
                categoryIcon = "c_food",
                categoryColorHex = "#EF4444",
                amount = 50.0,
                note = "快餐外卖",
                accountType = "BANK",
                timestamp = now - 1000
            ),
            TransactionEntity(
                id = "tx-4",
                type = "INCOME",
                categoryId = "c_salary",
                categoryName = "工资",
                categoryIcon = "c_salary",
                categoryColorHex = "#10B981",
                amount = 10000.0,
                note = "月薪打卡",
                accountType = "BANK",
                timestamp = now
            )
        )
    }

    @Test
    fun testEmptyTransactionsCalculation() {
        val state = TransactionCalculationEngine.filterAndCalculate(
            allList = emptyList(),
            currentOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )

        assertEquals(0, state.filteredTransactions.size)
        assertEquals(0.0, state.totalExpense, 0.001)
        assertEquals(0.0, state.totalIncome, 0.001)
        assertEquals(0.0, state.netBalance, 0.001)
        assertFalse(state.isOverBudget)
        assertEquals(0.0f, state.budgetUsageRatio, 0.001f)
        assertNull(state.maxExpenseTransaction)
        assertNull(state.maxIncomeTransaction)
        assertTrue(state.categoryShares.isEmpty())
        assertTrue(state.incomeCategoryShares.isEmpty())
    }

    @Test
    fun testTotalsAndNetBalance() {
        val state = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 100.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )

        // Total expense: 120 + 30 + 50 = 200
        assertEquals(200.0, state.totalExpense, 0.001)
        // Total income: 10000
        assertEquals(10000.0, state.totalIncome, 0.001)
        // Net balance: 10000 - 200 = 9800
        assertEquals(9800.0, state.netBalance, 0.001)

        // Budget is 100, expense is 200 -> over budget
        assertTrue(state.isOverBudget)
        assertEquals(2.0f, state.budgetUsageRatio, 0.001f)

        assertNotNull(state.maxExpenseTransaction)
        assertEquals("tx-1", state.maxExpenseTransaction?.id)

        assertNotNull(state.maxIncomeTransaction)
        assertEquals("tx-4", state.maxIncomeTransaction?.id)
    }

    @Test
    fun testSearchQueryFilter() {
        // Search by category
        val stateFood = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "餐饮",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(2, stateFood.filteredTransactions.size)
        assertTrue(stateFood.filteredTransactions.all { it.categoryName == "餐饮" })

        // Search by note keyword
        val stateSubway = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "地铁",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(1, stateSubway.filteredTransactions.size)
        assertEquals("tx-2", stateSubway.filteredTransactions[0].id)
    }

    @Test
    fun testAccountFilter() {
        val stateCash = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "CASH",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(2, stateCash.filteredTransactions.size)
        assertTrue(stateCash.filteredTransactions.all { it.accountType == "CASH" })

        val stateBank = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "BANK",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(2, stateBank.filteredTransactions.size)
        assertTrue(stateBank.filteredTransactions.all { it.accountType == "BANK" })
    }

    @Test
    fun testSortingOrders() {
        val stateDateDesc = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals("tx-4", stateDateDesc.filteredTransactions.first().id)
        assertEquals("tx-1", stateDateDesc.filteredTransactions.last().id)

        val stateDateAsc = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.DATE_ASC
        )
        assertEquals("tx-1", stateDateAsc.filteredTransactions.first().id)
        assertEquals("tx-4", stateDateAsc.filteredTransactions.last().id)

        val stateAmountDesc = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.AMOUNT_DESC
        )
        assertEquals(10000.0, stateAmountDesc.filteredTransactions.first().amount, 0.001)
        assertEquals(30.0, stateAmountDesc.filteredTransactions.last().amount, 0.001)

        val stateAmountAsc = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions,
            currentOffset = 0,
            query = "",
            accountFilter = "ALL",
            budget = 5000.0,
            sortOrder = TransactionSortOrder.AMOUNT_ASC
        )
        assertEquals(30.0, stateAmountAsc.filteredTransactions.first().amount, 0.001)
        assertEquals(10000.0, stateAmountAsc.filteredTransactions.last().amount, 0.001)
    }

    @Test
    fun testCategorySharesAggregation() {
        val expenses = sampleTransactions.filter { it.type == "EXPENSE" }
        val total = expenses.sumOf { it.amount } // 200.0
        val shares = TransactionCalculationEngine.calculateCategoryShares(expenses, total)

        assertEquals(2, shares.size)
        // 餐饮 total: 170.0 / 200.0 = 0.85
        assertEquals("餐饮", shares[0].label)
        assertEquals(170.0, shares[0].value, 0.001)
        assertEquals(0.85f, shares[0].percentage, 0.01f)

        // 交通 total: 30.0 / 200.0 = 0.15
        assertEquals("交通", shares[1].label)
        assertEquals(30.0, shares[1].value, 0.001)
        assertEquals(0.15f, shares[1].percentage, 0.01f)
    }

    @Test
    fun testRecentDaysTrendCalculation() {
        val expenses = sampleTransactions.filter { it.type == "EXPENSE" }
        val trend = TransactionCalculationEngine.calculateRecentDaysTrend(expenses)
        assertEquals(7, trend.size)
        trend.forEach {
            assertNotNull(it.label)
            assertTrue(it.value >= 0.0)
        }
    }

    @Test
    fun testMonthRangeAndDaysInMonth() {
        val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(0)
        assertTrue(startTs < endTs)
        assertTrue(title.contains("本月"))

        val days = TransactionCalculationEngine.getDaysInMonth(0)
        assertTrue(days in 28..31)

        val (prevStart, prevEnd, prevTitle) = TransactionCalculationEngine.getMonthRangeAndTitle(-1)
        assertTrue(prevStart < prevEnd)
        assertFalse(prevTitle.contains("本月"))
    }
}
