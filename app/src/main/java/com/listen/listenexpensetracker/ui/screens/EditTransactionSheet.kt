package com.listen.listenexpensetracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.data.db.TransactionEntity
import com.listen.listenexpensetracker.data.model.Category
import com.listen.listenexpensetracker.data.model.CategoryRepository
import com.listen.uicomponent.keypad.NumericKeypad
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSaveEdit: (TransactionEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(transaction.type) }
    var selectedCategory by remember {
        mutableStateOf(CategoryRepository.getCategoryById(transaction.categoryId))
    }
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    var accountType by remember { mutableStateOf(transaction.accountType) }
    var selectedTimestamp by remember { mutableLongStateOf(transaction.timestamp) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val categories = if (type == "EXPENSE") {
        CategoryRepository.expenseCategories
    } else {
        CategoryRepository.incomeCategories
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "编辑账单明细",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Type Toggle: Expense vs Income
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                SegmentedButton(
                    selected = type == "EXPENSE",
                    onClick = {
                        type = "EXPENSE"
                        selectedCategory = CategoryRepository.expenseCategories.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("支出", color = if (type == "EXPENSE") ExpenseRed else Color.Unspecified)
                }
                SegmentedButton(
                    selected = type == "INCOME",
                    onClick = {
                        type = "INCOME"
                        selectedCategory = CategoryRepository.incomeCategories.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("收入", color = if (type == "INCOME") IncomeGreen else Color.Unspecified)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Amount Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = selectedCategory.nameZh,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = parseHexColor(selectedCategory.colorHex)
                    )

                    Text(
                        text = "￥${if (amountStr.isEmpty()) "0" else amountStr}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (type == "EXPENSE") ExpenseRed else IncomeGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Account Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "WECHAT" to "微信",
                    "ALIPAY" to "支付宝",
                    "BANK" to "银行卡",
                    "CASH" to "现金"
                ).forEach { (accKey, label) ->
                    FilterChip(
                        selected = accountType == accKey,
                        onClick = { accountType = accKey },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            // Date Picker & Note Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                val newCal = Calendar.getInstance().apply { set(y, m, d) }
                                selectedTimestamp = newCal.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    }
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(sdf.format(Date(selectedTimestamp)), fontSize = 11.sp)
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("修改备注...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            // Category Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(110.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(categories, key = { it.id }) { cat ->
                    val isSelected = selectedCategory.id == cat.id
                    val color = parseHexColor(cat.colorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedCategory = cat }
                            .padding(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.nameZh,
                                tint = if (isSelected) Color.White else color,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = cat.nameZh,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Numeric Keypad
            NumericKeypad(
                onKeyPress = { key ->
                    if (amountStr == "0" && key != ".") {
                        amountStr = key
                    } else {
                        amountStr += key
                    }
                },
                onDeletePress = {
                    amountStr = if (amountStr.length <= 1) {
                        "0"
                    } else {
                        amountStr.dropLast(1)
                    }
                },
                onDonePress = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val updated = transaction.copy(
                            type = type,
                            categoryId = selectedCategory.id,
                            categoryName = selectedCategory.nameZh,
                            categoryIcon = selectedCategory.id,
                            categoryColorHex = selectedCategory.colorHex,
                            amount = amount,
                            note = note,
                            accountType = accountType,
                            timestamp = selectedTimestamp
                        )
                        onSaveEdit(updated)
                        onDismiss()
                    }
                },
                showOperators = false,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
