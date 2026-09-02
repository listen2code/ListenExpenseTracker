package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.settings.viewmodel.SettingsDialog
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.expensetracker.features.budget.components.BudgetDialogMode
import com.listen.expensetracker.features.budget.components.CategoryBudgetModalDialog
import com.listen.expensetracker.features.transactions.components.AccountManageDialog
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard

/**
 * Dedicated Dialog Host for Settings Feature.
 * Encapsulates presentation and intent dispatching for budget, categories, currency, clear confirmation,
 * logout confirmation, and syncing HUD using standardized ListenUiComponent elements.
 */
@Composable
fun SettingsDialogHost(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit
) {
    val lang = state.language
    val sym = state.currencySymbol

    when (state.activeDialog) {
        is SettingsDialog.CurrencySelect -> {
            CurrencySelectDialog(
                currentSymbol = sym,
                onSymbolSelected = {
                    onIntent(SettingsIntent.ChangeCurrencySymbol(it))
                    onIntent(SettingsIntent.DismissDialog)
                },
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                lang = lang
            )
        }
        is SettingsDialog.CategoryManage -> {
            CategoryManageDialog(
                type = TransactionType.EXPENSE,
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                onCategoriesChanged = { /* Handled reactively */ },
                lang = lang
            )
        }
        is SettingsDialog.AccountManage -> {
            AccountManageDialog(
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                lang = lang
            )
        }
        is SettingsDialog.MonthlyBudget -> {
            CategoryBudgetModalDialog(
                allTransactions = emptyList(),
                monthlyBudget = state.monthlyBudget,
                categoryRatios = state.categoryBudgetRatios,
                currencySymbol = sym,
                lang = lang,
                hideAmount = false,
                initialMode = BudgetDialogMode.EDIT,
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                onSave = { newBudget, newRatios ->
                    onIntent(SettingsIntent.UpdateCategoryBudgets(newBudget, newRatios))
                    onIntent(SettingsIntent.DismissDialog)
                }
            )
        }
        is SettingsDialog.ClearConfirm -> {
            CommonDialog(
                onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
                title = AppStrings.CONFIRM_CLEAR_TITLE.tr(lang),
                confirmButton = {
                    CommonButton(
                        text = AppStrings.BTN_DELETE.tr(lang),
                        onClick = {
                            onIntent(SettingsIntent.ClearAllData)
                            onIntent(SettingsIntent.DismissDialog)
                        },
                        style = CommonButtonStyle.Danger
                    )
                },
                dismissButton = {
                    CommonButton(
                        text = AppStrings.BTN_CANCEL.tr(lang),
                        onClick = { onIntent(SettingsIntent.DismissDialog) },
                        style = CommonButtonStyle.Outlined
                    )
                }
            ) {
                CommonText(
                    text = AppStrings.CONFIRM_CLEAR_DESC.tr(lang),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is SettingsDialog.LogoutConfirm -> {
            CommonDialog(
                onDismissRequest = { onIntent(SettingsIntent.DismissDialog) },
                title = AppStrings.GOOGLE_LOGOUT_CONFIRM_TITLE.tr(lang),
                confirmButton = {
                    CommonButton(
                        text = AppStrings.GOOGLE_LOGOUT_BTN.tr(lang),
                        onClick = {
                            onIntent(SettingsIntent.UnlinkGoogleAccount)
                            onIntent(SettingsIntent.DismissDialog)
                        },
                        style = CommonButtonStyle.Danger
                    )
                },
                dismissButton = {
                    CommonButton(
                        text = AppStrings.BTN_CANCEL.tr(lang),
                        onClick = { onIntent(SettingsIntent.DismissDialog) },
                        style = CommonButtonStyle.Outlined
                    )
                }
            ) {
                CommonText(
                    text = AppStrings.GOOGLE_LOGOUT_CONFIRM_DESC.tr(lang),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        is SettingsDialog.AboutApp -> {
            AboutAppDialog(
                isCheckingUpdate = state.isCheckingUpdate,
                onCheckUpdates = { currentVersion ->
                    onIntent(SettingsIntent.CheckForUpdates(currentVersion))
                },
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                lang = lang
            )
        }
        is SettingsDialog.UpdateAvailable -> {
            UpdateAvailableDialog(
                releaseInfo = state.activeDialog.releaseInfo,
                onDismiss = { onIntent(SettingsIntent.DismissDialog) },
                lang = lang
            )
        }
        null -> Unit
    }

    // Global Syncing HUD Dialog
    if (state.isOperating) {
        Dialog(onDismissRequest = {}) {
            SurfaceCard(
                cornerRadius = AppDimens.CornerCard,
                contentPadding = AppDimens.SpaceLarge
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppDimens.SpaceMedium)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    CommonText(
                        text = AppStrings.CLOUD_STATUS_SYNCING.tr(lang),
                        fontSize = AppDimens.TextBody,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
