package com.listen.expensetracker.features.settings.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.SurfaceCard

/**
 * APM Observability, Testing Seeds, and About App Section Card.
 */
@Composable
fun SettingsApmSection(
    onOpenApmInspector: () -> Unit,
    onSeedDemoData: () -> Unit,
    onConfirmClearAll: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier,
    targetMonthTitle: String = ""
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
                    text = AppStrings.settings_system_ops.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // APM Inspector Button
            CommonButton(
                text = AppStrings.apm_inspector.tr(lang),
                onClick = onOpenApmInspector,
                style = CommonButtonStyle.Outlined,
                icon = { Icon(Icons.Default.BugReport, contentDescription = "APM", modifier = Modifier.size(AppDimens.IconSizeMedium)) },
                modifier = Modifier.fillMaxWidth()
            )

            // Seed & Clear Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                val seedBtnText = if (targetMonthTitle.isNotBlank()) {
                    "${AppStrings.seed_data_btn.tr(lang)} ($targetMonthTitle)"
                } else {
                    AppStrings.seed_data_btn.tr(lang)
                }
                CommonButton(
                    text = seedBtnText,
                    onClick = onSeedDemoData,
                    style = CommonButtonStyle.Outlined,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.clear_all.tr(lang),
                    onClick = onConfirmClearAll,
                    style = CommonButtonStyle.Danger,
                    icon = { Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", modifier = Modifier.size(AppDimens.IconSizeMedium)) },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

        }
    }
}
