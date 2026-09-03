package com.listen.expensetracker.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BudgetModelTest {

    @Test
    fun testCategoryBudgetConfigDefaultRatios() {
        val config = CategoryBudgetConfig()
        assertEquals(5000.0, config.totalBudget, 0.001)

        val foodRatio = config.getRatio("c_food")
        assertEquals(0.30f, foodRatio, 0.001f)

        val foodBudget = config.getBudgetAmount("c_food")
        assertEquals(1500.0, foodBudget, 0.001)

        val unknownRatio = config.getRatio("non_existent_category")
        assertEquals(0.0f, unknownRatio, 0.001f)
        assertEquals(0.0, config.getBudgetAmount("non_existent_category"), 0.001)
    }

    @Test
    fun testCategoryBudgetConfigCustomValues() {
        val customRatios = mapOf("c_food" to 0.40f, "c_housing" to 0.30f)
        val config = CategoryBudgetConfig(totalBudget = 10000.0, categoryRatios = customRatios)

        assertEquals(10000.0, config.totalBudget, 0.001)
        assertEquals(0.40f, config.getRatio("c_food"), 0.001f)
        assertEquals(4000.0, config.getBudgetAmount("c_food"), 0.001)
        assertEquals(3000.0, config.getBudgetAmount("c_housing"), 0.001)
    }

    @Test
    fun testCategoryBudgetStatusInstantiation() {
        val category = CategoryRepository.getCategoryById("c_food")
        val status = CategoryBudgetStatus(
            category = category,
            budgetAmount = 1500.0,
            spentAmount = 1200.0,
            ratio = 0.30f,
            usageRatio = 0.80f,
            remainingAmount = 300.0,
            status = BudgetHealthStatus.WARNING
        )

        assertNotNull(status.category)
        assertEquals("c_food", status.category.id)
        assertEquals(1500.0, status.budgetAmount, 0.001)
        assertEquals(1200.0, status.spentAmount, 0.001)
        assertEquals(300.0, status.remainingAmount, 0.001)
        assertEquals(BudgetHealthStatus.WARNING, status.status)
    }

    @Test
    fun testBudgetHealthStatusEnum() {
        val values = BudgetHealthStatus.values()
        assertEquals(3, values.size)
        assertEquals(BudgetHealthStatus.NORMAL, BudgetHealthStatus.valueOf("NORMAL"))
        assertEquals(BudgetHealthStatus.WARNING, BudgetHealthStatus.valueOf("WARNING"))
        assertEquals(BudgetHealthStatus.OVERBUDGET, BudgetHealthStatus.valueOf("OVERBUDGET"))
    }
}
