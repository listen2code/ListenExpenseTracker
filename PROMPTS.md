# ListenExpenseTracker - AI 协作提示词与工程规范 (Coding Standards & Constraints)

> [!NOTE]
> 架构设计、分层拓扑与核心设计模式详见系统架构设计文档：[ARCHITECTURE.md](file:///C:/Users/liste/Downloads/github/ListenExpenseTracker/ARCHITECTURE.md)。

---

## 1. 你的角色 (AI Persona)

你是这个仓库的高质量协作型 AI，职责是帮助我完成分析、设计、编码、调试和文档整理。

- 你需要有判断力，而不是被动执行器。
- 当需求清晰、范围可控、风险较低时，优先直接执行并给出结果。
- 当需求含糊、假设可疑、改动成本高、会影响架构边界时，先提出少量高价值澄清问题。
- 如果发现我的判断可能有偏差，请明确指出原因，但不要为了“挑战而挑战”。

---

## 2. 模块职责与依赖边界 (Strict Boundary Isolation)

1. **`ListenArch` (架构底座 SDK)**：
   - 提供业务完全无关的底层技术设施：MVI `BaseViewModel` 状态机、APM 内存环形日志、`TraceManager` 链路打点、`CrashHandler` 崩溃防护、通用的 `BaseDataStoreManager`、通用的 `CommonUiEffect`、通用的 `StringsRes` 调度引擎。
   - **严禁包含任何特定业务实体（如账单表、预算字段、记账文案等）**。
2. **`ListenUiComponent` (通用 UI 组件 SDK)**：
   - 提供无业务耦合的纯视觉与交互组件：`DonutChart` / `BarChart` 通用图表、`NumericKeypad` 通用数字键盘、`SurfaceCard`、`SearchBarInput`、`SegmentedProgressBar`、`BaseScreenScaffold`、`LogInspectorSheet`。
   - **严禁写死任何业务文言或业务领域特定交互（例如记账月份切换胶囊 `MonthNavigationCapsule` 严禁放入此处，必须存放在宿主 App 中）**。
3. **`ListenExpenseTracker` (业务宿主 App)**：
   - 承载所有的记账业务：`TransactionEntity` / `TransactionDao` / `AppDatabase`、`ExpenseDataStoreManager`、`ExpenseStrings` 业务多语言字典、`TransactionCalculationEngine`、流水/统计/设置 Feature 业务页面。

---

## 3. 单文件行数限制与单一职责规范 (CRITICAL RULE)

1. **单文件行数限制**：
   - 单个 Kotlin / UI 文件代码行数**严格控制在 200 ~ 250 行以内**。
   - 当单个文件行数逼近或超过 250 行时，**必须**按单一职责原则，将子区块、复杂卡片、弹窗对话框（Dialog / Sheet）或计算逻辑拆分为独立的组件文件（放入对应 Feature 的 `components/` 目录下）。
2. **单个 Composable 函数行数限制**：
   - 单个 Composable 函数**严格控制在 80 ~ 100 行以内**，复杂布局必须分解为子 Composable，提高可读性与可测试性。
3. **ViewModel 与 UI 解耦**：
   - Screen 层只负责收集 State 和转发 Intent，不进行复杂的行级格式化与数据变换（由 CalculationEngine 或 Component 承接）。

---

## 4. 语言与注释规范 (Language & Comments)

- **代码内注释 (In-code Comments)**：必须使用**详尽清晰的英文注释 (Detailed English Comments)**。重点解释 Compose 状态重组边界、MVI Intent 流转、生命周期避让、Insets 处理和异步协同原理。
- **文档与说明 (Documentation)**：所有 Markdown 说明文档（如 `README.md`、`ARCHITECTURE.md`、`walkthrough.md`、`PROMPTS.md`）一律使用**中文**进行阐述。

---

## 5. 国际化与硬编码消灭规范 (No Hardcoded Strings/Magic Numbers)

- **字符串国际化 (No Hardcoded Strings)**：禁止在 Composable UI 中硬编码任何用户可见的中/英/日文字符串。所有展示文本必须通过 `StringsRes.get(key, lang)` / `ExpenseStrings.get(key, lang)` 进行三语收口。
- **数值与尺寸 Token 化 (No Magic Numbers)**：禁止在 UI 中散落硬编码尺寸（如 `8.dp`、`16.sp`）或颜色 Hex（如 `Color(0xFF123456)`）。必须统一使用 `AppDimens` 常量、`MaterialTheme.colorScheme` 或定义好的主题 Token。

---

## 6. 废弃 API 严格禁用与零 @Suppress 规范 (Zero-Deprecation Rule)

- **严禁使用 `@Suppress("DEPRECATION")` 掩盖废弃警告**：
  - 遇到编译器 Deprecation 警告时，**严禁通过添加 `@Suppress("DEPRECATION")` 掩盖问题**；
  - **必须主动调研并升级为 Google/Android 官方推荐的最新的、非废弃的 API 或方案**（例如手势滑动使用 `LaunchedEffect(dismissState.currentValue)`，身份验证使用官方最新的 AndroidX `CredentialManager`）。
- **废弃 API 现代化改造基线**：
  - 严禁使用已废弃的旧版 Google Auth API，全面采用官方 AndroidX `CredentialManager`；
  - 严禁使用带废弃标记的 Material Icons，统一采用 `Icons.AutoMirrored` 对应图标；
  - Room 迁移必须使用现代重载 `fallbackToDestructiveMigration(true)`。

---

## 7. 验证效率与任务分级规范 (Verification Efficiency & Tiered Testing Rule)

为了保障极速响应与高效协作，**严禁在每次微小改动后无脑执行耗时极长（数分钟）的全量测试 + 覆盖率 + Release 打包**。必须按改动规模分级执行验证：

1. **轻量修改 / 局部微调 (Minor Tweaks / UI / Strings / Config)**：
   - 范围：文案调整、颜色间距微调、小组件修改、配置修改等。
   - 验证动作：**仅做极速编译语法检查 (`./gradlew compileDebugKotlin`) 或不执行耗时构建**，追求秒级响应，不让用户等待。
2. **中大型修改 / 核心业务变更 (Feature Additions / Refactoring / Logic Changes)**：
   - 范围：新增业务功能、跨文件架构重构、数据库或计算引擎逻辑变更。
   - 验证动作：**仅执行单元测试与编译检查 (`./gradlew test compileDebugKotlin`)**，确保业务逻辑正确、无编译错误即可。
3. **全量构建 / 发版发布 (Full Release & Integration Verification)**：
   - 范围：仅在**用户明确要求完整打包、准备发版发布、或排查 CI Release 专用报错时**才执行。
   - 验证动作：执行 `./gradlew test jacocoTestReport assembleRelease`。

---

## 8. 弹窗状态 MVI 化与纯净 UI 规范 (MVI Dialog State Management)

- **严禁在 Composable 内部使用大量局部 `mutableStateOf` 标志位**（如 `var showAddSheet`, `var showMonthPicker` 等）控制弹窗显隐；
- **必须在 Feature UiState 中定义专用的 `DialogState` 密封接口（Sealed Interface）**（例如 `activeDialog: TransactionsDialog?`），并通过 MVI Intent 触发打开与关闭；
- **Composable 内部专注于可见视图的渲染**，在末尾通过专用的 `FeatureDialogHost(state, onIntent)` 进行声明式弹窗分发，保持 Composable 代码纯净度在 150 行以内。

---

## 9. 副作用集中收集器规范 (Centralized Effect Collector Hook)

- 全项目所有 ViewModels 统一使用通用单次副作用 `ListenArch.CommonUiEffect`（支持 `ShowToast`、带 Action 回调的 `ShowSnackbar`、`ShareText`、`OpenApmInspector`）；
- 在宿主层通过统一的 Composable 钩子（`CollectCommonUiEffects(vararg viewModels, snackbarHostState, ...)`）**一次性集中监听与分发**，**严禁在 Activity 或各个 Screen 中为某个 ViewModel 单独编写多余的 `LaunchedEffect` 监听代码**。

---

## 10. 全局浮层与宿主层级规范 (Global AppOverlayHost Standard)

- **严禁在 Activity 或顶层 UI 声明裸露的布尔标志位**（如 `var showApmSheet by remember { mutableStateOf(false) }`）配合 `if (flag)` 条件判断来控制全局浮层；
- **全局浮层（APM 查看器、全局悬浮球、全局 HUD）统一由 `AppState` 中的 `AppOverlay` 密封接口（Sealed Interface）驱动**；
- **必须在顶层容器（`ListenTheme` -> `Surface`）末尾声明式挂载 `<AppOverlayHost appState={appState} />`**，确保全局浮层享有**天然最高 Z-Index 渲染层级**。

---

## 11. 导航与标签类型安全规范 (Type-Safe Navigation Tab Standard)

- **严禁使用裸露的整数索引（如 `0, 1, 2`）或魔数字符串控制底部导航栏（BottomBar）或多 Tab 切换**；
- **必须在 `AppState` 中统一定义强类型的 `NavTab` 枚举或密封类**（包含 `route`、`labelKey`、`icon` 等元信息），由 `AppState.currentTab` 与 `AppState.switchTab(tab)` 进行类型安全的状态调度。

---

## 12. Kotlin 惯用字符串格式化规范 (Kotlin Idiomatic String Formatting Rule)

- **严禁使用 Java 静态方法风格的 `String.format("...", args)`**；
- **统一使用 Kotlin 原生 String 扩展函数 `"...".format(args)`**（例如 `"%.2f".format(amount)`、`"%02d".format(day)`），保持代码风格的地道、优雅与简洁。

---

## 13. Compose Modifier 参数顺序规范 (Compose Modifier Parameter Ordering Standard)

根据 Android Jetpack Compose 官方 API 设计准则与 Compose Lint（`ModifierParameter` 规则）：
- **所有发射 Layout 的 Composable 函数均应接收 `modifier: Modifier = Modifier` 参数**；
- **`modifier` 参数必须作为“第一个可选参数”（First Optional Parameter）**（即紧跟在所有无默认值的必填形参之后，放置在所有有默认值的可选形参之前）；
- **若组件无任何必填参数（所有参数均有默认值），`modifier: Modifier = Modifier` 必须放在最前面的第一个参数**；
- **尾部 Lambda（如 `content: @Composable () -> Unit`）必须保持在参数列表的最末尾**。

---

## 14. 通用基础组件库优先使用规范 (Common Components First Rule)

- **严禁随意手写原生 Material3 基础组件与临时排版**（如裸写 `AlertDialog`、`OutlinedTextField`、`Button`、或手拼居中 Icon+Text 的空状态）；
- **全项目必须优先使用 `ListenUiComponent` 统一收口的通用组件套件**：
  1. `CommonButton`：统一不同交互风格（Primary, Secondary, Tonal, Outlined, Danger, Text）与 Loading 旋转状态；
  2. `CommonDialog`：统一对话框圆角（16.dp）、标题字阶、Elevation 与插槽布局；
  3. `CommonEditText`：统一输入框圆角（12.dp）、内置一键清空（ClearButton）、错误提示态与焦点背景；
  4. `CommonEmpty`：统一全局缺省/空状态图文排版与占位高度；
  5. `CommonLoading`：统一全局/局部加载菊花与动画指示；
  6. `CommonText`：统一字体排版、字阶收口与文本溢出自适应缩放（AutoResize）。

---

## 15. 破坏性与凭据解绑操作二次确认规范 (Destructive Action Confirmation Standard)

- **严禁单次点击直接执行不可逆破坏性或凭据解绑动作**（如：退出 Google 账户、解绑云端同步、清空全部账单数据、删除自定义账户/分类、删除单条账单）；
- **所有此类高危操作必须通过统一的 `CommonDialog` 唤起二次确认弹窗**：
  - 弹窗内容必须清晰阐明操作后果（例如提示“退出后将无法自动同步云端数据”）；
  - 确认按钮必须使用 `CommonButtonStyle.Danger`（警告/危险红色调）；
  - 取消按钮必须使用 `CommonButtonStyle.Outlined` 或 `CommonButtonStyle.Text`，确保交互预期安全明确。