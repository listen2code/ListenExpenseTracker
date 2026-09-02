package com.listen.expensetracker.data.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.listen.arch.data.pref.BaseDataStoreManager
import com.listen.arch.data.pref.archDataStore
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore Preferences Manager specifically tailored for ListenExpenseTracker.
 * Inherits common system preferences from BaseDataStoreManager (language, theme, accent, auth)
 * and extends ledger-specific preferences (currency symbol, monthly budget, auto backup).
 */
class ExpenseDataStoreManager(context: Context) : BaseDataStoreManager(context) {

    companion object {
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("expense_currency_symbol")
        val KEY_MONTHLY_BUDGET = doublePreferencesKey("expense_monthly_budget")
        val KEY_CUSTOM_ACCOUNTS = stringPreferencesKey("expense_custom_accounts")
        val KEY_AUTO_BACKUP_DRIVE = booleanPreferencesKey("expense_auto_backup_drive")
        val KEY_AUTO_BACKUP_WIFI_ONLY = booleanPreferencesKey("expense_auto_backup_wifi_only")
        val KEY_LAST_BACKUP_HASH = stringPreferencesKey("expense_last_backup_hash")
        val KEY_DEVELOPER_MODE = booleanPreferencesKey("expense_developer_mode")
        val KEY_HIDE_BALANCE = booleanPreferencesKey("expense_hide_balance")
        val KEY_CATEGORY_BUDGETS = stringPreferencesKey("expense_category_budgets")
    }

    val preferencesFlow: Flow<ExpensePreferences> = context.archDataStore.data.map { prefs ->
        ExpensePreferences(
            language = prefs[KEY_LANGUAGE] ?: "zh",
            themeMode = try {
                ThemeMode.valueOf(prefs[KEY_THEME_MODE] ?: "SYSTEM")
            } catch (_: Exception) {
                ThemeMode.SYSTEM
            },
            accentColor = try {
                AccentColor.valueOf(prefs[KEY_ACCENT_COLOR] ?: "EMERALD")
            } catch (_: Exception) {
                AccentColor.EMERALD
            },
            currencySymbol = prefs[KEY_CURRENCY_SYMBOL] ?: "￥",
            monthlyBudget = prefs[KEY_MONTHLY_BUDGET] ?: 5000.0,
            categoryBudgetRatios = parseCategoryRatios(prefs[KEY_CATEGORY_BUDGETS]),
            customAccounts = prefs[KEY_CUSTOM_ACCOUNTS] ?: "",
            autoBackupDrive = prefs[KEY_AUTO_BACKUP_DRIVE] ?: true,
            autoBackupWifiOnly = prefs[KEY_AUTO_BACKUP_WIFI_ONLY] ?: false,
            isDeveloperMode = prefs[KEY_DEVELOPER_MODE] ?: false,
            hideBalance = prefs[KEY_HIDE_BALANCE] ?: false
        )
    }

    val currencySymbolFlow: Flow<String> = context.archDataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_SYMBOL] ?: "￥"
    }

    val monthlyBudgetFlow: Flow<Double> = context.archDataStore.data.map { prefs ->
        prefs[KEY_MONTHLY_BUDGET] ?: 5000.0
    }

    val customAccountsFlow: Flow<String> = context.archDataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_ACCOUNTS] ?: ""
    }

    val autoBackupDriveFlow: Flow<Boolean> = context.archDataStore.data.map { prefs ->
        prefs[KEY_AUTO_BACKUP_DRIVE] ?: true
    }

    val autoBackupWifiOnlyFlow: Flow<Boolean> = context.archDataStore.data.map { prefs ->
        prefs[KEY_AUTO_BACKUP_WIFI_ONLY] ?: false
    }

    val lastBackupHashFlow: Flow<String> = context.archDataStore.data.map { prefs ->
        prefs[KEY_LAST_BACKUP_HASH] ?: ""
    }

    val isDeveloperModeFlow: Flow<Boolean> = context.archDataStore.data.map { prefs ->
        prefs[KEY_DEVELOPER_MODE] ?: false
    }

    val hideBalanceFlow: Flow<Boolean> = context.archDataStore.data.map { prefs ->
        prefs[KEY_HIDE_BALANCE] ?: false
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.archDataStore.edit { prefs -> prefs[KEY_CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setMonthlyBudget(budget: Double) {
        context.archDataStore.edit { prefs -> prefs[KEY_MONTHLY_BUDGET] = budget }
    }

    suspend fun setCustomAccountsJson(json: String) {
        context.archDataStore.edit { prefs -> prefs[KEY_CUSTOM_ACCOUNTS] = json }
    }

    suspend fun setAutoBackupDrive(enabled: Boolean) {
        context.archDataStore.edit { prefs -> prefs[KEY_AUTO_BACKUP_DRIVE] = enabled }
    }

    suspend fun setAutoBackupWifiOnly(enabled: Boolean) {
        context.archDataStore.edit { prefs -> prefs[KEY_AUTO_BACKUP_WIFI_ONLY] = enabled }
    }

    suspend fun setLastBackupHash(hash: String) {
        context.archDataStore.edit { prefs -> prefs[KEY_LAST_BACKUP_HASH] = hash }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        context.archDataStore.edit { prefs -> prefs[KEY_DEVELOPER_MODE] = enabled }
    }

    suspend fun setHideBalance(hide: Boolean) {
        context.archDataStore.edit { prefs -> prefs[KEY_HIDE_BALANCE] = hide }
    }

    suspend fun setCategoryBudgetRatios(ratios: Map<String, Float>) {
        val serialized = ratios.entries.joinToString(",") { "${it.key}:${it.value}" }
        context.archDataStore.edit { prefs -> prefs[KEY_CATEGORY_BUDGETS] = serialized }
    }

    private fun parseCategoryRatios(raw: String?): Map<String, Float> {
        if (raw.isNullOrBlank()) return com.listen.expensetracker.data.model.CategoryBudgetConfig.defaultRatios
        return try {
            raw.split(",").mapNotNull { part ->
                val kv = part.split(":")
                if (kv.size == 2) kv[0].trim() to (kv[1].trim().toFloatOrNull() ?: return@mapNotNull null) else null
            }.toMap().ifEmpty { com.listen.expensetracker.data.model.CategoryBudgetConfig.defaultRatios }
        } catch (_: Exception) {
            com.listen.expensetracker.data.model.CategoryBudgetConfig.defaultRatios
        }
    }
}
