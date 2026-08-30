package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText

/**
 * Dedicated confirmation sub-dialog for safely deleting a custom account.
 * Conforms to Destructive Action Confirmation standards using CommonButtonStyle.Danger.
 */
@Composable
fun AccountDeleteConfirmDialog(
    accountName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "zh"
) {
    val haptic = LocalHapticFeedback.current

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.DELETE_ACCOUNT_CONFIRM_TITLE.tr(lang),
        modifier = modifier,
        confirmButton = {
            CommonButton(
                text = AppStrings.COMMON_DELETE.tr(lang),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm()
                },
                style = CommonButtonStyle.Danger
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.COMMON_CANCEL.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Text
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
            CommonText(
                text = accountName,
                fontSize = AppDimens.TextTitle,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            CommonText(
                text = AppStrings.DELETE_ACCOUNT_CONFIRM_DESC.tr(lang),
                fontSize = AppDimens.TextSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
