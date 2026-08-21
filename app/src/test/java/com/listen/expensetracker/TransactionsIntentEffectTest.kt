package com.listen.expensetracker

import com.listen.arch.data.db.TransactionEntity
import com.listen.expensetracker.ui.state.TransactionSortOrder
import com.listen.expensetracker.ui.state.TransactionsEffect
import com.listen.expensetracker.ui.state.TransactionsIntent
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TransactionsIntentEffectTest {

    @Test
    fun testIntentInstantiations() {
        val addIntent = TransactionsIntent.AddTransaction(
            type = "EXPENSE",
            categoryId = "c_food",
            categoryName = "餐饮",
            categoryIcon = "c_food",
            categoryColorHex = "#EF4444",
            amount = 55.0,
            note = "午餐测试",
            accountType = "WECHAT",
            timestamp = 1723900000000L
        )
        assertEquals(55.0, addIntent.amount, 0.001)
        assertEquals("WECHAT", addIntent.accountType)

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

        val linkGoogleIntent = TransactionsIntent.LinkGoogleAccount("user@gmail.com", "Test User", "https://avatar.png")
        assertEquals("user@gmail.com", linkGoogleIntent.email)
        assertEquals("Test User", linkGoogleIntent.displayName)

        val unlinkGoogleIntent = TransactionsIntent.UnlinkGoogleAccount
        assertNotNull(unlinkGoogleIntent)

        val searchIntent = TransactionsIntent.SearchQueryChange("餐饮")
        assertEquals("餐饮", searchIntent.query)

        val sortIntent = TransactionsIntent.ChangeSortOrder(TransactionSortOrder.AMOUNT_DESC)
        assertEquals(TransactionSortOrder.AMOUNT_DESC, sortIntent.order)

        val langIntent = TransactionsIntent.ChangeLanguage("ja")
        assertEquals("ja", langIntent.langCode)

        val themeIntent = TransactionsIntent.ChangeThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, themeIntent.mode)

        val accentIntent = TransactionsIntent.ChangeAccentColor(AccentColor.ROSE)
        assertEquals(AccentColor.ROSE, accentIntent.accent)
    }

    @Test
    fun testEffects() {
        val toastEffect = TransactionsEffect.ShowToast("测试提示信息")
        assertEquals("测试提示信息", toastEffect.message)

        val tx = TransactionEntity(id = "tx-undo", type = "EXPENSE", categoryId = "c_food", categoryName = "餐饮", categoryIcon = "c_food", categoryColorHex = "#EF4444", amount = 20.0, note = "咖啡", accountType = "CASH")
        val undoEffect = TransactionsEffect.ShowUndoSnackbar("已删除账单", tx)
        assertEquals("已删除账单", undoEffect.message)
        assertEquals("tx-undo", undoEffect.transaction.id)

        val addedSuccess = TransactionsEffect.TransactionAddedSuccess
        assertNotNull(addedSuccess)
    }
}
