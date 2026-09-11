# ListenExpenseTracker - 系统演进路线图、架构设计与任务追踪 (Roadmap & Engineering Blueprint)

本文档系统性梳理 **ListenExpenseTracker (lExpense)** 从底层架构奠基到全功能落地、再到体验跃升的完整演进路线图。记录各阶段的**设计思路、实现细节、架构拓扑图、核心重点难点代码实现（配备详尽中文教学注释）**以及后续需求池规划。

---

## 1. 里程碑进度总览与工程看板 (Milestone Dashboard)

### 1.1 阶段演进概览

| 阶段 | 里程碑名称 | 核心关注点 | 状态 | 交付产物 / 规范文档 |
| :--- | :--- | :--- | :---: | :--- |
| **Stage 1** | 核心应用与库架构落地 | Composite Build 独立模块解耦、工程脚手架 | **100%** | [`ListenArch/`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/ListenArch), [`ListenUiComponent/`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/ListenUiComponent) |
| **Stage 2** | 底层 SDK 核心与通用组件实装 | Room Flow、DataStore 偏好、MVI 基类、Canvas 原生图表 | **100%** | `BaseViewModel`, `ListenTheme`, `DonutChart`, `BarChart` |
| **Stage 3** | 主 App 记账核心功能与页面实装 | 4 维流水排序、按日分组、月份胶囊、隐额、统计排行榜 | **100%** | `TransactionsScreen`, `StatisticsScreen`, `SettingsScreen` |
| **Stage 4** | 高阶体验、多账户云同步与自动化测试 | Google Drive 云同步、APM 监控、33 套测试矩阵 (126 项测试用例) | **100%** | `CloudSyncManager`, `ApmLogger`, `TraceManager` |
| **Stage 5** | 体验精细化、CI/CD 与架构解耦 | 资产账户管理重构、微触觉震动、图表平滑动效、单文件 $\le 250$ 行 | **100%** | `AccountManageDialog`, `DonutChart` (Animatable), `deploy.yml` |
| **Stage 6** | 异步更新机制与版本调度 | 静态托管 `version.json`、SemVer 语义双轨比对、更新弹窗 | **100%** | `UpdateCheckerService`, `UpdateAvailableDialog` |
| **Stage 7** | 严格分类预算管理与复合过滤器 | 440dp 恒定高度单模态、整数均分算法、Pager 跟手切月 | **100%** | `CategoryBudgetModalDialog`, `TransactionFilterBottomSheet` |
| **Stage 8** | 高优先级功能规划与深度体验跃升 | 周期订阅账单、Widget 2.0、财务洞察、防窥应用锁、本地通知预警 | **进行中** | 见下文 Stage 8 专项规划与落地记录 |
| **Backlog** | 需求池与长期探索性功能 | 预算跨月结转、架构可视化面板、向量时钟增量差分同步 | **待排期** | 详见本文档第 4 节需求池 |

### 1.2 系统架构演进全景拓扑图

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           【表现层 / 交互驱动 (Presentation Layer)】                             │
│  MainActivity  ─┬─ TransactionsScreen (4维流水/日汇总/双FAB/长按重置)                             │
│                 ├─ StatisticsScreen   (财务洞察轮播/环形占比/趋势折线/排行榜)                     │
│                 ├─ SettingsScreen     (数据中心/Google Drive/资产账户/开发者模式)                │
│                 └─ AppWidget 2.0      (4大高频闪电直达/5级自适应字号/3态进度条互斥显隐)           │
└─────────────────────────────────┬───────────────────────────────────────────────────────────────┘
                                  │ Jetpack Compose UI / RemoteViews
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           【状态管理与领域调度层 (Domain & State Layer)】                       │
│  ExpenseAppState (单例状态协调器: 年月双向联动 / 下钻返回保护 / 快速置顶双击复位 / 闪电记账路由)  │
│  TransactionsViewModel ── (MVI: handleIntent / updateState / emitEffect: CommonUiEffect)        │
│  StatisticsViewModel   ── (MVI: handleIntent / updateState / emitEffect: CommonUiEffect)        │
└─────────────────────────────────┬───────────────────────────────────────────────────────────────┘
                                  │ 纯函数领域模型聚合驱动
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           【计算与决策引擎层 (Pure Functional Engines)】                        │
│  TransactionCalculationEngine  │ 多维收支聚合 / 4维排序 / 按日分组汇总 / 复合过滤                 │
│  CategoryBudgetEngine          │ 80%警戒 / 100%超支三态健康度判定 / 整数均分算法 (无浮点漂移)   │
│  FinancialInsightEngine        │ 9大财务指标 / 月环比(MoM) / 消费斜率预测 / 方差离散度           │
│  RecurringTransactionEngine    │ 周期规则冷启动巡检 / 自动入账履约 / 下次执行时间递进           │
│  BudgetAlertGuard & Notifier   │ 本地通知防骚扰去重状态机 (yyyy_MM:target:LEVEL)                │
└─────────────────────────────────┬───────────────────────────────────────────────────────────────┘
                                  │ 数据存取与监控
                                  ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           【底层独立 SDK 体系 (Composite Build SDKs)】                          │
