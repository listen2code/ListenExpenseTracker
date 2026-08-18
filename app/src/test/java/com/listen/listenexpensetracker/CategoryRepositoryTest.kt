package com.listen.listenexpensetracker

import com.listen.listenexpensetracker.data.model.CategoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRepositoryTest {

    @Test
    fun testExpenseCategoriesLoaded() {
        val expenseCats = CategoryRepository.expenseCategories
        assertTrue(expenseCats.isNotEmpty())
        assertTrue(expenseCats.any { it.id == "c_food" && it.nameZh == "餐饮" })
        assertTrue(expenseCats.any { it.id == "c_transport" && it.nameZh == "交通" })
        assertTrue(expenseCats.any { it.id == "c_shopping" && it.nameZh == "购物" })
    }

    @Test
    fun testIncomeCategoriesLoaded() {
        val incomeCats = CategoryRepository.incomeCategories
        assertTrue(incomeCats.isNotEmpty())
        assertTrue(incomeCats.any { it.id == "c_salary" && it.nameZh == "工资" })
    }

    @Test
    fun testGetCategoryByIdValidAndFallback() {
        val foodCat = CategoryRepository.getCategoryById("c_food")
        assertEquals("c_food", foodCat.id)
        assertEquals("餐饮", foodCat.nameZh)

        val unknownCat = CategoryRepository.getCategoryById("non_existent_category")
        assertNotNull(unknownCat)
        assertEquals("c_other_exp", unknownCat.id)
    }
}
