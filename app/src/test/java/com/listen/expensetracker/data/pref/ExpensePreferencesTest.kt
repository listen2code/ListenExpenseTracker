package com.listen.expensetracker.data.pref

import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpensePreferencesTest {

    @Test
    fun testDefaultValues() {
        val prefs = ExpensePreferences()
        assertEquals("zh", prefs.language)
        assertEquals(ThemeMode.SYSTEM, prefs.themeMode)
        assertEquals(AccentColor.EMERALD, prefs.accentColor)
        assertEquals("￥", prefs.currencySymbol)
        assertEquals(5000.0, prefs.monthlyBudget, 0.001)
        assertTrue(prefs.autoBackupDrive)
        assertFalse(prefs.autoBackupWifiOnly)
        assertFalse(prefs.isDeveloperMode)
        assertFalse(prefs.hideBalance)
    }

    @Test
    fun testCopyAndModification() {
        val original = ExpensePreferences()
        val modified = original.copy(
            language = "en",
            themeMode = ThemeMode.DARK,
            currencySymbol = "$",
            monthlyBudget = 8000.0,
            hideBalance = true
        )

        assertEquals("en", modified.language)
        assertEquals(ThemeMode.DARK, modified.themeMode)
        assertEquals("$", modified.currencySymbol)
        assertEquals(8000.0, modified.monthlyBudget, 0.001)
        assertTrue(modified.hideBalance)
        assertEquals("zh", original.language)
    }
}