│  ┌─────────────────────────────────────────────┐ ┌─────────────────────────────────────────────┐ │
│  │             ListenArch SDK                  │ │            ListenUiComponent SDK            │ │
│  │ - Room DB (TransactionDao, RecurringDao)    │ │ - ListenTheme (6+ AccentColor 调色盘)       │ │
│  │ - DataStore (语言/货币/预算/账户 Flow)       │ │ - 纯 Canvas 图表 (DonutChart, LineChart)     │ │
│  │ - BaseViewModel & ResultExtensions         │ │ - NumericKeypad (微触觉震动按键)            │ │
│  │ - APM (ApmLogger, TraceManager, CrashGuard) │ │ - SurfaceCard, CommonButton, CommonEditText │ │
│  └─────────────────────────────────────────────┘ └─────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 各阶段设计思路、实现细节与重点难点代码剖析

---

### 阶段一：核心应用与库架构落地 (Stage 1 - 100% Completed)

#### 1. 设计思路与架构决策
- **痛点**：传统 Android 单体模块（Monolithic Architecture）随着业务扩展，极易出现跨层级直接依赖、循环引用、构建耗时线性增长、无法跨项目复用核心技术底座等问题。
- **架构决策**：全面拥抱 Gradle **Composite Build (组合构建)**。将系统划分为 1 个主工程 (`app`) 与 2 个完全独立的 SDK 工程（`ListenArch` 与 `ListenUiComponent`）。
- **优势**：
  1. **零代码耦合**：SDK 具备独立的 `settings.gradle.kts`、`build.gradle.kts` 与版本号生命周期，物理级隔离业务代码；
  2. **快速增量编译**：Gradle 对独立构建进行完全隔离的任务缓存（Configuration Cache），SDK 未变动时不参与重编；
  3. **依赖倒置原则**：主 App 依赖 SDK 接口与抽象模型，底层组件严禁反向依赖主 App 的任何类。

#### 2. 实现细节与拓扑配置
主工程 `settings.gradle.kts` 通过 `includeBuild` 建立映射：

```kotlin
// settings.gradle.kts
rootProject.name = "ListenExpenseTracker"

// 引入独立的系统架构底座 SDK
includeBuild("ListenArch")
// 引入独立的跨端通用 UI 设计系统 SDK
includeBuild("ListenUiComponent")

include(":app")
```

---

### 阶段二：底层 SDK 核心与通用组件实装 (Stage 2 - 100% Completed)

#### 1. 设计思路与架构决策
- **数据层设计**：使用 Room 持久化核心交易流水（`TransactionEntity`），通过 `Flow<List<TransactionEntity>>` 实现全异步响应式数据流。偏好设置采用 Preferences DataStore，提供强类型、跨进程安全的设置项读取。
- **MVI 状态机与错误收敛**：
  - 定义 `BaseViewModel<State, Intent>` 基类；
  - 传统 MVI 允许每个 ViewModel 自定义 `Effect` 接口，但极易造成全局 UI 处理分散；
  - **核心设计**：将 UI 一次性副作用强制统一固化为 `CommonUiEffect`（如弹出 Toast、唤起全局 Loading、展示异常信息等），极大降低页面处理复杂度。
