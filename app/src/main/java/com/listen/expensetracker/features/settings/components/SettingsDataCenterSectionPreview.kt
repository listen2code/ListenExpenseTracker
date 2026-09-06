package com.listen.expensetracker.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.arch.sync.SyncState
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 设置数据中心与云端备份模块预览 (SettingsDataCenterSectionPreview)。
 */
@Preview(showBackground = true)
@Composable
fun SettingsDataCenterSectionPreview() {
    ExpenseStrings.init()
    ListenTheme {
        SettingsDataCenterSection(
            googleAccountEmail = "user@example.com",
            googleDisplayName = "Listen User",
            autoBackupDrive = true,
            autoBackupWifiOnly = true,
            syncState = SyncState(),
            onLoginGoogle = {},
            onLogoutGoogle = {},
            onTriggerBackup = {},
            onTriggerRestore = {},
            onExportJson = {},
            onImportJson = {},
            lang = "zh"
        )
    }
}
