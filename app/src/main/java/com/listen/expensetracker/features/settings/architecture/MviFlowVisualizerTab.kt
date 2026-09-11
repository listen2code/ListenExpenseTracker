package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * MVI 单向数据流拓扑可视化 Tab (MviFlowVisualizerTab)。
 * 直观呈现 View -> Intent -> ViewModel -> State/Effect -> Render 的单向闭环。
 */
@Composable
fun MviFlowVisualizerTab(
    selectedNodeId: String,
    onSelectNode: (ArchitectureNode) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val nodes = ArchitectureModelProvider.getMviNodes()
    val viewNode = nodes.first { it.id == "view" }
    val intentNode = nodes.first { it.id == "intent" }
    val vmNode = nodes.first { it.id == "viewmodel" }
    val stateNode = nodes.first { it.id == "state" }
    val effectNode = nodes.first { it.id == "effect" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // 1. View / UI
        TopologyNodeCard(
            node = viewNode,
            isSelected = selectedNodeId == viewNode.id,
            onClick = { onSelectNode(viewNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "User Dispatches Intent ↓")

        // 2. Intent
        TopologyNodeCard(
            node = intentNode,
            isSelected = selectedNodeId == intentNode.id,
            onClick = { onSelectNode(intentNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "handleIntent() ↓")

        // 3. ViewModel & Delegates
        TopologyNodeCard(
            node = vmNode,
            isSelected = selectedNodeId == vmNode.id,
            onClick = { onSelectNode(vmNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "State Reduction & Effects ↓")

        // 4. State & Effect 并列分支
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                TopologyNodeCard(
                    node = stateNode,
                    isSelected = selectedNodeId == stateNode.id,
                    onClick = { onSelectNode(stateNode) },
                    lang = lang
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                TopologyNodeCard(
                    node = effectNode,
                    isSelected = selectedNodeId == effectNode.id,
                    onClick = { onSelectNode(effectNode) },
                    lang = lang
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        // 5. 循环反馈闭环指示条
        FlowLoopIndicator(
            icon = Icons.Default.SyncAlt,
            label = "Recompose UI Tree (StateFlow.collectAsStateWithLifecycle)",
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
            contentColor = MaterialTheme.colorScheme.primary
        )
    }
}
