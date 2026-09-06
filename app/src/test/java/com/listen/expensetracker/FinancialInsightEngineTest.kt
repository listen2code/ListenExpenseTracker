package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.FinancialInsightEngine
import com.listen.expensetracker.data.engine.InsightSeverity
import com.listen.expensetracker.data.i18n.ExpenseStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar

/**
 * 智能财务洞察与深度环比诊断核心引擎全量单元测试 (FinancialInsightEngineTest)。
 */
class FinancialInsightEngineTest {

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
    fun testMoMIncreaseDetection() {
        // 当前月支出 3000，上月支出 1000 -> 环比增长 200% (> 12% 阈值，应触发 WARNING)
        val calNow = Calendar.getInstance()
        val currentTs = calNow.timeInMillis

        val calPrev = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevTs = calPrev.timeInMillis

        val txList = listOf(
            createTx("1", 3000.0, timestamp = currentTs),
            createTx("2", 1000.0, timestamp = prevTs)
        )

        val insights = FinancialInsightEngine.generateInsights(
            allTransactions = txList,
            currentOffset = 0,
            monthlyBudget = 5000.0,
            lang = "zh"
        )

        val momInc = insights.find { it.id == "insight_mom_increase" }
        assertNotNull("Should detect MoM increase", momInc)
        assertEquals(InsightSeverity.WARNING, momInc?.severity)
    }

    @Test
    fun testMoMDecreaseDetection() {
        // 当前月支出 800，上月支出 2000 -> 环比减少 60% (<-12% 阈值，应触发 POSITIVE)
        val calNow = Calendar.getInstance()
        val currentTs = calNow.timeInMillis

        val calPrev = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevTs = calPrev.timeInMillis

        val txList = listOf(
            createTx("1", 800.0, timestamp = currentTs),
            createTx("2", 2000.0, timestamp = prevTs)
        )

        val insights = FinancialInsightEngine.generateInsights(
            allTransactions = txList,
            currentOffset = 0,
            monthlyBudget = 5000.0,
            lang = "zh"
        )

        val momDec = insights.find { it.id == "insight_mom_decrease" }
        assertNotNull("Should detect MoM decrease", momDec)
        assertEquals(InsightSeverity.POSITIVE, momDec?.severity)
    }

    @Test
    fun testCategorySpikeDrilldown() {
        // 当前月某一分类从 100 跃升至 500 (> 1.8x)，应触发分类突增提示
        val calNow = Calendar.getInstance()
        val currentTs = calNow.timeInMillis

        val calPrev = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val prevTs = calPrev.timeInMillis

        val txList = listOf(
            createTx("1", 500.0, categoryId = "c_entertainment", categoryName = "娱乐", timestamp = currentTs),
            createTx("2", 100.0, categoryId = "c_entertainment", categoryName = "娱乐", timestamp = prevTs)
        )

        val insights = FinancialInsightEngine.generateInsights(
            allTransactions = txList,
            currentOffset = 0,
            monthlyBudget = 5000.0,
            lang = "zh"
        )

        val catJump = insights.find { it.categoryId == "c_entertainment" }
        assertNotNull("Should detect category jump", catJump)
        assertTrue(catJump?.isCategoryAction == true)
    }

    @Test
    fun testBurnRatePredictor() {
        // 当月日均支出较高，推算整月将超出预算时触发
        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (currentDay in 3..(maxDays - 2)) {
            val dailyExpense = 200.0
            val totalExpense = dailyExpense * currentDay
            val txList = (1..currentDay).map { d ->
                val c = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
                createTx("tx_$d", dailyExpense, timestamp = c.timeInMillis)
            }
            // 预算设置为低于预估月总支出 (预估: 200 * maxDays >= 5600)，且高于当前支出
            val budget = totalExpense + 500.0
            val insights = FinancialInsightEngine.generateInsights(
                allTransactions = txList,
                currentOffset = 0,
                monthlyBudget = budget,
                lang = "zh"
            )
            val burnRate = insights.find { it.id == "insight_burn_rate" }
            assertNotNull("Should detect burn rate warning", burnRate)
            assertEquals(InsightSeverity.WARNING, burnRate?.severity)
            assertTrue("Burn rate should trigger budget adjustment action", burnRate?.isBudgetAction == true)
        }
    }

    @Test
    fun testPeakDayDetection() {
        val cal = Calendar.getInstance()
        val c1 = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val c2 = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 2) }
        val c3 = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 3) }

        val txList = listOf(
            createTx("1", 1200.0, timestamp = c1.timeInMillis), // 峰值占 1200 / 2000 = 60% >= 35%
            createTx("2", 400.0, timestamp = c2.timeInMillis),
            createTx("3", 400.0, timestamp = c3.timeInMillis)
        )

        val insights = FinancialInsightEngine.generateInsights(
            allTransactions = txList,
            currentOffset = 0,
            monthlyBudget = 5000.0,
            lang = "zh"
        )

        val peakDay = insights.find { it.id == "insight_peak_day" }
        assertNotNull("Should detect peak day", peakDay)
        assertEquals(1, peakDay?.targetDay)
        val expectedMonth = cal.get(Calendar.MONTH) + 1
        assertEquals("${expectedMonth}月1日", peakDay?.targetDateLabel)
    }

    @Test
    fun testDemoDataEngineMultiStateCoverage() {
        val generated = com.listen.expensetracker.data.engine.DemoDataEngine.generate(
            monthOffset = 0,
            lang = "zh"
        )
        assertTrue(generated.isNotEmpty())

        val insights = FinancialInsightEngine.generateInsights(
            allTransactions = generated,
            currentOffset = 0,
            monthlyBudget = 5000.0,
            lang = "zh"
        )

        // 验证 4 种主要告警洞察是否在生成的数据中得到覆盖
        val hasMom = insights.any { it.id.startsWith("insight_mom_") }
        val hasSpike = insights.any { it.id.startsWith("insight_cat_jump_") }
        val hasPeak = insights.any { it.id == "insight_peak_day" }

        assertTrue("Demo data should trigger MoM insight", hasMom)
        assertTrue("Demo data should trigger category spike insight", hasSpike)
        assertTrue("Demo data should trigger peak day insight", hasPeak)
    }

    @Test
    fun testAnnualOverviewCalculation() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.MONTH, 0) // 1月
            set(Calendar.DAY_OF_MONTH, 5)
        }
        val janTs = cal.timeInMillis

        val cal2 = Calendar.getInstance().apply {
            set(Calendar.MONTH, 1) // 2月
            set(Calendar.DAY_OF_MONTH, 10)
        }
        val febTs = cal2.timeInMillis

        val txList = listOf(
            createTx("1", 1500.0, type = TransactionType.EXPENSE, timestamp = janTs),
            createTx("2", 5000.0, type = TransactionType.INCOME, timestamp = janTs),
            createTx("3", 800.0, type = TransactionType.EXPENSE, timestamp = febTs)
        )

        val summaries = FinancialInsightEngine.calculateAnnualOverview(
            allTransactions = txList,
            currentOffset = 0,
            lang = "zh"
        )

        assertEquals(12, summaries.size)
        assertEquals(1500.0, summaries[0].totalExpense, 0.01)
        assertEquals(5000.0, summaries[0].totalIncome, 0.01)
        assertEquals(3500.0, summaries[0].netBalance, 0.01)
        assertEquals(800.0, summaries[1].totalExpense, 0.01)
    }
}
