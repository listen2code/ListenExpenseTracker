package com.listen.expensetracker.features.settings.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.listen.expensetracker.R

/**
 * About Application Dialog displaying dynamic package version info and update triggers.
 */
@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit,
    lang: String = "zh",
    isCheckingUpdate: Boolean = false,
    onCheckUpdates: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val (versionName, versionCode) = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val vName = pInfo.versionName ?: "0.0.1"
        val vCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
        Pair(vName, vCode)
    } catch (_: Exception) {
        Pair("0.0.1", 1L)
    }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = "lExpense",
        icon = {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(R.mipmap.ic_launcher)
                    .crossfade(true)
                    .build(),
                contentDescription = "lExpense",
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            CommonText(
                text = "${AppStrings.app_version_label.tr(lang)}: v$versionName (Build $versionCode)",
                fontSize = AppDimens.TextBody,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            CommonText(
                text = "${AppStrings.app_architecture_label.tr(lang)}: MVI + Clean Architecture + Room + Google Drive Sync",
                fontSize = AppDimens.TextSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CommonText(
                text = "${AppStrings.app_core_sdk_label.tr(lang)}: ListenArch, ListenUiComponent",
                fontSize = AppDimens.TextSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CommonText(
                text = AppStrings.app_features_desc.tr(lang),
                fontSize = AppDimens.TextSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))

            // Check for Updates on GitHub Button
            CommonButton(
                text = if (isCheckingUpdate) AppStrings.checking_updates.tr(lang) else AppStrings.check_update.tr(lang),
                onClick = {
                    if (onCheckUpdates != null) {
                        onCheckUpdates(versionName)
                    } else {
                        openGooglePlay(context)
                        onDismiss()
                    }
                },
                enabled = !isCheckingUpdate,
                style = CommonButtonStyle.Primary,
                icon = {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(AppDimens.IconSizeSmall),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Update",
                            modifier = Modifier.size(AppDimens.IconSizeMedium)
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Google Play fallback button
            CommonButton(
                text = "Google Play",
                onClick = {
                    openGooglePlay(context)
                    onDismiss()
                },
                style = CommonButtonStyle.Secondary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Shop,
                        contentDescription = "Google Play",
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Launches the Google Play Store page for this application or falls back to web browser.
 */
fun openGooglePlay(context: Context) {
    val packageName = context.packageName
    try {
        val intent = Intent(Intent.ACTION_VIEW, "market://details?id=$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        val webIntent = Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$packageName".toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(webIntent)
    }
}
