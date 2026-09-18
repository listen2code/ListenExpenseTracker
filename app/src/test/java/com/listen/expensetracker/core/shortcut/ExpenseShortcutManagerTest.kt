package com.listen.expensetracker.core.shortcut

import android.content.Context
import com.listen.expensetracker.R
import com.listen.expensetracker.core.quickadd.QuickAddActivity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ExpenseShortcutManagerTest {

    @Test
    fun constants_areExpected() {
        assertEquals("shortcut_dynamic_food", ExpenseShortcutManager.SHORTCUT_ID_FOOD)
        assertEquals("shortcut_dynamic_transport", ExpenseShortcutManager.SHORTCUT_ID_TRANSPORT)
    }

    @Test
    fun buildShortcutForCategory_validCategories_buildsProperShortcuts() {
        val mockContext: Context = mock()
        whenever(mockContext.getString(R.string.shortcut_category_food)).thenReturn("记餐饮")
        whenever(mockContext.getString(R.string.shortcut_category_transport)).thenReturn("记交通")
        whenever(mockContext.packageName).thenReturn("com.listen.expensetracker")

        val foodShortcut = ExpenseShortcutManager.buildShortcutForCategory(mockContext, "c_food")
        assertNotNull(foodShortcut)
        assertEquals("shortcut_dynamic_c_food", foodShortcut?.id)
        assertEquals("记餐饮", foodShortcut?.shortLabel)

        val transportShortcut = ExpenseShortcutManager.buildShortcutForCategory(mockContext, "c_transport")
        assertNotNull(transportShortcut)
        assertEquals("shortcut_dynamic_c_transport", transportShortcut?.id)
        assertEquals("记交通", transportShortcut?.shortLabel)
    }

    @Test
    fun buildShortcutForCategory_unknownCategory_returnsNull() {
        val mockContext: Context = mock()
        val unknownShortcut = ExpenseShortcutManager.buildShortcutForCategory(mockContext, "unknown_category_999")
        assertNull(unknownShortcut)
    }

    @Test
    fun quickAddActivity_createIntent_buildsProperIntent() {
        val mockContext: Context = mock()
        val intent = QuickAddActivity.createIntent(mockContext, "c_food", TransactionType.EXPENSE)
        assertNotNull("Generated intent should not be null", intent)
        assertEquals("extra_quick_add_category", ListenExpenseAppWidgetProvider.EXTRA_QUICK_ADD_CATEGORY)
        assertEquals("extra_quick_add_type", ListenExpenseAppWidgetProvider.EXTRA_QUICK_ADD_TYPE)
    }
}
