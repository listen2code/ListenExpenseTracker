package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonSegmentedControl

/**
 * 统一记账弹窗页眉操作栏 (TransactionSheetHeader)。
 *
 * 采用极简通栏设计：
 * - 移除冗余的左侧关闭按钮（用户可通过下滑手势、点击蒙层或系统返回键轻松关闭）；
 * - 收支类型分段切换器通栏自适应（弹性伸缩）；
 * - 编辑模式下右侧提供醒目的红色删除按钮 [🗑️]；
 * - 严格锁定 40.dp 容器高度，新增与编辑模式弹窗高度 100% 像素级对齐，零视觉抖动。
 *
 * @param selectedType 当前选中的收支类型
 * @param onTypeChange 收支类型切换回调
 * @param typeOptions 选项文案列表
 * @param isEditMode 是否为编辑模式
 * @param onDeleteClick 点击删除按钮时的回调（触发二次确认弹窗）
 * @param modifier 外部修饰符
 */
@Composable
fun TransactionSheetHeader(
    selectedType: String,
    onTypeChange: (String) -> Unit,
    typeOptions: List<String>,
    isEditMode: Boolean,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 收支类型分段选择器（占据全部可用宽度）
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            CommonSegmentedControl(
                items = typeOptions,
                selectedIndex = if (selectedType == TransactionType.EXPENSE) 0 else 1,
                onIndexChange = { index ->
                    onTypeChange(if (index == 0) TransactionType.EXPENSE else TransactionType.INCOME)
                }
            )
        }

        // 2. 右侧操作区：编辑模式下提供删除按钮
        if (isEditMode) {
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
