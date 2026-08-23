package com.listen.expensetracker

import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccountRepositoryTest {

    @Before
    fun setup() {
        ExpenseStrings.init()
    }

    @Test
    fun testDefaultAccounts() {
        val accounts = AccountRepository.getAllAccounts()
        assertTrue(accounts.size >= 3)
        assertTrue(accounts.any { it.key == "CASH" && it.getDisplayName("zh") == "现金" && it.isSystem })
        assertTrue(accounts.any { it.key == "BANK" && it.getDisplayName("zh") == "银行卡" && it.isSystem })
        assertTrue(accounts.any { it.key == "CREDIT" && it.getDisplayName("zh") == "信用卡" && it.isSystem })
    }

    @Test
    fun testFilterKeysContainsAll() {
        val filterKeys = AccountRepository.getFilterKeys()
        assertTrue(filterKeys.contains("ALL"))
        assertTrue(filterKeys.contains("CASH"))
        assertTrue(filterKeys.contains("BANK"))
        assertTrue(filterKeys.contains("CREDIT"))
    }

    @Test
    fun testAddEditDeleteCustomAccount() {
        val created = AccountRepository.addAccount("理财通")
        assertNotNull(created.key)
        assertEquals("理财通", created.getDisplayName("zh"))
        assertFalse(created.isSystem)

        // Get Name
        assertEquals("理财通", AccountRepository.getAccountDisplayName(created.key, "zh"))

        // Update Name
        AccountRepository.updateAccount(created.key, "微信理财通")
        assertEquals("微信理财通", AccountRepository.getAccountDisplayName(created.key, "zh"))

        // Delete Account
        val deleted = AccountRepository.deleteAccount(created.key)
        assertTrue(deleted)
    }

    @Test
    fun testGetAccountNameFallback() {
        assertEquals("未知账户", AccountRepository.getAccountDisplayName("未知账户", "zh"))
    }
}