- **UI 设计系统自研原则**：
  - 坚决杜绝引入第三方开源图表库（如 MPAndroidChart），彻底避免引入无用代码（平均削减 APK 体积约 8~12MB）；
  - 全面基于 Jetpack Compose 原生 `Canvas` 绘制环形图 (`DonutChart`)、折线图 (`LineChart`) 与柱状图 (`BarChart`)，具备极佳的重组性能与深浅色适配灵活性。

#### 2. 重点/难点代码解析：MVI 通用基类与状态流转机制

```kotlin
/**
 * MVI 通用抽象基类 (BaseViewModel)。
 * 严格收拢单向数据流 (Unidirectional Data Flow)：
 * UI 发送 Intent -> handleIntent 处理领域逻辑 -> updateState 变更只读 State -> emitEffect 触发单次瞬态副作用。
 */
abstract class BaseViewModel<State, Intent>(initialState: State) : ViewModel() {

    // 1. 内部可变状态流，仅限 ViewModel 内部通过 updateState() 安全变更
    private val _viewState = MutableStateFlow(initialState)
    // 对外暴露只读 StateFlow，UI 界面通过 collectAsStateWithLifecycle() 监听
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    // 2. 一次性副作用通道 (Single-event Side Effect Channel)
    // 采用 BUFFERED 容量，确保在 UI 处于后台或配置变更重建时不丢失关键 Toast 或弹窗事件
    private val _effectChannel = Channel<CommonUiEffect>(Channel.BUFFERED)
    val effectFlow: Flow<CommonUiEffect> = _effectChannel.receiveAsFlow()

    /**
     * 业务 Intent 分发入口，由子类实现具体的业务决策与用例调用。
     */
    abstract fun handleIntent(intent: Intent)

    /**
     * 线程安全的状态更新算子。
     * 利用 MutableStateFlow.update 原子操作，避免多协程并发写入导致的状态覆盖问题。
     */
    protected fun updateState(reducer: State.() -> State) {
        _viewState.update { currentState -> currentState.reducer() }
    }

    /**
     * 发射 UI 瞬态副作用（如 Toast 提示、导航跳转指令）。
     * 在 ViewModelScope 异步发射，避免阻塞当前主调用线程。
     */
    protected fun emitEffect(effect: CommonUiEffect) {
        viewModelScope.launch {
            _effectChannel.send(effect)
        }
    }
}
```

---

### 阶段三：主 App 记账核心功能与页面实装 (Stage 3 - 100% Completed)

#### 1. 设计思路与架构决策
- **时间流驱动中枢**：记账的核心维度是时间。顶部固定居中**时间胶囊**（`[ < 2026年08月 > ]`），支持点击呼出 `MonthPickerDialog` 快速穿梭历史，支持左右箭头平滑切月。
- **4 维排序与日汇总流式明细**：
  - 流水明细按天分组（`formatDayGroupHeader`），展示当日支出合计与收入合计；
  - 提供 4 维复合排序引擎（时间降序、时间升序、金额降序、金额升序）；
  - 单笔长按调起二次确认弹窗进行破坏性删除。
- **隐私保护与一键隐额**：
  - 顶部结余总览卡片提供小眼睛切换按钮，状态持久化至 DataStore，一键将敏感金额替换为掩码 `••••`。

#### 2. 重点/难点代码解析：多维流水计算与按日分组聚合算法

