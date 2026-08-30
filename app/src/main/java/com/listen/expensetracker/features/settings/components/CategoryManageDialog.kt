package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonEditText
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.parseHexColor

/**
 * Category Management Dialog allowing custom category creation and deletion using standardized ListenUiComponent elements.
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
    var refreshKey by remember { mutableIntStateOf(0) }

    val currentCategories = remember(activeType, refreshKey) {
        if (activeType == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.SETTINGS_CATEGORY_MANAGE.tr(lang),
        confirmButton = {
            CommonButton(
                text = AppStrings.BTN_ADD_TRANSACTION.tr(lang),
                onClick = { showAddDialog = true },
                style = CommonButtonStyle.Primary,
                icon = {
                    Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(AppDimens.IconSizeMedium))
                }
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.BTN_DONE.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Text
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = activeType == TransactionType.EXPENSE,
                    onClick = { activeType = TransactionType.EXPENSE },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) {
                    CommonText(AppStrings.TYPE_EXPENSE.tr(lang), fontSize = AppDimens.TextBody)
                }
                SegmentedButton(
                    selected = activeType == TransactionType.INCOME,
                    onClick = { activeType = TransactionType.INCOME },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) {
                    CommonText(AppStrings.TYPE_INCOME.tr(lang), fontSize = AppDimens.TextBody)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                modifier = Modifier.height(200.dp)
            ) {
                items(currentCategories, key = { it.id }) { cat ->
                    val color = parseHexColor(cat.colorHex)
                    val catName = cat.getDisplayName(lang)
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
                        CommonText(catName, fontSize = AppDimens.TextMicro, maxLines = 1, fontWeight = FontWeight.Medium)
                        if (!cat.isSystem) {
                            IconButton(
                                onClick = {
                                    CategoryRepository.deleteCategory(cat.id)
                                    refreshKey++
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
    }

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

    CommonDialog(
        onDismissRequest = onDismiss,
        title = AppStrings.SETTINGS_CATEGORY_MANAGE.tr(lang),
        confirmButton = {
            CommonButton(
                text = AppStrings.BTN_SAVE.tr(lang),
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
                },
                enabled = name.isNotBlank(),
                style = CommonButtonStyle.Primary
            )
        },
        dismissButton = {
            CommonButton(
                text = AppStrings.BTN_CANCEL.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Text
            )
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceLarge)) {
            CommonEditText(
                value = name,
                onValueChange = { name = it },
                placeholder = AppStrings.SEARCH_PLACEHOLDER.tr(lang),
                singleLine = true
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
    }
}
