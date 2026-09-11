package com.listen.expensetracker.data.i18n

import com.listen.arch.i18n.StringsRes

/**
 * 架构全景可视化多语言字典 (ArchitectureStrings)。
 * 集中管理 MVI 单向流、Clean Architecture 分层与模块解耦拓扑的节点文案。
 */
object ArchitectureStrings {

    const val TITLE = "arch_visualizer_title"
    const val SUBTITLE = "arch_visualizer_subtitle"
    const val TAB_MVI = "arch_tab_mvi"
    const val TAB_CLEAN = "arch_tab_clean"
    const val TAB_MODULES = "arch_tab_modules"

    const val SECTION_RESPONSIBILITY = "arch_sec_responsibility"
    const val SECTION_EXAMPLES = "arch_sec_examples"
    const val SECTION_RULES = "arch_sec_rules"
    const val TIP_SELECT_NODE = "arch_tip_select_node"

    // MVI 节点
    const val MVI_VIEW_TITLE = "arch_mvi_view_title"
    const val MVI_VIEW_DESC = "arch_mvi_view_desc"
    const val MVI_INTENT_TITLE = "arch_mvi_intent_title"
    const val MVI_INTENT_DESC = "arch_mvi_intent_desc"
    const val MVI_VM_TITLE = "arch_mvi_vm_title"
    const val MVI_VM_DESC = "arch_mvi_vm_desc"
    const val MVI_STATE_TITLE = "arch_mvi_state_title"
    const val MVI_STATE_DESC = "arch_mvi_state_desc"
    const val MVI_EFFECT_TITLE = "arch_mvi_effect_title"
    const val MVI_EFFECT_DESC = "arch_mvi_effect_desc"

    // Clean Architecture 节点
    const val CLEAN_PRESENTATION_TITLE = "arch_clean_pres_title"
    const val CLEAN_PRESENTATION_DESC = "arch_clean_pres_desc"
    const val CLEAN_DOMAIN_TITLE = "arch_clean_domain_title"
    const val CLEAN_DOMAIN_DESC = "arch_clean_domain_desc"
    const val CLEAN_DATA_TITLE = "arch_clean_data_title"
    const val CLEAN_DATA_DESC = "arch_clean_data_desc"
    const val CLEAN_CORE_TITLE = "arch_clean_core_title"
    const val CLEAN_CORE_DESC = "arch_clean_core_desc"

    // 模块拓扑节点
    const val MOD_APP_TITLE = "arch_mod_app_title"
    const val MOD_APP_DESC = "arch_mod_app_desc"
    const val MOD_ARCH_TITLE = "arch_mod_arch_title"
    const val MOD_ARCH_DESC = "arch_mod_arch_desc"
    const val MOD_UI_TITLE = "arch_mod_ui_title"
    const val MOD_UI_DESC = "arch_mod_ui_desc"
    const val MOD_SYS_TITLE = "arch_mod_sys_title"
    const val MOD_SYS_DESC = "arch_mod_sys_desc"

    fun init() {
        StringsRes.registerAppStrings("zh", zhMap)
        StringsRes.registerAppStrings("en", enMap)
        StringsRes.registerAppStrings("ja", jaMap)
    }

