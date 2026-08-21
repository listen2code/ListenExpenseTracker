package com.listen.expensetracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.Category
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor

@Composable
fun CategoryManageDialog(
    initialType: String = "EXPENSE",
    onCategoryChanged: () -> Unit,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    var selectedType by remember { mutableStateOf(initialType) }
    var newCategoryName by remember { mutableStateOf("") }

    val categories = if (selectedType == "EXPENSE") CategoryRepository.expenseCategories else CategoryRepository.incomeCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("记账分类标签管理", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedType == "EXPENSE",
                        onClick = { selectedType = "EXPENSE" },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("支出分类", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = selectedType == "INCOME",
                        onClick = { selectedType = "INCOME" },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("收入分类", fontSize = 12.sp)
                    }
                }

                // Add Category Row with unconstrained height for full placeholder visibility
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        placeholder = { Text("新分类名称（如：数码）", fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                CategoryRepository.addCustomCategory(
                                    name = newCategoryName.trim(),
                                    type = selectedType,
                                    colorHex = if (selectedType == "EXPENSE") "#F59E0B" else "#10B981"
                                )
                                newCategoryName = ""
                                onCategoryChanged()
                            }
                        }
                    ) {
                        Text(StringsRes.get("btn_save", lang), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Text("已有分类列表（可上下滑动）：", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Smooth Scrollable LazyColumn with height cap
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        val color = parseHexColor(cat.colorHex)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(CircleShape)
                                        .background(color.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = cat.icon,
                                        contentDescription = cat.nameZh,
                                        tint = color,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(cat.nameZh, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }

                            if (!cat.isSystem) {
                                IconButton(
                                    onClick = {
                                        CategoryRepository.deleteCategory(cat.id)
                                        onCategoryChanged()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                Text("系统预设", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("btn_done", lang))
            }
        }
    )
}
