package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed

/**
 * APM Observability, Testing Seeds, and About App Section Card.
 */
@Composable
fun SettingsApmSection(
    onOpenApmInspector: () -> Unit,
    onSeedDemoData: () -> Unit,
    onConfirmClearAll: () -> Unit,
    onOpenAboutDialog: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
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
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Ops",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                )
                Text(
                    text = StringsRes.get("settings_system_ops", lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // APM Inspector Button
            OutlinedButton(
                onClick = onOpenApmInspector,
                shape = RoundedCornerShape(AppDimens.CornerButton),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.BugReport, contentDescription = "APM", modifier = Modifier.size(AppDimens.IconSizeMedium))
                Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                Text(StringsRes.get("apm_inspector", lang), fontSize = AppDimens.TextBody)
            }

            // Seed & Clear Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                OutlinedButton(
                    onClick = onSeedDemoData,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(StringsRes.get("seed_data_btn", lang), fontSize = AppDimens.TextSmall)
                }

                OutlinedButton(
                    onClick = onConfirmClearAll,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(StringsRes.get("clear_all", lang), fontSize = AppDimens.TextSmall)
                }
            }

            // About App Button
            OutlinedButton(
                onClick = onOpenAboutDialog,
                shape = RoundedCornerShape(AppDimens.CornerButton),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Info, contentDescription = "About", modifier = Modifier.size(AppDimens.IconSizeMedium))
                Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                Text(StringsRes.get("about_app", lang), fontSize = AppDimens.TextBody)
            }
        }
    }
}
