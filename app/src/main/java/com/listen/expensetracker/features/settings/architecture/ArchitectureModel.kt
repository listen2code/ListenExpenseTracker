package com.listen.expensetracker.features.settings.architecture

import com.listen.expensetracker.data.i18n.ArchitectureStrings

/**
 * 架构全景可视化模型体系 (ArchitectureModel)。
 * 纯数据结构承载 MVI、Clean Architecture 及 Gradle Composite 拓扑各节点元数据。
 */
enum class ArchitectureTab {
    MVI,
    CLEAN,
    MODULES
}

data class ArchitectureNode(
    val id: String,
    val titleKey: String,
    val subtitle: String,
    val descKey: String,
    val examples: List<String>,
    val rules: List<String>,
    val badge: String,
    val colorHex: Long
)

object ArchitectureModelProvider {

    fun getMviNodes(): List<ArchitectureNode> = listOf(
        ArchitectureNode(
            id = "view",
            titleKey = ArchitectureStrings.MVI_VIEW_TITLE,
            subtitle = "Composable Tree & StateHolder",
            descKey = ArchitectureStrings.MVI_VIEW_DESC,
            examples = listOf("TransactionsScreen.kt", "SettingsScreen.kt", "CategoryBudgetModalDialog.kt"),
            rules = listOf("禁止直接调用 ViewModel 修改状态", "遵循 Rule 17 StateHolder 模式隔离框架状态", "纯函数式只读渲染"),
            badge = "Render",
            colorHex = 0xFF10B981L // Emerald
        ),
        ArchitectureNode(
            id = "intent",
            titleKey = ArchitectureStrings.MVI_INTENT_TITLE,
            subtitle = "Sealed Interface Action",
            descKey = ArchitectureStrings.MVI_INTENT_DESC,
            examples = listOf("TransactionsIntent", "SettingsIntent", "StatisticsIntent"),
            rules = listOf("所有用户操作必须建模为密封接口", "意图天然支持事件溯源与测试回放", "不可变携带参数"),
            badge = "Action",
            colorHex = 0xFF3B82F6L // Blue
        ),
        ArchitectureNode(
            id = "viewmodel",
            titleKey = ArchitectureStrings.MVI_VM_TITLE,
            subtitle = "Decision Center & Delegates",
            descKey = ArchitectureStrings.MVI_VM_DESC,
            examples = listOf("SettingsViewModel", "TransactionsViewModel", "SettingsNotificationDelegate"),
            rules = listOf("单文件红线 <= 250 行，复杂业务必须拆解为 Delegate", "只通过原子 copy 产出新 UiState", "禁止对外暴露可变流"),
            badge = "Reducer",
            colorHex = 0xFF8B5CF6L // Violet
        ),
        ArchitectureNode(
            id = "state",
            titleKey = ArchitectureStrings.MVI_STATE_TITLE,
            subtitle = "Immutable StateFlow (SSOT)",
            descKey = ArchitectureStrings.MVI_STATE_DESC,
            examples = listOf("SettingsUiState", "TransactionsViewState", "StatisticsUiState"),
            rules = listOf("全部字段强制为 val 不可变定义", "单一数据源真实快照，杜绝状态撕裂", "利用 data class copy 机制"),
            badge = "SSOT",
            colorHex = 0xFFF59E0BL // Amber
        ),
        ArchitectureNode(
            id = "effect",
            titleKey = ArchitectureStrings.MVI_EFFECT_TITLE,
            subtitle = "Channel<CommonUiEffect>",
            descKey = ArchitectureStrings.MVI_EFFECT_DESC,
            examples = listOf("CommonUiEffect.ShowToast", "SettingsEffect.LaunchGoogleSignIn", "ScrollToTop"),
            rules = listOf("基于缓冲 Channel 传输单次瞬态副作用", "配置变更与旋转屏幕绝不重放", "与持久 State 严格解耦"),
            badge = "Transient",
            colorHex = 0xFFEC4899L // Pink
        )
    )

