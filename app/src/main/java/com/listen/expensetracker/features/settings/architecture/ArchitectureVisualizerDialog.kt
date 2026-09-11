package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ArchitectureStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.*

/**
 * 架构全景交互式可视化对话框 (ArchitectureVisualizerDialog)。
 * 承载 MVI 单向流、Clean Architecture 四层拓扑与 Gradle Composite Build 模块全景。
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
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hub,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column {
                            Text(
                                text = ArchitectureStrings.TITLE.tr(lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = ArchitectureStrings.SUBTITLE.tr(lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                // 2. Tab 选择器
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                // 3. 拓扑图画板展示
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

                // 4. 选中节点下钻详情抽屉
                selectedNode?.let { node ->
                    NodeDetailInspectorCard(node = node, lang = lang)
                }

                // 5. 底部关闭按钮
                CommonButton(
                    text = AppStrings.BTN_DONE.tr(lang),
                    onClick = onDismiss,
                    style = CommonButtonStyle.Outlined,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun NodeDetailInspectorCard(
    node: ArchitectureNode,
    lang: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = node.titleKey.tr(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(node.colorHex).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = node.badge,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(node.colorHex),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = node.descKey.tr(lang),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 代表类列表
            Text(
                text = "${ArchitectureStrings.SECTION_EXAMPLES.tr(lang)}:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                node.examples.forEach { example ->
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // 架构守则列表
            Text(
                text = "${ArchitectureStrings.SECTION_RULES.tr(lang)}:",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary
            )
            node.rules.forEach { rule ->
                Text(
                    text = "• $rule",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
