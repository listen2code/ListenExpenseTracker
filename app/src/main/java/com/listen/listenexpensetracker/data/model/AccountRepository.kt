package com.listen.listenexpensetracker.data.model

data class AccountTypeItem(
    val key: String,
    val nameZh: String,
    val isSystem: Boolean = false
)

object AccountRepository {
    private val defaultAccounts = mutableListOf(
        AccountTypeItem(key = "CASH", nameZh = "现金", isSystem = true),
        AccountTypeItem(key = "BANK", nameZh = "银行卡", isSystem = true)
    )

    private val customAccounts = mutableListOf<AccountTypeItem>()

    fun getAllAccounts(): List<AccountTypeItem> {
        return defaultAccounts + customAccounts
    }

    fun addAccount(name: String): AccountTypeItem {
        val key = "ACC_" + System.currentTimeMillis()
        val item = AccountTypeItem(key = key, nameZh = name, isSystem = false)
        customAccounts.add(item)
        return item
    }

    fun deleteAccount(key: String): Boolean {
        return customAccounts.removeAll { it.key == key }
    }

    fun updateAccount(key: String, newName: String) {
        val index = customAccounts.indexOfFirst { it.key == key }
        if (index >= 0) {
            customAccounts[index] = customAccounts[index].copy(nameZh = newName)
        }
    }

    fun getAccountName(key: String): String {
        return getAllAccounts().find { it.key == key }?.nameZh ?: key
    }
}
