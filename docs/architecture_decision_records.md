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

---

## ADR-014: 设置页信息架构现代化重组与数据中心收拢 (Settings Information Architecture)

### 背景 (Context)
旧版设置页将云端备份与本地文件导入导出割裂置于不同卡片，且记账核心业务缺乏资产账户管理的系统级入口，违背了高内聚低耦合的直觉逻辑。

### 决策 (Decision)
1. **板块分层重组 (方案一)**：
   - **记账规则中枢 (`SettingsFinanceSection`)**：将月度预算、分类管理与全新的「资产账户管理 (`SettingsDialog.AccountManage`)」聚合呈现。
   - **统一数据中心 (`SettingsDataCenterSection`)**：将 Google Drive 自动备份与本地 JSON 导出/导入合并为一体化数据中心，通过 `HorizontalDivider` 优雅分隔。
2. **遵循 PROMPTS.md 架构拆分**：
   - 提取 `GoogleAccountProfileCard` 与 `SyncStatusIndicator`，彻底消除原先超长单文件，使所有组件控制在 120~230 行之间，保证组件纯粹性与可维护性。

---

## ADR-015: 全局月份状态联动与统计排行榜视觉体系升级

### 背景 (Context)
1. 记账流水页与多维统计页独立维护月份偏移状态，用户在流水页选定历史月份后切至统计页需再次选择；此外设置页的演练数据生成仅固化在当前系统月，无法针对所选历史月份生成演练数据。
2. 统计页「支出分类排行榜」此前为零散堆叠的单行卡片，缺少榜单排名属性（名次勋章）、分类图标指示与宽幅进度对比。
3. `LineChart` 组件中固化包含英文字符串 `"Max: "`，未接入全局统一多语言系统。

### 决策 (Decision)
1. **全局月份多端联动与按月生成演练数据**：
   - 在 `ExpenseAppState` 统一维护跨 Tab 选中的月份偏移 (`activeMonthOffset`)，并在切换 Tab 时自动双向同步 `TransactionsViewModel` 与 `StatisticsViewModel`。
   - `SettingsIntent.SeedDemoData(val monthOffset: Int)` 支持传入目标月份偏移，`SettingsViewModel` 精确依据指定年月的实际天数范围生成分布逼真的测试收支明细。
