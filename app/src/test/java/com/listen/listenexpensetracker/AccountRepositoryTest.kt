package com.listen.listenexpensetracker

import com.listen.listenexpensetracker.data.model.AccountRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountRepositoryTest {

    @Test
    fun testDefaultAccounts() {
        val accounts = AccountRepository.getAllAccounts()
        assertTrue(accounts.size >= 2)
        assertTrue(accounts.any { it.key == "CASH" && it.nameZh == "现金" && it.isSystem })
        assertTrue(accounts.any { it.key == "BANK" && it.nameZh == "银行卡" && it.isSystem })
    }

    @Test
    fun testAddEditDeleteCustomAccount() {
        val created = AccountRepository.addAccount("信用卡")
        assertNotNull(created.key)
        assertEquals("信用卡", created.nameZh)
        assertFalse(created.isSystem)

        // Get Name
        assertEquals("信用卡", AccountRepository.getAccountName(created.key))

        // Update Name
        AccountRepository.updateAccount(created.key, "招商信用卡")
        assertEquals("招商信用卡", AccountRepository.getAccountName(created.key))

        // Delete Account
        val deleted = AccountRepository.deleteAccount(created.key)
        assertTrue(deleted)
    }

    @Test
    fun testGetAccountNameFallback() {
        assertEquals("未知账户", AccountRepository.getAccountName("未知账户"))
    }
}
