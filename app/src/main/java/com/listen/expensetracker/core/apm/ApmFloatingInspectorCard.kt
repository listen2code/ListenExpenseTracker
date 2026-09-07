package com.listen.expensetracker.core.apm

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.components.SearchBarInput

/**
 * 全局 APM 悬浮窗展开面板卡片 (ApmFloatingInspectorCard)。
 * 优化排版布局：顶栏按钮紧凑居中杜绝截断，频道轻量芯片与通用搜索组件对齐，日志列表清晰规整。
 */
@Composable
fun ApmFloatingInspectorCard(
    logs: List<LogEntryUi>,
    onClearLogs: () -> Unit,
    onExportLogs: () -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "zh"
) {
    var selectedChannel by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, selectedChannel, searchQuery) {
        logs.filter { entry ->
            val channelMatch = selectedChannel == null || entry.channelName == selectedChannel
            val queryMatch = searchQuery.isBlank() ||
                    entry.message.contains(searchQuery, ignoreCase = true) ||
                    entry.tag.contains(searchQuery, ignoreCase = true) ||
                    (entry.traceId?.contains(searchQuery, ignoreCase = true) == true)
            channelMatch && queryMatch
        }
    }

    val channels = remember { listOf("APP", "DB", "SYNC", "CRASH") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        modifier = modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.82f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            // 1. 顶部操作栏（严密约束防止标题与按钮碰撞）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "APM",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = AppStrings.APM_LOGS_TITLE.tr(lang),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ApmActionButton(
                        icon = Icons.Default.DeleteSweep,
                        label = AppStrings.APM_BTN_CLEAR.tr(lang),
                        isPrimary = false,
                        onClick = onClearLogs
                    )
                    ApmActionButton(
                        icon = Icons.Default.Share,
                        label = AppStrings.APM_BTN_EXPORT.tr(lang),
                        isPrimary = true,
                        onClick = onExportLogs
                    )
                    IconButton(
                        onClick = onCollapse,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Collapse",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. 频道筛选轻量芯片列表
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    ApmChannelChip(
                        label = "ALL",
                        count = logs.size,
                        isSelected = selectedChannel == null,
                        onClick = { selectedChannel = null }
                    )
                }
                items(channels) { channel ->
                    val count = logs.count { it.channelName == channel }
                    ApmChannelChip(
                        label = channel,
                        count = count,
                        isSelected = selectedChannel == channel,
                        onClick = { selectedChannel = channel }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. 通用现代化搜索过滤条 (基于通用 SearchBarInput 统一视觉比例)
            SearchBarInput(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = AppStrings.APM_SEARCH_PLACEHOLDER.tr(lang),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 4. 日志列表流
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = AppStrings.APM_EMPTY_LOGS.tr(lang),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        ApmFloatingLogRow(log)
                    }
                }
            }
        }
    }
}
