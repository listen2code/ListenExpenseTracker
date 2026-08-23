package com.listen.expensetracker.data.model

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import com.listen.arch.i18n.StringsRes

data class AccountTypeItem(
    val key: String,
    val nameKey: String = "",
    val customName: String? = null,
    val isSystem: Boolean = true
) {
    fun getDisplayName(lang: String = "zh"): String {
        return customName ?: if (nameKey.isNotBlank()) nameKey.tr(lang) else key
    }
}

object AccountRepository {
    const val ALL_ACCOUNTS_KEY = "ALL"

    private val defaultAccounts = listOf(
        AccountTypeItem(key = "CASH", nameKey = AppStrings.filter_cash, isSystem = true),
        AccountTypeItem(key = "BANK", nameKey = AppStrings.filter_bank, isSystem = true),
        AccountTypeItem(key = "CREDIT", nameKey = AppStrings.filter_credit, isSystem = true)
    )

    private val customAccounts = mutableListOf<AccountTypeItem>()

    fun getAllAccounts(): List<AccountTypeItem> {
        return defaultAccounts + customAccounts
    }

    fun getFilterKeys(): List<String> {
        return listOf(ALL_ACCOUNTS_KEY) + getAllAccounts().map { it.key }
    }

    fun addAccount(name: String): AccountTypeItem {
        val key = "ACC_" + System.currentTimeMillis()
        val item = AccountTypeItem(key = key, nameKey = "", customName = name, isSystem = false)
        customAccounts.add(item)
        return item
    }

    fun deleteAccount(key: String): Boolean {
        return customAccounts.removeAll { it.key == key }
    }

    fun updateAccount(key: String, newName: String) {
        val index = customAccounts.indexOfFirst { it.key == key }
        if (index >= 0) {
            customAccounts[index] = customAccounts[index].copy(customName = newName)
        }
    }

    fun getAccountDisplayName(key: String, lang: String = "zh"): String {
        if (key == ALL_ACCOUNTS_KEY) return AppStrings.filter_all.tr(lang)
        return getAllAccounts().find { it.key == key }?.getDisplayName(lang) ?: key
    }

    fun getAccountName(key: String, lang: String = "zh"): String {
        return getAccountDisplayName(key, lang)
    }
}
