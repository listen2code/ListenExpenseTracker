package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.SurfaceCard

/**
 * Local Data Management, Backup, Budget, and Category Settings Card.
 * Exclusively provides file-based JSON export & import.
 */
@Composable
fun SettingsDataSection(
    monthlyBudget: Double,
    currencySymbol: String,
    onOpenBudgetDialog: () -> Unit,
    onOpenCategoryDialog: () -> Unit,
    onExportJson: () -> Unit,
    onImportJson: () -> Unit,
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
                    text = AppStrings.settings_data_manage.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Budget & Category Management Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                CommonButton(
                    text = "${AppStrings.monthly_budget.tr(lang)}: $currencySymbol${"%.0f".format(monthlyBudget)}",
                    onClick = onOpenBudgetDialog,
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.Savings, contentDescription = "Budget", modifier = Modifier.size(16.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.settings_category_manage.tr(lang),
                    onClick = onOpenCategoryDialog,
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.Category, contentDescription = "Categories", modifier = Modifier.size(16.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            // File-based JSON Export & Import Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                CommonButton(
                    text = AppStrings.export_json.tr(lang),
                    onClick = onExportJson,
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = "Export JSON", modifier = Modifier.size(16.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.import_json.tr(lang),
                    onClick = onImportJson,
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = "Import JSON", modifier = Modifier.size(16.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
