package com.listen.expensetracker.features.settings.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.update.ReleaseInfo
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText

/**
 * Modern dialog presented when a newer GitHub Release is detected.
 */
@Composable
fun UpdateAvailableDialog(
    releaseInfo: ReleaseInfo,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    CommonDialog(
        onDismissRequest = onDismiss,
        title = String.format(AppStrings.update_found_title.tr(lang), releaseInfo.tagName),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = "New Version",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Release Title
            if (releaseInfo.title.isNotBlank()) {
                Text(
                    text = releaseInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Changelog Content (Scrollable if lengthy)
            if (releaseInfo.changelog.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceExtraSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDimens.CornerCard))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(AppDimens.SpaceMedium)
                ) {
                    CommonText(
                        text = AppStrings.changelog_title.tr(lang),
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = releaseInfo.changelog.trim(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Download Update / Open Release
                val targetDownloadUrl = releaseInfo.apkDownloadUrl ?: releaseInfo.htmlUrl
                CommonButton(
                    text = AppStrings.update_now_btn.tr(lang),
                    onClick = {
                        openUrlInBrowser(context, targetDownloadUrl)
                        onDismiss()
                    },
                    style = CommonButtonStyle.Primary,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(AppDimens.IconSizeMedium)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. View on GitHub button (if direct apk download exists)
                if (releaseInfo.apkDownloadUrl != null) {
                    CommonButton(
                        text = AppStrings.view_on_github_btn.tr(lang),
                        onClick = {
                            openUrlInBrowser(context, releaseInfo.htmlUrl)
                            onDismiss()
                        },
                        style = CommonButtonStyle.Secondary,
                        icon = {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "GitHub",
                                modifier = Modifier.size(AppDimens.IconSizeMedium)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 3. Cancel button
                CommonButton(
                    text = AppStrings.btn_cancel.tr(lang),
                    onClick = onDismiss,
                    style = CommonButtonStyle.Secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun openUrlInBrowser(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback or ignore
    }
}