2. **排行榜视觉现代化升级**：
   - 聚合为单一 `SurfaceCard` 大卡片，配备金/银/铜领奖台名次勋章 (#1 🥇 `#F59E0B`、#2 🥈 `#94A3B8`、#3 🥉 `#D97706`)。
   - 引入分类专属图标 + 彩色透明光晕背景气泡、百分比胶囊徽标与全宽平滑补间进度条 (`animateFloatAsState`)。
3. **图表国际化规范**：
   - `LineChart` 扩展 `currencySymbol`、`maxLabel`、`totalLabel` 属性，彻底消除硬编码，全项目所有组件实现 100% 国际化适配。

---

## ADR-016: 状态树持久化保持与图表动态刷新动效协议 (State Retention & Chart Animations)

### 背景 (Context)
1. 之前的底栏 Tab 切换采用原生的 `when (appState.currentTab)` 分支，切走 Tab 时组件树被直接卸载，导致滑动列表重置置顶。
2. 基础图表组件 (`DonutChart`, `SegmentedProgressBar`, `LineChart`, `BarChart`) 内部仅在初次由空变非空时启动动画；在数据更新、月份切换或收支切换时，由于 `targetValue` 已处于最终态 `1f`，无法触发入场重启动画。

### 决策 (Decision)
1. **多 Tab 状态保持协议**：
   - 在 `MainActivity` 中引入 `rememberSaveableStateHolder()` 托管全局 Tab 切换，通过 `SaveableStateProvider(tab)` 保持离开 Tab 的状态快照。
   - 列表统一采用 `rememberSaveable(inputs = arrayOf(monthOffset), saver = LazyListState.Saver)`，彻底杜绝切回时列表置顶。
2. **图表数据驱动动态刷新与滚动防抖规范 (Data Signature Guard)**：
   - 解决 `LazyColumn` 上下滑动时子项回收挂载导致重复触发 `snapTo(0f)` 动画的问题。
   - 所有图表引入 `dataSignature = remember(data) { data.hashCode().toString() }` 配合 `rememberSaveable { mutableStateOf("") }`。
   - 组件挂载时若已播放过相同签名的动画，直接以满格值初始渲染，仅当 `dataSignature` 发生实质性变更时才触发重置与播放。不仅消除了上下滚动时的重复动画干扰，更保证了真实数据刷新时动效的准时触发。

---

## ADR-017: 引入 StateHolder 模式管理 UI 框架状态

### 背景 (Context)
随着页面复杂度增加，直接在 Composable 函数内部维护大量的 `remember` 状态（如系统文件选择器 `ActivityResultLauncher`、滚动位置 `LazyListState`）和副作用生命周期，导致 UI 代码急剧膨胀，与纯粹的布局逻辑混杂，违背了单一职责原则。

### 决策 (Decision)
1. 全面引入 **StateHolder (UI 状态容器)** 模式，将页面级的非业务状态提升至独立的 `XxxStateHolder.kt` 中。
2. 明确职责边界：`ViewModel` 仅负责业务领域与偏好数据的流转；`StateHolder` 专职负责持有与 Compose 生命周期绑定的框架级对象和事件。
3. 采用 `rememberSaveable` 结合 `Saver` 在 StateHolder 内部保护关键滚动状态等，防止配置变更（如屏幕旋转）导致的丢失。

---

## ADR-018: 统一合并新增与编辑弹窗 (Unified Transaction Sheet)

### 背景 (Context)
早期设计中“记一笔 (Add)”与“编辑账单 (Edit)”分别为两个独立的 BottomSheet 组件，导致键盘、日期选择、分类选择等大量核心交互逻辑冗余。每次调整输入体验时都需要同步修改两处，极易出现不一致的 Bug。

### 决策 (Decision)
1. 废弃分离设计，重构为单一的 `TransactionSheet.kt` 核心组件。
2. 通过引入 `transaction: TransactionEntity? = null` 参数，内部依据是否为空自动在新增/编辑两种模式间无缝切换。
3. 严格遵循**状态提升 (State Hoisting)** 原则，Sheet 内部维护所有临时输入状态（避免 ViewModel 频繁介入未完成的输入过程），仅在用户最终点击“完成/保存”时，才将组装好的数据通过 Lambda 向上层业务层抛出。

---

## ADR-019: 分类维度精细化预算系统 (Category-based Budget)

### 背景 (Context)
初始版本仅支持“全局月度预算”，无法满足用户对不同领域（如餐饮、购物、娱乐）差异化管控的诉求，缺乏直观的超支预警机制。

### 决策 (Decision)
1. 设计并引入独立弹窗系统 `CategoryBudgetCenterDialog` 及底层计算逻辑。
2. 不直接在数据库保存每月的绝对值预算额，而是建立“动态分类占比权重 (Category Ratio)”，结合全局月度预算动态换算各类别的额度，降低用户跨月设置的维护成本。
3. 提供直观的平滑动效进度条指示各类别的预算使用情况。

---

## ADR-020: 统一生命周期事件分发代理 (LifecycleEvent Integration)

### 背景 (Context)
在 Compose MVI 架构中，某些业务（如云同步状态的检查、本地数据的及时拉取）需要精确获知页面的前后台切换状态。然而，在各个业务 Composable 中手动监听 `LocalLifecycleOwner` 导致大量样板代码。

### 决策 (Decision)
1. 在 `CommonRoute` 泛型组件中，集中捕获 `ON_RESUME` 和 `ON_PAUSE` 等系统级事件。
2. 将这些系统事件统一定义为 `LifecycleEvent`（`ON_APPEAR` / `ON_DISAPPEAR`），并通过 `BaseViewModel.dispatchLifecycleEvent` 分发入 ViewModel 状态机。
3. 各个业务 ViewModel 仅需按需重写 `toLifecycleIntent(event)` 即可，零成本获取生命周期感知能力，同时保持了核心业务流处理与 Compose 框架层的严格解耦。

---

## ADR-021: 桌面小部件体验 2.0 (App Widget 2.0 - 4x2 智能双模看板与闪电分类直达记账)

### 背景 (Context)
早期小部件仅支持展示单日开销且无法查看月度预算执行健康度；记账时用户必须解锁手机、查找图标、打开 App、寻找分类并输入金额，平均耗时 5~10 秒。在碎片化即时消费场景（如超市结算、地铁闸机、餐厅点单）下极易导致用户放弃记账。此外，传统定时器定期轮询小部件造成不必要的后台电池消耗。

### 决策 (Decision)
1. **4x2 智能双模看板布局**：
   - 左侧集成收支仪表盘：当月总支出、剩余可用额度、水平进度条及“正常/预警/超支”三态健康度徽章，轻触直达流水页面；
   - 右侧集成 4 大高频分类快捷记账按钮（🍔 餐饮、🚗 交通、🛍️ 购物、📦 日用）与通用添加入口。
2. **DeepLink 零延迟穿透与预选参数**：
   - 绑定 `lexpense://quick_add?category={cat}&type=EXPENSE` 专属 PendingIntent；
   - `MainActivity` 声明为 `singleTop` 并在 `onNewIntent` 与 `onCreate` 中高效捕获意图，通过 `ExpenseAppState.openQuickAdd` 自动唤醒并预选中对应分类，达成 2 秒闪电记账。
3. **变动驱动刷新 (Zero-Polling)**：
   - 小部件元数据设置 `updatePeriodMillis="0"`，杜绝定时器频繁唤醒；
   - 仅在 Room 数据库账单增删改或 DataStore 预算/币种变更时，由 Flow 响应式毫秒级推送到 RemoteViews。
4. **原生动态主题适配**：
   - 引入 `colors_widget.xml` 及 `values-night/` 调色盘，完美遵循 Android RemoteViews 规范在深浅模式间自适应切换。

### 影响 (Consequences)
- 优点：碎片化即时记账路径从 10 秒缩短至 2 秒以内；桌面实时掌控预算健康度；无后台轮询开销，极致节能。
- 成本：需在 `TransactionSheet` 中支持 `initialCategoryId` 预选分类并在 `TransactionsDialog.AddTransaction` 中扩展参数兼容。

---

## ADR-022: 生物识别应用锁与生命周期拦截 (Biometric & Lifecycle Security)

### 背景 (Context)
财务应用涉及用户高隐私的收支数据与资产情况。设备即使被他人临时借用，也存在意外点开应用泄露资产金额的风险。同时，系统多任务卡片 (Recent Apps) 截图也会缓存包含敏感金额的界面快照。

### 决策 (Decision)
1. **基于 androidx.biometric**：引入标准 `BiometricPrompt`，收敛管理指纹、面容及后备锁屏密码 (PIN/图案)，保证最佳碎片化机型兼容性。
2. **切后台延迟锁定**：在 `BiometricSecurityManager` 中记录 `ON_PAUSE` 时间戳，并在 `ON_RESUME` 时根据用户配置的宽限期（立即、1分钟、5分钟）决定是否弹出全局拦截层 `BiometricLockOverlay`。
3. **FLAG_SECURE 与常驻防窥调度**：通过 `AppSecurityCoordinator` 统一管理 `Window.FLAG_SECURE`。当启用应用锁或常驻防窥 (`recentAppsShieldEnabled`) 时持续置位安全标志，防止应用在解锁状态下切到多任务界面时被系统截取并暴露财务快照。

### 影响 (Consequences)
- 优点：满足高度安全诉求；通过弹窗阻挡无需重构底层导航；
- 成本：引入生物识别库依赖，需在各种厂商 ROM 层面做测试适配（如熄屏再亮的生命周期重入边界问题）。

---

## ADR-023: 智能财务洞察诊断与计算前置 (Financial Insight Engine)

### 背景 (Context)
用户除了记录账单外，更需要深度的财务诊断（例如月环比暴涨、预测超支日、异常单项花销突增）。将这些复杂分析逻辑揉杂在 ViewModel 或 View 层会导致代码极度臃肿且难以测试。

### 决策 (Decision)
1. **纯函数式引擎隔离**：将所有诊断分析逻辑收敛于单一无状态纯对象 `FinancialInsightEngine`。接收 `TransactionEntity` 原始列表与 `currentOffset`，返回标准的 `FinancialInsightItem` 领域实体列表。
2. **分级情感化卡片**：洞察卡片区分 `INFO`、`POSITIVE`（翡翠绿）、`WARNING`（琥珀黄）、`DANGER`（珊瑚红）四个情感化级别，使核心财务状态对用户产生强直觉。
3. **基于时间的局部快照对比**：针对环比分析 (MoM)，动态切片当月与上月同期的花销聚合并通过除法差值找出突增项。

### 影响 (Consequences)
- 优点：引擎函数可完全隔离 UI 在纯 JVM 环境进行 100% 单元测试覆盖；扩展新洞察策略（如年度概览）成本极低。
- 成本：每次渲染均会在协程作用域下扫描历史两月的完整交易列表；但基于内存快照，耗时保持在几毫秒，无 UI 卡顿。

---

## ADR-024: 记账表单金额展示规范与双小数位输入防溢流 (Transaction Form Amount Formatting & Keypad Decimal Guard)

### 背景 (Context)
流水列表中展示整数金额如 `78`，但点击进入编辑弹窗时若初始化为 `"78.00"`，会破坏视觉一致性并违背全局金额去零规范（Rule 21）。此外，自定义数字键盘 `NumericKeypad` 早期仅限制了小数点唯一性与最大字符长度，缺少对小数位数的拦截，导致用户输入两位小数后仍可随意追加按键。

### 决策 (Decision)
1. **初始化严格对齐 Rule 21**：`TransactionSheet` 在编辑模式初始化金额表达式时，全量使用 `Double.formatAmount()` 扩展，自动去除末尾无意义的 `.00` 和 `.0`。
2. **键盘级小数位拦截**：在 `NumericKeypad` 的 `onKeyPress` 中引入动态小数位校验 `amountExpression.indexOf('.') != -1 && key != "." && amountExpression.length - dotIdx - 1 >= 2`，在按键源头杜绝超出 2 位小数的输入行为。

### 影响 (Consequences)
- 优点：列表与编辑态金额表现 100% 统一；彻底消除非法小数输入风险；
- 成本：无额外运行时性能损耗，逻辑内聚在键盘事件回调中。

---

## ADR-025: 跨 Tab 年月视图双向联动与统计下钻保持机制 (Cross-Tab Year/Month View Linkage & Drill-Down State Preservation)

### 背景 (Context)
流水与统计画面均具备月视图与年视图。在常规场景下，用户希望两大画面的时间周期与偏移保持全局自然联动（例如流水切换为 2025 年视图，切至统计也应呈现 2025 年视图）。
然而存在一个特殊的关键下钻场景：当用户在统计画面的年视图中点击 12 个月概览卡片或年度走势折线图上的某个特定月份时，系统会跳转到流水画面并切换为月视图以便用户查看当月详细流水。在此场景下，用户查看完流水后很可能会立即（或翻看邻近月份后）点击底部导航栏切回统计画面。若无差别的双向联动强制将流水的月视图覆盖统计画面，会导致统计画面的年视图被意外破坏，违背用户直觉。

### 决策 (Decision)
1. **常规全局双向联动 (`syncTimeState`)**：
   - 当用户在底部导航栏正常切换 Tab 时，`ExpenseAppState` 自动同步源页面与目标页面的周期（`period: MONTH / YEAR`）与对应的时间偏移（`selectedMonthOffset` / `selectedYearOffset`）。
2. **下钻返回保护机制 (`preserveStatisticsYearOnReturn`)**：
   - 统计年视图点击图表 item 调用 `navigateToTransactionsMonth(monthOffset)` 时，置位 `preserveStatisticsYearOnReturn = true`；
   - 此时仅将流水画面切换为 `TransactionPeriod.MONTH` 并定位至目标月份，**统计画面自身保持 `StatisticsPeriod.YEAR` 不变**；
   - 当用户从流水画面点击底栏切回统计画面时，`syncTimeState` 识别到下钻保护状态有效且流水仍为月视图，主动拦截流水月视图对统计年视图的覆盖，直接保持统计画面的年视图状态，并重置保护标志；
   - 若用户在流水画面主动将周期切换为年视图，则解除保护，恢复常规双向联动。

### 影响 (Consequences)
- 优点：完美兼顾了日常使用的自然年月双向联动，以及年视图下钻至月流水后返回的无缝状态保持；
- 成本：在 `ExpenseAppState` 中增加一个轻量布尔标志与分支判断，由自动化单元测试（`ExpenseAppStateTest`）严密保证稳定性。

---

## ADR-026: 开发者模式 5 击隐秘唤醒与高危操作防误触 (Developer Mode 5-Tap Reveal & Safe Demo Data Seeding)

### 背景 (Context)
1. “清除所有数据”属于完全不可逆的极高危破坏性操作，长期置于设置页中即使带有二次确认弹窗也存在误触和引发用户心理焦虑的风险；
2. “生成模拟数据”在用户当月已有真实手工记账记录时若被触发，会导致测试数据与真实数据深度混杂且难以剥离清理。

### 决策 (Decision)
1. **隐秘开发者模式 (5-Tap Reveal Gesture)**：
   - 默认状态下收起并彻底隐藏“清除所有数据”按钮；
   - 用户在版本号/设置特定区域连续点击 5 次触发彩蛋式开发者模式唤醒（支持剩余点击次数 Toast 引导）；
   - 一旦激活开发者模式，状态持久化保留，不再提供反向关闭机制，确保技术调试人员无需反复激活。
2. **按钮规范统一**：
   - 统一“生成模拟数据”与“清空所有账单”按钮高度与文案命名规范，保证视觉对齐与语义对称。
3. **真实数据防脏守卫 (Real Data Guard)**：
   - 在 `TransactionMutationHandler.seedDemoData` 中，执行前先调用 `dao.getTransactionCountInRange` 查询目标月份是否存在既有账单；
   - 若记录数大于 0，立即终止生成流程并弹出错误提示“当月已有数据，无法生成模拟数据”，杜绝脏数据污染真实账目。

### 影响 (Consequences)
- 优点：彻底消除生产环境下误清空所有账单的潜在风险；保护了真实账目数据的纯洁性；
- 成本：技术人员测试时需通过 5 击手势激活开发者模式。

---

## ADR-027: 版本在线校验与 Google Play 更新闭环 (In-App Version Check & Google Play Update Flow)

### 背景 (Context)
用户在应用内需要方便快捷地获知是否有新版本发布，并查看新版本更新说明后决定是否更新。

### 决策 (Decision)
1. **异步远程版本校验**：
   - 在设置页底部版本区域提供“检查更新”按钮，点击后呈现旋转 Loading 状态；
   - 异步拉取 GitHub 官方发布的 `version.json`，比对当前本地 `BuildConfig.VERSION_CODE` 与远程最新版本号；
2. **无缝结果反馈与更新闭环**：
   - 若当前已为最新版本，显示“已经是最新版本”友好 Toast 并结束 Loading；
   - 若检测到新版本，呼出 `UpdateAvailableDialog` 弹窗，展示新版本版本号与更新说明（Changelog）；
   - 底部提供“取消”与“立即更新”两个按钮，点击“立即更新”通过 `Intent.ACTION_VIEW` 零延迟拉起系统 Google Play 商店对应应用详情页，若未安装 Play 商店则平滑降级至网页版商店。

### 影响 (Consequences)
- 优点：轻量无侵入，无需依赖第三方庞大的热更新或升级 SDK，形成闭环升级链路；
- 成本：依赖远程网络访问及 GitHub 静态文件托管。

---

## ADR-028: 底部导航双击置顶与智能归位当前周期 (Double-Tap Active Tab to Reset to Current Period)

### 背景 (Context)
用户在流水或统计画面翻看数月甚至数年前的历史数据后，想要回到当月/当年时，往往需要连续多次点击箭头或重新打开月份选择器，操作链路较长。

### 决策 (Decision)
1. **双击手势捕获**：
   - 在底部导航栏 `NavigationBarItem` 中，当用户点击当前已选中的 Tab 时，通过判断连续两次点击间隔（$< 350\text{ms}$）识别双击手势。
2. **两段式智能归位**：
   - **第一击/未置顶**：如果列表尚未滚动至顶部，双击优先平滑滚动列表至第一项（`ScrollToTop`）；
   - **已置顶状态下双击**：如果列表已经在顶部（`firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0`），则根据当前处于的视图模式智能归位：
     - 若为月视图，自动重置选中月份为当月（`selectedMonthOffset = 0`）；
     - 若为年视图，自动重置选中年份为当年（`selectedYearOffset = 0`）。

### 影响 (Consequences)
- 优点：符合现代移动应用主流交互习惯（如微博、Twitter 双击 Tab 置顶并刷新），大幅提升重度用户的交互效率；
- 成本：需在 `TransactionsEffects` 与 `StatisticsEffects` 中捕获 `ScrollToTop` 事件并结合 `LazyListState` 状态做两段式分支判断。
