package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.SurfaceCard

/**
 * Local Data Management, Backup, Budget, and Category Settings Card.
 */
@Composable
fun SettingsDataSection(
    monthlyBudget: Double,
    currencySymbol: String,
    onOpenBudgetDialog: () -> Unit,
    onOpenCategoryDialog: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenImportSheet: () -> Unit,
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
                    imageVector = Icons.Default.Storage,
                    contentDescription = "Data",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                )
                Text(
                    text = StringsRes.get("settings_data_manage", lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Budget & Category Management Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                OutlinedButton(
                    onClick = onOpenBudgetDialog,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Savings, contentDescription = "Budget", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text("${StringsRes.get("monthly_budget", lang)}: $currencySymbol${"%.0f".format(monthlyBudget)}", fontSize = AppDimens.TextSmall, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onOpenCategoryDialog,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Category, contentDescription = "Categories", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(StringsRes.get("settings_category_manage", lang), fontSize = AppDimens.TextSmall, maxLines = 1)
                }
            }

            // Export & Import Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                OutlinedButton(
                    onClick = onExportJson,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export JSON", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(StringsRes.get("export_json", lang), fontSize = AppDimens.TextSmall)
                }

                OutlinedButton(
                    onClick = onExportCsv,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(StringsRes.get("export_csv", lang), fontSize = AppDimens.TextSmall)
                }

                OutlinedButton(
                    onClick = onOpenImportSheet,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = "Import JSON", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(StringsRes.get("import_json", lang), fontSize = AppDimens.TextSmall)
                }
            }
        }
    }
}
