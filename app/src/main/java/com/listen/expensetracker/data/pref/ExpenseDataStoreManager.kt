package com.listen.expensetracker.data.pref

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.listen.arch.data.pref.BaseDataStoreManager
import com.listen.arch.data.pref.archDataStore
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
}
