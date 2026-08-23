package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.theme.parseHexColor

/**
 * Category Management Dialog allowing custom category creation and deletion.
 */
@Composable
fun CategoryManageDialog(
    type: String,
    onDismiss: () -> Unit,
    onCategoriesChanged: () -> Unit,
    lang: String = "zh"
) {
    var activeType by remember { mutableStateOf(type) }
    var showAddDialog by remember { mutableStateOf(false) }
    val currentCategories = if (activeType == "EXPENSE") CategoryRepository.expenseCategories else CategoryRepository.incomeCategories

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(StringsRes.get("settings_category_manage", lang), fontWeight = FontWeight.Bold, fontSize = AppDimens.TextHeader)
                Spacer(modifier = Modifier.height(AppDimens.SpaceSmall))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = activeType == "EXPENSE",
                        onClick = { activeType = "EXPENSE" },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) {
                        Text(StringsRes.get("type_expense", lang), fontSize = AppDimens.TextBody)
                    }
                    SegmentedButton(
                        selected = activeType == "INCOME",
                        onClick = { activeType = "INCOME" },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) {
                        Text(StringsRes.get("type_income", lang), fontSize = AppDimens.TextBody)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(currentCategories) { cat ->
                        val color = parseHexColor(cat.colorHex)
                        val catName = when (lang.lowercase()) {
                            "en" -> cat.nameEn
                            "ja" -> cat.nameJa
                            else -> cat.nameZh
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(AppDimens.CornerCard))
                                .padding(AppDimens.SpaceSmall)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = cat.icon, contentDescription = catName, tint = color, modifier = Modifier.size(20.dp))
                            }
                            Text(catName, fontSize = AppDimens.TextMicro, maxLines = 1, fontWeight = FontWeight.Medium)
                            if (!cat.isSystem) {
                                IconButton(
                                    onClick = {
                                        CategoryRepository.deleteCategory(cat.id)
                                        onCategoriesChanged()
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showAddDialog = true }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(AppDimens.IconSizeMedium))
                    Text(StringsRes.get("btn_add_transaction", lang))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("btn_done", lang))
            }
        }
    )

    if (showAddDialog) {
        AddCustomCategoryDialog(
            type = activeType,
            onDismiss = { showAddDialog = false },
            onCategoryAdded = {
                onCategoriesChanged()
                showAddDialog = false
            },
            lang = lang
        )
    }
}

@Composable
private fun AddCustomCategoryDialog(
    type: String,
    onDismiss: () -> Unit,
    onCategoryAdded: () -> Unit,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    val colorHexOptions = listOf("#EF4444", "#F59E0B", "#10B981", "#3B82F6", "#8B5CF6", "#EC4899", "#06B6D4", "#64748B")
    var selectedColor by remember { mutableStateOf(colorHexOptions.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringsRes.get("settings_category_manage", lang), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceLarge)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text(StringsRes.get("search_placeholder", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorHexOptions.forEach { hex ->
                        val col = parseHexColor(hex)
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color.White, modifier = Modifier.size(AppDimens.IconSizeMedium))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        CategoryRepository.addCustomCategory(
                            name = name.trim(),
                            type = type,
                            colorHex = selectedColor,
                            icon = Icons.Default.CardGiftcard
                        )
                        onCategoryAdded()
                    }
                }
            ) {
                Text(StringsRes.get("btn_save", lang), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("btn_cancel", lang))
            }
        }
    )
}
