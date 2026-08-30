package com.listen.expensetracker.features.settings.components

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens

/**
 * Settings Bottom Version Footer Item.
 * Displays application version and triggers Developer Mode upon rapid repeated taps (5x).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsVersionFooter(
    isDeveloperMode: Boolean,
    onToggleDeveloperMode: (Boolean) -> Unit,
    onOpenAboutDialog: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (versionName, versionCode) = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val vName = pInfo.versionName ?: "0.0.1"
            val vCode = PackageInfoCompat.getLongVersionCode(pInfo)
            Pair(vName, vCode)
        } catch (_: Exception) {
            Pair("0.0.1", 1L)
        }
    }

    var clickCount by remember { mutableIntStateOf(0) }
    var lastClickTime by remember { mutableLongStateOf(0L) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppDimens.SpaceExtraSmall, bottom = 24.dp)
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
            .combinedClickable(
                onClick = {
                    val now = System.currentTimeMillis()
                    if (now - lastClickTime > 1500L) {
                        clickCount = 1
                    } else {
                        clickCount++
                    }
                    lastClickTime = now

                    if (clickCount >= 5) {
                        clickCount = 0
                        onToggleDeveloperMode(!isDeveloperMode)
                    } else if (clickCount in 3..4 && !isDeveloperMode) {
                        val remaining = 5 - clickCount
                        val stepMsg = String.format(AppStrings.DEVELOPER_MODE_STEPS.tr(lang), remaining)
                        Toast.makeText(context, stepMsg, Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {
                    onOpenAboutDialog()
                }
            )
            .padding(vertical = AppDimens.SpaceLarge, horizontal = AppDimens.SpaceLarge)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
        ) {
            Text(
                text = "v$versionName ($versionCode)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
