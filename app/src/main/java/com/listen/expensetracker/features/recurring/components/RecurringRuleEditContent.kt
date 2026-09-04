package com.listen.expensetracker.features.recurring.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.RecurringTransactionEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.transactions.components.TransactionAccountPicker
import com.listen.expensetracker.features.transactions.components.TransactionCategoryPicker
import com.listen.uicomponent.components.CommonSegmentedControl
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen

const val MAX_RECURRING_AMOUNT = 999_999.99

class RecurringEditState(
    val initialRule: RecurringRuleEntity?,
    val lang: String
) {
    var title by mutableStateOf(initialRule?.title ?: "")
    var type by mutableStateOf(initialRule?.type ?: TransactionType.EXPENSE)
    var amountStr by mutableStateOf(initialRule?.amount?.let { "%.2f".format(it) } ?: "")
    var note by mutableStateOf(initialRule?.note ?: "")

    private val initialCats = if (type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    var selectedCategoryId by mutableStateOf(initialRule?.categoryId ?: initialCats.first().id)
    var selectedAccount by mutableStateOf(initialRule?.accountType ?: "CASH")
    var frequency by mutableStateOf(initialRule?.frequency ?: RecurringFrequency.MONTHLY)
    var dayOfPeriod by mutableIntStateOf(initialRule?.dayOfPeriod ?: 1)
    var executionType by mutableStateOf(initialRule?.executionType ?: ExecutionType.AUTO_INSERT)

    val rawAmount: Double
        get() = amountStr.toDoubleOrNull() ?: 0.0

    val isValid: Boolean
        get() = title.isNotBlank() && rawAmount > 0 && rawAmount <= MAX_RECURRING_AMOUNT

    fun buildEntity(): RecurringRuleEntity {
        val amt = rawAmount.coerceIn(0.01, MAX_RECURRING_AMOUNT)
        val now = System.currentTimeMillis()
        val nextDate = initialRule?.nextExecutionDate ?: RecurringTransactionEngine.calculateNextExecutionDate(frequency, dayOfPeriod, now - 86400000L)
        val activeCats = if (type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
        val cat = activeCats.find { it.id == selectedCategoryId } ?: activeCats.first()
        return (initialRule ?: RecurringRuleEntity(
            title = title.trim(), categoryId = cat.id, categoryName = cat.getDisplayName(lang),
            categoryIcon = cat.id, categoryColorHex = cat.colorHex, amount = amt
        )).copy(
            title = title.trim(), type = type, categoryId = cat.id,
            categoryName = cat.getDisplayName(lang), categoryIcon = cat.id,
            categoryColorHex = cat.colorHex, amount = amt, accountType = selectedAccount,
            note = note.trim(), frequency = frequency, dayOfPeriod = dayOfPeriod,
            executionType = executionType, nextExecutionDate = nextDate
        )
    }
}

/**
 * 周期规则紧凑型编辑表单 (RecurringRuleEditContent)。
 * 采用一体化表单卡片设计与单行紧凑日期预设，极大压缩高度，彻底告别冗余堆叠。
 */
@Composable
fun RecurringRuleEditContent(
    state: RecurringEditState,
    currencySymbol: String,
    lang: String,
    modifier: Modifier = Modifier
) {
    val categories = remember(state.type) {
        if (state.type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    }
    val selectedCategory = remember(state.type, state.selectedCategoryId, categories) {
        categories.find { it.id == state.selectedCategoryId } ?: categories.first()
    }
    val accounts = remember { AccountRepository.getAllAccounts() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = AppDimens.SpaceSmall),
        verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
    ) {
        // 1. 收支分段切换
        CommonSegmentedControl(
            items = listOf(AppStrings.TYPE_EXPENSE.tr(lang), AppStrings.TYPE_INCOME.tr(lang)),
            selectedIndex = if (state.type == TransactionType.EXPENSE) 0 else 1,
            onIndexChange = {
                state.type = if (it == 0) TransactionType.EXPENSE else TransactionType.INCOME
                val newCats = if (state.type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
                state.selectedCategoryId = newCats.first().id
            }
        )

        // 2. 基础信息卡片：规则名称与每期金额一体化
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                // 行 1: 规则名称
                Row(
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommonText(
                        text = "名称",
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                    BasicTextField(
                        value = state.title,
                        onValueChange = { if (it.length <= 20) state.title = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (state.title.isEmpty()) {
                                    CommonText(
                                        text = "如：房租、Netflix",
                                        fontSize = AppDimens.TextSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f), thickness = 0.5.dp)

                // 行 2: 每期金额
                Row(
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CommonText(
                        text = "金额",
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(38.dp)
                    )
                    CommonText(
                        text = currencySymbol,
                        fontSize = AppDimens.TextBody,
                        fontWeight = FontWeight.Bold,
                        color = if (state.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    BasicTextField(
                        value = state.amountStr,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            if (parts.size <= 2 && (parts.size < 2 || parts[1].length <= 2)) {
                                val num = filtered.toDoubleOrNull()
                                if (num == null || num <= MAX_RECURRING_AMOUNT) {
                                    state.amountStr = filtered
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (state.amountStr.isEmpty()) {
                                    CommonText(
                                        text = "0.00",
                                        fontSize = AppDimens.TextSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }

        // 3. 分类与账户选择
        TransactionCategoryPicker(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { state.selectedCategoryId = it.id },
            lang = lang
        )

        TransactionAccountPicker(
            accounts = accounts,
            selectedAccount = state.selectedAccount,
            onAccountSelected = { state.selectedAccount = it },
            onAccountLongClick = {},
            lang = lang
        )

        // 4. 周期与扣款日一体化单行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommonText(text = "扣款日", fontSize = AppDimens.TextSmall, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = { state.dayOfPeriod = if (state.dayOfPeriod > 1) state.dayOfPeriod - 1 else 28 },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Prev", modifier = Modifier.size(14.dp))
                }
                Surface(
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    CommonText(
                        text = if (state.dayOfPeriod == 28) "月末" else "${state.dayOfPeriod}日",
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                IconButton(
                    onClick = { state.dayOfPeriod = if (state.dayOfPeriod < 28) state.dayOfPeriod + 1 else 1 },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Next", modifier = Modifier.size(14.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(1, 5, 10, 15, 28).forEach { day ->
                    val isSel = state.dayOfPeriod == day
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { state.dayOfPeriod = day }
                    ) {
                        CommonText(
                            text = if (day == 28) "月末" else "${day}日",
                            fontSize = 10.sp,
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        // 5. 履约模式
        CommonSegmentedControl(
            items = listOf(AppStrings.RECURRING_EXEC_AUTO.tr(lang), AppStrings.RECURRING_EXEC_NOTIFY.tr(lang)),
            selectedIndex = if (state.executionType == ExecutionType.AUTO_INSERT) 0 else 1,
            onIndexChange = { state.executionType = if (it == 0) ExecutionType.AUTO_INSERT else ExecutionType.NOTIFY_CONFIRM }
        )
    }
}
