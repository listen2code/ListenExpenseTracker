# ListenExpenseTracker - AI 协作提示词与工程规范 (Coding Standards & Constraints)

> [!NOTE]
> 架构设计、分层拓扑与核心设计模式详见系统架构设计文档：[docs/architecture.md](docs/architecture.md)。

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

## 4. 语言与注释规范 (Learning-Oriented Chinese Comments & Documentation)

本项目是一个**学习型 App (Educational / Learning App)**，代码不仅要保证工业级健壮性，更要具备极高的一线教学与学习价值：
- **代码内注释 (In-code Comments)**：**必须使用详尽清晰的中文注释 (Detailed Chinese Comments)**：
  - **特殊业务与实现逻辑**：重点阐明业务逻辑的设计初衷、状态机流转决策、MVI 架构边界、重组性能优化与异步协同原理；
  - **Kotlin & Compose 官方进阶 API 说明**：针对较为进阶、不常见的官方 API（例如 `rememberUpdatedState` 解决协程闭包捕获旧值问题、`snapshotFlow` 将 Compose State 转换为 Cold Flow、`rememberSaveable(saver = ...)` 实现自定义状态跨进程保存与恢复、`drop(1)` 过滤初始发射值等），**必须附带通俗易懂的原理解析与使用场景注释**，帮助开发者在阅读源码的同时系统学习掌握这些关键技术。
- **文档与说明 (Documentation)**：所有 Markdown 说明文档（如 `README.md`、`ARCHITECTURE.md`、`walkthrough.md`、`PROMPTS.md`）一律使用**中文**进行阐述。

---

## 5. 国际化通用收口与硬编码消灭规范 (Universal i18n & Zero Hardcoding Rule)

- **字符串国际化与零硬编码 (No Hardcoded Strings)**：
  - **核心原则**：禁止在 Composable UI 中硬编码任何用户可见的中/英/日文字符串；所有展示文本必须走统一的全局多语言框架（`AppStrings.KEY.tr(lang)` 或 `ExpenseStrings.get(key, lang)`）；
  - **严禁局部语言分支硬编码**：**严禁在 Composable 组件或 ViewModel 内部使用 `when (lang)`、`if (lang == "en") ...` 等方式单独判断和硬编码多语言字符串**；
  - **标准执行流程**：任何新增文案均应先在 `AppStrings.kt` 声明键常量，并在 `ExpenseStrings.kt`（中/英/日三个 Map）中集中统一配置对应翻译，UI 中仅通过 `AppStrings.XXX.tr(lang)` 无条件通用调用，确保多语言维护集中收敛，杜绝漏翻与散落判断。
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
   - 验证动作：**优先执行改动文件直接关联的针对性单测 (`--tests "TargetTestClass"`) 与编译检查**，无需无脑全量执行，确保核心逻辑正确无误即可。
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
- **严禁错误**：严禁将 `modifier: Modifier = Modifier` 声明在其它带有默认值的形参（如 `lang: String = "zh"`、`enabled: Boolean = true`）之后，否则会直接触发 Compose Lint 警告：`Modifier parameter should be the first optional parameter`；
- **若组件无任何必填参数（所有参数均有默认值），`modifier: Modifier = Modifier` 必须放在最前面的第一个参数**；
- **标准函数签名范式**：
  ```kotlin
  // 正确范式：必填形参 -> modifier (首个可选参数) -> 其他带默认值可选形参 -> 尾部 Lambda
  @Composable
  fun ExampleComponent(
      data: CustomData,                      // 1. 无默认值的必选数据
      onAction: () -> Unit,                  // 2. 无默认值的回调函数
      modifier: Modifier = Modifier,         // 3. 必须是第一个带有默认值的可选参数
      lang: String = "zh",                   // 4. 后续其他带默认值的可选配置
      content: @Composable () -> Unit = {}   // 5. 尾部 Lambda 保持在最末尾
  )
  ```

---

## 14. 通用基础组件库优先使用规范 (Common Components First Rule)

