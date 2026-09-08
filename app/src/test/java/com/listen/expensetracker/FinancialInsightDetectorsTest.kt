package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.FinancialInsightDetectors
import com.listen.expensetracker.data.engine.InsightSeverity
import com.listen.expensetracker.data.i18n.ExpenseStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * 财务行为生活方式洞察规则检测器独立单元测试 (FinancialInsightDetectorsTest)。
 */
class FinancialInsightDetectorsTest {

    @Before
    fun setUp() {
        ExpenseStrings.init()
    }

    private fun createExpense(id: String, amount: Double, timestamp: Long): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = "c_food",
            categoryName = "餐饮",
            categoryIcon = "Restaurant",
            categoryColorHex = "#EF4444",
            accountType = "CASH",
            timestamp = timestamp,
            note = "Test $id"
        )
    }

    @Test
    fun testDetectSavingsRate_Healthy() {
        val item = FinancialInsightDetectors.detectSavingsRate(
            currentIncomeTotal = 10000.0,
            currentTotal = 6000.0,
            currencySymbol = "￥",
            lang = "zh"
        )
        assertNotNull(item)
        assertEquals("insight_savings_rate", item?.id)
        assertEquals(InsightSeverity.POSITIVE, item?.severity)
        assertEquals(40.0f, item?.diffPercentage ?: 0f, 0.1f)
    }

    @Test
    fun testDetectSavingsRate_Deficit() {
        val item = FinancialInsightDetectors.detectSavingsRate(
            currentIncomeTotal = 5000.0,
            currentTotal = 6000.0,
            currencySymbol = "￥",
            lang = "zh"
        )
        assertNotNull(item)
        assertEquals("insight_deficit", item?.id)
        assertEquals(InsightSeverity.DANGER, item?.severity)
    }

    @Test
    fun testDetectLatteFactor() {
        val cal = Calendar.getInstance()
        val txs = (1..7).map {
            createExpense("tx_$it", 25.0, cal.timeInMillis)
        }
        val item = FinancialInsightDetectors.detectLatteFactor(txs, "￥", "zh")
        assertNotNull(item)
        assertEquals("insight_latte_factor", item?.id)
        assertEquals(InsightSeverity.INFO, item?.severity)
    }

    @Test
    fun testDetectWeekendSpendingShift() {
        val cal = Calendar.getInstance()
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        val satTs = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 2)
        val monTs = cal.timeInMillis

        val txs = listOf(
            createExpense("sat_1", 1000.0, satTs),
            createExpense("sat_2", 500.0, satTs),
            createExpense("mon_1", 50.0, monTs),
            createExpense("mon_2", 50.0, monTs)
        )
        val item = FinancialInsightDetectors.detectWeekendSpendingShift(txs, "￥", "zh")
        assertNotNull(item)
        assertEquals("insight_weekend_shift", item?.id)
    }

    @Test
    fun testDetectNoSpendDays() {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        cal.set(Calendar.DAY_OF_MONTH, 5)
        val txs = listOf(createExpense("1", 100.0, cal.timeInMillis))
        val item = FinancialInsightDetectors.detectNoSpendDays(txs, -1, "zh")
        assertNotNull(item)
        assertEquals("insight_no_spend_days", item?.id)
        assertEquals(InsightSeverity.POSITIVE, item?.severity)
    }
}
