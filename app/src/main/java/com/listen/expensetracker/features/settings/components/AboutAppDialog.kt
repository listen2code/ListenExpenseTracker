package com.listen.expensetracker.features.settings.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import androidx.core.net.toUri

/**
 * About Application Dialog displaying dynamic package version info and Google Play update trigger.
 */
@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    val context = LocalContext.current
    val (versionName, versionCode) = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val vName = pInfo.versionName ?: "0.0.4"
        val vCode = androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(pInfo)
        Pair(vName, vCode)
    } catch (_: Exception) {
        Pair("0.0.4", 4L)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = "lExpense",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text("lExpense", fontWeight = FontWeight.Bold, fontSize = AppDimens.TextHeader)
                    Text("ListenExpenseTracker", fontSize = AppDimens.TextMicro, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
                Text(
                    text = "${AppStrings.app_version_label.tr(lang)}: v$versionName (Build $versionCode)",
                    fontSize = AppDimens.TextBody,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${AppStrings.app_architecture_label.tr(lang)}: MVI + Clean Architecture + Room + Google Drive Sync",
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${AppStrings.app_core_sdk_label.tr(lang)}: ListenArch, ListenUiComponent",
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = AppStrings.app_features_desc.tr(lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))

                // Check for Updates on Google Play Button
                Button(
                    onClick = {
                        openGooglePlay(context)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = "Update", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(AppStrings.check_update.tr(lang), fontSize = AppDimens.TextBody)
                }
            }
        },
        confirmButton = {}
    )
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