- **严禁随意手写原生 Material3 基础组件与临时排版**（如裸写 `AlertDialog`、`OutlinedTextField`、`Button`、或手拼居中 Icon+Text 的空状态）；
- **全项目必须优先使用 `ListenUiComponent` 统一收口的通用组件套件**：
  1. `CommonButton`：统一不同交互风格（Primary, Secondary, Tonal, Outlined, Danger, Text）、前置图标与 Loading 旋转状态；
  2. `CommonDialog`：统一对话框圆角（16.dp）、标题字阶、Elevation 与插槽布局；
  3. `CommonEditText`：统一输入框圆角（12.dp）、内置一键清空（ClearButton）、错误提示态与焦点背景；
  4. `CommonEmpty`：统一全局缺省/空状态图文排版与占位高度；
  5. `CommonLoading`：统一全局/局部加载菊花与动画指示；
  6. `CommonText`：统一字体排版、字阶收口与文本溢出自适应缩放（AutoResize）；
  7. `CommonSnackbar`：统一悬浮胶囊 Toast，支持 Success, Error, Warning, Info 四大语义与 Action 按钮；
  8. `CommonBanner`：统一顶部常驻/可交互通知横幅，支持展开/收起动画与关闭操作；
  9. `CommonBadge`：统一状态微型徽标与标签芯片（带点/图标/多色风格）；
  10. `CommonListItem`：统一列表项/设置行（左侧图标/头像 + 主副标题 + 右侧 Chevron 箭头/自定义内容 + 下分割线）；
  11. `CommonSwitchRow`：统一整行可点击触发切换的 Switch 交互行；
  12. `CommonSegmentedControl`：统一 iOS 风格带弹性动画滑块背景的分段胶囊选择器；
  13. `CommonBottomSheet`：统一 24.dp 圆角底部抽屉，内置 Header 标题、确认按钮与软键盘安全避让；
  14. `CommonSkeleton` / `shimmer()`：统一骨架屏微光扫光加载占位组件；
  15. `CommonError`：统一居中异常图文与点击重试页；
  16. `CommonList`：统一自动调度 Loading -> Error -> Empty -> Content 的多状态列表容器；
  17. `CommonDivider`：统一 0.5.dp 细线与左右缩进分割线。

---

## 15. 破坏性与凭据解绑操作二次确认规范 (Destructive Action Confirmation Standard)

- **严禁单次点击直接执行不可逆破坏性或凭据解绑动作**（如：退出 Google 账户、解绑云端同步、清空全部账单数据、删除自定义账户/分类、删除单条账单）；
- **所有此类高危操作必须通过统一的 `CommonDialog` 唤起二次确认弹窗**：
  - 弹窗内容必须清晰阐明操作后果（例如提示“退出后将无法自动同步云端数据”）；
  - 确认按钮必须使用 `CommonButtonStyle.Danger`（警告/危险红色调）；
  - 取消按钮必须使用 `CommonButtonStyle.Outlined` 或 `CommonButtonStyle.Text`，确保交互预期安全明确。

---

## 16. Git 提交权限与控制规范 (Git Commit Control Rule)

- **除非用户明确要求（例如“帮我提交到git”、“commit并推送到远程”），否则每次代码修改后严禁主动执行 `git commit`**；
- 日常编码中只需完成代码修改、根据任务分级执行必要编译或测试验证，改动保持在工作区供用户审核检视，不得擅自生成 commit 记录。

---

## 17. 状态标签与业务类型强类型规范 (Type-Safe State & Tab Enum Rule)

- **严禁在 ViewModel、UiState、Composable UI 中使用硬编码魔数字符串（Magic Strings，如 `state.statisticsTab == "EXPENSE"`、`"INCOME"` 等）作为状态标志位、Tab 切换标识或类型分类**；
- **必须通过显式声明的枚举（Enum）或密封类（Sealed Class/Interface）进行强类型安全收口**（例如使用 `StatisticsTab.EXPENSE` 代替 `"EXPENSE"`）；
- **全栈杜绝字符串拼写错误引发的潜在隐患，充分利用 Kotlin 编译期类型检查与 `when` 表达式穷举完整性保障**。

---

## 18. UI 状态持有者与业务状态分层规范 (StateHolder vs UiState Separation Standard)