```kotlin
/**
 * 纯函数流水计算引擎 (TransactionCalculationEngine)。
 * 严格保持无状态与零副作用，输入纯列表与计算参数，输出高阶视图模型。
 */
object TransactionCalculationEngine {

    /**
     * 对原始流水列表进行排序、筛选并按自然日降序聚合分组。
     */
    fun groupAndSortTransactions(
        transactions: List<TransactionEntity>,
        sortOrder: TransactionSortOrder,
        lang: String
    ): List<DayGroupedTransactions> {
        // 1. 首先依据用户选定的 4 维策略进行稳定排序
        val sortedList = when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> transactions.sortedWith(
                compareByDescending<TransactionEntity> { it.date }.thenByDescending { it.id }
            )
            TransactionSortOrder.DATE_ASC -> transactions.sortedWith(
                compareBy<TransactionEntity> { it.date }.thenBy { it.id }
            )
            TransactionSortOrder.AMOUNT_DESC -> transactions.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> transactions.sortedBy { it.amount }
        }

        // 2. 按自然日零点毫秒戳进行 GroupBy 聚合
        return sortedList.groupBy { tx ->
            getStartOfDayTimestamp(tx.date)
        }.map { (dayStartTimestamp, dayTransactions) ->
            // 计算当天总支出与总收入，严格排除非支出类型的污染
            val totalExpense = dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val totalIncome = dayTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            
            DayGroupedTransactions(
                dayStartTimestamp = dayStartTimestamp,
                headerTitle = formatDayGroupHeader(dayStartTimestamp, lang),
                dayExpense = totalExpense,
                dayIncome = totalIncome,
                transactions = dayTransactions
            )
        }
    }
}
```

---

### 阶段四：高阶体验、多账户云同步与自动化测试 (Stage 4 - 100% Completed)

#### 1. 设计思路与架构决策
- **多账户隔离的 Google Drive 云端快照**：
  - 接入 AndroidX `CredentialManager` 获取 Google 授权令牌；
  - 解决多用户切换串号痛点：采用**账号级隔离存储路径**，云端文件命名携带用户哈希，备份内容包含全量 JSON 实体与 MD5 校验和（Checksum），防止因网络中断导致数据损坏；
- **全链路 APM 可观测性系统**：
  - 内存无锁环形队列：固定容量 500 条（`ApmLogger`），自动滚动覆盖老日志，高频打点零 GC 负担；
  - 链路毫秒级追踪：`TraceManager` 支持跨协程的短 TraceId 生成与端到端耗时分析；
  - 未捕获异常双写：`CrashHandler` 拦截崩溃时，不仅写入系统崩溃栈，同时同步持久化到 APM 环形缓冲区，以便下次启动时上报。
- **33 套自动化单元测试全矩阵 (126 项测试用例 100% Pass)**：
  - 涵盖 `ApmLoggerTest`、`TraceManagerTest`、`CloudSyncManagerTest`、`TransactionCalculationEngineTest`、`CategoryBudgetEngineTest`、`BaseViewModelTest` 等 33 套独立测试类共计 126 项用例，执行 `./gradlew testDebugUnitTest`（57 项 Gradle 组合构建任务）保持 100% 绿灯，保证零 Regression。

#### 2. 重点/难点代码解析：APM 500 条环形内存日志管理器

```kotlin
/**
 * APM 环形日志管理器 (ApmLogger)。
 * 采用环形缓冲区 (Circular Buffer) 原理，固定 500 条内存槽位，零内存膨胀。
 */
object ApmLogger {
    private const val MAX_LOG_CAPACITY = 500
    // 使用双端队列 LinkedList 配合对象锁，确保多线程并发打点安全
    private val logBuffer = LinkedList<ApmLogItem>()
    private val lock = Any()

    fun log(level: ApmLogLevel, tag: String, message: String) {
        val entry = ApmLogItem(
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            threadName = Thread.currentThread().name
        )
        synchronized(lock) {
            // 当达到容量阈值时，弹出最早的一条日志，压入新日志
            if (logBuffer.size >= MAX_LOG_CAPACITY) {
                logBuffer.removeFirst()
            }
            logBuffer.addLast(entry)
        }
    }

    /**
     * 发生致命崩溃时，瞬时导出快照列表（防并发浅拷贝）供 CrashHandler 记录。
     */
    fun dumpLogs(): List<ApmLogItem> {
        synchronized(lock) {
            return ArrayList(logBuffer)
        }
    }
}
```

---

