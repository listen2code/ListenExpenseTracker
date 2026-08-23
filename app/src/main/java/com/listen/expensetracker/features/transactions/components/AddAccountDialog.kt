package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens

/**
 * Dialog allowing users to create custom accounts (e.g. PayPal, Crypto, Investment).
 */
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAccountAdded: (String) -> Unit,
    lang: String = "zh"
) {
    var accountName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = StringsRes.get("add_account", lang),
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TextHeader
            )
        },
        text = {
            OutlinedTextField(
                value = accountName,
                onValueChange = { accountName = it },
                label = { Text(StringsRes.get("account_name_input", lang)) },
                singleLine = true,
                shape = RoundedCornerShape(AppDimens.CornerButton),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = accountName.trim()
                    if (trimmed.isNotBlank()) {
                        val newAccount = AccountRepository.addAccount(trimmed)
                        onAccountAdded(newAccount.key)
                        onDismiss()
                    }
                },
                enabled = accountName.isNotBlank(),
                shape = RoundedCornerShape(AppDimens.CornerButton)
            ) {
                Text(StringsRes.get("common_confirm", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("common_cancel", lang))
            }
        }
    )
}
