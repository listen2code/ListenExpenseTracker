package com.listen.expensetracker.data.model

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.listen.arch.i18n.StringsRes

data class Category(
    val id: String,
    val nameKey: String,
    val customName: String? = null,
    val icon: ImageVector,
    val colorHex: String,
    val type: String, // "EXPENSE" or "INCOME"
    val isSystem: Boolean = true
) {
    fun getDisplayName(lang: String = "zh"): String {
        return customName ?: nameKey.tr(lang)
    }
}

object CategoryRepository {

    private val defaultExpenseCategories = listOf(
        Category("c_food", AppStrings.cat_food, null, Icons.Default.Restaurant, "#EF4444", "EXPENSE", true),
        Category("c_transport", AppStrings.cat_transport, null, Icons.Default.DirectionsBus, "#3B82F6", "EXPENSE", true),
        Category("c_shopping", AppStrings.cat_shopping, null, Icons.Default.ShoppingBag, "#EC4899", "EXPENSE", true),
        Category("c_entertainment", AppStrings.cat_entertainment, null, Icons.Default.SportsEsports, "#8B5CF6", "EXPENSE", true),
        Category("c_housing", AppStrings.cat_housing, null, Icons.Default.Home, "#F59E0B", "EXPENSE", true),
        Category("c_medical", AppStrings.cat_medical, null, Icons.Default.LocalHospital, "#10B981", "EXPENSE", true),
        Category("c_social", AppStrings.cat_social, null, Icons.Default.CardGiftcard, "#6366F1", "EXPENSE", true),
        Category("c_pets", AppStrings.cat_pets, null, Icons.Default.Pets, "#F97316", "EXPENSE", true),
        Category("c_fitness", AppStrings.cat_fitness, null, Icons.Default.FitnessCenter, "#06B6D4", "EXPENSE", true),
        Category("c_cafe", AppStrings.cat_cafe, null, Icons.Default.LocalCafe, "#84CC16", "EXPENSE", true),
        Category("c_other_exp", AppStrings.cat_other_exp, null, Icons.Default.MoreHoriz, "#6B7280", "EXPENSE", true)
    )

    private val defaultIncomeCategories = listOf(
        Category("c_salary", AppStrings.cat_salary, null, Icons.Default.AccountBalance, "#10B981", "INCOME", true),
        Category("c_investment", AppStrings.cat_investment, null, Icons.AutoMirrored.Filled.TrendingUp, "#3B82F6", "INCOME", true),
        Category("c_gift", AppStrings.cat_gift, null, Icons.Default.CardGiftcard, "#F59E0B", "INCOME", true),
        Category("c_other_inc", AppStrings.cat_other_inc, null, Icons.Default.MoreHoriz, "#6B7280", "INCOME", true)
    )

    private val customCategories = mutableListOf<Category>()

    val expenseCategories: List<Category>
        get() = defaultExpenseCategories + customCategories.filter { it.type == "EXPENSE" }

    val incomeCategories: List<Category>
        get() = defaultIncomeCategories + customCategories.filter { it.type == "INCOME" }

    fun addCustomCategory(
        name: String,
        type: String,
        colorHex: String = "#8B5CF6",
        icon: ImageVector = Icons.Default.CardGiftcard
    ): Category {
        val id = "cat_custom_" + System.currentTimeMillis()
        val cat = Category(
            id = id,
            nameKey = "",
            customName = name,
            icon = icon,
            colorHex = colorHex,
            type = type,
            isSystem = false
        )
        customCategories.add(cat)
        return cat
    }

    fun deleteCategory(id: String): Boolean {
        return customCategories.removeAll { it.id == id }
    }

    fun updateCategory(id: String, newName: String) {
        val idx = customCategories.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val item = customCategories[idx]
            customCategories[idx] = item.copy(customName = newName)
        }
    }

    fun getCategoryById(id: String): Category {
        return (expenseCategories + incomeCategories).find { it.id == id }
            ?: Category("c_other_exp", "cat_other_exp", null, Icons.Default.MoreHoriz, "#6B7280", "EXPENSE", true)
    }
}
