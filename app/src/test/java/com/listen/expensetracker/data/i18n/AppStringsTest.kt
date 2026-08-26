package com.listen.expensetracker.data.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStringsTest {
    @Test
    fun testNavigationStrings() {
        assertEquals("nav_transactions", AppStrings.nav_transactions)
        assertEquals("nav_statistics", AppStrings.nav_statistics)
        assertEquals("nav_settings", AppStrings.nav_settings)
    }

    @Test
    fun testBalanceAndLedgerStrings() {
        assertEquals("balance_title", AppStrings.balance_title)
        assertEquals("total_expense", AppStrings.total_expense)
        assertEquals("total_income", AppStrings.total_income)
        assertEquals("monthly_budget", AppStrings.monthly_budget)
    }

    @Test
    fun testAccountsAndFiltersStrings() {
        assertEquals("filter_all", AppStrings.filter_all)
        assertEquals("filter_wechat", AppStrings.filter_wechat)
        assertEquals("sort_date_desc", AppStrings.sort_date_desc)
    }

    @Test
    fun testCategoryStrings() {
        assertEquals("cat_food", AppStrings.cat_food)
        assertEquals("cat_transport", AppStrings.cat_transport)
        assertEquals("cat_salary", AppStrings.cat_salary)
    }
    
    @Test
    fun testCloudAuthStrings() {
        assertEquals("cloud_status_idle", AppStrings.cloud_status_idle)
        assertEquals("google_account", AppStrings.google_account)
    }
}
