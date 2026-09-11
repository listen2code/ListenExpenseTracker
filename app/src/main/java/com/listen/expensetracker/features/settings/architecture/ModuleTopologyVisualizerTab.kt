package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.model.AppDimens

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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Top: :app Feature Host
        TopologyNodeCard(
            node = appNode,
            isSelected = selectedNodeId == appNode.id,
            onClick = { onSelectNode(appNode) },
            lang = lang
        )

        ModuleLinkArrow(label = "includeBuild(':ListenArch') & includeBuild(':ListenUiComponent')")

        // Middle: :ListenArch & :ListenUiComponent (Composite Sub-repositories)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        ModuleLinkArrow(label = "Decoupled Clean Interfaces & SAF Contracts")

        // Bottom: System & Cloud Ecosystem
        TopologyNodeCard(
            node = sysNode,
            isSelected = selectedNodeId == sysNode.id,
            onClick = { onSelectNode(sysNode) },
            lang = lang
        )

        // 模块解耦红线提示
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountTree,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Rule 8 & 18: Common SDKs never depend on host business entities",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun ModuleLinkArrow(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ArrowDownward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
