package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.AnnualCalculationEngine
import com.listen.expensetracker.data.i18n.ExpenseStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * 年度计算引擎单元测试 (AnnualCalculationEngineTest)。
 */
class AnnualCalculationEngineTest {

    @Before
    fun setUp() {
        ExpenseStrings.init()
    }

    private fun createTx(
        id: String,
        amount: Double,
        type: String = TransactionType.EXPENSE,
        categoryId: String = "c_food",
        categoryName: String = "餐饮",
        timestamp: Long
    ): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = type,
            categoryId = categoryId,
            categoryName = categoryName,
            categoryIcon = "Restaurant",
            categoryColorHex = "#EF4444",
            accountType = "CASH",
            timestamp = timestamp,
            note = "Test Tx $id"
        )
    }

    @Test
    fun testYearRangeAndTitle() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val (startTs, endTs, titleZh) = AnnualCalculationEngine.getYearRangeAndTitle(0, "zh")
        val (_, _, titleEn) = AnnualCalculationEngine.getYearRangeAndTitle(0, "en")
        val (_, _, titleJa) = AnnualCalculationEngine.getYearRangeAndTitle(0, "ja")

        val startCal = Calendar.getInstance().apply { timeInMillis = startTs }
        assertEquals(currentYear, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, startCal.get(Calendar.MONTH))
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))

        val endCal = Calendar.getInstance().apply { timeInMillis = endTs }
        assertEquals(currentYear, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, endCal.get(Calendar.MONTH))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))

        assertTrue(titleZh.contains("今年 ($currentYear"))
        assertTrue(titleEn.contains("This Year ($currentYear)"))
        assertTrue(titleJa.contains("今年 ($currentYear"))

        val (_, _, lastYearTitleZh) = AnnualCalculationEngine.getYearRangeAndTitle(-1, "zh")
        assertEquals("${currentYear - 1}年", lastYearTitleZh)
    }

    @Test
    fun testFilterAndCalculateYear() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // March tx of this year
        val marchCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        // August tx of this year
        val augustCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 10)
        }
        // Last year tx (should be filtered out)
        val lastYearCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear - 1)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, 5)
        }

        val txs = listOf(
            createTx("1", 300.0, TransactionType.EXPENSE, "c_food", "餐饮", marchCal.timeInMillis),
            createTx("2", 500.0, TransactionType.EXPENSE, "c_shopping", "购物", augustCal.timeInMillis),
            createTx("3", 2000.0, TransactionType.INCOME, "c_salary", "工资", marchCal.timeInMillis),
            createTx("4", 1000.0, TransactionType.EXPENSE, "c_food", "餐饮", lastYearCal.timeInMillis)
        )

        val result = AnnualCalculationEngine.filterAndCalculateYear(txs, 0, "zh")

        assertEquals(3, result.filteredTransactions.size)
        assertEquals(800.0, result.totalExpense, 0.001)
        assertEquals(2000.0, result.totalIncome, 0.001)
        assertEquals(1200.0, result.netBalance, 0.001)

        assertEquals(800.0 / 12.0, result.monthlyAverageExpense, 0.001)
        assertEquals(2000.0 / 12.0, result.monthlyAverageIncome, 0.001)

        assertEquals(2, result.categoryShares.size)
        assertEquals("购物", result.categoryShares[0].label)
        assertEquals(500.0, result.categoryShares[0].value, 0.001)
        assertEquals("餐饮", result.categoryShares[1].label)
        assertEquals(300.0, result.categoryShares[1].value, 0.001)

        assertEquals(1, result.incomeCategoryShares.size)
        assertEquals("工资", result.incomeCategoryShares[0].label)

        assertNotNull(result.maxExpenseTransaction)
        assertEquals("2", result.maxExpenseTransaction?.id)
        assertNotNull(result.maxIncomeTransaction)
        assertEquals("3", result.maxIncomeTransaction?.id)

        assertEquals(12, result.monthlyTrendPoints.size)
        assertEquals(300.0, result.monthlyTrendPoints[2].value, 0.001) // March (index 2)
        assertEquals(500.0, result.monthlyTrendPoints[7].value, 0.001) // August (index 7)
        assertEquals(0.0, result.monthlyTrendPoints[0].value, 0.001) // Jan (index 0)

        assertEquals(12, result.annualSummaries.size)
        assertEquals(currentYear, result.year)
    }

    @Test
    fun testEmptyTransactions() {
        val result = AnnualCalculationEngine.filterAndCalculateYear(emptyList(), 0, "zh")
        assertEquals(0, result.filteredTransactions.size)
        assertEquals(0.0, result.totalExpense, 0.001)
        assertEquals(0.0, result.totalIncome, 0.001)
        assertEquals(0.0, result.netBalance, 0.001)
        assertEquals(0.0, result.monthlyAverageExpense, 0.001)
        assertEquals(0.0, result.monthlyAverageIncome, 0.001)
        assertTrue(result.categoryShares.isEmpty())
        assertNull(result.maxExpenseTransaction)
        assertNull(result.maxIncomeTransaction)
        assertEquals(12, result.monthlyTrendPoints.size)
        assertEquals(12, result.annualSummaries.size)
    }

    @Test
    fun testFilterAnnualCategory() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val marchCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val augustCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear)
            set(Calendar.MONTH, Calendar.AUGUST)
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val lastYearCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, currentYear - 1)
            set(Calendar.MONTH, Calendar.MARCH)
            set(Calendar.DAY_OF_MONTH, 15)
        }

        val txs = listOf(
            createTx("1", 300.0, TransactionType.EXPENSE, "c_food", "餐饮", marchCal.timeInMillis),
            createTx("2", 500.0, TransactionType.EXPENSE, "c_food", "餐饮", augustCal.timeInMillis),
            createTx("3", 200.0, TransactionType.EXPENSE, "c_shopping", "购物", marchCal.timeInMillis),
            createTx("4", 1000.0, TransactionType.EXPENSE, "c_food", "餐饮", lastYearCal.timeInMillis)
        )

        val result = AnnualCalculationEngine.filterAnnualCategory(
            allList = txs,
            year = currentYear,
            categoryName = "餐饮",
            categoryId = "c_food",
            lang = "zh"
        )

        assertEquals(2, result.transactions.size)
        assertEquals(800.0, result.totalExpense, 0.001)
        assertEquals(0.0, result.totalIncome, 0.001)
        assertTrue(result.yearTitle.contains("$currentYear"))
    }
}
