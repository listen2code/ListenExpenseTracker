package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText

/**
 * Modern Account Management Dialog.
 * Categorizes built-in vs custom payment accounts and orchestrates create, edit, and delete flows.
 */
@Composable
fun AccountManageDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onAccountChanged: (String) -> Unit = {},
    lang: String = "zh"
) {
    var refreshKey by remember { mutableIntStateOf(0) }
    val allAccounts = remember(refreshKey) { AccountRepository.getAllAccounts() }
    val systemAccounts = remember(allAccounts) { allAccounts.filter { it.isSystem } }
    val customAccounts = remember(allAccounts) { allAccounts.filter { !it.isSystem } }

    var accountToEdit by remember { mutableStateOf<AccountTypeItem?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountTypeItem?>(null) }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.manage_accounts_title.tr(lang),
        modifier = modifier,
        icon = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        confirmButton = {
            CommonButton(
                text = AppStrings.add_account.tr(lang),
                onClick = { showAddDialog = true },
                style = CommonButtonStyle.Primary,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                }
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.common_done.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Text
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            // 1. Built-in Accounts Section
            item(key = "header_system") {
                CommonText(
                    text = AppStrings.system_accounts_section.tr(lang),
                    fontSize = AppDimens.TextCaption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(systemAccounts, key = { it.key }) { acct ->
                AccountCardItem(
                    acct = acct,
                    onEdit = {},
                    onDelete = {},
                    lang = lang
                )
            }

            // 2. Custom Accounts Section
            item(key = "header_custom") {
                Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))
                CommonText(
                    text = AppStrings.custom_accounts_section.tr(lang),
                    fontSize = AppDimens.TextCaption,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (customAccounts.isEmpty()) {
                item(key = "custom_empty") {
                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerCard),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(AppDimens.SpaceMedium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(18.dp)
                            )
                            CommonText(
                                text = AppStrings.custom_accounts_empty.tr(lang),
                                fontSize = AppDimens.TextSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(customAccounts, key = { it.key }) { acct ->
                    AccountCardItem(
                        acct = acct,
                        onEdit = { accountToEdit = acct },
                        onDelete = { accountToDelete = acct },
                        lang = lang
                    )
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AccountEditDialog(
            initialName = "",
            title = AppStrings.add_account.tr(lang),
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                val created = AccountRepository.addAccount(name)
                refreshKey++
                onAccountChanged(created.key)
                showAddDialog = false
            },
            lang = lang
        )
    }

    // Edit Account Dialog
    accountToEdit?.let { acct ->
        AccountEditDialog(
            initialName = acct.customName ?: "",
            title = AppStrings.edit_account.tr(lang),
            onDismiss = { accountToEdit = null },
            onConfirm = { name ->
                AccountRepository.updateAccount(acct.key, name)
                refreshKey++
                onAccountChanged(acct.key)
                accountToEdit = null
            },
            lang = lang
        )
    }

    // Delete Account Confirmation Dialog
    accountToDelete?.let { acct ->
        AccountDeleteConfirmDialog(
            accountName = acct.getDisplayName(lang),
            onDismiss = { accountToDelete = null },
            onConfirm = {
                AccountRepository.deleteAccount(acct.key)
                refreshKey++
                onAccountChanged("ALL")
                accountToDelete = null
            },
            lang = lang
        )
    }
}