### 阶段五：体验精细化、CI/CD 自动化与架构组件解耦 (Stage 5 - 100% Completed)

#### 1. 设计思路与架构决策
- **PROMPTS.md 单文件 $\le 250$ 行红线治理**：
  - 早期 `AccountManageDialog.kt` 膨胀至 400+ 行，严重违反单一职责；
  - 拆分抽取为 4 个独立小文件：`AccountCardItem.kt`（视觉行卡片）、`AccountEditDialog.kt`（增改弹窗）、`AccountDeleteConfirmDialog.kt`（破坏性操作二次确认，遵从 Rule 15 红色警告样式）以及 `AccountManageDialog.kt`（精简至 220 行以内调度器）。
- **Compose Tab 滚动状态记忆 (State Retention)**：
  - 在 `MainActivity` 引入 `SaveableStateHolder` 配合 `rememberSaveable`，为每个 Tab 和不同月份列表分配唯一的 SaveableKey，彻底解决切换 Tab 导致列表滚回顶部的经典体验缺陷。
- **物理微触觉反馈与图表扫掠动效**：
  - `NumericKeypad` 按键轻触伴随轻微震动（`TextHandleMove`），完成记账触发强烈确认震动（`LongPress`）；
  - `DonutChart` 圆环 650ms 顺时针扫开，`LineChart` 600ms 阻尼升起。

---

### 阶段六：异步更新机制与版本调度 (Stage 6 - 100% Completed)

#### 1. 设计思路与架构决策
- **零服务端极简更新系统**：
  - 传统方案需要部署应用更新服务器，存在成本与运维负担；
  - **创新方案**：将更新元数据托管于 GitHub Pages 静态文件 (`version.json`)：
    ```json
    {
      "versionName": "1.2.0",
      "versionCode": 10200,
      "releaseDate": "2026-09-10",
      "changelog": { "zh": "...", "en": "...", "ja": "..." },
      "downloadUrl": "https://play.google.com/store/apps/details?id=com.listen.expensetracker"
    }
    ```
  - 免鉴权、全球 CDN 加速、零服务器成本，结合 SemVer 与 versionCode 双轨比对。

---

### 阶段七：严格分类预算管理与全功能交互体验升级 (Stage 7 - 100% Completed)

#### 1. 设计思路与架构决策
- **单一模态零闪烁宿主 (`CategoryBudgetModalDialog`)**：
  - 早期方案在“查看分类预算看板”与“编辑预算比例”之间通过弹出关闭两个 `AlertDialog` 切换，引起惨烈的蒙层重构白屏闪烁；
  - **解决方案**：统一收拢在单个 `CommonDialog` 宿主中，使用 `AnimatedContent` 驱动内部视图水平推移与交叉淡入淡出（`slideInHorizontally + fadeIn`）。
- **440dp 恒定高度锁定**：
  - 容器外层绝对锁定 `Modifier.height(440.dp)`，列表使用 `Modifier.weight(1f)` 填充剩余空间，消灭因分类数据增减导致的对话框拉伸跳动。
- **纯整数分区均分算法 (Integer Partitioning)**：
  - 浮点数比例（如 $100 / 7 = 14.2857\%$）在多次加减舍入后必然累积误差，导致总和偏离 100%；
  - 算法改用**整数百分比分配法**，计算商与余数，保证总分配比例严格恒等于 100%。

#### 2. 重点/难点代码解析：纯整数分区均分算法

```kotlin
// CategoryBudgetEditContent.kt
// 整数均分算法：保证分配总和严格恒等于 100%，无任何浮点舍入漂移
val currentIntMap = categories.associate { cat ->
    cat.id to ((ratios[cat.id] ?: 0f) * 100).roundToInt()
}.toMutableMap()

val currentTotal = currentIntMap.values.sum()
val diff = 100 - currentTotal

// 如果当前总分配未满 100%，将剩余差额均分给各个分类
if (diff > 0 && categories.isNotEmpty()) {
    // 1. 计算每个分类应得的基础增量 (整除商)
    val base = diff / categories.size
    // 2. 计算无法整除的余数 (0 <= rem < size)
    val rem = diff % categories.size
    
    // 3. 将基础增量分配给所有人，余数优先补齐前 rem 个分类
    categories.forEachIndexed { i, cat ->
        val extra = if (i < rem) 1 else 0
        currentIntMap[cat.id] = (currentIntMap[cat.id] ?: 0) + base + extra
    }
    
    // 4. 将整数映射恢复为标准比率浮点数 (如 14 -> 0.14f)
    onRatiosChange(currentIntMap.mapValues { it.value / 100f })
}
```

