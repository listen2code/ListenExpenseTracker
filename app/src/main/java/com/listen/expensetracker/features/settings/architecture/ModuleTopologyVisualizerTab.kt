package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Gradle Composite Build 模块解耦拓扑可视化 Tab (ModuleTopologyVisualizerTab)。
 * 展示 :app 宿主、:ListenArch 架构库与 :ListenUiComponent 组件库的单向依赖拓扑。
 */
@Composable
fun ModuleTopologyVisualizerTab(
    selectedNodeId: String,
    onSelectNode: (ArchitectureNode) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val nodes = ArchitectureModelProvider.getModuleNodes()
    val appNode = nodes.first { it.id == "mod_app" }
    val archNode = nodes.first { it.id == "mod_arch" }
    val uiNode = nodes.first { it.id == "mod_ui" }
    val sysNode = nodes.first { it.id == "mod_sys" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        // Top: :app Feature Host
        TopologyNodeCard(
            node = appNode,
            isSelected = selectedNodeId == appNode.id,
            onClick = { onSelectNode(appNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "includeBuild(':ListenArch') & includeBuild(':ListenUiComponent') ↓")

        // Middle: :ListenArch & :ListenUiComponent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TopologyNodeCard(
                    node = archNode,
                    isSelected = selectedNodeId == archNode.id,
                    onClick = { onSelectNode(archNode) },
                    lang = lang
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                TopologyNodeCard(
                    node = uiNode,
                    isSelected = selectedNodeId == uiNode.id,
                    onClick = { onSelectNode(uiNode) },
                    lang = lang
                )
            }
        }

        FlowConnectorArrow(label = "Decoupled Clean Interfaces & SAF Contracts ↓")

        // Bottom: System & Cloud Ecosystem
        TopologyNodeCard(
            node = sysNode,
            isSelected = selectedNodeId == sysNode.id,
            onClick = { onSelectNode(sysNode) },
            lang = lang
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 模块解耦红线提示
        FlowLoopIndicator(
            icon = Icons.Default.AccountTree,
            label = "Rule 8 & 18: Common SDKs never depend on host business entities",
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.secondary
        )
    }
}\n