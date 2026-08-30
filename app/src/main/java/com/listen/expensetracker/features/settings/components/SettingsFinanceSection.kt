package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Tune
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
 * Finance Preferences & Rules Section Card.
 * Groups Monthly Budget, Category Management, and Asset Account Management.
 *
 * @param monthlyBudget Configured monthly budget amount
 * @param currencySymbol Active currency symbol
 * @param onOpenBudgetDialog Callback to open monthly budget dialog
 * @param onOpenCategoryDialog Callback to open category management dialog
 * @param onOpenAccountDialog Callback to open asset account management dialog
 * @param modifier Composable modifier (first optional parameter)
 * @param lang ISO language code
 */
@Composable
fun SettingsFinanceSection(
    monthlyBudget: Double,
    currencySymbol: String,
    onOpenBudgetDialog: () -> Unit,
    onOpenCategoryDialog: () -> Unit,
    onOpenAccountDialog: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "zh"
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
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Finance Rules",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                )
                Text(
                    text = AppStrings.SETTINGS_FINANCE_RULES.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Monthly Budget Button (Full Width)
            CommonButton(
                text = "${AppStrings.MONTHLY_BUDGET.tr(lang)}: $currencySymbol${"%.0f".format(monthlyBudget)}",
                onClick = onOpenBudgetDialog,
                style = CommonButtonStyle.Outlined,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Savings,
                        contentDescription = "Budget",
                        modifier = Modifier.size(18.dp)
                    )
                },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category & Account Management Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                CommonButton(
                    text = AppStrings.SETTINGS_CATEGORY_MANAGE.tr(lang),
                    onClick = onOpenCategoryDialog,
                    style = CommonButtonStyle.Outlined,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Categories",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.MANAGE_ACCOUNTS_TITLE.tr(lang),
                    onClick = onOpenAccountDialog,
                    style = CommonButtonStyle.Outlined,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = "Accounts",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
