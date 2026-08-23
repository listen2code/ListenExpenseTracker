package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens

/**
 * Clean Google Account Manual Binding Dialog.
 * Allows binding a custom Google Drive email when official Google Play services dialog is cancelled or unavailable.
 */
@Composable
fun GoogleLinkDialog(
    currentEmail: String?,
    onAccountLinked: (String) -> Unit,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    var inputEmail by remember { mutableStateOf(currentEmail ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4285F4)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Google",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = StringsRes.get("google_link_title", lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextHeader
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
                Text(
                    text = StringsRes.get("google_link_desc", lang),
                    fontSize = AppDimens.TextSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(AppDimens.SpaceExtraSmall))
                OutlinedTextField(
                    value = inputEmail,
                    onValueChange = { inputEmail = it },
                    placeholder = { Text(StringsRes.get("google_email_placeholder", lang), fontSize = AppDimens.TextBody) },
                    singleLine = true,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (inputEmail.isNotBlank()) {
                        onAccountLinked(inputEmail.trim())
                        onDismiss()
                    }
                }
            ) {
                Text(StringsRes.get("btn_save", lang), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("btn_cancel", lang))
            }
        }
    )
}
