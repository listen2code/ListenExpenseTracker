package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.listen.arch.i18n.tr
import com.listen.arch.sync.SyncState
import com.listen.arch.sync.SyncStatus
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults

/**
 * Cloud Sync and Google Account Settings Card.
 * Displays user profile avatar, display name, email, auto-backup toggles, and Google Drive sync indicators.
 */
@Composable
fun SettingsCloudSection(
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
    lang: String,
    modifier: Modifier = Modifier,
    isOperating: Boolean = false
) {
    val isLoggedIn = !googleAccountEmail.isNullOrBlank()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            // Header with theme-adaptive primary tint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Cloud",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                )
                Text(
                    text = AppStrings.settings_cloud.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isLoggedIn) {
                // Logged-in Google Account Information Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.CornerButton))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = AppDimens.SpaceLarge, vertical = AppDimens.SpaceStandard),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        // Google Avatar (Network profile picture or initial letter fallback)
                        if (!googleAvatarUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(googleAvatarUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Google Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (googleDisplayName?.firstOrNull() ?: googleAccountEmail.firstOrNull() ?: 'G').uppercase(),
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = AppDimens.TextTitle
                                )
                            }
                        }

                        Column {
                            Text(
                                text = googleDisplayName ?: "Google User",
                                fontSize = AppDimens.TextSubtitle,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = googleAccountEmail,
                                fontSize = AppDimens.TextMicro,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    // Logout Icon
                    IconButton(onClick = onLogoutGoogle) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = ExpenseRed
                        )
                    }
                }

                // Auto-Backup to Google Drive Switch Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = AppDimens.SpaceMedium)) {
                        Text(
                            text = AppStrings.auto_backup_drive_title.tr(lang),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = AppStrings.auto_backup_drive_desc.tr(lang),
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
                            text = AppStrings.auto_backup_wifi_only_title.tr(lang),
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (syncState.status) {
                        SyncStatus.SYNCING -> AppStrings.cloud_status_syncing.tr(lang)
                        SyncStatus.SUCCESS -> AppStrings.cloud_status_success.tr(lang)
                        SyncStatus.ERROR -> AppStrings.cloud_status_error.tr(lang)
                        SyncStatus.IDLE -> AppStrings.cloud_status_idle.tr(lang)
                    }
                    val statusColor = when (syncState.status) {
                        SyncStatus.SYNCING -> MaterialTheme.colorScheme.primary
                        SyncStatus.SUCCESS -> IncomeGreen
                        SyncStatus.ERROR -> ExpenseRed
                        SyncStatus.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                    ) {
                        if (syncState.status == SyncStatus.SYNCING) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                        }
                        Text(
                            text = statusText,
                            fontSize = AppDimens.TextSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                    }

                    if (syncState.lastSyncTimestamp > 0) {
                        Text(
                            text = "${AppStrings.cloud_last_sync.tr(lang)}${sdf.format(Date(syncState.lastSyncTimestamp))}",
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cloud Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
                ) {
                    val isBusy = isOperating || syncState.status == SyncStatus.SYNCING
                    CommonButton(
                        text = if (isBusy) AppStrings.cloud_status_syncing.tr(lang) else AppStrings.cloud_backup_btn.tr(lang),
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
                        text = AppStrings.cloud_restore_btn.tr(lang),
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
                    text = AppStrings.google_login_required.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                CommonButton(
                    text = AppStrings.google_login_btn.tr(lang),
                    onClick = onLoginGoogle,
                    style = CommonButtonStyle.Primary,
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Login", modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
