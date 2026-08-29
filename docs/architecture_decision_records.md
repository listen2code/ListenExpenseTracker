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

---

## ADR-008: Listen 系列多 App 架构边界与通用库业务解耦 (Zero Business Coupling)

### 背景 (Context)
随着 Listen 产品矩阵（记账、资产管理、习惯打卡、备忘录等）的演进，旧版 `ListenArch` 与 `ListenUiComponent` 中混入了记账专属的 Room 实体（`TransactionEntity`）、数据库表、记账多语言文案与固定文案按键，导致通用库无法被其他 App 直接复用。

### 决策 (Decision)
1. **`ListenArch` 通用底座化**：移除所有 Room Entity/DAO/Database 与记账 CSV 导出，迁移至 `ListenExpenseTracker`；将 `CloudSyncManager` 改造为支持任意泛型/JSON 字符串的通用 Payload 传输引擎；`StringsRes` 仅保留系统级通用词并开放 `registerAppStrings()` 动态注册能力。
2. **`ListenUiComponent` 纯粹化**：消除 UI 控件中的业务硬编码文案（如 `NumericKeypad` 的 `doneText` 参数化，`SearchBarInput` 默认通用占位符）。
3. **`ListenExpenseTracker` 业务闭环**：通过 `ExpenseDataStoreManager`、`ExpenseStrings` 及 `data/db/` 完整承接领域模型与业务计算。

---

## ADR-009: 资产账户多维分层与破坏性操作确认规范 (Rule 15 Danger Button)

### 背景 (Context)
随着用户自定义资产账户（交通卡、理财专户等）数量增加，混杂内置账户造成层级混乱；且账户删除属于高危破坏性操作（需防止误触，并让用户明确知晓已关联账单不会丢失）。

### 决策 (Decision)
1. **双层账户结构**：将账户划分为标准系统账户 (`CASH`/`BANK`/`CREDIT`) 与动态持久化自定义账户 (`ACC_*`)。
2. **多端交互一致性**：在流水头部筛选胶囊、账户管理弹窗、添加账单 Sheet、编辑账单 Sheet 四个场景统一支持长按自定义账户调出删除二次确认。
3. **Rule 15 破坏性确认标准**：删除弹窗的确认按钮强制采用 `CommonButtonStyle.Danger` 醒目红色调，取消按钮采用弱化 Text 样式。

---

## ADR-010: UI 组件深度拆分解耦与单文件行数控制标准 (PROMPTS.md Rule 3)

### 背景 (Context)
`AccountManageDialog.kt` 随业务迭代膨胀至近 500 行，内部混杂了卡片渲染、改名弹窗、删除确认与列表编排，降低了代码可读性与复用性。

### 决策 (Decision)
1. **单一职责物理拆分**：
   - 提取 `AccountCardItem.kt` 负责单行卡片、徽标与着色。
   - 提取 `AccountEditDialog.kt` 负责新增/重命名录入。
   - 提取 `AccountDeleteConfirmDialog.kt` 负责安全删除确认。
   - `AccountManageDialog.kt` 降至 220 行以内，仅承载 Section 列表编排。
2. **严格参数规范 (Rule 13)**：所有独立 Composable 组件首个可选参数统一为 `modifier: Modifier = Modifier`。

---

## ADR-011: CI/CD 自动化构建与发布成功邮件通知机制

### 背景 (Context)
持续集成每次 push 或 PR 执行工作流都会触发状态，但开发者仅在正式发版到 Google Play 成功时需要接收确认通知，无需为日常常规 CI 接收干扰邮件。

### 决策 (Decision)
1. 在 `.github/workflows/deploy.yml` 的 `publish-google-play` 任务末尾集成邮件通知步骤。
2. 设定条件限制 `if: success()`，仅在 AAB 生成、签名、元数据校验及 Google Play 发布成功后，向 `listen2code@gmail.com` 发送带版本号与 commit 信息的高优邮件。

---

## ADR-012: 触觉反馈系统与组件级平滑动效体系 (Haptics & Motion Design System)

### 背景 (Context)
移动记账属于高频操作场景，缺乏物理按键的触感反馈会导致输入确定性不足；同时，数据卡片与图表在月份或收支 Tab 切换时的突兀跳变会削弱界面的精致度。

### 决策 (Decision)
1. **统一触觉反馈梯队 (Compose LocalHapticFeedback)**：
   - **轻触级 (`TextHandleMove`)**：用于 `NumericKeypad` 数字键/退格键按压、账户筛选胶囊切换、排序规则选择，模拟高频机械微触感。
   - **脉冲确认级 (`LongPress`)**：用于「完成记账 ✓」全宽提交、危险操作 (`AccountDeleteConfirmDialog`) 确认删除，强化关键状态流转的确定感。
2. **渐进式图表展开与过渡补间**：
   - `DonutChart`：引入 `animateFloatAsState`（650ms `FastOutSlowInEasing`），圆环从 -90° 顺时针优雅展开。
   - `LineChart`：引入 600ms 阻尼插值，折线与填充自底向上拔起。
   - `BalanceOverviewCard`：预算进度条应用 500ms 平滑插值，避免硬切抖动。
   - 页面级 Tab 切换：使用 `AnimatedContent` 进行交叉淡入淡出（Crossfade 300ms）。

---

## ADR-013: Release Pipeline 工业级上线交付与多轨道发布规范

### 背景 (Context)
发布到 Google Play 需面对全球多设备机型与多语言环境，必须具备严格的版本递增、混淆优化、密钥安全隔离、AAB 动态下发与阶段性灰度防护。

### 决策 (Decision)
1. **自动化构建与版本控制**：
   - `versionCode` 在 Gradle/CI 中基于构建号自动化递增，`versionName` 采用严格 SemVer 语义化规范。
2. **生产构建加固 (R8 / Shrinking)**：
   - Release 构建强制启用 `minifyEnabled true` 与 `shrinkResources true`，保护 Room、Kotlinx Serialization 等实体规则，并归档 `mapping.txt`。
3. **Play App Signing 与 Secrets 隔离**：
   - 仅使用 Upload Keystore 本地/CI 签名，主签名由 Google Play 托管。敏感凭据全量注入 GitHub Secrets。
4. **渐进式灰度发布 (Staged Rollout)**：
   - 生产环境发布严格推行 `10% -> 20% -> 50% -> 100%` 灰度流转，依托 Android Vitals（Crash / ANR < 0.47%）守护质量红线。
