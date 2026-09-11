package com.listen.expensetracker.features.budget.engine

import android.content.Context
import android.content.SharedPreferences
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.CategoryBudgetConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Calendar

class BudgetAlertGuardTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()
    private val inMemoryNotifiedKeys = mutableSetOf<String>()

    @Before
    fun setUp() {
        ExpenseStrings.init()
        inMemoryNotifiedKeys.clear()
        whenever(context.getSharedPreferences(eq("expense_notification_prefs"), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putString(any(), any())).thenReturn(editor)
        whenever(editor.putLong(any(), any())).thenReturn(editor)
        whenever(editor.putStringSet(any(), any())).thenAnswer { invocation ->
            @Suppress("UNCHECKED_CAST")
            val set = invocation.getArgument(1) as Set<String>
            inMemoryNotifiedKeys.clear()
            inMemoryNotifiedKeys.addAll(set)
            editor
        }

        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(true)
        whenever(prefs.getBoolean(eq("budget_alerts_enabled"), eq(true))).thenReturn(true)
        whenever(prefs.getBoolean(eq("budget_warning_threshold_enabled"), eq(true))).thenReturn(true)
        whenever(prefs.getStringSet(eq("notified_keys"), any())).thenAnswer { inMemoryNotifiedKeys }
    }

    private fun createExpense(amount: Double, categoryId: String = "c_food"): TransactionEntity {
        val now = Calendar.getInstance().timeInMillis
        return TransactionEntity(
            id = "tx_${amount}_$categoryId",
            type = TransactionType.EXPENSE,
            categoryId = categoryId,
            categoryName = "餐饮美食",
            categoryIcon = "icon_food",
            categoryColorHex = "#FF5722",
            amount = amount,
            note = "测试支出",
            accountType = "CASH",
            timestamp = now
        )
    }

    @Test
    fun testNotificationsDisabled_returnsEmpty() {
        whenever(prefs.getBoolean(eq("notifications_enabled"), eq(true))).thenReturn(false)
        val list = listOf(createExpense(900.0))
        val alerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )
        assertTrue(alerts.isEmpty())
    }

    @Test
    fun testBudgetWarningThreshold_triggersWarningAt80Percent() {
        // Spent 850 of 1000 -> 85% usage ratio -> triggers WARNING
        val list = listOf(createExpense(850.0))
        val alerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )

        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertEquals("TOTAL", alert.targetId)
        assertEquals(BudgetAlertLevel.WARNING, alert.level)
        assertTrue(alert.title.contains("警戒线"))
    }

    @Test
    fun testBudgetOverrun_triggersOverbudgetAlertAt100Percent() {
        // Spent 1100 of 1000 -> 110% usage ratio -> triggers OVERBUDGET
        val list = listOf(createExpense(1100.0))
        val alerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )

        assertEquals(1, alerts.size)
        val alert = alerts.first()
        assertEquals("TOTAL", alert.targetId)
        assertEquals(BudgetAlertLevel.OVERBUDGET, alert.level)
        assertTrue(alert.title.contains("超支"))
    }

    @Test
    fun testDedup_preventsDuplicateAlertsSameLevel() {
        val list = listOf(createExpense(850.0))
        val firstAlerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )
        assertEquals(1, firstAlerts.size)

        // Second evaluation with same spending should return 0 alerts due to dedup
        val secondAlerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )
        assertEquals(0, secondAlerts.size)
    }

    @Test
    fun testDedup_allowsUpgradeFromWarningToOverbudget() {
        val listWarning = listOf(createExpense(850.0))
        val warningAlerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = listWarning,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )
        assertEquals(1, warningAlerts.size)
        assertEquals(BudgetAlertLevel.WARNING, warningAlerts.first().level)

        // Further spending reaches overbudget threshold (1200.0)
        val listOver = listOf(createExpense(1200.0))
        val overAlerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = listOver,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )
        assertEquals(1, overAlerts.size)
        assertEquals(BudgetAlertLevel.OVERBUDGET, overAlerts.first().level)
    }

    @Test
    fun testCategoryBudgetAlert_triggersCategorySpecificOverrun() {
        val ratios = mapOf("c_food" to 0.3f) // Budget for food: 300.0
        val list = listOf(createExpense(350.0, categoryId = "c_food")) // Spent 350.0 > 300.0
        val alerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = ratios,
            sendNotifications = false
        )

        // Should include category overbudget alert
        val catAlert = alerts.find { it.targetId == "c_food" }
        assertTrue(catAlert != null)
        assertEquals(BudgetAlertLevel.OVERBUDGET, catAlert?.level)
    }

    @Test
    fun testWarningThresholdDisabled_suppresses80PercentWarning() {
        whenever(prefs.getBoolean(eq("budget_warning_threshold_enabled"), eq(true))).thenReturn(false)
        val list = listOf(createExpense(850.0)) // 85%
        val alerts = BudgetAlertGuard.evaluate(
            context = context,
            allTransactions = list,
            monthOffset = 0,
            totalBudget = 1000.0,
            categoryRatios = emptyMap(),
            sendNotifications = false
        )
        assertTrue(alerts.isEmpty())
    }
}
