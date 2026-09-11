package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Layer 1: Presentation
        TopologyNodeCard(
            node = presNode,
            isSelected = selectedNodeId == presNode.id,
            onClick = { onSelectNode(presNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "Calls Engines & Observes States ↓")

        // Layer 2: Domain & Engines
        TopologyNodeCard(
            node = domainNode,
            isSelected = selectedNodeId == domainNode.id,
            onClick = { onSelectNode(domainNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "Pure Functional Decisions & Rules ↓")

        // Layer 3: Data Layer
        TopologyNodeCard(
            node = dataNode,
            isSelected = selectedNodeId == dataNode.id,
            onClick = { onSelectNode(dataNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "SSOT & Platform Hardware Storage ↓")

        // Layer 4: Core & Platform
        TopologyNodeCard(
            node = coreNode,
            isSelected = selectedNodeId == coreNode.id,
            onClick = { onSelectNode(coreNode) },
            lang = lang
        )

        Spacer(modifier = Modifier.height(2.dp))

        // 底部架构原则胶囊
        FlowLoopIndicator(
            icon = Icons.Default.Shield,
            label = "Dependency Rule: Source code dependencies only point inward/downward",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.tertiary
        )
    }
}\n