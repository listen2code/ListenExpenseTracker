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
        AccountTypeItem(key = "CASH", nameKey = AppStrings.FILTER_CASH, isSystem = true),
        AccountTypeItem(key = "BANK", nameKey = AppStrings.FILTER_BANK, isSystem = true),
        AccountTypeItem(key = "CREDIT", nameKey = AppStrings.FILTER_CREDIT, isSystem = true)
    )

    private val customAccounts = mutableListOf<AccountTypeItem>()
    var onAccountsChangedListener: ((String) -> Unit)? = null

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
        notifyChanged()
        return item
    }

    fun deleteAccount(key: String): Boolean {
        val removed = customAccounts.removeAll { it.key == key }
        if (removed) notifyChanged()
        return removed
    }

    fun updateAccount(key: String, newName: String) {
        val index = customAccounts.indexOfFirst { it.key == key }
        if (index >= 0) {
            customAccounts[index] = customAccounts[index].copy(customName = newName)
            notifyChanged()
        }
    }

    fun getAccountDisplayName(key: String, lang: String = "zh"): String {
        if (key == ALL_ACCOUNTS_KEY) return AppStrings.FILTER_ALL.tr(lang)
        return getAllAccounts().find { it.key == key }?.getDisplayName(lang) ?: key
    }

    fun getAccountName(key: String, lang: String = "zh"): String {
        return getAccountDisplayName(key, lang)
    }

    fun serializeCustomAccounts(): String {
        val items = customAccounts.map { acct ->
            val safeKey = acct.key.replace("\"", "\\\"")
            val safeName = (acct.customName ?: "").replace("\"", "\\\"")
            "{\"key\":\"$safeKey\",\"customName\":\"$safeName\"}"
        }
        return "[${items.joinToString(",")}]"
    }

    fun deserializeCustomAccounts(json: String) {
        if (json.isBlank()) return
        try {
            val loaded = mutableListOf<AccountTypeItem>()
            val regex = Regex("""\{"key":"(.*?)","customName":"(.*?)"\}""")
            regex.findAll(json).forEach { match ->
                val key = match.groupValues[1].replace("\\\"", "\"")
                val name = match.groupValues[2].replace("\\\"", "\"")
                if (key.isNotBlank() && name.isNotBlank()) {
                    loaded.add(AccountTypeItem(key = key, nameKey = "", customName = name, isSystem = false))
                }
            }
            customAccounts.clear()
            customAccounts.addAll(loaded)
        } catch (_: Exception) {}
    }

    private fun notifyChanged() {
        onAccountsChangedListener?.invoke(serializeCustomAccounts())
    }
}
