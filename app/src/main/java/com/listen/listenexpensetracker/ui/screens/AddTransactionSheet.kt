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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.listenexpensetracker.data.model.Category
import com.listen.listenexpensetracker.data.model.CategoryRepository
import com.listen.uicomponent.keypad.NumericKeypad
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    onSaveTransaction: (
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf("EXPENSE") } // "EXPENSE" or "INCOME"
    val categories = if (type == "EXPENSE") CategoryRepository.expenseCategories else CategoryRepository.incomeCategories

    var selectedCategory by remember { mutableStateOf(categories.first()) }
    var amountExpression by remember { mutableStateOf("0") }
    var note by remember { mutableStateOf("") }
    var selectedAccount by remember { mutableStateOf("WECHAT") } // WECHAT, ALIPAY, BANK, CASH
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

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
            // Expense / Income Segmented Switch
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == "EXPENSE",
                    onClick = {
                        type = "EXPENSE"
                        selectedCategory = CategoryRepository.expenseCategories.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("支出")
                }
                SegmentedButton(
                    selected = type == "INCOME",
                    onClick = {
                        type = "INCOME"
                        selectedCategory = CategoryRepository.incomeCategories.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("收入")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Category Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(130.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = cat.id == selectedCategory.id
                    val color = parseHexColor(cat.colorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat }
                            .padding(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
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
                                tint = if (isSelected) Color.White else color
                            )
                        }
                        Text(
                            text = cat.nameZh,
                            fontSize = 11.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 2.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Amount Display & Account Chip Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        "WECHAT" to "微信",
                        "ALIPAY" to "支付宝",
                        "BANK" to "银行卡",
                        "CASH" to "现金"
                    ).forEach { (accKey, accName) ->
                        FilterChip(
                            selected = selectedAccount == accKey,
                            onClick = { selectedAccount = accKey },
                            label = { Text(accName, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }
                }

                Text(
                    text = "￥$amountExpression",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = parseHexColor(selectedCategory.colorHex),
                    textAlign = TextAlign.End
                )
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
                                val newCal = Calendar.getInstance().apply {
                                    set(y, m, d)
                                }
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
                    placeholder = { Text("添加备注（可选）...", fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            // Custom Keypad from ListenUiComponent
            NumericKeypad(
                onKeyPress = { key ->
                    if (amountExpression == "0" && key != ".") {
                        amountExpression = key
                    } else {
                        amountExpression += key
                    }
                },
                onDeletePress = {
                    amountExpression = if (amountExpression.length <= 1) {
                        "0"
                    } else {
                        amountExpression.dropLast(1)
                    }
                },
                onDonePress = {
                    val amountVal = amountExpression.toDoubleOrNull() ?: 0.0
                    if (amountVal > 0) {
                        onSaveTransaction(
                            type,
                            selectedCategory.id,
                            selectedCategory.nameZh,
                            selectedCategory.id,
                            selectedCategory.colorHex,
                            amountVal,
                            note,
                            selectedAccount,
                            selectedTimestamp
                        )
                        onDismiss()
                    }
                },
                showOperators = false
            )
        }
    }
}
