package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.model.AppDimens

/**
 * Clean Architecture 四层分层拓扑可视化 Tab (CleanLayersVisualizerTab)。
 * 直观呈现 Presentation -> Domain -> Data -> Core 的单向依赖与职责隔离。
 */
@Composable
fun CleanLayersVisualizerTab(
    selectedNodeId: String,
    onSelectNode: (ArchitectureNode) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val nodes = ArchitectureModelProvider.getCleanLayersNodes()
    val presNode = nodes.first { it.id == "presentation" }
    val domainNode = nodes.first { it.id == "domain" }
    val dataNode = nodes.first { it.id == "data" }
    val coreNode = nodes.first { it.id == "core" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Layer 1: Presentation
        TopologyNodeCard(
            node = presNode,
            isSelected = selectedNodeId == presNode.id,
            onClick = { onSelectNode(presNode) },
            lang = lang
        )

        LayerDependencyArrow(label = "Calls Engines & Observes States")

        // Layer 2: Domain & Engines
        TopologyNodeCard(
            node = domainNode,
            isSelected = selectedNodeId == domainNode.id,
            onClick = { onSelectNode(domainNode) },
            lang = lang
        )

        LayerDependencyArrow(label = "Pure Functional Decisions & Rules")

        // Layer 3: Data Layer
        TopologyNodeCard(
            node = dataNode,
            isSelected = selectedNodeId == dataNode.id,
            onClick = { onSelectNode(dataNode) },
            lang = lang
        )

        LayerDependencyArrow(label = "SSOT & Platform Hardware Storage")

        // Layer 4: Core & Platform
        TopologyNodeCard(
            node = coreNode,
            isSelected = selectedNodeId == coreNode.id,
            onClick = { onSelectNode(coreNode) },
            lang = lang
        )

        // 底部架构原则胶囊
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
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
                    imageVector = Icons.Default.Shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dependency Rule: Source code dependencies only point inward/downward",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun LayerDependencyArrow(
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
