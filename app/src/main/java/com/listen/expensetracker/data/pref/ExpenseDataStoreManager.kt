package com.listen.expensetracker.data.pref

import android.content.Context
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
 * and extends ledger-specific preferences (currency symbol, monthly budget).
 */
class ExpenseDataStoreManager(context: Context) : BaseDataStoreManager(context) {

    companion object {
        val KEY_CURRENCY_SYMBOL = stringPreferencesKey("expense_currency_symbol")
        val KEY_MONTHLY_BUDGET = doublePreferencesKey("expense_monthly_budget")
    }

    val currencySymbolFlow: Flow<String> = context.archDataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_SYMBOL] ?: "￥"
    }

    val monthlyBudgetFlow: Flow<Double> = context.archDataStore.data.map { prefs ->
        prefs[KEY_MONTHLY_BUDGET] ?: 5000.0
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.archDataStore.edit { prefs -> prefs[KEY_CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setMonthlyBudget(budget: Double) {
        context.archDataStore.edit { prefs -> prefs[KEY_MONTHLY_BUDGET] = budget }
    }
}
