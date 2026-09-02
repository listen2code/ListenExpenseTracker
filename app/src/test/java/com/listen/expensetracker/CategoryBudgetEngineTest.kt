package com.listen.expensetracker

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.CategoryBudgetEngine
import com.listen.expensetracker.data.model.BudgetHealthStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryBudgetEngineTest {

    @Test
    fun testCalculateCategoryBudgets() {
        val now = System.currentTimeMillis()
        val transactions = listOf(
            TransactionEntity(
                id = "tx1",
                type = TransactionType.EXPENSE,
                categoryId = "c_food",
                categoryName = "餐饮",
                categoryIcon = "c_food",
                categoryColorHex = "#EF4444",
                amount = 1600.0,
                timestamp = now
            ),
            TransactionEntity(
                id = "tx2",
                type = TransactionType.EXPENSE,
                categoryId = "c_shopping",
                categoryName = "购物",
                categoryIcon = "c_shopping",
                categoryColorHex = "#EC4899",
                amount = 900.0,
                timestamp = now
            )
        )

        val totalBudget = 5000.0
        val ratios = mapOf(
            "c_food" to 0.30f,     // 1500 budget (spent 1600 -> OVERBUDGET)
            "c_shopping" to 0.20f, // 1000 budget (spent 900 -> 90% -> WARNING)
            "c_housing" to 0.50f   // 2500 budget (spent 0 -> NORMAL)
        )

        val result = CategoryBudgetEngine.calculate(
            allTransactions = transactions,
            currentOffset = 0,
            totalBudget = totalBudget,
            categoryRatios = ratios
        )

        assertEquals(5000.0, result.totalBudget, 0.01)
        assertEquals(2500.0, result.totalSpent, 0.01)
        assertEquals(2500.0, result.remainingBudget, 0.01)

        val foodStatus = result.statusList.find { it.category.id == "c_food" }
        val shoppingStatus = result.statusList.find { it.category.id == "c_shopping" }
        val housingStatus = result.statusList.find { it.category.id == "c_housing" }

        assertTrue(foodStatus != null)
        assertEquals(BudgetHealthStatus.OVERBUDGET, foodStatus?.status)
        assertEquals(1500.0, foodStatus?.budgetAmount ?: 0.0, 0.01)
        assertEquals(1600.0, foodStatus?.spentAmount ?: 0.0, 0.01)

        assertTrue(shoppingStatus != null)
        assertEquals(BudgetHealthStatus.WARNING, shoppingStatus?.status)

        assertTrue(housingStatus != null)
        assertEquals(BudgetHealthStatus.NORMAL, housingStatus?.status)

        assertEquals(1, result.overBudgetCount)
        assertEquals(1, result.warningCount)
    }
}
