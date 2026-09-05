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
                        text = AppStrings.RECURRING_RULE_NAME.tr(lang),
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(42.dp)
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
                                        text = AppStrings.RECURRING_NAME_HINT.tr(lang),
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
                        text = AppStrings.RECURRING_AMOUNT_LABEL.tr(lang),
                        fontSize = AppDimens.TextSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(42.dp)
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

        // 4. 周期频次（每天/每周/每月/每年）与扣款日选择
        RecurringFrequencySelector(
            frequency = state.frequency,
            dayOfPeriod = state.dayOfPeriod,
            lang = lang,
            onFrequencyChange = { state.frequency = it },
            onDayChange = { state.dayOfPeriod = it }
        )

        // 5. 履约模式
        CommonSegmentedControl(
            items = listOf(AppStrings.RECURRING_EXEC_AUTO.tr(lang), AppStrings.RECURRING_EXEC_NOTIFY.tr(lang)),
            selectedIndex = if (state.executionType == ExecutionType.AUTO_INSERT) 0 else 1,
            onIndexChange = { state.executionType = if (it == 0) ExecutionType.AUTO_INSERT else ExecutionType.NOTIFY_CONFIRM }
        )
    }
}
