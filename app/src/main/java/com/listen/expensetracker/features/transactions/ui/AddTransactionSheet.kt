package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.settings.components.CategoryManageDialog
import com.listen.expensetracker.features.transactions.components.AccountDeleteConfirmDialog
import com.listen.uicomponent.components.CommonBottomSheet
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonSegmentedControl
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.keypad.NumericKeypad
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen

/**
 * Bottom Sheet for creating a new transaction.
 * Uses standardized CommonEditText for consistent styling with MonthlyBudgetDialog and the whole app.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (type: String, categoryId: String, categoryName: String, categoryIcon: String, categoryColorHex: String, amount: Double, note: String, accountType: String, timestamp: Long) -> Unit,
    modifier: Modifier = Modifier,
    initialTimestamp: Long = System.currentTimeMillis(),
    lang: String = "zh"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(TransactionType.EXPENSE) }
    var categoryVersion by remember { mutableIntStateOf(0) }
    val categories = remember(type, categoryVersion) {
        if (type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    }
    var selectedCategory by remember(categories) { mutableStateOf(categories.first()) }
    var amountExpression by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var accountVersion by remember { mutableIntStateOf(0) }
    val availableAccounts = remember(accountVersion) { AccountRepository.getAllAccounts() }
    var selectedAccount by remember { mutableStateOf("CASH") }
    var accountToDelete by remember { mutableStateOf<AccountTypeItem?>(null) }
    var selectedTimestamp by remember(initialTimestamp) { mutableLongStateOf(initialTimestamp) }
    var showCategoryManageDialog by remember { mutableStateOf(false) }

    val typeOptions = listOf(AppStrings.TYPE_EXPENSE.tr(lang), AppStrings.TYPE_INCOME.tr(lang))

    CommonBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
        ) {
            // Expense / Income Segmented Switch
            CommonSegmentedControl(
                items = typeOptions,
                selectedIndex = if (type == TransactionType.EXPENSE) 0 else 1,
                onIndexChange = { index ->
                    type = if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME
                }
            )

            // Standardized Amount Input Field (CommonEditText, Custom Keypad Driven)
            CommonEditText(
                value = amountExpression,
                onValueChange = { input ->
                    val clean = input.filter { it.isDigit() || it == '.' }
                    if (clean.count { it == '.' } <= 1 && clean.length <= 10) {
                        amountExpression = clean
                    }
                },
                placeholder = "0.00",
                readOnly = true,
                leadingIcon = {
                    CommonText(
                        text = currencySymbol,
                        fontSize = AppDimens.TextBody,
                        fontWeight = FontWeight.Bold,
                        color = if (type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            // Category Horizontal Picker with Add Category button
            TransactionCategoryPicker(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                lang = lang,
                onManageCategories = { showCategoryManageDialog = true }
            )

            // Standardized Note Input Field (CommonEditText)
            CommonEditText(
                value = note,
                onValueChange = { note = it },
                placeholder = AppStrings.TRANSACTION_NOTE_HINT.tr(lang),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            )

            // Date Picker Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TransactionDatePickerButton(
                    selectedTimestamp = selectedTimestamp,
                    onDateSelected = { selectedTimestamp = it }
                )
            }

            // Account Selection Chips
            TransactionAccountPicker(
                accounts = availableAccounts,
                selectedAccount = selectedAccount,
                onAccountSelected = { selectedAccount = it },
                onAccountLongClick = { accountToDelete = it },
                lang = lang
            )

            // Numeric Keypad
            NumericKeypad(
                onKeyPress = { key ->
                    if (key == "." && amountExpression.contains(".")) {
                        // ignore secondary decimal points
                    } else if ((amountExpression == "0" || amountExpression.isEmpty()) && key != ".") {
                        amountExpression = key
                    } else if (amountExpression.length < 10) {
                        amountExpression += key
                    }
                },
                onDeletePress = {
                    amountExpression = if (amountExpression.length > 1) {
                        amountExpression.dropLast(1)
                    } else {
                        "0"
                    }
                },
                onDonePress = {
                    val finalAmount = amountExpression.toDoubleOrNull() ?: 0.0
                    if (finalAmount > 0) {
                        val catName = selectedCategory.getDisplayName(lang)
                        onSave(
                            type,
                            selectedCategory.id,
                            catName,
                            selectedCategory.id,
                            selectedCategory.colorHex,
                            finalAmount,
                            note.trim(),
                            selectedAccount,
                            selectedTimestamp
                        )
                    }
                },
                doneText = AppStrings.COMMON_DONE.tr(lang) + " ✓",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showCategoryManageDialog) {
        CategoryManageDialog(
            type = type,
            onDismiss = { showCategoryManageDialog = false },
            onCategoriesChanged = { categoryVersion++ },
            lang = lang
        )
    }

    accountToDelete?.let { acct ->
        AccountDeleteConfirmDialog(
            accountName = acct.getDisplayName(lang),
            onDismiss = { accountToDelete = null },
            onConfirm = {
                AccountRepository.deleteAccount(acct.key)
                accountVersion++
                if (selectedAccount == acct.key) {
                    selectedAccount = "CASH"
                }
                accountToDelete = null
            },
            lang = lang
        )
    }
}
