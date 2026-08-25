package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ExpenseRed

/**
 * Modern, polished Account Management Dialog using standardized ListenUiComponent elements.
 * Allows users to inspect existing accounts, create custom payment accounts, rename, and delete accounts.
 *
 * @param onDismiss Dismiss callback
 * @param onAccountChanged Callback when accounts are modified
 * @param lang Active language code
 */
@Composable
fun AccountManageDialog(
    onDismiss: () -> Unit,
    onAccountChanged: (String) -> Unit = {},
    lang: String = "zh"
) {
    val accounts = remember { mutableStateListOf<AccountTypeItem>().apply { addAll(AccountRepository.getAllAccounts()) } }
    var newAccountName by remember { mutableStateOf("") }
    var editingAccountKey by remember { mutableStateOf<String?>(null) }
    var editingAccountName by remember { mutableStateOf("") }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.manage_accounts_title.tr(lang),
        icon = {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        confirmButton = {
            CommonButton(
                text = AppStrings.common_done.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Primary
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Account List Container
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accounts, key = { it.key }) { acct ->
                    AccountItemRow(
                        acct = acct,
                        isEditing = editingAccountKey == acct.key,
                        editingName = editingAccountName,
                        onEditingNameChange = { editingAccountName = it },
                        onStartEdit = {
                            editingAccountKey = acct.key
                            editingAccountName = acct.customName ?: ""
                        },
                        onSaveEdit = {
                            val trimmed = editingAccountName.trim()
                            if (trimmed.isNotBlank()) {
                                AccountRepository.updateAccount(acct.key, trimmed)
                                accounts.clear()
                                accounts.addAll(AccountRepository.getAllAccounts())
                                onAccountChanged(acct.key)
                            }
                            editingAccountKey = null
                        },
                        onCancelEdit = { editingAccountKey = null },
                        onDelete = {
                            AccountRepository.deleteAccount(acct.key)
                            accounts.clear()
                            accounts.addAll(AccountRepository.getAllAccounts())
                            onAccountChanged("ALL")
                        },
                        lang = lang
                    )
                }
            }

            // Add Account Input Form
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommonEditText(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        placeholder = AppStrings.account_name_input.tr(lang),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    CommonButton(
                        text = "",
                        onClick = {
                            val trimmed = newAccountName.trim()
                            if (trimmed.isNotBlank()) {
                                val created = AccountRepository.addAccount(trimmed)
                                accounts.clear()
                                accounts.addAll(AccountRepository.getAllAccounts())
                                newAccountName = ""
                                onAccountChanged(created.key)
                            }
                        },
                        enabled = newAccountName.isNotBlank(),
                        style = CommonButtonStyle.Tonal,
                        icon = {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual account item row in the management dialog.
 */
@Composable
private fun AccountItemRow(
    acct: AccountTypeItem,
    isEditing: Boolean,
    editingName: String,
    onEditingNameChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onDelete: () -> Unit,
    lang: String
) {
    val icon = getAccountIcon(acct.key)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditing) {
                CommonEditText(
                    value = editingName,
                    onValueChange = onEditingNameChange,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(6.dp))
                CommonButton(
                    text = AppStrings.common_save.tr(lang),
                    onClick = onSaveEdit,
                    style = CommonButtonStyle.Text
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    CommonText(
                        text = acct.getDisplayName(lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (acct.isSystem) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            CommonText(
                                text = if (lang == "en") "Built-in" else "内置",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                if (!acct.isSystem) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onStartEdit, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Returns icon corresponding to account key.
 */
private fun getAccountIcon(key: String): ImageVector {
    return when (key) {
        "BANK" -> Icons.Default.AccountBalance
        "CREDIT" -> Icons.Default.CreditCard
        "CASH" -> Icons.Default.Payments
        else -> Icons.Default.AccountBalanceWallet
    }
}
