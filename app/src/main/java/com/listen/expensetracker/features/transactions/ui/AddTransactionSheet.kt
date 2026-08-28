package com.listen.expensetracker.features.transactions.ui

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.settings.components.CategoryManageDialog
import com.listen.uicomponent.keypad.NumericKeypad
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Bottom Sheet for creating a new transaction with custom keypad and category picker.
 */

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AddTransactionSheet(
    modifier: Modifier = Modifier,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (
        type: String,
        categoryId: String,
        categoryName: String,
        categoryIconName: String,
        categoryColorHex: String,
        amount: Double,
        note: String,
        accountType: String,
        timestamp: Long
    ) -> Unit,
    lang: String = "zh",
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf("EXPENSE") }
    var categoryVersion by remember { mutableIntStateOf(0) }

    val categories = remember(type, categoryVersion) {
        if (type == "EXPENSE") CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    }

    var selectedCategory by remember(categories) { mutableStateOf(categories.first()) }
    var amountExpression by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var accountVersion by remember { mutableIntStateOf(0) }
    val availableAccounts = remember(accountVersion) { AccountRepository.getAllAccounts() }
    var selectedAccount by remember { mutableStateOf(availableAccounts.firstOrNull()?.key ?: "CASH") }
    var accountToDelete by remember { mutableStateOf<AccountTypeItem?>(null) }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var showCategoryManageDialog by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val amountFontSize = when {
        amountExpression.length > 10 -> 22.sp
        amountExpression.length > 7 -> 26.sp
        else -> 30.sp
    }

    val typeOptions = listOf(AppStrings.type_expense.tr(lang), AppStrings.type_income.tr(lang))

    com.listen.uicomponent.components.CommonBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Expense / Income Segmented Switch
            com.listen.uicomponent.components.CommonSegmentedControl(
                items = typeOptions,
                selectedIndex = if (type == "EXPENSE") 0 else 1,
                onIndexChange = { index ->
                    type = if (index == 0) "EXPENSE" else "INCOME"
                }
            )

            Spacer(modifier = Modifier.height(AppDimens.SpaceStandard))

            // Amount Display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppDimens.SpaceExtraSmall),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currencySymbol,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                )
                Text(
                    text = amountExpression,
                    fontSize = amountFontSize,
                    fontWeight = FontWeight.Bold,
                    color = if (type == "EXPENSE") ExpenseRed else IncomeGreen,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }

            // Category Horizontal Picker
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
                contentPadding = PaddingValues(vertical = AppDimens.SpaceSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = selectedCategory.id == cat.id
                    val catColor = parseHexColor(cat.colorHex)
                    val catName = cat.getDisplayName(lang)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.CornerCard))
                            .clickable { selectedCategory = cat }
                            .padding(AppDimens.SpaceSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) catColor else catColor.copy(alpha = 0.16f))
                                .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = catName,
                                tint = if (isSelected) Color.White else catColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = catName,
                            fontSize = AppDimens.TextMicro,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.CornerCard))
                            .clickable { showCategoryManageDialog = true }
                            .padding(AppDimens.SpaceSmall)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                        Text(AppStrings.settings_category_manage.tr(lang), fontSize = AppDimens.TextMicro, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Note & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.listen.uicomponent.components.CommonEditText(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = AppStrings.search_placeholder.tr(lang),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Date Picker Button
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply {
                                    set(y, m, d)
                                }
                                selectedTimestamp = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    contentPadding = PaddingValues(horizontal = AppDimens.SpaceStandard, vertical = AppDimens.SpaceExtraSmall)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Spacer(modifier = Modifier.size(AppDimens.SpaceSmall))
                    Text(sdf.format(Date(selectedTimestamp)), fontSize = AppDimens.TextSmall)
                }
            }

            // Account Selection Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                modifier = Modifier.fillMaxWidth().padding(vertical = AppDimens.SpaceSmall)
            ) {
                items(availableAccounts, key = { it.key }) { acct ->
                    val isSelected = selectedAccount == acct.key
                    val acctName = acct.getDisplayName(lang)
                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    val labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

                    Surface(
                        shape = RoundedCornerShape(AppDimens.CornerButton),
                        color = containerColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .clip(RoundedCornerShape(AppDimens.CornerButton))
                            .combinedClickable(
                                onClick = { selectedAccount = acct.key },
                                onLongClick = if (!acct.isSystem) ({ accountToDelete = acct }) else null
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = acctName,
                                fontSize = AppDimens.TextSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = labelColor
                            )
                        }
                    }
                }
            }

            // Keypad Component (Option A 4x3)
            NumericKeypad(
                onKeyPress = { key ->
                    if (key == "." && amountExpression.contains(".")) {
                        // Ignore secondary decimal points
                    } else if (amountExpression == "0" && key != ".") {
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
                doneText = AppStrings.common_done.tr(lang) + " ✓",
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

    // Delete Account Confirmation Dialog on Long Press
    accountToDelete?.let { acct ->
        com.listen.expensetracker.features.transactions.components.AccountDeleteConfirmDialog(
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
