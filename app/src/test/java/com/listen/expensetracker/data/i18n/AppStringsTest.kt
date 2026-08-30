package com.listen.expensetracker.data.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStringsTest {
    @Test
    fun testNavigationStrings() {
        assertEquals("nav_transactions", AppStrings.NAV_TRANSACTIONS)
        assertEquals("nav_statistics", AppStrings.NAV_STATISTICS)
        assertEquals("nav_settings", AppStrings.NAV_SETTINGS)
    }

    @Test
    fun testBalanceAndLedgerStrings() {
        assertEquals("balance_title", AppStrings.BALANCE_TITLE)
        assertEquals("total_expense", AppStrings.TOTAL_EXPENSE)
        assertEquals("total_income", AppStrings.TOTAL_INCOME)
        assertEquals("monthly_budget", AppStrings.MONTHLY_BUDGET)
    }

    @Test
    fun testAccountsAndFiltersStrings() {
        assertEquals("filter_all", AppStrings.FILTER_ALL)
        assertEquals("filter_cash", AppStrings.FILTER_CASH)
        assertEquals("sort_date_desc", AppStrings.SORT_DATE_DESC)
    }

    @Test
    fun testCategoryStrings() {
        assertEquals("cat_food", AppStrings.CAT_FOOD)
        assertEquals("cat_transport", AppStrings.CAT_TRANSPORT)
        assertEquals("cat_salary", AppStrings.CAT_SALARY)
    }
    
    @Test
    fun testCloudAuthStrings() {
        assertEquals("cloud_status_idle", AppStrings.CLOUD_STATUS_IDLE)
        assertEquals("google_account", AppStrings.GOOGLE_ACCOUNT)
    }
}
