package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonEditText

/**
 * Dedicated sub-dialog for creating or editing custom payment account names.
 */
@Composable
fun AccountEditDialog(
    initialName: String,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "zh"
) {
    var name by remember { mutableStateOf(initialName) }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = title,
        modifier = modifier,
        confirmButton = {
            CommonButton(
                text = AppStrings.common_save.tr(lang),
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isNotBlank()) onConfirm(trimmed)
                },
                enabled = name.trim().isNotBlank(),
                style = CommonButtonStyle.Primary
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.common_cancel.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Text
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            CommonEditText(
                value = name,
                onValueChange = { name = it },
                placeholder = AppStrings.account_name_input.tr(lang),
                singleLine = true
            )
        }
    }
}
