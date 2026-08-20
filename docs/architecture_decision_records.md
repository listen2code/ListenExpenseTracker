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
3. **单次副作用通道**：Toast、震动、导航等单次事件通过 `Channel<ViewEffect>` 独立分发。

### 影响 (Consequences)
- 优点：彻底杜绝 UI 状态不同步问题；TraceId 可贯穿每个 Intent 的全链路处理。
- 成本：需定义清晰的 Intent 与 Effect 封闭枚举类。

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