    private val zhMap = mapOf(
        TITLE to "架构设计全景可视化",
        SUBTITLE to "系统 MVI 响应式流、Clean 分层与模块解耦拓扑",
        TAB_MVI to "MVI 响应式流",
        TAB_CLEAN to "Clean Architecture",
        TAB_MODULES to "模块解耦拓扑",
        SECTION_RESPONSIBILITY to "核心职责与设计理念",
        SECTION_EXAMPLES to "工程代表类 / 关键接口",
        SECTION_RULES to "架构红线与约束守则",
        TIP_SELECT_NODE to "轻按上方拓扑节点，查看架构规范与代码下钻",

        MVI_VIEW_TITLE to "View / 纯函数 UI 渲染",
        MVI_VIEW_DESC to "纯 Compose 组件树，只依赖不可变 State 快照，用户交互仅向下游派发 Intent，不直接执行业务逻辑与状态变更。",
        MVI_INTENT_TITLE to "Intent / 唯一用户意图",
        MVI_INTENT_DESC to "Sealed Interface 密封类型系统，所有 UI 事件必须包装为强类型意图，便于日志追踪、自动化测试与状态回放。",
        MVI_VM_TITLE to "ViewModel & 业务委托",
        MVI_VM_DESC to "唯一的业务决策中心，通过 Delegate 分解复杂职责，单向消费 Intent，以原子化 copy 产出新的 UiState。",
        MVI_STATE_TITLE to "UiState / 不可变单向快照",
        MVI_STATE_DESC to "单一真实事实来源 (SSOT)，以只读 StateFlow 暴露，内部属性全部为 val，杜绝外部直接篡改与状态撕裂。",
        MVI_EFFECT_TITLE to "CommonUiEffect / 瞬态事件通道",
        MVI_EFFECT_DESC to "基于 Channel<UiEffect> 独立派发 Toast、导航跳转、Snackbar 撤销等一次性事件，配置变更时不重复触发。",

        CLEAN_PRESENTATION_TITLE to "展示层 (Presentation Layer)",
        CLEAN_PRESENTATION_DESC to "包含各 Feature Screen、无状态 UI 组件与 StateHolder，遵从 MVI 模式，严格隔离领域与数据层细节。",
        CLEAN_DOMAIN_TITLE to "领域引擎层 (Domain & Engines)",
        CLEAN_DOMAIN_DESC to "纯函数业务计算核心（预算超支预警、智能财务诊断、周期履约计算），100% 零 Android SDK 强依赖，秒级全量单测。",
        CLEAN_DATA_TITLE to "数据与持久化层 (Data Layer)",
        CLEAN_DATA_DESC to "Room SQLite 本地优先数据库、DataStore 响应式偏好流、SAF 文件导出以及 Google Drive 云端快照存储。",
        CLEAN_CORE_TITLE to "系统与能力层 (Core & Platform)",
        CLEAN_CORE_DESC to "系统通知矩阵 (Channel)、桌面微件 (AppWidget 2.0)、生物识别隐私防窥 (Biometric) 以及 APM 性能监控底座。",

        MOD_APP_TITLE to ":app (业务宿主模块)",
        MOD_APP_DESC to "顶级业务容器，包含记账、统计、设置三屏全量页面，编排全局状态机，集成 AndroidX 依赖与应用配置。",
        MOD_ARCH_TITLE to ":ListenArch (通用架构底座)",
        MOD_ARCH_DESC to "独立 Composite Build 模块，提供 MVI 核心基类、多语言 i18n 动态注册中心与轻量同步状态定义，零业务耦合。",
        MOD_UI_TITLE to ":ListenUiComponent (设计系统库)",
        MOD_UI_DESC to "通用 Compose 纯 UI 库，定义 SurfaceCard、CommonButton、颜色令牌与主题规范，严禁反向依赖宿主业务模型。",
        MOD_SYS_TITLE to "系统与外部云服务 (External Ecosystem)",
        MOD_SYS_DESC to "Android System OS (通知、微件、安全)、Google Drive REST API 以及本地文件系统 SAF，通过接口完全解耦隔离。"
    )

    private val enMap = mapOf(
        TITLE to "Architecture Visualizer",
        SUBTITLE to "MVI Reactive Flow, Clean Architecture & Module Topology",
        TAB_MVI to "MVI Reactive Flow",
        TAB_CLEAN to "Clean Architecture",
        TAB_MODULES to "Module Topology",
        SECTION_RESPONSIBILITY to "Core Responsibilities",
        SECTION_EXAMPLES to "Representative Classes / Interfaces",
        SECTION_RULES to "Architecture Rules & Constraints",
        TIP_SELECT_NODE to "Tap any node above to inspect architectural rules & details",

        MVI_VIEW_TITLE to "View / Pure UI Rendering",
        MVI_VIEW_DESC to "Stateless Compose UI tree relying solely on immutable State snapshot, emitting Intents upon user actions.",
        MVI_INTENT_TITLE to "Intent / Unified User Intent",
        MVI_INTENT_DESC to "Sealed interface hierarchy ensuring all events are typed, traceable, testable, and replayable.",
        MVI_VM_TITLE to "ViewModel & Business Delegates",
        MVI_VM_DESC to "Single source of decision logic, processing Intents and reducing state via atomic copy operations.",
        MVI_STATE_TITLE to "UiState / Immutable Snapshot",
        MVI_STATE_DESC to "Single Source of Truth (SSOT) exposed as read-only StateFlow. All fields are val to prevent state mutations.",
        MVI_EFFECT_TITLE to "CommonUiEffect / Transient Event Channel",
        MVI_EFFECT_DESC to "Single-shot event bus for Toasts, navigations, and Snackbars, immune to configuration change re-triggers.",

        CLEAN_PRESENTATION_TITLE to "Presentation Layer",
        CLEAN_PRESENTATION_DESC to "Feature screens, stateless composables, and StateHolders following MVI patterns without leaking data details.",
        CLEAN_DOMAIN_TITLE to "Domain & Engine Layer",
        CLEAN_DOMAIN_DESC to "Pure functional engines (BudgetAlertGuard, FinancialInsightEngine) completely decoupled from Android SDK.",
        CLEAN_DATA_TITLE to "Data & Persistence Layer",
        CLEAN_DATA_DESC to "Local-first Room SQLite, reactive DataStore flows, SAF file exports, and Google Drive snapshot storage.",
        CLEAN_CORE_TITLE to "Core & Platform Layer",
        CLEAN_CORE_DESC to "Notification Channel matrix, AppWidget 2.0, biometric privacy shields, and APM observability.",

        MOD_APP_TITLE to ":app (Feature Host)",
        MOD_APP_DESC to "Top-level host containing Transactions, Statistics, and Settings features, orchestrating global states.",
        MOD_ARCH_TITLE to ":ListenArch (Architecture Core)",
        MOD_ARCH_DESC to "Independent Composite Build module providing MVI base classes, i18n registry, and sync abstractions.",
        MOD_UI_TITLE to ":ListenUiComponent (Design System)",
        MOD_UI_DESC to "Pure Compose UI library providing design tokens, themes, SurfaceCard, and buttons without business dependencies.",
        MOD_SYS_TITLE to "System & External Ecosystem",
        MOD_SYS_DESC to "Android OS services, Google Drive API, and SAF storage decoupled via clean interfaces."
    )

