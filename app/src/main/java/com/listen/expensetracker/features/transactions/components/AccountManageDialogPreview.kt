package com.listen.expensetracker.features.transactions.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 账户管理弹窗预览 (AccountManageDialogPreview)。
 */
@Preview(showBackground = true)
@Composable
fun AccountManageDialogPreview() {
    ExpenseStrings.init()
    ListenTheme {
        AccountManageDialog(
            onDismiss = {},
            lang = "zh"
        )
    }
}
