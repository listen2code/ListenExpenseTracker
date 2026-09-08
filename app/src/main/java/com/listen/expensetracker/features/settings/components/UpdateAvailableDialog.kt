package com.listen.expensetracker.features.settings.components

import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
        title = String.format(AppStrings.UPDATE_FOUND_TITLE.tr(lang), releaseInfo.tagName),
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
        },
        confirmButton = {
            CommonButton(
                text = AppStrings.BTN_UPDATE.tr(lang),
                onClick = {
                    openGooglePlay(context)
                    onDismiss()
                },
                style = CommonButtonStyle.Primary
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.BTN_CANCEL.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Outlined
            )
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
                        text = AppStrings.CHANGELOG_TITLE.tr(lang),
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
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
        }
    }
}
@Preview(showBackground = true)
@Composable
fun UpdateAvailableDialogPreview() {
    ExpenseStrings.init()
    val sampleRelease = ReleaseInfo(
        tagName = "v0.0.30",
        title = "Version 0.0.30",
        changelog = "New UI features and bug fixes.",
        htmlUrl = "https://github.com/listen2code/ListenExpenseTracker",
        apkDownloadUrl = null
    )
    ListenTheme {
        UpdateAvailableDialog(
            releaseInfo = sampleRelease,
            onDismiss = {},
            lang = "zh"
        )
    }
}
