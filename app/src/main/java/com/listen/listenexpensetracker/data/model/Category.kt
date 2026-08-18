package com.listen.listenexpensetracker.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

data class Category(
    val id: String,
    val nameZh: String,
    val nameEn: String,
    val nameJa: String,
    val icon: ImageVector,
    val colorHex: String,
    val type: String // "EXPENSE" or "INCOME"
)

object CategoryRepository {
    val expenseCategories = listOf(
        Category("c_food", "餐饮", "Food", "外食", Icons.Default.Restaurant, "#EF4444", "EXPENSE"),
        Category("c_transport", "交通", "Transport", "交通", Icons.Default.DirectionsBus, "#3B82F6", "EXPENSE"),
        Category("c_shopping", "购物", "Shopping", "買い物", Icons.Default.ShoppingBag, "#EC4899", "EXPENSE"),
        Category("c_entertainment", "娱乐", "Entertainment", "娯楽", Icons.Default.SportsEsports, "#8B5CF6", "EXPENSE"),
        Category("c_housing", "居住", "Housing", "住居", Icons.Default.Home, "#F59E0B", "EXPENSE"),
        Category("c_medical", "医疗", "Medical", "医療", Icons.Default.LocalHospital, "#10B981", "EXPENSE"),
        Category("c_social", "人情", "Social", "交際費", Icons.Default.CardGiftcard, "#6366F1", "EXPENSE"),
        Category("c_other_exp", "其他", "Other", "その他", Icons.Default.MoreHoriz, "#6B7280", "EXPENSE")
    )

    val incomeCategories = listOf(
        Category("c_salary", "工资", "Salary", "给料", Icons.Default.AccountBalance, "#10B981", "INCOME"),
        Category("c_investment", "理财", "Investment", "投资", Icons.Default.TrendingUp, "#3B82F6", "INCOME"),
        Category("c_gift", "礼金", "Gift", "祝仪", Icons.Default.CardGiftcard, "#F59E0B", "INCOME"),
        Category("c_other_inc", "其他", "Other", "その他", Icons.Default.MoreHoriz, "#6B7280", "INCOME")
    )

    fun getCategoryById(id: String): Category {
        return (expenseCategories + incomeCategories).find { it.id == id }
            ?: Category("c_other_exp", "其他", "Other", "その他", Icons.Default.MoreHoriz, "#6B7280", "EXPENSE")
    }
}
