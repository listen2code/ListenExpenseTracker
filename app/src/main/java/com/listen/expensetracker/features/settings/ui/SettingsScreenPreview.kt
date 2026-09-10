package com.listen.expensetracker.features.settings.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.uicomponent.theme.ListenTheme

/**
 * 设置主画面预览 (SettingsScreenPreview)。
 */
@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ExpenseStrings.init()
    val state = SettingsUiState(
        language = "zh",
        currencySymbol = "¥",
        monthlyBudget = 5000.0,
        googleAccountEmail = "user@example.com",
        googleDisplayName = "Listen User",
        isLoggedIn = true,
        isDeveloperMode = true
    )
    ListenTheme {
        SettingsScreen(
            state = state,
            onIntent = {}
        )
    }
}
