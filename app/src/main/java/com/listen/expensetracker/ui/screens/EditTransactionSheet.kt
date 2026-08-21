package com.listen.expensetracker.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.IconButton
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
import com.listen.arch.data.db.TransactionEntity
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.ui.components.CategoryManageDialog
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
    var categoryVersion by remember { mutableIntStateOf(0) }

    val categories = remember(type, categoryVersion) {
        if (type == "EXPENSE") CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    }

    var selectedCategory by remember(categories) {
        mutableStateOf(
            categories.find { it.id == transaction.categoryId } ?: categories.first()
        )
    }
    var amountStr by remember { mutableStateOf(transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    val availableAccounts = remember { AccountRepository.getAllAccounts() }
    var accountType by remember {
        val initialAcc = if (availableAccounts.any { it.key == transaction.accountType }) {
            transaction.accountType
        } else {
            availableAccounts.firstOrNull()?.key ?: "CASH"
        }
        mutableStateOf(initialAcc)
    }
    var selectedTimestamp by remember { mutableLongStateOf(transaction.timestamp) }
    var showCategoryManageDialog by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val amountFontSize = when {
        amountStr.length > 10 -> 22.sp
        amountStr.length > 7 -> 26.sp
        else -> 30.sp
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            // 1. Expense / Income Switch (Consistent matching colors)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == "EXPENSE",
                    onClick = {
                        type = "EXPENSE"
                        selectedCategory = CategoryRepository.expenseCategories.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = ExpenseRed.copy(alpha = 0.15f),
                        activeContentColor = ExpenseRed,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("支出", fontWeight = if (type == "EXPENSE") FontWeight.Bold else FontWeight.Normal)
                }
                SegmentedButton(
                    selected = type == "INCOME",
                    onClick = {
                        type = "INCOME"
                        selectedCategory = CategoryRepository.incomeCategories.first()
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = IncomeGreen.copy(alpha = 0.15f),
                        activeContentColor = IncomeGreen,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("收入", fontWeight = if (type == "INCOME") FontWeight.Bold else FontWeight.Normal)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Prominent Full-Width Amount Card (Category on Left, Large Single-Line Amount on Right)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val catColor = parseHexColor(selectedCategory.colorHex)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(catColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedCategory.icon,
                                contentDescription = selectedCategory.nameZh,
                                tint = catColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = selectedCategory.nameZh,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = catColor
                        )
                    }

                    // Scaled Single-Line Amount
                    Text(
                        text = "￥${if (amountStr.isEmpty()) "0" else amountStr}",
                        fontSize = amountFontSize,
                        fontWeight = FontWeight.Bold,
                        color = if (type == "EXPENSE") ExpenseRed else IncomeGreen,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Smooth Horizontal Category Carousel with '+' Manage Categories Button
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(categories, key = { it.id }) { cat ->
                    val isSelected = cat.id == selectedCategory.id
                    val color = parseHexColor(cat.colorHex)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedCategory = cat }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                .border(
                                    width = if (isSelected) 2.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = cat.nameZh,
                                tint = if (isSelected) Color.White else color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = cat.nameZh,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.padding(top = 4.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // '+' Manage / Add Categories Button
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCategoryManageDialog = true }
                            .padding(vertical = 4.dp, horizontal = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Manage Categories",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "管理",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 4. Dedicated Account Selection Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "账户：",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 4.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(availableAccounts, key = { it.key }) { acc ->
                        FilterChip(
                            selected = accountType == acc.key,
                            onClick = { accountType = acc.key },
                            label = { Text(acc.nameZh, fontSize = 11.sp, fontWeight = if (accountType == acc.key) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // 5. Date Picker & Note Input Row
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
                    Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
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

            // 6. Numeric Keypad with Max Amount Limit Protection
            NumericKeypad(
                onKeyPress = { key ->
                    val potential = if (amountStr == "0" && key != ".") key else amountStr + key
                    val doubleVal = potential.toDoubleOrNull()
                    if (potential.length <= 10 && doubleVal != null && doubleVal <= 99999999.99) {
                        amountStr = potential
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

    if (showCategoryManageDialog) {
        CategoryManageDialog(
            initialType = type,
            onCategoryChanged = { categoryVersion++ },
            onDismiss = { showCategoryManageDialog = false }
        )
    }
}
