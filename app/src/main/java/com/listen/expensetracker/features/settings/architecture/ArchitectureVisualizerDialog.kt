package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ArchitectureStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.*

/**
 * 架构全景交互式可视化对话框 (ArchitectureVisualizerDialog)。
 * 采用固定上下分区分割布局：上半区固定拓扑画板，下半区独立滚动下钻详情。
 */
@Composable
fun ArchitectureVisualizerDialog(
    onDismiss: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ArchitectureTab.MVI) }
    var selectedNode by remember { mutableStateOf<ArchitectureNode?>(ArchitectureModelProvider.getMviNodes().first()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SurfaceCard(
            cornerRadius = AppDimens.CornerCard,
            contentPadding = AppDimens.SpaceStandard,
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
            ) {
                // 1. Header (标题 + 简述 + 关闭图标)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = ArchitectureStrings.TITLE.tr(lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = ArchitectureStrings.SUBTITLE.tr(lang),
                                fontSize = 9.5.sp,
                                lineHeight = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }

                // 2. Tab 选择器 (MVI / Clean / Modules)
                val tabs = listOf(
                    ArchitectureStrings.TAB_MVI.tr(lang),
                    ArchitectureStrings.TAB_CLEAN.tr(lang),
                    ArchitectureStrings.TAB_MODULES.tr(lang)
                )
                CommonSegmentedControl(
                    items = tabs,
                    selectedIndex = currentTab.ordinal,
                    onIndexChange = { index ->
                        currentTab = ArchitectureTab.entries[index]
                        selectedNode = when (currentTab) {
                            ArchitectureTab.MVI -> ArchitectureModelProvider.getMviNodes().first()
                            ArchitectureTab.CLEAN -> ArchitectureModelProvider.getCleanLayersNodes().first()
                            ArchitectureTab.MODULES -> ArchitectureModelProvider.getModuleNodes().first()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // 3. 上半区：固定拓扑画板 (不随详情卡片滚动，同屏对照)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp)
                ) {
                    when (currentTab) {
                        ArchitectureTab.MVI -> MviFlowVisualizerTab(
                            selectedNodeId = selectedNode?.id.orEmpty(),
                            onSelectNode = { selectedNode = it },
                            lang = lang
                        )
                        ArchitectureTab.CLEAN -> CleanLayersVisualizerTab(
                            selectedNodeId = selectedNode?.id.orEmpty(),
                            onSelectNode = { selectedNode = it },
                            lang = lang
                        )
                        ArchitectureTab.MODULES -> ModuleTopologyVisualizerTab(
                            selectedNodeId = selectedNode?.id.orEmpty(),
                            onSelectNode = { selectedNode = it },
                            lang = lang
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // 4. 下半区：独立可滚动卡片抽屉，查看所选节点的职责理念、代表类、架构红线守则
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    selectedNode?.let { node ->
                        ArchitectureNodeDetailDrawer(
                            node = node,
                            lang = lang,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }

                // 5. 底部操作按钮
                CommonButton(
                    text = AppStrings.BTN_DONE.tr(lang),
                    onClick = onDismiss,
                    style = CommonButtonStyle.Outlined,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimens.ButtonHeightCompact)
                )
            }
        }
    }
}
