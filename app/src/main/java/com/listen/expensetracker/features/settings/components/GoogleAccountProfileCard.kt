package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Clean profile banner displaying the connected Google user account with avatar and logout action.
 */
@Composable
fun GoogleAccountProfileCard(
    email: String,
    displayName: String?,
    avatarUrl: String?,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
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
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(avatarUrl)
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
                        text = (displayName?.firstOrNull() ?: email.firstOrNull() ?: 'G').uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = AppDimens.TextTitle
                    )
                }
            }

            Column {
                Text(
                    text = displayName ?: "Google User",
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = email,
                    fontSize = AppDimens.TextMicro,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        IconButton(onClick = onLogout) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Logout",
                tint = ExpenseRed
            )
        }
    }
}

/**
 * Unified Status Row showing sync progress, error, or timestamp.
 */
@Composable
fun SyncStatusIndicator(
    syncState: SyncState,
    sdf: SimpleDateFormat,
    lang: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statusText = when (syncState.status) {
            SyncStatus.SYNCING -> AppStrings.CLOUD_STATUS_SYNCING.tr(lang)
            SyncStatus.SUCCESS -> AppStrings.CLOUD_STATUS_SUCCESS.tr(lang)
            SyncStatus.ERROR -> AppStrings.CLOUD_STATUS_ERROR.tr(lang)
            SyncStatus.IDLE -> AppStrings.CLOUD_STATUS_IDLE.tr(lang)
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
                text = "${AppStrings.CLOUD_LAST_SYNC.tr(lang)}${sdf.format(Date(syncState.lastSyncTimestamp))}",
                fontSize = AppDimens.TextMicro,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