    private val jaMap = mapOf(
        TITLE to "アーキテクチャ全景ビジュアライザー",
        SUBTITLE to "MVIリアクティブフロー、クリーンアーキテクチャ、モジュール結合トポロジー",
        TAB_MVI to "MVI リアクティブ",
        TAB_CLEAN to "クリーン構成",
        TAB_MODULES to "モジュール結合",
        SECTION_RESPONSIBILITY to "主要責務と設計理念",
        SECTION_EXAMPLES to "代表クラス / 主要インターフェース",
        SECTION_RULES to "アーキテクチャ規約と制約",
        TIP_SELECT_NODE to "ノードをタップして設計詳細と規範を確認",

        MVI_VIEW_TITLE to "View / 描画UI",
        MVI_VIEW_DESC to "不変のStateスナップショットのみに依存し、ユーザー操作をIntentとして下流へ発行する純粋Composeツリー。",
        MVI_INTENT_TITLE to "Intent / 統一ユーザー意図",
        MVI_INTENT_DESC to "全てのUIイベントを型安全に集約するSealed Interface階層。ログ追跡や単体テストが容易。",
        MVI_VM_TITLE to "ViewModel & 業務デリゲート",
        MVI_VM_DESC to "Intentを消費し、アトミックなcopy操作により新しいUiStateを生成する唯一の意思決定センター。",
        MVI_STATE_TITLE to "UiState / 不変スナップショット",
        MVI_STATE_DESC to "単一の信頼できる情報源（SSOT）として読み取り専用StateFlowで公開される不変状態。",
        MVI_EFFECT_TITLE to "CommonUiEffect / 単発イベント",
        MVI_EFFECT_DESC to "画面回転などで重複発行されない、Toastや画面遷移を処理するChannelベースの単発イベント伝送路。",

        CLEAN_PRESENTATION_TITLE to "プレゼンテーション層",
        CLEAN_PRESENTATION_DESC to "各画面ComposableとStateHolderで構成され、MVIパターンに従ってデータ層を直接参照しません。",
        CLEAN_DOMAIN_TITLE to "ドメイン・計算エンジン層",
        CLEAN_DOMAIN_DESC to "純粋関数による計算コア（予算超過判定、財務診断、定期自動記帳）。Android SDK非依存で高速単体テストが可能。",
        CLEAN_DATA_TITLE to "データ・永続化層",
        CLEAN_DATA_DESC to "ローカル優先のRoom SQLite、DataStore、SAFエクスポート、Google Driveクラウドバックアップ。",
        CLEAN_CORE_TITLE to "システム・基盤層",
        CLEAN_CORE_DESC to "通知チャネル、デスクトップウィジェット、生体認証プライバシー保護、APMパフォーマンス監視基盤。",

        MOD_APP_TITLE to ":app (機能ホスト)",
        MOD_APP_DESC to "収支、統計、設定画面を包含し、全体の状態を束ねる最上位のアプリケーションモジュール。",
        MOD_ARCH_TITLE to ":ListenArch (共通基盤)",
        MOD_ARCH_DESC to "MVI基底、多言語i18n登録、同期モデルを提供する完全独立のComposite Buildモジュール。",
        MOD_UI_TITLE to ":ListenUiComponent (デザインシステム)",
        MOD_UI_DESC to "テーマ、SurfaceCard、ボタンなどの共通Compose UIコンポーネントライブラリ。業務依存なし。",
        MOD_SYS_TITLE to "外部エコシステム",
        MOD_SYS_DESC to "Android OS通知、ウィジェット、Google Drive APIなど、インターフェースを介して疎結合された外部機能。"
    )
}
