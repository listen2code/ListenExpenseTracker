package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonSwitchRow
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ListenTheme

/**
 * APM Observability, Testing Seeds, and About App Section Card.
 */
@Composable
fun SettingsApmSection(
    apmFloatingWindowEnabled: Boolean,
    onToggleApmFloatingWindow: (Boolean) -> Unit,
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
                    text = AppStrings.SETTINGS_SYSTEM_OPS.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // APM Floating Window Switch (Rule 22: 控制全局可拖拽调试悬浮球)
            CommonSwitchRow(
                title = AppStrings.APM_FLOATING_WINDOW_TITLE.tr(lang),
                checked = apmFloatingWindowEnabled,
                onCheckedChange = onToggleApmFloatingWindow,
                subtitle = AppStrings.APM_FLOATING_WINDOW_DESC.tr(lang),
                contentPadding = 0.dp
            )

            // Seed & Clear Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                val seedBtnText = if (targetMonthTitle.isNotBlank()) {
                    "${AppStrings.SEED_DATA_BTN.tr(lang)} ($targetMonthTitle)"
                } else {
                    AppStrings.SEED_DATA_BTN.tr(lang)
                }
                CommonButton(
                    text = seedBtnText,
                    onClick = onSeedDemoData,
                    style = CommonButtonStyle.Outlined,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.CLEAR_ALL.tr(lang),
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
@Preview(showBackground = true)
@Composable
fun SettingsApmSectionPreview() {
    ExpenseStrings.init()
    ListenTheme {
        SettingsApmSection(
            apmFloatingWindowEnabled = true,
            onToggleApmFloatingWindow = {},
            onSeedDemoData = {},
            onConfirmClearAll = {},
            lang = "zh"
        )
    }
}
