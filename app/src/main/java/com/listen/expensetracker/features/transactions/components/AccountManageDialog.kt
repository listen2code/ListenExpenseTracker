package com.listen.expensetracker.features.transactions.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.theme.ExpenseRed

/**
 * Account Management Dialog allowing users to view all accounts, create new custom accounts,
 * rename existing custom accounts, and delete custom accounts.
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.manage_accounts_title.tr(lang),
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TextTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Account List Container
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(accounts, key = { it.key }) { acct ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (editingAccountKey == acct.key) {
                                // Inline Rename Mode
                                OutlinedTextField(
                                    value = editingAccountName,
                                    onValueChange = { editingAccountName = it },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                TextButton(
                                    onClick = {
                                        val trimmed = editingAccountName.trim()
                                        if (trimmed.isNotBlank()) {
                                            AccountRepository.updateAccount(acct.key, trimmed)
                                            accounts.clear()
                                            accounts.addAll(AccountRepository.getAllAccounts())
                                            onAccountChanged(acct.key)
                                        }
                                        editingAccountKey = null
                                    }
                                ) {
                                    Text(AppStrings.common_save.tr(lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                // Normal Display Mode
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
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
                                            Text(
                                                text = if (lang == "en") "System" else "内置",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                // Actions for Custom Accounts
                                if (!acct.isSystem) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingAccountKey = acct.key
                                                editingAccountName = acct.customName ?: ""
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                AccountRepository.deleteAccount(acct.key)
                                                accounts.clear()
                                                accounts.addAll(AccountRepository.getAllAccounts())
                                                onAccountChanged("ALL")
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
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

                Spacer(modifier = Modifier.height(4.dp))

                // New Account Creation Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newAccountName,
                        onValueChange = { newAccountName = it },
                        placeholder = { Text(AppStrings.account_name_input.tr(lang), fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    )

                    Button(
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
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(AppStrings.common_done.tr(lang))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
