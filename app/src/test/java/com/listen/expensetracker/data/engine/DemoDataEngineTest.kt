package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataEngineTest {

    @Test
    fun testDemoTemplateDataClass() {
        val template = DemoTemplate(
            categoryId = "c_custom",
            categoryNameKey = AppStrings.CAT_FOOD,
            notes = listOf("Note 1", "Note 2"),
            minAmount = 10,
            maxAmount = 100,
            colorHex = "#FF0000"
        )

        assertEquals("c_custom", template.categoryId)
        assertEquals(AppStrings.CAT_FOOD, template.categoryNameKey)
        assertEquals(2, template.notes.size)
        assertEquals(10, template.minAmount)
        assertEquals(100, template.maxAmount)
        assertEquals("#FF0000", template.colorHex)
    }

    @Test
    fun testGenerateCurrentMonthChineseTransactions() {
        val transactions = DemoDataEngine.generate(monthOffset = 0, lang = "zh")

        assertTrue(transactions.isNotEmpty())
        // Should contain salary + weekend expenses + weekday expenses + latte factors + baseline
        assertTrue(transactions.size >= 10)

        // 1. Check salary income
        val salaryTx = transactions.find { it.categoryId == "c_salary" }
        assertNotNull(salaryTx)
        assertEquals(TransactionType.INCOME, salaryTx?.type)
        assertEquals(16000.0, salaryTx?.amount ?: 0.0, 0.001)
        assertEquals("月度薪资发放", salaryTx?.note)

        // 2. Check major weekend expenses
        val earphoneTx = transactions.find { it.note == "降噪无线耳机" }
        assertNotNull(earphoneTx)
        assertEquals("c_shopping", earphoneTx?.categoryId)
        assertEquals(1350.0, earphoneTx?.amount ?: 0.0, 0.001)
        assertEquals("CREDIT", earphoneTx?.accountType)

        val concertTx = transactions.find { it.note == "演唱会门票" }
        assertNotNull(concertTx)
        assertEquals(480.0, concertTx?.amount ?: 0.0, 0.001)

        // 3. Check micro latte factor transactions
        val latteTx = transactions.find { it.note == "星巴克拿铁" }
        assertNotNull(latteTx)
        assertEquals(22.0, latteTx?.amount ?: 0.0, 0.001)
        assertEquals("CASH", latteTx?.accountType)

        // 4. Check past baseline items exist
        val pastDiningTx = transactions.find { it.note == "上月日常餐饮" }
        assertNotNull(pastDiningTx)
        assertEquals(450.0, pastDiningTx?.amount ?: 0.0, 0.001)

        // 5. Verify all transactions have valid IDs and timestamps
        transactions.forEach { tx ->
            assertTrue(tx.id.isNotBlank())
            assertTrue(tx.timestamp > 0L)
            assertTrue(tx.categoryColorHex.startsWith("#"))
        }
    }

    @Test
    fun testGeneratePastMonthEnglishTransactions() {
        val transactions = DemoDataEngine.generate(monthOffset = -1, lang = "en")

        assertTrue(transactions.isNotEmpty())

        val salaryTx = transactions.find { it.categoryId == "c_salary" }
        assertNotNull(salaryTx)
        assertEquals("Monthly Salary", salaryTx?.note)

        val earphoneTx = transactions.find { it.note == "Noise Canceling Earbuds" }
        assertNotNull(earphoneTx)
        assertEquals(1350.0, earphoneTx?.amount ?: 0.0, 0.001)

        val concertTx = transactions.find { it.note == "Concert Tickets" }
        assertNotNull(concertTx)

        val pastMovieTx = transactions.find { it.note == "Past Movie" }
        assertNotNull(pastMovieTx)
        assertEquals(60.0, pastMovieTx?.amount ?: 0.0, 0.001)
    }

    @Test
    fun testGenerateDefaultRecurringRulesChinese() {
        val rules = DemoDataEngine.generateDefaultRecurringRules("zh")

        assertEquals(4, rules.size)

        // Rule 1: Rent
        val rentRule = rules.find { it.title == "住房租金" }
        assertNotNull(rentRule)
        assertEquals(TransactionType.EXPENSE, rentRule?.type)
        assertEquals(2600.0, rentRule?.amount ?: 0.0, 0.001)
        assertEquals(1, rentRule?.dayOfPeriod)
        assertEquals(RecurringFrequency.MONTHLY, rentRule?.frequency)
        assertEquals(ExecutionType.AUTO_INSERT, rentRule?.executionType)
        assertEquals("每月1日房租", rentRule?.note)

        // Rule 2: Netflix
        val netflixRule = rules.find { it.title == "Netflix 会员" }
        assertNotNull(netflixRule)
        assertEquals(45.0, netflixRule?.amount ?: 0.0, 0.001)
        assertEquals(5, netflixRule?.dayOfPeriod)
        assertEquals(ExecutionType.AUTO_INSERT, netflixRule?.executionType)

        // Rule 3: Salary
        val salaryRule = rules.find { it.title == "每月薪资" }
        assertNotNull(salaryRule)
        assertEquals(TransactionType.INCOME, salaryRule?.type)
        assertEquals(18000.0, salaryRule?.amount ?: 0.0, 0.001)
        assertEquals(10, salaryRule?.dayOfPeriod)
        assertEquals(ExecutionType.NOTIFY_CONFIRM, salaryRule?.executionType)

        // Rule 4: iCloud
        val icloudRule = rules.find { it.title == "iCloud 云存储" }
        assertNotNull(icloudRule)
        assertEquals(21.0, icloudRule?.amount ?: 0.0, 0.001)
        assertEquals(15, icloudRule?.dayOfPeriod)
        assertEquals("CREDIT", icloudRule?.accountType)
    }

    @Test
    fun testGenerateDefaultRecurringRulesEnglishAndJapanese() {
        val enRules = DemoDataEngine.generateDefaultRecurringRules("en")
        assertEquals(4, enRules.size)
        assertTrue(enRules.any { it.title == "Apartment Rent" && it.note == "Monthly Rent" })
        assertTrue(enRules.any { it.title == "Netflix" && it.note == "Premium" })
        assertTrue(enRules.any { it.title == "Monthly Salary" && it.note == "Base Salary" })
        assertTrue(enRules.any { it.title == "iCloud Storage" && it.note == "200GB Plan" })

        val jaRules = DemoDataEngine.generateDefaultRecurringRules("ja")
        assertEquals(4, jaRules.size)
        assertTrue(jaRules.any { it.title == "家賃" })
        assertTrue(jaRules.any { it.title == "Netflix 会員" })
        assertTrue(jaRules.any { it.title == "毎月の給与" })
        assertTrue(jaRules.any { it.title == "iCloud ストレージ" })
    }
}
