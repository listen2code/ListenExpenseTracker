package com.listen.expensetracker.features.settings.architecture

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.ArchitectureStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.BaseScreenScaffold
import com.listen.uicomponent.components.CommonSegmentedControl

/**
 * 架构全景独立主画面 (ArchitectureVisualizerScreen)。
 * 从弹窗彻底解耦为全屏沉浸式页面，提供全宽拓扑画板与下钻代码守则。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchitectureVisualizerScreen(
    onBack: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(ArchitectureTab.MVI) }
    var selectedNode by remember { mutableStateOf<ArchitectureNode?>(ArchitectureModelProvider.getMviNodes().first()) }

    BaseScreenScaffold(
        title = ArchitectureStrings.TITLE.tr(lang),
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = AppDimens.SpaceLarge, vertical = AppDimens.SpaceSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
        ) {
            // 1. Tab 选择器 (MVI / Clean / Modules)
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

            // 2. 上半区：固定拓扑画板 (稳定置顶，同屏对照)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
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

            // 3. 下半区：独立可滚动卡片抽屉，深度查看选中节点的职责、代表类与架构守则
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
        }
    }
}