- **严格分离业务数据状态 (UiState) 与界面控件状态 (StateHolder)**：
  - **业务数据状态 (`[Feature]UiState`)**：由 ViewModel 管理并暴露，属于不可变的纯 Kotlin 数据类（如流水列表、账户余额、筛选标记），支持 JVM 快速单元测试；
  - **界面控件状态持有者 (`[Feature]StateHolder`)**：由 Compose UI 树通过 `@Composable fun remember[Feature]StateHolder(...)` 创建并持有，统一管理 `PagerState`、`LazyListState`、滚动动画计算、系统契约回调（如 `ActivityResultLauncher`）以及底层副作用（`[Feature]Effects`）的生命周期挂载。
- **严禁将 UI 控件对象放入 ViewModel**：禁止在 ViewModel 或 `UiState` 中持有 `PagerState`、`LazyListState` 等持有 Compose 布局或 Context 引用的对象，防止屏幕旋转或配置变更时发生内存泄漏；
- **Screen 纯声明式排版**：Screen Composable 头部通过一行 `val holder = remember[Feature]StateHolder(...)` 收口所有控制器，开门见山声明 `BaseScreenScaffold` 与视图布局，消灭散落逻辑。

---

## 19. Kotlin 惯用排版与分号禁用规范 (Kotlin Idiomatic Formatting & Semicolon Prohibition)

- **严禁使用分号 (`;`) 将多个语句合并在同一行**：
  - 必须遵循 Kotlin 惯用的“一行一语句”原则；
  - 严禁通过分号来刻意压缩代码行数（如 `applyA(); updateB()`），这会严重破坏代码的可读性、调试断点的精准度以及 Git Diff 的清晰度；
  - 除非是在极少数为了配合特定语法（如枚举类中带方法时必须在枚举项末尾加分号）的情况，否则全项目严禁出现任何不必要的分号。

---

## 20. 金额展示布局防变形、严禁缩略与自适应缩小规范 (Amount Layout Anti-Deformation & Zero-Truncation Standard)

- **布局设计双向极端值考量 (Small & Large Amount Extremes)**：
  - 在设计任何展示金额的组件布局（如账户余额、分类结余、预算进度、账单条目等）时，**必须同时全面考虑金额数值极小（如 `￥0`、`￥0.01`）与极大（如千万/亿级 `￥99,999,999.00`）两种边界场景**；
  - **严禁布局变形与异常换行**：必须确保在数值极大或极小时，组件布局**绝对不会变形、挤压错位，也严禁发生非预期的断词折行（Unintended Line Wrapping）**；
- **严禁截断与缩略 (Strictly Zero Truncation & No Ellipsis)**：
  - 金额是记账类 App 的核心生命线，**绝对不允许使用省略号截断（`TextOverflow.Ellipsis` / `android:ellipsize="end"`）**，也**严禁任何形式的字符缩略（如 `￥12...` 或 `￥99...`）**，杜绝误导用户或引发财务信息遗漏；
- **自适应字号缩小机制 (Mandatory Auto-Resize Font Scaling)**：
  - **如果空间实在展示不下，必须通过自适应字体缩小（Auto-Resize）来完整呈现**，保持单行展示并将字号从标准字阶平滑缩小至设定的下限字阶：
    1. **Compose UI 视图**：
       - 所有金额展示统一使用通用组件 `CommonText`，配置 `maxLines = 1` 并显式开启 `autoResize = true`；
       - 必须配合合理配置 `minFontSize`（例如主卡片大金额 `targetFontSize = 24.sp, minFontSize = 14.sp`；列表副金额 `targetFontSize = 14.sp, minFontSize = 9.sp`），确保在极端大金额或极窄屏设备上等比缩小，完整展示每一位数字、小数与货币符号；
    2. **水平 Row 排版防挤压规范**：
       - 在“标题/分类 + 金额”、“图标 + 金额”等水平排布中，需明确伸缩优先级（如标题使用 `Modifier.weight(1f, fill = false)`，金额配置单行自适应缩放），防止金额膨胀时将同行其他元素挤出屏幕边界；
    3. **桌面小部件 / RemoteViews XML**：
       - 严禁在金额 `TextView` 上声明 `android:ellipsize="end"`；
       - 必须配置原生自适应字号属性 `android:autoSizeTextType="uniform"` / `app:autoSizeTextType="uniform"`，配合 `autoSizeMinTextSize` 与 `autoSizeMaxTextSize`，确保 RemoteViews 在各类启动器与桌面分辨率下完整呈现无缺漏；
