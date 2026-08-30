package com.listen.expensetracker.data.pref

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 偏好设置聚合数据类 (ExpensePreferences) 与观察委托扩展。
 *
 * 响应式配置聚合 (Reactive Preference Aggregation)：
 * 1. 传统做法：每个 ViewModel 分别监听 8 个独立 Flow，导致启动大量重复协程且代码冗长。
 * 2. 聚合模式：利用 DataStore 底层单一 `archDataStore.data` 流，通过一次映射为不可变的 [ExpensePreferences] 领域模型。
 * 3. 极简委托：通过 [observeExpensePreferences] 扩展函数，各 ViewModel 只需声明 1 个协程即可响应全量配置变更。
 */
data class ExpensePreferences(
    val language: String = "zh",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.EMERALD,
    val currencySymbol: String = "￥",
    val monthlyBudget: Double = 5000.0,
    val customAccounts: String = "",
    val autoBackupDrive: Boolean = true,
    val autoBackupWifiOnly: Boolean = false,
    val isDeveloperMode: Boolean = false,
    val hideBalance: Boolean = false
)

/**
 * ViewModel 统一偏好设置观察委托扩展。
 * 在 [viewModelScope] 中启动单一协程监听配置变更，避免在各 ViewModel 中编写重复的协程启动模板代码。
 */
fun ViewModel.observeExpensePreferences(
    prefManager: ExpenseDataStoreManager,
    onChanged: suspend (ExpensePreferences) -> Unit
) {
    viewModelScope.launch {
        prefManager.preferencesFlow.collectLatest { prefs ->
            onChanged(prefs)
        }
    }
}
