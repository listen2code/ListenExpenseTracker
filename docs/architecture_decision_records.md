# ListenExpenseTracker - 架构决策记录 (Architecture Decision Records - ADR)

本文档记录 `ListenExpenseTracker` 与基础架构 SDK 在设计与实现过程中的关键技术决策与权衡依据。

---

## ADR-001: 采用 MVI (Model-View-Intent) 作为核心展示层架构

### 背景 (Context)
传统 MVVM 模式中，ViewModel 往往暴露大量零散的 LiveData / StateFlow 变量，导致状态组合碎片化、事件竞态条件频发，UI 无法形成严密的“单一事实来源”。

### 决策 (Decision)
全工程统一采用 `BaseViewModel<ViewState, UserIntent, ViewEffect>` 实现 MVI：
1. **单一不可变状态**：UI 层仅观察单一 `StateFlow<ViewState>`，保证画面重绘一致性。
2. **显式 Intent 输入**：所有用户动作通过 `handleIntent(intent)` 派发，便于打点追踪与单元测试。
3. **单次副作用通道**：Toast、撤销 Snackbar、震动等单次事件通过 `Channel<ViewEffect>` 独立分发。

### 影响 (Consequences)
- 优点：彻底杜绝 UI 状态不同步问题；TraceId 可贯穿每个 Intent 的全链路处理。
- 成本：需定义清晰的 Intent 与 Effect 封闭接口。

---

## ADR-002: 本地优先 (Local-First) 与 Room SQLite 结合

### 背景 (Context)
记账应用对录入响应时间与离线可用性要求极高（冷启动打开即记，断网也能记）。

### 决策 (Decision)
采用 Local-First 架构：
1. 所有写操作直接写入本地 Room SQLite，并立即通过 `Flow` 响应式刷新 UI。
2. 云端同步作为非阻塞的异步后台服务 (`CloudSyncManager`)，支持网络恢复后双向合并。

---

## ADR-003: Gradle Composite Build 多仓库模块解耦

### 背景 (Context)
`ListenArch` 和 `ListenUiComponent` 是面向未来多款 Listen 系列 App 的通用 SDK。若放在单一项目子模块中容易产生隐式耦合。

### 决策 (Decision)
采用独立 Git 仓库 + Gradle Composite Build (`includeBuild`)：
1. 模块边界清晰，代码物理隔离。
2. 开发者在主项目中开发时享受子项目直接联调与断点调试便利。

---

## ADR-004: APM 性能监控与 500 条环形内存日志

### 背景 (Context)
上线后难以捕获偶发性能卡顿与非崩溃逻辑异常，引入大型第三方 APM SDK 会显著增加 APK 体积。

### 决策 (Decision)
自主研发轻量级 `ApmLogger` 与 `TraceManager`：
1. 500 条内存环形链表，零 I/O 开销。
2. 统一分发 `APP`, `DB`, `SYNC`, `CRASH` 四大频道。
3. 配套 UI 浮窗 `LogInspectorSheet` 支持现场排查与导出。

---

## ADR-005: 真实 Google 账户连携与多账户隔离云端快照同步体系

### 背景 (Context)
跨设备换机和防丢账单需要可靠的云端备份，且需要确保用户隐私与数据按账号隔离。

### 决策 (Decision)
1. 接入 Google Play Services Auth SDK，实现系统级账号授权，支持优雅降级至备选账号选择器。
2. 将 `isLoggedIn`、`userEmail`、`displayName` 及 `avatarUrl` 在 `BaseDataStoreManager` 中持久化存储，冷启动自动恢复。
3. `CloudSyncManager` 引入基于账号的隔离快照机制与 MD5 校验和，确保备份数据的完整性与多账户隔离。

---

## ADR-006: 账单滑动删除“软删除撤销通道” (Undo Snackbar Pattern)

### 背景 (Context)
Swipe-to-Delete 滑动删除极易因误触导致账单丢失，若每次删除都弹出确认 Dialog 则严重破坏流畅体验。

### 决策 (Decision)
1. 将滑动删除防误触阈值提高至 `70%`（需大幅度左滑才触发）。
2. 删除触发时，通过 `TransactionsEffect.ShowUndoSnackbar` 在屏幕底部唤起带“撤销”按钮的 Snackbar（持续 4 秒）。
3. 用户点击“撤销”即可一键将内存中暂存的 `TransactionEntity` 重新插回数据库，实现无缝恢复。

---

## ADR-007: 专属年月选择器与日聚合高信息密度投影

### 背景 (Context)
传统 Android DatePickerDialog 强迫用户选择具体日期，切换年份极慢；同时旧版流水列表条目过高，一屏能容纳的账单太少。

### 决策 (Decision)
1. 研发独立的 `MonthPickerDialog`：顶部 `< 2026年 >` 极速切年，下方 3x4 12 月份方块网格直选。
2. 列表层通过 `groupBy { formatDayGroupHeader(it.timestamp) }` 实现按日自动聚合分组，并提供每日收支小计。
3. 将条目图标与内边距压缩为 24dp/6dp，使信息密度提高 100%。

·---

## ADR-008: Listen 系列多 App 架构边界与通用库业务解耦 (Zero Business Coupling)

### 背景 (Context)
随着 Listen 产品矩阵（记账、资产管理、习惯打卡、备忘录等）的演进，旧版 `ListenArch` 与 `ListenUiComponent` 中混入了记账专属的 Room 实体（`TransactionEntity`）、数据库表、记账多语言文案与固定文案按键，导致通用库无法被其他 App 直接复用。

### 决策 (Decision)
1. **`ListenArch` 通用底座化**：移除所有 Room Entity/DAO/Database 与记账 CSV 导出，迁移至 `ListenExpenseTracker`；将 `CloudSyncManager` 改造为支持任意泛型/JSON 字符串的通用 Payload 传输引擎；`StringsRes` 仅保留系统级通用词并开放 `registerAppStrings()` 动态注册能力。
2. **`ListenUiComponent` 纯粹化**：消除 UI 控件中的业务硬编码文案（如 `NumericKeypad` 的 `doneText` 参数化，`SearchBarInput` 默认通用占位符）。
3. **`ListenExpenseTracker` 业务闭环**：通过 `ExpenseDataStoreManager`、`ExpenseStrings` 及 `data/db/` 完整承接领域模型与业务计算。
