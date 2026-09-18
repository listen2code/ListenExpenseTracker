package com.listen.expensetracker.core.shortcut

import android.content.Context
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.listen.expensetracker.R
import com.listen.expensetracker.core.quickadd.QuickAddActivity
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.CategoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 桌面快捷方式动态管理器 (ExpenseShortcutManager)。
 * 负责分析用户近期高频消费场景，动态在系统桌面注入专属快捷入口（如“记餐饮”、“记交通”）。
 */
object ExpenseShortcutManager {

    const val SHORTCUT_ID_FOOD = "shortcut_dynamic_food"
    const val SHORTCUT_ID_TRANSPORT = "shortcut_dynamic_transport"

    /**
     * 根据最近消费记录动态刷新快捷方式列表。
     */
    fun updateDynamicShortcuts(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
                val recentTransactions = db.transactionDao().getAllTransactions()
                    .filter { it.type == TransactionType.EXPENSE && it.timestamp >= sevenDaysAgo }

                // 统计最近 7 天使用频率最高的 2 个分类
                val topCategoryIds = if (recentTransactions.isNotEmpty()) {
                    recentTransactions.groupingBy { it.categoryId }
                        .eachCount()
                        .entries
                        .sortedByDescending { it.value }
                        .take(2)
                        .map { it.key }
                } else {
                    listOf("c_food", "c_transport")
                }

                val shortcuts = topCategoryIds.mapNotNull { catId ->
                    buildShortcutForCategory(context, catId)
                }

                if (shortcuts.isNotEmpty()) {
                    ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
                }
            } catch (_: Exception) {
                // 静默容错，快捷方式不阻碍主业务
            }
        }
    }

    /**
     * 构建单个分类的动态快捷方式。
     */
    fun buildShortcutForCategory(context: Context, categoryId: String): ShortcutInfoCompat? {
        val labelRes = when (categoryId) {
            "c_food", "cat_food" -> R.string.shortcut_category_food
            "c_transport", "cat_transport" -> R.string.shortcut_category_transport
            "c_shopping", "cat_shopping" -> R.string.shortcut_category_shopping
            else -> return null
        }

        val shortcutId = "shortcut_dynamic_$categoryId"
        val label = context.getString(labelRes)
        val intent = QuickAddActivity.createIntent(context, categoryId, TransactionType.EXPENSE)
        intent.action = android.content.Intent.ACTION_VIEW

        return ShortcutInfoCompat.Builder(context, shortcutId)
            .setShortLabel(label)
            .setLongLabel(label)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_quick_add))
            .setIntent(intent)
            .build()
    }
}