- **Preview 边界值验证要求**：
  - 编写 `@Preview` 预览时，应有意识地 Mock 极端大金额（如 `￥88,888,888.88`）或包含多语言长前缀的金额状态，验证布局抗压能力。

---

## 21. 金额格式化末尾零省略与整数精简规范 (Trailing Zero Amount Truncation Standard)

- **核心原则**：
  - 在全项目所有金额展示场景中，若金额格式化后末尾为 `".00"` 或 `".0"`，**必须自动转换为纯整数格式，移除小数点及后续无意义的零**；
  - 示例：
    - `100.00` -> `"100"`（展示为 `￥100`，而不是 `￥100.00`）
    - `50.0` -> `"50"`（展示为 `￥50`）
    - `12.50` -> `"12.50"` 或 `"12.5"`（若有非零小数如 `12.34`，则完整保留展示为 `￥12.34`）
- **统一扩展函数规范**：
  - 全局统一使用封装好的标准金额格式化扩展：
    ```kotlin
    fun Double.formatAmount(): String {
        val str = "%.2f".format(this)
        return when {
            str.endsWith(".00") -> str.removeSuffix(".00")
            str.endsWith(".0") -> str.removeSuffix(".0")
            else -> str
        }
    }
    ```
  - 禁止在各个 UI 处手动拼装不一致的正则或字符串截断，确保全 App（流水明细、结余卡片、预算看板、桌面小部件等）风格统一清爽，进一步提升屏幕空间利用率并杜绝挤压溢出。

---

## 22. Bug 修复与小修正代码注释规范 (Bugfix & Minor Tweak Comment Standard)

- **核心原则**：在修复任何 Bug、性能微调、或者细节小修正（Minor Tweaks / Bug Fixes）时，**修改或新增的代码必须附带清晰详尽的中文注释**；
- **注释必须明确涵盖两点**：
  1. **原因分析 (Root Cause / Rationale)**：为什么要这样改？原先逻辑存在什么缺陷（如：为什么会发生时序冲突、闪烁、遮挡、白底白字不可见等）；
  2. **解决的问题 (Problem Solved / Expected Effect)**：通过这行/这段代码达成了什么效果，消除了什么具体问题；
- **杜绝无意义或纯机械翻译的注释**（例如不要只写 `// 滚动到指定页面`），必须说明背后的工程思考（例如：`// [Bugfix] 解决跨 Tab 切换时旧月份卡片闪现问题：在首帧测量前同步对齐 Pager 目标月份，防止异步协程触发前的页面闪烁`）。

---

## 23. 全面 @Composable 组件 @Preview 预览强制覆盖规范 (Mandatory @Composable @Preview Rule)

- **核心强制原则 (Mandatory Rule)**：
  - **凡是新增或封装的任何 `@Composable` UI 组件**（包括但不限于通用基础组件、功能卡片、列表项、弹窗 Dialog / 抽屉 Sheet、设置区块 Section 以及 Screen 界面），**必须强制为其添加配套的 `@Preview` 预览支持**；
  - 杜绝“盲写 UI”，确保每个新添或修改的组件在 Android Studio / IDE Compose 预览面板中免运行直接可视化走查、校验边距排版与交互态。
- **分层与单文件行数红线控制（$\le 250$ 行）**：
  1. **同文件内预览（文件行数 $\le 195$ 行）**：直接在组件文件末尾编写简洁配套的 `@Preview` 函数；
  2. **独立预览文件（文件行数接近或超过 200 行）**：**严禁为塞入 Preview 而突破 250 行代码红线**，必须在同级目录下创建专用的 `[ComponentName]Preview.kt` 文件承接预览（如 `TransactionSheetPreview.kt`、`SettingsScreenPreview.kt`、`SettingsDataCenterSectionPreview.kt`）。
