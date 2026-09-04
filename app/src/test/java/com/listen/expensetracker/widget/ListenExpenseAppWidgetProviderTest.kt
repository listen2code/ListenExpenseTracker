package com.listen.expensetracker.widget

import com.listen.expensetracker.MainActivity
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.BudgetHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ListenExpenseAppWidgetProviderTest {

    @Test
    fun calculateMonthlyExpense_correctlySumsExpensesWithinRange() {
        val startTs = 1000L
        val endTs = 5000L

        val list = listOf(
            TransactionEntity(id = "1", type = TransactionType.EXPENSE, categoryId = "c_food", categoryName = "餐饮", categoryIcon = "", categoryColorHex = "", amount = 100.0, note = "", accountType = "CASH", timestamp = 2000L),
            TransactionEntity(id = "2", type = TransactionType.EXPENSE, categoryId = "c_transport", categoryName = "交通", categoryIcon = "", categoryColorHex = "", amount = 50.0, note = "", accountType = "CASH", timestamp = 4000L),
            // 收入应忽略
            TransactionEntity(id = "3", type = TransactionType.INCOME, categoryId = "c_salary", categoryName = "工资", categoryIcon = "", categoryColorHex = "", amount = 1000.0, note = "", accountType = "BANK", timestamp = 3000L),
            // 范围外的支出应忽略
            TransactionEntity(id = "4", type = TransactionType.EXPENSE, categoryId = "c_shopping", categoryName = "购物", categoryIcon = "", categoryColorHex = "", amount = 300.0, note = "", accountType = "CASH", timestamp = 6000L),
            TransactionEntity(id = "5", type = TransactionType.EXPENSE, categoryId = "c_food", categoryName = "餐饮", categoryIcon = "", categoryColorHex = "", amount = 80.0, note = "", accountType = "CASH", timestamp = 500L)
        )

        val total = ListenExpenseAppWidgetProvider.calculateMonthlyExpense(list, startTs, endTs)
        assertEquals(150.0, total, 0.001)
    }

    @Test
    fun calculateHealthStatus_evaluatesNormalWarningAndOverBudget() {
        val budget = 5000.0

        // < 80% 正常
        assertEquals(BudgetHealthStatus.NORMAL, ListenExpenseAppWidgetProvider.calculateHealthStatus(3000.0, budget))
        assertEquals(BudgetHealthStatus.NORMAL, ListenExpenseAppWidgetProvider.calculateHealthStatus(3999.0, budget))

        // 80% ~ 100% 预警
        assertEquals(BudgetHealthStatus.WARNING, ListenExpenseAppWidgetProvider.calculateHealthStatus(4000.0, budget))
        assertEquals(BudgetHealthStatus.WARNING, ListenExpenseAppWidgetProvider.calculateHealthStatus(4999.0, budget))

        // >= 100% 超支
        assertEquals(BudgetHealthStatus.OVERBUDGET, ListenExpenseAppWidgetProvider.calculateHealthStatus(5000.0, budget))
        assertEquals(BudgetHealthStatus.OVERBUDGET, ListenExpenseAppWidgetProvider.calculateHealthStatus(5200.0, budget))

        // 未设预算 (<= 0) 恒为正常
        assertEquals(BudgetHealthStatus.NORMAL, ListenExpenseAppWidgetProvider.calculateHealthStatus(200.0, 0.0))
        assertEquals(BudgetHealthStatus.NORMAL, ListenExpenseAppWidgetProvider.calculateHealthStatus(200.0, -100.0))
    }

    @Test
    fun normalizeCategoryId_mapsAliasesCorrectly() {
        assertEquals("c_food", MainActivity.normalizeCategoryId("cat_food"))
        assertEquals("c_food", MainActivity.normalizeCategoryId("c_food"))
        assertEquals("c_transport", MainActivity.normalizeCategoryId("cat_transport"))
        assertEquals("c_shopping", MainActivity.normalizeCategoryId("cat_shopping"))
        assertEquals("c_other_exp", MainActivity.normalizeCategoryId("cat_daily"))
        assertEquals("c_other_exp", MainActivity.normalizeCategoryId("cat_other"))
        assertEquals("c_other_exp", MainActivity.normalizeCategoryId("c_other_exp"))
        assertEquals("custom_123", MainActivity.normalizeCategoryId("custom_123"))
        assertEquals(null, MainActivity.normalizeCategoryId(null))
    }
}
