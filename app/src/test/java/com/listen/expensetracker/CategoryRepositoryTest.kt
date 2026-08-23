package com.listen.expensetracker

import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.CategoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    @Before
    fun setup() {
        ExpenseStrings.init()
    }

    @Test
    fun testExpenseCategoriesLoaded() {
        val expenseCats = CategoryRepository.expenseCategories
        assertTrue(expenseCats.isNotEmpty())
        assertTrue(expenseCats.any { it.id == "c_food" && it.getDisplayName("zh") == "餐饮" })
        assertTrue(expenseCats.any { it.id == "c_transport" && it.getDisplayName("zh") == "交通" })
        assertTrue(expenseCats.any { it.id == "c_shopping" && it.getDisplayName("zh") == "购物" })
    }

    @Test
    fun testIncomeCategoriesLoaded() {
        val incomeCats = CategoryRepository.incomeCategories
        assertTrue(incomeCats.isNotEmpty())
        assertTrue(incomeCats.any { it.id == "c_salary" && it.getDisplayName("zh") == "工资" })
    }

    @Test
    fun testGetCategoryByIdValidAndFallback() {
        val foodCat = CategoryRepository.getCategoryById("c_food")
        assertEquals("c_food", foodCat.id)
        assertEquals("餐饮", foodCat.getDisplayName("zh"))
        assertEquals("Food", foodCat.getDisplayName("en"))

        val unknownCat = CategoryRepository.getCategoryById("non_existent_category")
        assertNotNull(unknownCat)
        assertEquals("c_other_exp", unknownCat.id)
    }
}
