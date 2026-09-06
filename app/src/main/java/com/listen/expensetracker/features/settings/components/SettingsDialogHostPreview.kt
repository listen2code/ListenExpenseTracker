package com.listen.expensetracker.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.uicomponent.theme.ListenTheme

/**
 * 设置全局弹窗宿主预览 (SettingsDialogHostPreview)。
 */
@Preview(showBackground = true)
@Composable
fun SettingsDialogHostPreview() {
    ExpenseStrings.init()
    val state = SettingsUiState()
    ListenTheme {
        SettingsDialogHost(
            state = state,
            onIntent = {}
        )
    }
}
