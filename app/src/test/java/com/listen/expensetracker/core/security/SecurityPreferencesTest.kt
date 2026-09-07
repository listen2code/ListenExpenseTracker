package com.listen.expensetracker.core.security

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SecurityPreferencesTest {

    private val context: Context = mock()
    private val prefs: SharedPreferences = mock()
    private val editor: SharedPreferences.Editor = mock()

    @Before
    fun setUp() {
        whenever(context.getSharedPreferences(eq("expense_security_prefs"), eq(Context.MODE_PRIVATE))).thenReturn(prefs)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)
    }

    @Test
    fun isBiometricEnabled_delegatesToSharedPreferences() {
        whenever(prefs.getBoolean(eq("biometric_lock_enabled"), eq(false))).thenReturn(true)
        assertTrue(SecurityPreferences.isBiometricEnabled(context))

        whenever(prefs.getBoolean(eq("biometric_lock_enabled"), eq(false))).thenReturn(false)
        assertFalse(SecurityPreferences.isBiometricEnabled(context))
    }

    @Test
    fun setBiometricEnabled_writesToEditorAndApplies() {
        SecurityPreferences.setBiometricEnabled(context, true)
        verify(editor).putBoolean("biometric_lock_enabled", true)
        verify(editor).apply()

        SecurityPreferences.setBiometricEnabled(context, false)
        verify(editor).putBoolean("biometric_lock_enabled", false)
    }

    @Test
    fun lockTimeoutSeconds_getsAndSetsCorrectly() {
        whenever(prefs.getInt(eq("lock_timeout_seconds"), eq(0))).thenReturn(30)
        assertEquals(30, SecurityPreferences.getLockTimeoutSeconds(context))

        SecurityPreferences.setLockTimeoutSeconds(context, 60)
        verify(editor).putInt("lock_timeout_seconds", 60)
        verify(editor).apply()
    }
}
