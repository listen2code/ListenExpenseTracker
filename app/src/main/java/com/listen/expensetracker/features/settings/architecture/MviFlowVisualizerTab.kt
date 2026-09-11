package com.listen.expensetracker.features.settings.architecture

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.model.AppDimens

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
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 1. View / UI
        TopologyNodeCard(
            node = viewNode,
            isSelected = selectedNodeId == viewNode.id,
            onClick = { onSelectNode(viewNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "User Dispatches Intent")

        // 2. Intent
        TopologyNodeCard(
            node = intentNode,
            isSelected = selectedNodeId == intentNode.id,
            onClick = { onSelectNode(intentNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "handleIntent()")

        // 3. ViewModel & Delegates
        TopologyNodeCard(
            node = vmNode,
            isSelected = selectedNodeId == vmNode.id,
            onClick = { onSelectNode(vmNode) },
            lang = lang
        )

        FlowConnectorArrow(label = "State Reduction & Effects")

        // 4. State & Effect 并列分支
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
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

        // 5. 循环反馈引导指示条
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                    imageVector = Icons.Default.SyncAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Recompose UI Tree (StateFlow.collectAsStateWithLifecycle)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun TopologyNodeCard(
    node: ArchitectureNode,
    isSelected: Boolean,
    onClick: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        label = "border"
    )
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = node.titleKey.tr(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = node.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color(node.colorHex).copy(alpha = 0.18f)
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
    }
}

@Composable
fun FlowConnectorArrow(
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