- **Preview 编写基准要求**：
  1. 必须添加 `@Preview(showBackground = true)` 注解，确保预览背景及内外部边距清晰可见；
  2. 必须统一包裹在 `ListenTheme` 主题容器中，确保主题色、文字样式、暗黑/明亮色阶正常应用；
  3. 宿主 App 组件预览首行必须执行 `ExpenseStrings.init()`，确保多语言资源正常解析，杜绝预览渲染报空指针崩溃；
  4. 优先覆盖有真实数据态（代表性 Mock 数据）与典型边缘状态（如空数据态、超长文本态、告警态、编辑态等）。

---

## 24. 针对性单测与局部验证规范 (Targeted Unit Testing & Diff-Driven Verification Rule)

- **核心原则**：日常功能迭代、简单修改、局部缺陷修复或代码重构时，**严禁每次修改都无脑运行耗时极长（数分钟）的全量单测（如 `:app:testDebugUnitTest` 或全模块 `test`）**；
- **精准运行目标单测**：
  - **原则**：**只需定向单测与本次修改文件直接相关的测试类或测试方法**，以实现秒级验证与敏捷反馈；
  - **精准单测命令示例**：
    - 针对具体单测类：`./gradlew.bat :app:testDebugUnitTest --tests "com.listen.expensetracker.widget.ListenExpenseAppWidgetProviderTest"`
    - 针对特定方法：`./gradlew.bat :app:testDebugUnitTest --tests "*.ListenExpenseAppWidgetProviderTest.normalizeCategoryId*"`
  - **纯 UI / 文案调整**：若修改仅涉及 Composable 视觉呈现、Strings 文案字典或样式微调，优先执行极速语法编译检查（`./gradlew compileDebugKotlin`）甚至跳过测试构建，杜绝让用户陷入漫长等待；
- **全量单测触发时机**：
  1. 涉及底层核心基础设施（`ListenArch` 底座架构、Room 数据库迁移、跨模块核心拦截器等）的破坏性改造；
  2. 准备发版打包、或用户明确要求“跑一下全量测试 / 完整回归”。

---

## 25. 优先使用与沉淀通用 UI 组件规范 (Prioritize & Extract Common UI Components to ListenUiComponent Rule)

- **优先复用原则 (ListenUiComponent First)**：
  - 在编写或重构任何业务界面与功能卡片时，**必须优先检索并使用通用组件库 `:ListenUiComponent` 中已有的组件**，严禁在业务模块中重复手写具有通用交互或视觉特征的基础控件；
  - 核心通用组件对照索引：
    - **弹窗与抽屉**：优先使用 `CommonDialog`、`CommonBottomSheet`（替代裸写 `AlertDialog`、`ModalBottomSheet`）；
    - **提示与空态**：全局统一使用 `CommonBanner`（各类 Informative/Warning/Success/Error 提示条）、`CommonEmpty`（空数据缺省页/缺省占位）；
    - **基础控件**：优先使用 `CommonButton`（主副操作按钮）、`CommonSwitchRow`（开关设置行）、`CommonDivider`（统一内边距与透明度的分割线）、`CommonBadge`（徽标与状态标签）、`CommonText`、`SurfaceCard` 等；
    - **特色输入与图表**：优先使用 `SearchBarInput`（带防抖与清空搜索栏）、`NumericKeypad`（金额与数字软键盘）、折线图/柱状图/饼图等图表组件。
- **积极下沉与沉淀原则 (Extraction & Generalization)**：
  - **识别标准**：任何业务 UI 中实现的、不包含特定业务实体（如 Transaction、Account、Budget 等）且不绑定宿主独占文言/逻辑的纯 UI 交互控件（例如步进器 `CommonStepper`、筛选胶囊 `CommonFilterChip`、颜色选择器 `ColorPicker`、轮播卡片容器等），**应积极提取并沉淀到 `:ListenUiComponent` 模块**；
  - **沉淀设计要求**：
    1. 保持无状态（Stateless）或状态提升（State Hoisting），对外暴露标准参数与回调；
    2. 支持 Compose Preview，遵循设计规范与主题适配；
    3. 杜绝反向依赖宿主业务模块，确保作为通用库的独立可复用性。

