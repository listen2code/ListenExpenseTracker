package com.listen.expensetracker.features.transactions.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.Category
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.settings.components.CategoryManageDialog
import com.listen.uicomponent.components.CommonBottomSheet
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.keypad.NumericKeypad
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen

/**
 * 统一记账弹窗 (Unified Transaction Sheet)。
 * 支持“新增”与“编辑”两种模式，显著减少代码重复。
 *
 * 1. UI 逻辑复用：通过可选参数 [transaction] 自动切换模式，共享大部分输入控件。
 * 2. 状态提升 (State Hoisting)：所有输入字段均为本地状态，仅在点击“完成”时统一回调给业务层。
 * 3. 动态分类过滤：利用 [remember(type)] 实现收支切换时分类列表的实时响应。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier,
    transaction: TransactionEntity? = null,
    onDelete: (() -> Unit)? = null,
    onSaveAndContinue: ((TransactionEntity) -> Unit)? = null,
    initialTimestamp: Long = System.currentTimeMillis(),
    initialCategoryId: String? = null,
    initialType: String = TransactionType.EXPENSE,
    lang: String = "zh"
) {
    val isEditMode = transaction != null
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // --- 状态驱动层 ---
    var type by remember { mutableStateOf(transaction?.type ?: initialType) }
    var categoryVersion by remember { mutableIntStateOf(0) }
    
    // 自动过滤分类列表
    val categories = remember(type, categoryVersion) { if (type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories }
    
    // 选中分类，编辑模式或快速指定分类下尝试匹配
    var selectedCategory: Category by remember(categories) {
        val matched = if (isEditMode) categories.find { it.id == transaction.categoryId }
        else if (initialCategoryId != null) categories.find { it.id == initialCategoryId } else null
        mutableStateOf(matched ?: categories.first())
    }

    var amountExpression by remember { mutableStateOf(if (isEditMode) "%.2f".format(transaction.amount ?: 0.0) else "0") }
    
    var note by remember { mutableStateOf(transaction?.note ?: "") }
    var accountVersion by remember { mutableIntStateOf(0) }
    val availableAccounts = remember(accountVersion) { AccountRepository.getAllAccounts() }
    var selectedAccount by remember { mutableStateOf(transaction?.accountType ?: "CASH") }
    var selectedTimestamp by remember(initialTimestamp) { mutableLongStateOf(transaction?.timestamp ?: initialTimestamp) }
    
    // 全局/局部弹窗标记
    var showCategoryManageDialog by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<AccountTypeItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

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
            // 1. 顶栏操作区：收支分段选择器与编辑模式删除按钮（保持新增与编辑高度 100% 对齐）
            TransactionSheetHeader(
                selectedType = type,
                onTypeChange = { type = it },
                typeOptions = typeOptions,
                isEditMode = isEditMode && onDelete != null,
                onDeleteClick = { showDeleteConfirmDialog = true }
            )

            // 2. 金额输入预览（只读，由下方数字键盘驱动；支持点击清除按钮重置为0）
            CommonEditText(
                value = amountExpression,
                onValueChange = { amountExpression = "0" },
                placeholder = "0.00",
                readOnly = true,
                showClearButton = amountExpression != "0" && amountExpression.isNotEmpty(),
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

            // 3. 分类滚动选择器（集成“管理”入口）
            TransactionCategoryPicker(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it },
                lang = lang,
                onManageCategories = { showCategoryManageDialog = true }
            )

            // 4. 备注文本框
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

            // 5. 日期选择胶囊
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TransactionDatePickerButton(selectedTimestamp = selectedTimestamp, onDateSelected = { selectedTimestamp = it })
            }

            // 6. 支付账户选择（支持长按管理）
            TransactionAccountPicker(
                accounts = availableAccounts,
                selectedAccount = selectedAccount,
                onAccountSelected = { selectedAccount = it },
                onAccountLongClick = { accountToDelete = it },
                lang = lang
            )

            val createTransaction = {
                val amt = amountExpression.toDoubleOrNull() ?: 0.0
                if (amt > 0) {
                    val cat = selectedCategory.getDisplayName(lang)
                    val now = System.currentTimeMillis()
                    val ts = if (isSameCalendarDay(selectedTimestamp, now)) maxOf(now, selectedTimestamp) else selectedTimestamp
                    (transaction ?: TransactionEntity(type = type, categoryId = "", categoryName = "", categoryIcon = "", categoryColorHex = "", amount = 0.0)).copy(
                        type = type, categoryId = selectedCategory.id, categoryName = cat,
                        categoryIcon = selectedCategory.id, categoryColorHex = selectedCategory.colorHex,
                        amount = amt, note = note.trim(), accountType = selectedAccount, timestamp = ts
                    )
                } else null
            }

            // 7. 响应式数字键盘
            NumericKeypad(
                onKeyPress = { key ->
                    if (key == "." && amountExpression.contains(".")) return@NumericKeypad
                    if ((amountExpression == "0" || amountExpression.isEmpty()) && key != ".") {
                        amountExpression = key
                    } else if (amountExpression.length < 10) {
                        amountExpression += key
                    }
                },
                onDeletePress = {
                    amountExpression = if (amountExpression.length > 1) amountExpression.dropLast(1) else "0"
                },
                onDonePress = { createTransaction()?.let { onSave(it) } },
                doneText = AppStrings.COMMON_DONE.tr(lang) + " ✓",
                onContinuePress = if (!isEditMode && onSaveAndContinue != null) {
                    {
                        createTransaction()?.let { tx ->
                            onSaveAndContinue(tx)
                            amountExpression = "0"; note = ""
                            val now = System.currentTimeMillis()
                            selectedTimestamp = if (isSameCalendarDay(tx.timestamp, now)) maxOf(now, tx.timestamp + 1000L) else tx.timestamp + 1000L
                            Toast.makeText(context, AppStrings.MSG_SAVED_CONTINUE.tr(lang), Toast.LENGTH_SHORT).show()
                        }
                    }
                } else null,
                continueText = if (!isEditMode && onSaveAndContinue != null) AppStrings.COMMON_CONTINUE.tr(lang) + " +" else null,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // --- 弹窗逻辑聚合 ---
    if (showCategoryManageDialog) {
        CategoryManageDialog(type = type, onDismiss = { showCategoryManageDialog = false }, onCategoriesChanged = { categoryVersion++ }, lang = lang)
    }

    accountToDelete?.let { acct ->
        AccountDeleteConfirmDialog(
            accountName = acct.getDisplayName(lang), onDismiss = { accountToDelete = null },
            onConfirm = {
                AccountRepository.deleteAccount(acct.key); accountVersion++
                if (selectedAccount == acct.key) selectedAccount = "CASH"
                accountToDelete = null
            }, lang = lang
        )
    }

    if (showDeleteConfirmDialog) {
        TransactionDeleteConfirmDialog(
            categoryName = selectedCategory.getDisplayName(lang), currencySymbol = currencySymbol,
            amount = amountExpression.toDoubleOrNull() ?: 0.0, onDismiss = { showDeleteConfirmDialog = false },
            onConfirm = { showDeleteConfirmDialog = false; onDelete?.invoke() }, lang = lang
        )
    }
}
