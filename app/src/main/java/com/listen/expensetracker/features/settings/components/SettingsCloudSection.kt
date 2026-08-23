package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.arch.sync.SyncState
import com.listen.arch.sync.SyncStatus
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Cloud Sync and Google Account Settings Card.
 */
@Composable
fun SettingsCloudSection(
    googleAccountEmail: String?,
    googleDisplayName: String?,
    syncState: SyncState,
    onLoginGoogle: () -> Unit,
    onLogoutGoogle: () -> Unit,
    onTriggerBackup: () -> Unit,
    onTriggerRestore: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val isLoggedIn = !googleAccountEmail.isNullOrBlank()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = StringsRes.get("settings_cloud", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle
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
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (googleDisplayName?.firstOrNull() ?: googleAccountEmail?.firstOrNull() ?: 'G').uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = AppDimens.TextTitle
                            )
                        }
                        Column {
                            Text(
                                text = googleDisplayName ?: "Google User",
                                fontSize = AppDimens.TextSubtitle,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = googleAccountEmail ?: "",
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

                // Sync Status Indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val statusText = when (syncState.status) {
                        SyncStatus.SYNCING -> StringsRes.get("cloud_status_syncing", lang)
                        SyncStatus.SUCCESS -> StringsRes.get("cloud_status_success", lang)
                        SyncStatus.ERROR -> StringsRes.get("cloud_status_error", lang)
                        SyncStatus.IDLE -> StringsRes.get("cloud_status_idle", lang)
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
                            text = "${StringsRes.get("cloud_last_sync", lang)}${sdf.format(Date(syncState.lastSyncTimestamp))}",
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
                    Button(
                        onClick = onTriggerBackup,
                        enabled = syncState.status != SyncStatus.SYNCING,
                        shape = RoundedCornerShape(AppDimens.CornerButton),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Backup", modifier = Modifier.size(AppDimens.IconSizeMedium))
                        Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                        Text(StringsRes.get("cloud_backup_btn", lang), fontSize = AppDimens.TextBody)
                    }

                    OutlinedButton(
                        onClick = onTriggerRestore,
                        enabled = syncState.status != SyncStatus.SYNCING,
                        shape = RoundedCornerShape(AppDimens.CornerButton),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Restore", modifier = Modifier.size(AppDimens.IconSizeMedium))
                        Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                        Text(StringsRes.get("cloud_restore_btn", lang), fontSize = AppDimens.TextBody)
                    }
                }
            } else {
                // Not Logged In State
                Text(
                    text = StringsRes.get("google_login_required", lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onLoginGoogle,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4285F4)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccountCircle, contentDescription = "Login", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(StringsRes.get("google_login_btn", lang), color = Color.White, fontWeight = FontWeight.Bold, fontSize = AppDimens.TextBody)
                }
            }
        }
    }
}
