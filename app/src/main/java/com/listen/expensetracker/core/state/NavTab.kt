package com.listen.expensetracker.core.state

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Type-safe Navigation Tab definitions for ListenExpenseTracker.
 * 使用类型安全的枚举替代魔术数字(0, 1, 2)进行 Tab 导航，提升可读性并防止越界错误。
 */
enum class NavTab(
    val route: String,
    val labelKey: String,
    val icon: ImageVector
) {
    TRANSACTIONS("transactions", "nav_transactions", Icons.AutoMirrored.Filled.List),
    STATISTICS("statistics", "nav_statistics", Icons.Default.PieChart),
    SETTINGS("settings", "nav_settings", Icons.Default.Settings)
}
