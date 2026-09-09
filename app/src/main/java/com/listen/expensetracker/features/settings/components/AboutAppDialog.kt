package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.graphics.drawable.toBitmap
import com.listen.arch.i18n.tr
import com.listen.expensetracker.R
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.theme.ListenTheme

/**
 * About Application Dialog displaying package version, design architecture, and app core highlights.
 */
@Composable
fun AboutAppDialog(
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    val context = LocalContext.current
    val (versionName, versionCode) = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val vName = pInfo.versionName ?: "0.0.1"
        val vCode = PackageInfoCompat.getLongVersionCode(pInfo)
        Pair(vName, vCode)
    } catch (_: Exception) {
        Pair("0.0.1", 1L)
    }

    val appIconBitmap = remember(context) {
        try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
                ?: context.packageManager.getApplicationIcon(context.packageName)
            drawable.toBitmap(128, 128).asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.ABOUT_APP.tr(lang),
        icon = {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        confirmButton = {
            CommonButton(
                text = AppStrings.BTN_DONE.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Primary
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // App Branding & Version Header Card
            AboutAppHeader(
                appIconBitmap = appIconBitmap,
                versionName = versionName,
                versionCode = versionCode
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = AppDimens.SpaceExtraSmall)
            )

            // Technical Specs & Key Highlights
            AboutInfoItem(
                icon = Icons.Default.Layers,
                label = AppStrings.APP_ARCHITECTURE_LABEL.tr(lang),
                value = "MVI + Clean Architecture + Room"
            )
            AboutInfoItem(
                icon = Icons.Default.Code,
                label = AppStrings.APP_CORE_SDK_LABEL.tr(lang),
                value = "ListenArch, ListenUiComponent"
            )
            AboutInfoItem(
                icon = Icons.Default.Security,
                label = AppStrings.APP_FEATURES_LABEL.tr(lang),
                value = AppStrings.APP_FEATURES_DESC.tr(lang)
            )
        }
    }
}

@Composable
private fun AboutAppHeader(
    appIconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    versionName: String,
    versionCode: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(AppDimens.SpaceMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
    ) {
        if (appIconBitmap != null) {
            Image(
                bitmap = appIconBitmap,
                contentDescription = "lExpense",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = "lExpense",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "lExpense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "v$versionName ($versionCode)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AboutInfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutAppDialogPreview() {
    ExpenseStrings.init()
    ListenTheme {
        AboutAppDialog(
            onDismiss = {},
            lang = "zh"
        )
    }
}