---

### 阶段八：高优先级功能规划与深度体验跃升 (Stage 8 Roadmap & Implementation)

---

#### 1. 周期性固定收支与订阅管理 (Recurring Transactions & Subscriptions) - [P0, 已落地]
* **详细设计文档**：[`docs/recurring_transactions_and_subscriptions_design.md`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/docs/recurring_transactions_and_subscriptions_design.md)
* **核心落地架构**：
  - 定义 `RecurringRuleEntity` 与 `RecurringRuleDao`；
  - 实现 `RecurringTransactionEngine`：冷启动时自动比对当前时间戳与 `nextExecutionTimestamp`，自动计算跨周期补漏记账，并推进下次执行日期；
  - UI 落地 `RecurringTransactionsDialog`，展示固定支出 Baseline 与占月预算比例卡片。

#### 2. 桌面小部件体验 2.0 (App Widget 2.0) - [P0, 已落地]
* **详细设计文档**：[`docs/app_widget_2_0_design.md`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/docs/app_widget_2_0_design.md)
* **核心难点与落地实现**：
  - **2 秒闪电记账**：外露 4 大高频场景（🍔 餐饮、🚗 交通、🛍️ 购物、📦 杂项），点击通过双源 DeepLink（URI + Intent Extras）直达记账键盘并预选分类；
  - **5 级动态降级字号**：根据金额文本长度在 `18sp` 到 `9.5sp` 间平滑降级，彻底解决百万金额截断出现 `...` 的难题；
  - **三态彩色进度条互斥显隐**：预置 3 条彩色 ProgressBar，运行时通过 `VISIBLE / GONE` 互斥切换，攻克 Android 定制系统 `progressTintList` 失效问题；
  - **防误触架构**：彻底解除外层根布局点击，仅在文本与按钮区绑定交互，消除桌面滑屏误触。

#### 3. 智能财务洞察与深度环比分析 (Smart Financial Insights & MoM) - [P1, 已落地]
* **详细设计文档**：[`docs/smart_financial_insights_design.md`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/docs/smart_financial_insights_design.md)
* **核心算法实现**：
  - 研发 `FinancialInsightEngine`，包含 9 大分析诊断公式：
    - **月环比公式 (MoM)**：$\text{GrowthRate} = \frac{E_{curr} - E_{prev}}{E_{prev}} \times 100\%$；
    - **预算耗尽日预测公式**：$\text{Slope} = \frac{E_{curr}}{Day_{now}}$，$\text{PredictedExhaustDay} = \frac{\text{TotalBudget}}{\text{Slope}}$；
    - **支出方差离散度评分**：计算日支出的均方差，评估消费是平稳还是冲动突发。
  - 统计页顶部轮播展示 `InsightCards`，并引入 12 个月年度趋势柱状图。

#### 4. 生物识别应用锁与隐私防窥模式 (Biometric App Lock & Privacy Shield) - [P1, 已落地]
* **详细设计文档**：[`docs/biometric_security_and_privacy_design.md`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/docs/biometric_security_and_privacy_design.md)
* **核心实现与防窥架构**：
  - 接入 `BiometricSecurityManager`：结合 `BIOMETRIC_STRONG or DEVICE_CREDENTIAL`，实现指纹/面容与备用锁屏密码双轨容灾；
  - 多任务防窥：动态注入 `WindowManager.LayoutParams.FLAG_SECURE`，阻止 Recent Apps 截屏泄露隐私；
  - 物理手势摇一摇隐额：`ShakeDetector` 监听加速度传感器，具备向量差计算与 1000ms 节流防抖。

