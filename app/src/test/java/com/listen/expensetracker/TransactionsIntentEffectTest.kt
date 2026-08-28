package com.listen.expensetracker

import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.core.state.NavTab
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransactionsIntentEffectTest {

    @Test
    fun testTransactionsIntentInstantiations() {
        val addIntent = TransactionsIntent.AddTransaction(
            type = "EXPENSE",
            categoryId = "c_food",
            categoryName = "餐饮",
            categoryIcon = "c_food",
            categoryColorHex = "#EF4444",
            amount = 55.0,
            note = "午餐测试",
            accountType = "CASH",
            timestamp = 1723900000000L
        )
        assertEquals(55.0, addIntent.amount, 0.001)
        assertEquals("CASH", addIntent.accountType)

        val tx = TransactionEntity(
            id = "tx-update",
            type = "INCOME",
            categoryId = "c_salary",
            categoryName = "工资",
            categoryIcon = "c_salary",
            categoryColorHex = "#10B981",
            amount = 10000.0,
            note = "发工资",
            accountType = "BANK"
        )
        val updateIntent = TransactionsIntent.UpdateTransaction(tx)
        assertEquals("tx-update", updateIntent.transaction.id)

        val deleteIntent = TransactionsIntent.DeleteTransaction("tx-delete-1")
        assertEquals("tx-delete-1", deleteIntent.id)

        val restoreIntent = TransactionsIntent.RestoreDeletedTransaction(tx)
        assertEquals("tx-update", restoreIntent.transaction.id)

        val searchIntent = TransactionsIntent.SearchQueryChange("餐饮")
        assertEquals("餐饮", searchIntent.query)

        val sortIntent = TransactionsIntent.ChangeSortOrder(TransactionSortOrder.AMOUNT_DESC)
        assertEquals(TransactionSortOrder.AMOUNT_DESC, sortIntent.order)
    }

    @Test
    fun testSettingsAndStatisticsIntents() {
        val linkGoogleIntent = SettingsIntent.LinkGoogleAccount("user@gmail.com", "Test User", "https://avatar.png")
        assertEquals("user@gmail.com", linkGoogleIntent.email)
        assertEquals("Test User", linkGoogleIntent.displayName)

        val unlinkGoogleIntent = SettingsIntent.UnlinkGoogleAccount
        assertNotNull(unlinkGoogleIntent)

        val langIntent = SettingsIntent.ChangeLanguage("ja")
        assertEquals("ja", langIntent.langCode)

        val themeIntent = SettingsIntent.ChangeThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, themeIntent.mode)

        val accentIntent = SettingsIntent.ChangeAccentColor(AccentColor.ROSE)
        assertEquals(AccentColor.ROSE, accentIntent.accent)

        val statsTabIntent = StatisticsIntent.ChangeStatisticsTab("INCOME")
        assertEquals("INCOME", statsTabIntent.tab)
    }

    @Test
    fun testEffects() {
        val toastEffect = CommonUiEffect.ShowToast("测试提示信息")
        assertEquals("测试提示信息", toastEffect.message)

        var actionTriggered = false
        val snackbarEffect = CommonUiEffect.ShowSnackbar("已删除账单", "撤销") { actionTriggered = true }
        assertEquals("已删除账单", snackbarEffect.message)
        assertEquals("撤销", snackbarEffect.actionLabel)
        snackbarEffect.onAction?.invoke()
        assertEquals(true, actionTriggered)

        val shareEffect = CommonUiEffect.ShareText("Title", "Content")
        assertEquals("Title", shareEffect.title)
        assertEquals("Content", shareEffect.content)

        val apmEffect = CommonUiEffect.OpenApmInspector
        assertNotNull(apmEffect)
    }

    @Test
    fun testNavTabs() {
        val tabs = NavTab.entries
        assertEquals(3, tabs.size)
        assertEquals(NavTab.TRANSACTIONS, tabs[0])
        assertEquals(NavTab.STATISTICS, tabs[1])
        assertEquals(NavTab.SETTINGS, tabs[2])
    }
}
