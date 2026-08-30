package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.arch.sync.SyncState
import com.listen.arch.sync.SyncStatus
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Unified Data Center Section Card.
 * Combines Google Drive Cloud Sync & Auto-Backup with Local File-Based JSON Export & Import.
 */
@Composable
fun SettingsDataCenterSection(
    modifier: Modifier = Modifier,
    googleAccountEmail: String?,
    googleDisplayName: String?,
    googleAvatarUrl: String? = null,
    autoBackupDrive: Boolean = true,
    autoBackupWifiOnly: Boolean = false,
    syncState: SyncState,
    onLoginGoogle: () -> Unit,
    onLogoutGoogle: () -> Unit,
    onToggleAutoBackupDrive: (Boolean) -> Unit = {},
    onToggleAutoBackupWifiOnly: (Boolean) -> Unit = {},
    onTriggerBackup: () -> Unit,
    onTriggerRestore: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
    isOperating: Boolean = false,
    lang: String = "zh"
) {
    val isLoggedIn = !googleAccountEmail.isNullOrBlank()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Data Center",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                )
                Text(
                    text = AppStrings.SETTINGS_DATA_CENTER.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Part A: Google Drive Cloud Sync
            if (isLoggedIn) {
                GoogleAccountProfileCard(
                    email = googleAccountEmail,
                    displayName = googleDisplayName,
                    avatarUrl = googleAvatarUrl,
                    onLogout = onLogoutGoogle
                )

                // Auto-Backup to Google Drive Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = AppDimens.SpaceMedium)) {
                        Text(
                            text = AppStrings.AUTO_BACKUP_DRIVE_TITLE.tr(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = AppStrings.AUTO_BACKUP_DRIVE_DESC.tr(lang),
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoBackupDrive,
                        onCheckedChange = onToggleAutoBackupDrive,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Wi-Fi Only Switch Row (Conditional on Auto Backup enabled)
                if (autoBackupDrive) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.AUTO_BACKUP_WIFI_ONLY_TITLE.tr(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Switch(
                            checked = autoBackupWifiOnly,
                            onCheckedChange = onToggleAutoBackupWifiOnly,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                // Sync Status Indicators
                SyncStatusIndicator(
                    syncState = syncState,
                    sdf = sdf,
                    lang = lang
                )

                // Cloud Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
                ) {
                    val isBusy = isOperating || syncState.status == SyncStatus.SYNCING
                    CommonButton(
                        text = if (isBusy) AppStrings.CLOUD_STATUS_SYNCING.tr(lang) else AppStrings.CLOUD_BACKUP_BTN.tr(lang),
                        onClick = onTriggerBackup,
                        enabled = !isBusy,
                        style = CommonButtonStyle.Primary,
                        icon = {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Backup", modifier = Modifier.size(16.dp))
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    )

                    CommonButton(
                        text = AppStrings.CLOUD_RESTORE_BTN.tr(lang),
                        onClick = onTriggerRestore,
                        enabled = !isBusy,
                        style = CommonButtonStyle.Outlined,
                        icon = { Icon(Icons.Default.CloudDownload, contentDescription = "Restore", modifier = Modifier.size(16.dp)) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Not Logged In State
                CommonText(
                    text = AppStrings.GOOGLE_LOGIN_REQUIRED.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CommonButton(
                    text = AppStrings.GOOGLE_LOGIN_BTN.tr(lang),
                    onClick = onLoginGoogle,
                    style = CommonButtonStyle.Primary,
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Login", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Subtle Divider separating Cloud and Local File operations
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = 2.dp)
            )

            // Part B: Local File-based JSON Export & Import
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                CommonButton(
                    text = AppStrings.EXPORT_JSON.tr(lang),
                    onClick = onExportJson,
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = "Export JSON", modifier = Modifier.size(16.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.IMPORT_JSON.tr(lang),
                    onClick = onImportJson,
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = "Import JSON", modifier = Modifier.size(16.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
