package com.listen.expensetracker.features.settings.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.theme.ListenTheme

/**
 * 分类管理弹窗预览 (CategoryManageDialogPreview)。
 */
@Preview(showBackground = true)
@Composable
fun CategoryManageDialogPreview() {
    ExpenseStrings.init()
    ListenTheme {
        CategoryManageDialog(
            type = "EXPENSE",
            onDismiss = {},
            onCategoriesChanged = {},
            lang = "zh"
        )
    }
}
