package com.listen.expensetracker.features.transactions.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText

/**
 * 账单删除二次确认对话框 (TransactionDeleteConfirmDialog)。
 * 统一收拢编辑页与外部列表触发的账单删除二次确认逻辑，避免重复编写确认弹窗代码。
 */
@Composable
fun TransactionDeleteConfirmDialog(
    categoryName: String,
    currencySymbol: String,
    amount: Double,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    lang: String = "zh"
) {
    val desc = AppStrings.DELETE_TRANSACTION_DESC.tr(lang).format(categoryName, currencySymbol, amount)
    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.DELETE_TRANSACTION_TITLE.tr(lang),
        confirmButton = {
            CommonButton(
                text = AppStrings.COMMON_DELETE.tr(lang),
                onClick = onConfirm,
                style = CommonButtonStyle.Danger
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.COMMON_CANCEL.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Outlined
            )
        }
    ) {
        CommonText(
            text = desc,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
