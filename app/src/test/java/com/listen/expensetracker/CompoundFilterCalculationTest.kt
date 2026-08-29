package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.AmountFilterPreset
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CompoundFilterCalculationTest {

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
                amount = 25.0,
                note = "早餐包子豆浆",
                accountType = "CASH",
                timestamp = now - 3000
            ),
            TransactionEntity(
                id = "tx-2",
                type = "EXPENSE",
                categoryId = "c_shopping",
                categoryName = "购物",
                categoryIcon = "c_shopping",
                categoryColorHex = "#3B82F6",
                amount = 260.0,
                note = "优衣库外套",
                accountType = "BANK",
                timestamp = now - 2000
            ),
            TransactionEntity(
                id = "tx-3",
                type = "EXPENSE",
                categoryId = "c_digital",
                categoryName = "数码",
                categoryIcon = "c_digital",
                categoryColorHex = "#8B5CF6",
                amount = 1299.0,
                note = "降噪耳机",
                accountType = "CREDIT",
                timestamp = now - 1000
            ),
            TransactionEntity(
                id = "tx-4",
                type = "INCOME",
                categoryId = "c_salary",
                categoryName = "工资",
                categoryIcon = "c_salary",
                categoryColorHex = "#10B981",
                amount = 15000.0,
                note = "月薪收入",
                accountType = "BANK",
                timestamp = now
            )
        )
    }

    @Test
    fun testFullTextSearchAcrossFields() {
        // Search by amount
        val byAmount = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "1299", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(1, byAmount.filteredTransactions.size)
        assertEquals("tx-3", byAmount.filteredTransactions.first().id)

        // Search by note
        val byNote = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "耳机", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(1, byNote.filteredTransactions.size)
        assertEquals("tx-3", byNote.filteredTransactions.first().id)

        // Search by account type
        val byAccount = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "credit", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC
        )
        assertEquals(1, byAccount.filteredTransactions.size)
        assertEquals("tx-3", byAccount.filteredTransactions.first().id)
    }

    @Test
    fun testTypeFilter() {
        val expenses = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, typeFilter = "EXPENSE"
        )
        assertEquals(3, expenses.filteredTransactions.size)
        assertTrue(expenses.filteredTransactions.all { it.type == "EXPENSE" })

        val incomes = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, typeFilter = "INCOME"
        )
        assertEquals(1, incomes.filteredTransactions.size)
        assertEquals("tx-4", incomes.filteredTransactions.first().id)
    }

    @Test
    fun testCategoryFilter() {
        val shopping = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, categoryFilter = "购物"
        )
        assertEquals(1, shopping.filteredTransactions.size)
        assertEquals("tx-2", shopping.filteredTransactions.first().id)

        val byId = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, categoryFilter = "c_food"
        )
        assertEquals(1, byId.filteredTransactions.size)
        assertEquals("tx-1", byId.filteredTransactions.first().id)
    }

    @Test
    fun testAmountRangePresets() {
        // Small < 50
        val small = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, amountPreset = AmountFilterPreset.SMALL_LT_50
        )
        assertEquals(1, small.filteredTransactions.size)
        assertEquals("tx-1", small.filteredTransactions.first().id)

        // Medium 50..500
        val medium = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, amountPreset = AmountFilterPreset.MEDIUM_50_500
        )
        assertEquals(1, medium.filteredTransactions.size)
        assertEquals("tx-2", medium.filteredTransactions.first().id)

        // Large > 500
        val large = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC, amountPreset = AmountFilterPreset.LARGE_GT_500
        )
        assertEquals(2, large.filteredTransactions.size) // 1299 and 15000
    }

    @Test
    fun testCustomAmountFilter() {
        val custom = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC,
            amountPreset = AmountFilterPreset.CUSTOM,
            customMinAmount = 100.0,
            customMaxAmount = 1000.0
        )
        assertEquals(1, custom.filteredTransactions.size)
        assertEquals("tx-2", custom.filteredTransactions.first().id) // 260.0
    }

    @Test
    fun testMultiDimensionalCompoundFilter() {
        val compound = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "",
            accountFilter = "BANK",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC,
            typeFilter = "EXPENSE",
            amountPreset = AmountFilterPreset.MEDIUM_50_500
        )
        assertEquals(1, compound.filteredTransactions.size)
    }

    @Test
    fun testMultiCategorySelection() {
        val multiple = TransactionCalculationEngine.filterAndCalculate(
            allList = sampleTransactions, currentOffset = 0, query = "", accountFilter = "ALL",
            budget = 5000.0, sortOrder = TransactionSortOrder.DATE_DESC,
            selectedCategories = setOf("c_food", "购物")
        )
        assertEquals(2, multiple.filteredTransactions.size)
        assertTrue(multiple.filteredTransactions.any { it.id == "tx-1" })
        assertTrue(multiple.filteredTransactions.any { it.id == "tx-2" })
    }

    @Test
    fun testAccountSelectionDoesNotIncrementDialogFilterCount() {
        val stateWithAccount = com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState(
            selectedAccountFilter = "CASH"
        )
        assertEquals(0, stateWithAccount.activeFilterCount)

        val stateWithDialogFilters = stateWithAccount.copy(
            selectedCategories = setOf("c_food"),
            amountPreset = AmountFilterPreset.SMALL_LT_50
        )
        assertEquals(2, stateWithDialogFilters.activeFilterCount)
    }
}
