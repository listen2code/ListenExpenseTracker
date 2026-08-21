package com.listen.expensetracker

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import com.listen.expensetracker.data.model.Category
import com.listen.expensetracker.data.model.CategoryRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryRepositoryComprehensiveTest {

    @Test
    fun testDefaultCategoriesPresence() {
        val expense = CategoryRepository.expenseCategories
        assertTrue(expense.size >= 8)
        assertTrue(expense.all { it.type == "EXPENSE" })

        val income = CategoryRepository.incomeCategories
        assertTrue(income.size >= 4)
        assertTrue(income.all { it.type == "INCOME" })
    }

    @Test
    fun testCustomCategoryAdditionAndDeletion() {
        val created = CategoryRepository.addCustomCategory(
            name = "盲盒潮玩",
            type = "EXPENSE",
            colorHex = "#FBBF24"
        )
        assertNotNull(created.id)
        assertEquals("盲盒潮玩", created.nameZh)
        assertFalse(created.isSystem)

        val retrieved = CategoryRepository.getCategoryById(created.id)
        assertEquals(created.id, retrieved.id)
        assertEquals("盲盒潮玩", retrieved.nameZh)

        // Update category name
        CategoryRepository.updateCategory(created.id, "潮玩手办")
        assertEquals("潮玩手办", CategoryRepository.getCategoryById(created.id).nameZh)

        // Delete custom category
        val deleted = CategoryRepository.deleteCategory(created.id)
        assertTrue(deleted)
    }

    @Test
    fun testGetCategoryByIdFallback() {
        val fallback = CategoryRepository.getCategoryById("unknown_id_9999")
        assertNotNull(fallback)
        assertEquals("c_other_exp", fallback.id)
    }
}
