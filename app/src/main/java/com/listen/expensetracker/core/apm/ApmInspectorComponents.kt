package com.listen.expensetracker.core.apm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.theme.ListenTheme

/**
 * APM 日志面板顶部紧凑操作按钮。
 */
@Composable
fun ApmActionButton(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    val contentColor = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        shape = RoundedCornerShape(AppDimens.CornerButton),
        color = bg,
        modifier = modifier
            .clip(RoundedCornerShape(AppDimens.CornerButton))
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
            Icon(imageVector = icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(13.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = contentColor, maxLines = 1)
        }
    }
}

/**
 * APM 频道筛选轻量芯片。
 */
@Composable
fun ApmChannelChip(
    label: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (count >= 0) "$label ($count)" else label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = textColor
        )
    }
}

/**
 * APM 日志频道与级别轻量色标。
 */
@Composable
fun ApmLogBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}

/**
 * APM 悬浮气泡图标与未读错误红点。
 */
@Composable
fun ApmBubbleContent(errorCount: Int) {
    Surface(
        shape = androidx.compose.foundation.shape.CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp,
        modifier = Modifier.size(54.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            androidx.compose.material3.BadgedBox(
                badge = {
                    if (errorCount > 0) {
                        androidx.compose.material3.Badge(
                            containerColor = com.listen.uicomponent.theme.ExpenseRed,
                            contentColor = Color.White
                        ) {
                            Text(if (errorCount > 99) "99+" else "$errorCount", fontSize = 9.sp)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "APM Bubble",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ApmInspectorComponentsPreview() {
    ListenTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApmActionButton(
                    icon = Icons.Default.BugReport,
                    label = "Export",
                    isPrimary = true,
                    onClick = {}
                )
                ApmActionButton(
                    icon = Icons.Default.BugReport,
                    label = "Clear",
                    isPrimary = false,
                    onClick = {}
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApmChannelChip(label = "ALL", count = 12, isSelected = true, onClick = {})
                ApmChannelChip(label = "SYNC", count = 3, isSelected = false, onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ApmLogBadge(text = "INFO", color = Color(0xFF3B82F6))
                ApmLogBadge(text = "WARN", color = Color(0xFFF59E0B))
                ApmLogBadge(text = "ERROR", color = Color(0xFFEF4444))
            }
            ApmBubbleContent(errorCount = 3)
        }
    }
}
