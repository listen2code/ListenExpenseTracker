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
        assertEquals("", prefs.customAccounts)
        assertTrue(prefs.autoBackupDrive)
        assertFalse(prefs.autoBackupWifiOnly)
        assertFalse(prefs.isDeveloperMode)
        assertFalse(prefs.hideBalance)
    }

    @Test
    fun testCustomValuesAndCopy() {
        val prefs = ExpensePreferences(
            language = "en",
            themeMode = ThemeMode.DARK,
            accentColor = AccentColor.OCEAN_BLUE,
            currencySymbol = "$",
            monthlyBudget = 8000.0,
            customAccounts = "[]",
            autoBackupDrive = false,
            autoBackupWifiOnly = true,
            isDeveloperMode = true,
            hideBalance = true
        )
        assertEquals("en", prefs.language)
        assertEquals(ThemeMode.DARK, prefs.themeMode)
        assertEquals(AccentColor.OCEAN_BLUE, prefs.accentColor)
        assertEquals("$", prefs.currencySymbol)
        assertEquals(8000.0, prefs.monthlyBudget, 0.001)
        assertFalse(prefs.autoBackupDrive)
        assertTrue(prefs.autoBackupWifiOnly)
        assertTrue(prefs.isDeveloperMode)
        assertTrue(prefs.hideBalance)

        val updated = prefs.copy(language = "ja", currencySymbol = "¥")
        assertEquals("ja", updated.language)
        assertEquals("¥", updated.currencySymbol)
        assertEquals(ThemeMode.DARK, updated.themeMode)
    }
}
