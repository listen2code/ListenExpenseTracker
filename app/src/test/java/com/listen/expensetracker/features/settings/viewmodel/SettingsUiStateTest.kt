package com.listen.expensetracker.features.settings.viewmodel

import android.net.Uri
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito.mock

class SettingsUiStateTest {
    @Test
    fun testDefaultValues() {
        val state = SettingsUiState()
        assertEquals("zh", state.language)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(AccentColor.EMERALD, state.accentColor)
        assertEquals("￥", state.currencySymbol)
        assertEquals(5000.0, state.monthlyBudget, 0.0)
        assertNull(state.googleAccountEmail)
        assertFalse(state.isLoggedIn)
        assertEquals(0L, state.lastSyncTimestamp)
        assertNull(state.activeDialog)
        assertFalse(state.isOperating)
    }

    @Test
    fun testIntents() {
        val langIntent = SettingsIntent.ChangeLanguage("en")
        assertEquals("en", langIntent.langCode)

        val themeIntent = SettingsIntent.ChangeThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, themeIntent.mode)

        val accentIntent = SettingsIntent.ChangeAccentColor(AccentColor.OCEAN_BLUE)
        assertEquals(AccentColor.OCEAN_BLUE, accentIntent.accent)

        val linkIntent = SettingsIntent.LinkGoogleAccount("test@test.com", "Test User", "http://avatar")
        assertEquals("test@test.com", linkIntent.email)
        assertEquals("Test User", linkIntent.displayName)
        assertEquals("http://avatar", linkIntent.avatarUrl)

        val mockUri = mock(Uri::class.java)
        val exportIntent = SettingsIntent.ExportJsonToFile(mockUri)
        assertEquals(mockUri, exportIntent.uri)
        
        val dialogIntent = SettingsIntent.OpenDialog(SettingsDialog.MonthlyBudget)
        assertEquals(SettingsDialog.MonthlyBudget, dialogIntent.dialog)
    }
}