##### 🔑 重点/难点代码解析：摇一摇加速度传感器防抖监听器

```kotlin
// ShakeDetector.kt
override fun onSensorChanged(event: SensorEvent?) {
    if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

    val x = event.values[0]
    val y = event.values[1]
    val z = event.values[2]

    // 1. 将三轴重力加速度归一化 (除以地球标准重力 g = 9.8 m/s²)
    val gX = x / SensorManager.GRAVITY_EARTH
    val gY = y / SensorManager.GRAVITY_EARTH
    val gZ = z / SensorManager.GRAVITY_EARTH

    // 2. 计算空间合成总重力矢量模长: |G| = sqrt(gx² + gy² + gz²)
    val gForce = sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()
    // 3. 剔除恒定存在的 1G 地球重力背景值，提取动态晃动净加速度
    val accelerationDelta = (gForce - 1.0f) * SensorManager.GRAVITY_EARTH

    // 4. 判定是否突破设定晃动阈值 (默认 13.0f m/s²)
    if (accelerationDelta > thresholdAcceleration) {
        val now = SystemClock.elapsedRealtime()
        // 5. 严格的时间戳节流过滤 (默认 1000ms)，杜绝一次甩动引发多次误触发
        if (now - lastShakeTimestamp >= throttleIntervalMs) {
            lastShakeTimestamp = now
            ApmLogger.i("ShakeDetector", "Shake gesture detected (delta: $accelerationDelta)")
            onShake() // 触发全局隐额模式切换
        }
    }
}
```

#### 5. 本地智能通知预警与提醒中枢 (Local Notification & Alert Hub) - [P1, 设计完成，待工程落地]
* **详细设计文档**：[`docs/local_notification_system_design.md`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/docs/local_notification_system_design.md)
* **任务清单与实施计划**：
  - [ ] **统一通知底座与渠道矩阵**：实现 `LocalNotificationManager`，注册 `channel_budget_alerts` (高优先级)、`channel_recurring_bills` (默认优先级)、`channel_app_updates` (静默/默认优先级)；适配 Android 13+ `POST_NOTIFICATIONS` 运行时动态权限。
  - [ ] **预算预警状态机与去重引擎**：实现 `BudgetAlertGuard`，监测 80% 警戒线与 100% 超支线，以 `yyyy_MM:targetId:LEVEL` 为去重键防骚扰，点击直达 `CategoryBudgetModalDialog`。
  - [ ] **周期账单履约通知**：`RecurringTransactionEngine` 入账后聚合通知，点击直达流水高亮。
  - [ ] **静默版本检测通知**：与 `UpdateCheckerService` 联动，带 3 天冷却防打扰。
  - [ ] **设置页通知管理面板**：提供集中控制卡片与独立场景开关。