    fun getCleanLayersNodes(): List<ArchitectureNode> = listOf(
        ArchitectureNode(
            id = "presentation",
            titleKey = ArchitectureStrings.CLEAN_PRESENTATION_TITLE,
            subtitle = "UI Screens, Themes & MVI",
            descKey = ArchitectureStrings.CLEAN_PRESENTATION_DESC,
            examples = listOf("presentation.screens.*", "uicomponent.theme.*", "uicomponent.components.*"),
            rules = listOf("展示层仅向下依赖 Domain 与 Data 契约", "无状态纯声明式渲染", "深浅色主题自适应"),
            badge = "Layer 1",
            colorHex = 0xFF3B82F6L // Blue
        ),
        ArchitectureNode(
            id = "domain",
            titleKey = ArchitectureStrings.CLEAN_DOMAIN_TITLE,
            subtitle = "Pure Calculation Engines",
            descKey = ArchitectureStrings.CLEAN_DOMAIN_DESC,
            examples = listOf("BudgetAlertGuard.kt", "FinancialInsightEngine.kt", "CategoryBudgetEngine.kt"),
            rules = listOf("纯函数式无副作用算法", "100% 隔离 Android SDK，毫秒级快速单测", "业务核心决策中心"),
            badge = "Layer 2",
            colorHex = 0xFF10B981L // Emerald
        ),
        ArchitectureNode(
            id = "data",
            titleKey = ArchitectureStrings.CLEAN_DATA_TITLE,
            subtitle = "Room SQLite, DataStore & SAF",
            descKey = ArchitectureStrings.CLEAN_DATA_DESC,
            examples = listOf("AppDatabase.kt", "TransactionDao.kt", "GoogleDriveSyncManager.kt"),
            rules = listOf("本地优先 (Local-First) 架构基石", "Room InvalidationTracker 响应式热流", "增量哈希校验避免冗余上传"),
            badge = "Layer 3",
            colorHex = 0xFFF59E0BL // Amber
        ),
        ArchitectureNode(
            id = "core",
            titleKey = ArchitectureStrings.CLEAN_CORE_TITLE,
            subtitle = "System Services, APM & Platform",
            descKey = ArchitectureStrings.CLEAN_CORE_DESC,
            examples = listOf("LocalNotificationManager.kt", "BiometricSecurityManager.kt", "ApmLogger.kt"),
            rules = listOf("严格适配 Android 13+ 规范", "500条环形内存日志零GC负担", "硬件时钟防回拨防窥保护"),
            badge = "Layer 4",
            colorHex = 0xFF6366F1L // Indigo
        )
    )

    fun getModuleNodes(): List<ArchitectureNode> = listOf(
        ArchitectureNode(
            id = "mod_app",
            titleKey = ArchitectureStrings.MOD_APP_TITLE,
            subtitle = "Top-level Feature Application",
            descKey = ArchitectureStrings.MOD_APP_DESC,
            examples = listOf("app/build.gradle.kts", "MainActivity.kt", "MainApp.kt"),
            rules = listOf("全工程顶层宿主，装配所有功能特性", "遵循 Gradle Composite Build 机制协同编译", "单文件红线严格限制 <= 250 行"),
            badge = "App Host",
            colorHex = 0xFF10B981L // Emerald
        ),
        ArchitectureNode(
            id = "mod_arch",
            titleKey = ArchitectureStrings.MOD_ARCH_TITLE,
            subtitle = "includeBuild(':ListenArch')",
            descKey = ArchitectureStrings.MOD_ARCH_DESC,
            examples = listOf("BaseMviViewModel.kt", "StringsRes.kt", "CommonUiEffect.kt"),
            rules = listOf("通用架构基石库，绝对零业务依赖", "跨项目复用 MVI 与多语言引擎", "独立子仓库版本化维护"),
            badge = "Foundation",
            colorHex = 0xFF8B5CF6L // Violet
        ),
        ArchitectureNode(
            id = "mod_ui",
            titleKey = ArchitectureStrings.MOD_UI_TITLE,
            subtitle = "includeBuild(':ListenUiComponent')",
            descKey = ArchitectureStrings.MOD_UI_DESC,
            examples = listOf("SurfaceCard.kt", "CommonButton.kt", "CommonSwitchRow.kt"),
            rules = listOf("纯粹通用的 Compose 设计系统组件", "严禁引用任何业务数据实体 (Rule 18)", "全量覆盖深浅色主题"),
            badge = "Design System",
            colorHex = 0xFF3B82F6L // Blue
        ),
        ArchitectureNode(
            id = "mod_sys",
            titleKey = ArchitectureStrings.MOD_SYS_TITLE,
            subtitle = "External Platform & Cloud APIs",
            descKey = ArchitectureStrings.MOD_SYS_DESC,
            examples = listOf("NotificationManagerCompat", "Google Drive REST v3", "StorageAccessFramework"),
            rules = listOf("通过隔离接口交互，保障无云服务下的完全可用", "遵循系统电池白名单与低电耗规范", "不引入重型第三方全家桶"),
            badge = "Ecosystem",
            colorHex = 0xFF64748BL // Slate
        )
    )
}
