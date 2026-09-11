package com.listen.expensetracker.features.recurring.engine

import android.content.Context
import android.content.SharedPreferences
import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class RecurringNotificationHelperTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()

    @Before
    fun setUp() {
        ExpenseStrings.init()
        whenever(context.getSharedPreferences(eq("expense_notification_prefs"), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(true)
        whenever(prefs.getBoolean(eq("recurring_alerts_enabled"), eq(true))).thenReturn(true)
    }

    private fun createRule(id: String, title: String, amount: Double): RecurringRuleEntity {
        return RecurringRuleEntity(
            id = id,
            title = title,
            type = TransactionType.EXPENSE,
            categoryId = "c_food",
            categoryName = "餐饮美食",
            categoryIcon = "icon_food",
            categoryColorHex = "#FF5722",
            amount = amount,
            accountType = "CASH",
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 1,
            startDate = 1000L,
            nextExecutionDate = 2000L,
            executionType = ExecutionType.AUTO_INSERT
        )
    }

    @Test
    fun testEmptyRules_returnsNull() {
        val result = RecurringNotificationHelper.notifyRecurringBillsExecuted(
            context = context,
            executedRules = emptyList(),
            sendNotification = false
        )
        assertNull(result)
    }

    @Test
    fun testNotificationsDisabled_returnsNull() {
        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(false)
        val rules = listOf(createRule("r1", "房租", 3500.0))
        val result = RecurringNotificationHelper.notifyRecurringBillsExecuted(
            context = context,
            executedRules = rules,
            sendNotification = false
        )
        assertNull(result)
    }

    @Test
    fun testSingleRule_formatsCorrectTitleAndBody() {
        val rules = listOf(createRule("r1", "iCloud订阅", 21.0))
        val result = RecurringNotificationHelper.notifyRecurringBillsExecuted(
            context = context,
            executedRules = rules,
            currencySymbol = "￥",
            lang = "zh",
            sendNotification = false
        )

        assertTrue(result != null)
        assertEquals("📅 周期账单已自动入账", result?.first)
        assertTrue(result?.second?.contains("iCloud订阅") == true)
        assertTrue(result?.second?.contains("21") == true)
    }

    @Test
    fun testMultipleRules_formatsAggregatedTitleAndBody() {
        val rules = listOf(
            createRule("r1", "宽带费", 100.0),
            createRule("r2", "健身房月卡", 300.0)
        )
        val result = RecurringNotificationHelper.notifyRecurringBillsExecuted(
            context = context,
            executedRules = rules,
            currencySymbol = "￥",
            lang = "zh",
            sendNotification = false
        )

        assertTrue(result != null)
        assertTrue(result?.first?.contains("2") == true) // 共 2 笔
        assertTrue(result?.second?.contains("400") == true) // 合计 400
    }
}
