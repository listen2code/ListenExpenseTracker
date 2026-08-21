package com.listen.expensetracker.data.model

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

data class Category(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val nameJa: String,
    val icon: ImageVector,
    val colorHex: String,
    val type: String, // "EXPENSE" or "INCOME"
    val isSystem: Boolean = true
)

object CategoryRepository {

    private val defaultExpenseCategories = listOf(
        Category("c_food", "餐饮", "Food", "外食", Icons.Default.Restaurant, "#EF4444", "EXPENSE", true),
        Category("c_transport", "交通", "Transport", "交通", Icons.Default.DirectionsBus, "#3B82F6", "EXPENSE", true),
        Category("c_shopping", "购物", "Shopping", "買い物", Icons.Default.ShoppingBag, "#EC4899", "EXPENSE", true),
        Category("c_entertainment", "娱乐", "Entertainment", "娯楽", Icons.Default.SportsEsports, "#8B5CF6", "EXPENSE", true),
        Category("c_housing", "居住", "Housing", "住居", Icons.Default.Home, "#F59E0B", "EXPENSE", true),
        Category("c_medical", "医疗", "Medical", "医療", Icons.Default.LocalHospital, "#10B981", "EXPENSE", true),
        Category("c_social", "人情", "Social", "交際費", Icons.Default.CardGiftcard, "#6366F1", "EXPENSE", true),
        Category("c_pets", "宠物", "Pets", "ペット", Icons.Default.Pets, "#F97316", "EXPENSE", true),
        Category("c_fitness", "运动健身", "Fitness", "フィットネス", Icons.Default.FitnessCenter, "#06B6D4", "EXPENSE", true),
        Category("c_cafe", "咖啡饮品", "Cafe", "カフェ", Icons.Default.LocalCafe, "#84CC16", "EXPENSE", true),
        Category("c_other_exp", "其他", "Other", "その他", Icons.Default.MoreHoriz, "#6B7280", "EXPENSE", true)
    )

    private val defaultIncomeCategories = listOf(
        Category("c_salary", "工资", "Salary", "給料", Icons.Default.AccountBalance, "#10B981", "INCOME", true),
        Category("c_investment", "理财", "Investment", "投資", Icons.AutoMirrored.Filled.TrendingUp, "#3B82F6", "INCOME", true),
        Category("c_gift", "礼金", "Gift", "祝儀", Icons.Default.CardGiftcard, "#F59E0B", "INCOME", true),
        Category("c_other_inc", "其他", "Other", "その他", Icons.Default.MoreHoriz, "#6B7280", "INCOME", true)
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
            nameZh = name,
            nameEn = name,
            nameJa = name,
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
            customCategories[idx] = item.copy(nameZh = newName, nameEn = newName, nameJa = newName)
        }
    }

    fun getCategoryById(id: String): Category {
        return (expenseCategories + incomeCategories).find { it.id == id }
            ?: Category("c_other_exp", "其他", "Other", "その他", Icons.Default.MoreHoriz, "#6B7280", "EXPENSE", true)
    }
}