#### 6. 跨 Tab 年月视图全维度联动与下钻保护 (Year/Month Linkage & Drilldown Protection) - [已落地]
* **设计思路**：解决用户在“统计”页看到年度走势，点击 8 月柱状图下钻到 8 月“流水”页，随后返回“统计”页时，统计页被错误重置为月视图的典型状态竞争 Bug。
* **状态机解决方案**：在 [`ExpenseAppState.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/core/state/ExpenseAppState.kt) 引入 `preserveStatisticsYearOnReturn` 独占状态锁。

##### 🔑 重点/难点代码解析：下钻保护与双向同步状态机

```kotlin
// ExpenseAppState.kt
private fun syncTimeState(fromTab: NavTab, toTab: NavTab) {
    if (fromTab == NavTab.TRANSACTIONS && toTab == NavTab.STATISTICS) {
        val stats = statisticsViewModel.viewState.value
        val tx = transactionsViewModel.viewState.value
        
        // 核心下钻保护：如果上一次是从统计页年视图点击月份下钻至流水页，
        // 此时返回统计页必须原样保持年视图，严禁强制将统计页降级覆盖为月视图！
        if (preserveStatisticsYearOnReturn && stats.period == StatisticsPeriod.YEAR && tx.period == TransactionPeriod.MONTH) {
            preserveStatisticsYearOnReturn = false
            return
        }
        preserveStatisticsYearOnReturn = false
        
        // 常规跨 Tab 切换：根据流水页视图模式，对齐统计页的统计维度
        val targetPeriod = if (tx.period == TransactionPeriod.YEAR) StatisticsPeriod.YEAR else StatisticsPeriod.MONTH
        if (stats.period != targetPeriod) statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(targetPeriod))
        if (targetPeriod == StatisticsPeriod.MONTH && stats.selectedMonthOffset != tx.selectedMonthOffset) {
            statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(tx.selectedMonthOffset))
        } else if (targetPeriod == StatisticsPeriod.YEAR && stats.selectedYearOffset != tx.selectedYearOffset) {
            statisticsViewModel.handleIntent(StatisticsIntent.SetYearOffset(tx.selectedYearOffset))
        }
    }
}
```

#### 7. 人体工学操作优化与交互细节打磨 (Ergonomic UX Refinements) - [已落地]
- **全行热区标准化**：所有 Switch 迁移至 `CommonSwitchRow`，全行可点；
- **流水页右下角双 FAB 黄金热区**：主记账 `+` 悬浮按钮伴随次级筛选按钮，支持长按 500ms 震动一键重置所有已生效的复合过滤；
- **导航栏双击快速复位**：双击当前已选中的 Tab 按钮，直接平滑复位至当月/当年（`offset = 0`），无需多次点击左右箭头。

---

## 3. 核心技术规范与质量红线 (Engineering Guardrails)

项目演进过程中，所有参与开发的工程师与代码贡献均需严格恪守以下 5 大硬性指标：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   【架构开发 5 大质量红线】                                     │
├───────────────────┬─────────────────────────────────────────────────────────────────────────────┤
│ 1. 物理单文件红线 │ 任何单个 Kotlin 源码文件严格控制在 250 行以内 (超出必须遵循职责边界抽取)。 │
│ 2. 零三方图表原则 │ 坚决杜绝引入第三方臃肿图表库，纯原生 Canvas 绘制，严守 APK 极小体积标准。  │
│ 3. 零 Mermaid 规范│ Markdown 文档禁止出现 ```mermaid 代码块，统一采用纯文本 ASCII / 表格排版。 │
│ 4. MVI 纯函数收敛 │ 领域计算引擎必须是 Pure Function，零状态、零副作用，只输入输出。          │
│ 5. 单元测试 100%  │ 任何核心计算或重构改动必须保证 ./gradlew testDebugUnitTest 全部绿灯通过。  │
└───────────────────┴─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. 需求池与长期探索性架构备选 (Backlog & Future Explorations)

以下需求作为长期演进池，按技术可行性与用户价值排序：

- [ ] **1. 分类预算跨月结转机制 (Budget Rollover)**
  - **业务场景**：用户本月“餐饮”预算设定 2,000 元，实际仅消费 1,600 元，剩余的 400 元可由用户自主勾选“结转至下月”，使下月餐饮可用预算临时上调为 2,400 元。
  - **技术方案**：在 DataStore 或 Room 中增设 `BudgetRolloverEntity`，在月度切换结算时计算滚入额度，并提供结转流水标记。
- [ ] **2. 架构设计全景可视化面板 (Architecture Visualizer)**
  - **业务场景**：在开发者设置面板中，点击“架构全景视窗”，以 Canvas 动态拓扑图渲染当前 App 的依赖注入拓扑、ViewModel 数据流订阅数与 Room 活跃观察者状态。
- [ ] **3. Google Drive 增量同步与向量时钟合并 (Vector Clock Incremental Sync)**
  - **业务场景**：当前采用全量快照 JSON 覆盖。当流水数据达到数万笔时，每次全量上传耗费流量；
  - **技术方案**：为 `TransactionEntity` 增设 `updatedAt` 与 `isDeleted` 软删除标记，采用增量版本向量（Vector Clock）比对两端变更差分，实现高效增量双向合并。
