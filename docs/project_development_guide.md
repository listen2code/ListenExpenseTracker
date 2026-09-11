# ListenExpenseTracker - 项目开发与架构设计工程指南
(Project Development & Architecture Engineering Guide)

本文档汇集了 Listen 系列软件工程哲学、MVI 分层架构设计、可观测性 APM 体系、设计系统与质量管控红线，作为 `ListenExpenseTracker` 及其底层跨模块库（`ListenArch`、`ListenUiComponent`）的核心编码指南与演进方针。

---

## 1. 软件工程哲学 (Software Philosophy & Principles)

1. **真实可维护优先 (Maintainability First)**：
   - 绝不为了炫技引入冷门或重型第三方库（例如彻底摒弃重量级 Arrow 函子库与庞大的 Google API Client Java SDK）；
   - 优先保证应用在**离线、无网、弱网、进程意外被杀、高频快速记账**等极端复杂现实场景下的最高稳定性与可维护性。
2. **可观测性高于炫技 (Observability over Cleverness)**：
   - 建立覆盖业务、同步、数据库与崩溃的四大日志频道；
   - 贯穿全局唯一的 `TraceId` 分布式追踪链，任何用户交互或数据流转均具备完整的调用上下文与耗时度量；
   - 建立防二次崩溃全局异常捕获（`CrashHandler`）与沙盒日志双写持久化机制。
3. **“实事求是”的文档规则 (Pragmatic Documentation)**：
   - `README.md` **只记录已稳定落地的生产级能力**与架构概况；
   - 所有的详细设计规范、技术方案（Spec）与排错指南**统一下沉至 `docs/` 目录**中；
   - **文档排版红线**：为了保障在 Android Studio 内置 Markdown 预览引擎中 100% 正常渲染，**严禁引入 ````mermaid` 代码块**，全篇图表统一采用标准 Unicode / ASCII 字符框线拓扑图与 Markdown 原生表格。
4. **零告警与单文件 250 行红线 (Zero-Warning & File Size Limit)**：
   - 所有 Kotlin 源码必须通过项目自定义 Lint 规则集（16 条架构红线）与严格静态检查；
   - 遵守严格的组件粒度控制：**单个 Kotlin 源文件代码行数必须严格控制在 $\le 250$ 行以内**，复杂逻辑通过 `Delegate` 模式与 `StateHolder` 模式彻底解耦。

---

## 2. 软件架构设计 (MVI + Clean Architecture)

系统基于 Android Native (Kotlin 2.x + Jetpack Compose) 的 **MVI (Model-View-Intent)** 单向数据流 (Unidirectional Data Flow, UDF) 与清晰架构分层模式：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  【表现层 (Presentation Layer)】                                 │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│   Composable Screens (TransactionsScreen, SettingsScreen, AnalyticsScreen, QuickAddDialog)      │
│   - 声明式 UI 树构建，纯函数式组件渲染，不持有任何业务与数据库状态                             │
│   - 观察 StateFlow<UiState> 触发局部智能重组 (Smart Recomposition)                             │
└───────────────────────────────┬─────────────────────────────────▲───────────────────────────────┘
                                │ 1. 派发 User Intent             │ 2. 消费 UiState 快照
                                ▼                                 │ 3. 消费 CommonUiEffect 副作用
┌─────────────────────────────────────────────────────────────────┴───────────────────────────────┐
│                               【状态机与调度层 (ViewModel & Delegate)】                          │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│   BaseViewModel<State, Intent>  <───>  Business Delegates (SettingsSyncDelegate, ...)           │
│   - updateState { copy(...) } 原子更新，消除并发竞态                                            │
│   - 单次即逝事件 (Toast, Snackbar, Navigation) 通过 MutableSharedFlow / Channel 广播            │
│   - 接入 TraceManager 注入全链路 TraceId，监控方法执行耗时                                      │
└───────────────────────────────┬─────────────────────────────────▲───────────────────────────────┘
                                │ 4. 驱动领域计算 / 数据交互      │ 5. 响应式 Flow / Result 数据流
                                ▼                                 │
┌─────────────────────────────────────────────────────────────────┴───────────────────────────────┐
│                                  【领域层 (Domain / Engine Layer)】                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│   TransactionCalculationEngine / AnnualTransactionEngine / DemoDataEngine                       │
│   - 纯 Kotlin 实现，完全脱离 Android Framework 与 Context 依赖，100% 单元测试覆盖率             │
│   - 负责月度/年度收支聚合、预算警戒线与超支阈值判定、复合过滤排序算法                          │
└───────────────────────────────┬─────────────────────────────────▲───────────────────────────────┘
                                │ 6. 持久化与网络请求             │ 7. 返回 Entity / REST 数据
                                ▼                                 │
┌─────────────────────────────────────────────────────────────────┴───────────────────────────────┐
│                                   【数据层 (Data & Storage Layer)】                              │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│   Room SQLite Database (TransactionDao, RecurringRuleDao)  +  DataStore (ExpensePreferences)    │
│   GoogleDriveService (REST API v3)  +  GoogleDriveAutoBackupManager (5s 防抖 + SHA-256 脏检查)  │
│   - 底层强捕获安全调用 (safeCall { ... } / Flow.asResult())，统一收敛为 Result<T>               │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.1 单向数据流 (UDF) 运作时序表

| 阶段 | 参与组件 | 行为规范与技术要点 |
| :--- | :--- | :--- |
| **1. 意图派发**<br>`(Intent Dispatch)` | `UI` $ightarrow$ `ViewModel` | 用户点击按钮或输入内容时，调用 `viewModel.handleIntent(intent)`。严禁在 UI 层直接调用 Repository 或处理复杂业务逻辑。 |
| **2. 状态原子更新**<br>`(Atomic Reducer)` | `ViewModel` 内部 | 通过 `updateState { copy(...) }` 修改不可变数据类，`_viewState` 自动发出最新状态快照，驱动 Compose 界面重绘。 |
| **3. 副作用单次发射**<br>`(One-off Effect)` | `ViewModel` $ightarrow$ `UI` | 导航跳转、Toast、系统弹窗等单次动作，调用 `emitEffect(CommonUiEffect)` 发射，由 UI 层的 `LaunchedEffect` 配合 `collectLatest` 消费，防止屏幕旋转重放。 |
| **4. 领域与数据交互**<br>`(Domain & Data)` | `ViewModel` $ightarrow$ `Data Layer` | 在 `viewModelScope` 协程中调用纯函数引擎或 DAO，数据源操作使用 `safeCall` 拦截异常并返回 `Result<T>`，杜绝 UI 崩溃。 |

---

## 3. 架构底层核心库与技术难点深度剖析 (Implementation Walkthrough)

### 3.1 难点一：泛型收敛的 MVI 基类 `BaseViewModel<State, Intent>` (`ListenArch`)

源码位于 [`ListenArch/BaseViewModel.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/mvi/BaseViewModel.kt)。

#### 🔑 设计思路与架构考量
1. **双泛型极简设计**：传统 MVI 架构常声明 `<State, Intent, Effect>` 三个泛型，导致继承时类头冗长繁琐。`ListenArch` 将 Effect 统一固定为泛型无关的通用契约接口 [`CommonUiEffect`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/mvi/CommonUiEffect.kt)，大幅简化代码；
2. **冷热流职责分离**：
   - `viewState` 使用 `MutableStateFlow`：因为状态具有“粘性”，任何新加入或重新附着的 Composable 都能立即获取当前最新的状态快照；
   - `viewEffect` 使用 `MutableSharedFlow`（`extraBufferCapacity = 64` / 默认 0 replay）：单次副作用事件不保留历史回放，避免屏幕旋转或配置变更时重复弹出 Toast 或重复导航；
3. **生命周期自动转译**：通过重写 `toLifecycleIntent(event)`，可将 Activity / Fragment 的生命周期事件直接映射为 ViewModel 的业务 Intent。

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
package com.listen.arch.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * MVI 架构的基础 ViewModel 基类
 *
 * @param State UI 不可变状态数据模型快照
 * @param Intent 用户在界面上触发的操作意图
 * @param initialState 初始状态实例
 */
abstract class BaseViewModel<State, Intent>(initialState: State) : ViewModel() {

    // 1. 状态流：使用 MutableStateFlow 保持最新状态，支持自动回放 (Replay 1)
    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    // 2. 副作用流：使用 MutableSharedFlow 广播单次即逝事件 (Replay 0)，防止配置变更重复触发
    private val _viewEffect = MutableSharedFlow<CommonUiEffect>()
    val viewEffect: SharedFlow<CommonUiEffect> = _viewEffect.asSharedFlow()

    // 快捷获取当前状态快照
    protected val currentState: State
        get() = _viewState.value

    /**
     * 核心抽象方法：接收并处理用户业务意图
     */
    abstract fun handleIntent(intent: Intent)

    /**
     * 原子状态更新 Reducer：基于旧状态产生新状态快照
     */
    protected fun updateState(reducer: State.() -> State) {
        _viewState.value = currentState.reducer()
    }

    /**
     * 异步派发通用单次副作用（如 Toast、弹窗、页面路由跳转）
     */
    protected fun emitEffect(effect: CommonUiEffect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }
    }

    protected fun emitEffect(builder: () -> CommonUiEffect) {
        viewModelScope.launch {
            _viewEffect.emit(builder())
        }
    }

    /**
     * 生命周期事件转译钩子：将系统生命周期事件转换为 ViewModel 内部专属 Intent
     */
    open fun toLifecycleIntent(event: LifecycleEvent): Intent? = null

    fun dispatchLifecycleEvent(event: LifecycleEvent) {
        val intent = toLifecycleIntent(event)
        if (intent != null) {
            handleIntent(intent)
        }
    }
}
```

---

### 3.2 难点二：架构拆分与单文件 $\le 250$ 行治理 (`StateHolder` & `Delegate`)

#### 🔑 为什么必须推行“单文件 $\le 250$ 行”规范？
当单个文件超过 300~500 行时，代码会演变为“神之类”（God Class），带来以下严重问题：
1. **认知负荷激增**：业务逻辑、网络 I/O、UI 绘制与状态管理交织在一起，新接手开发者难以通读；
2. **Git 协同冲突率飙升**：多名开发者同时修改同一个大文件，极易产生大量人工代码冲突；
3. **单元测试难以编写**：外部依赖庞杂，Mock 测试极其繁琐。

#### 💡 落地解耦方案：Delegate 模式与 StateHolder 模式
1. **Delegate 模式（剥离重型逻辑）**：
   - 典型代表：[`SettingsSyncDelegate.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/settings/viewmodel/SettingsSyncDelegate.kt)；
   - 将复杂的 Google Drive REST API 上传/下载、JSON 文件导出导入、CSV/Excel 报表生成、演示数据生成逻辑从 `SettingsViewModel.kt` 中完全剥离，使 `SettingsViewModel.kt` 聚焦于纯粹的状态流转，行数稳定在 246 行；
2. **StateHolder 模式（管理全局跨页状态）**：
   - 典型代表：[`ExpenseAppState.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/core/state/ExpenseAppState.kt)；
   - 统一承载主界面当前导航 Tab、抽屉展开状态、全局 Snackbar 调度与快捷记账弹窗控制；
   - 将底栏 Tab 枚举独立抽出至 [`NavTab.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/core/state/NavTab.kt)，让 `ExpenseAppState.kt` 维持在 214 行。

```kotlin
// 典型解耦范式：SettingsViewModel 委托给 SettingsSyncDelegate 执行具体业务
class SettingsViewModel(
    private val application: Application
) : BaseViewModel<SettingsUiState, SettingsIntent>(SettingsUiState()) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.transactionDao()
    private val recurringDao = db.recurringRuleDao()
    private val prefManager = ExpenseDataStoreManager(application)

    // 引入业务代理类 (Delegate)，彻底化解 God Class
    private val syncDelegate = SettingsSyncDelegate(application, dao, recurringDao, prefManager)

    override fun handleIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.TriggerCloudBackup -> viewModelScope.launch {
                syncDelegate.triggerCloudBackup(
                    email = currentState.googleAccountEmail,
                    lang = currentState.language,
                    traceId = TraceManager.newTraceId(),
                    onOperating = { updateState { copy(isOperating = it) } },
                    onToast = { emitEffect(CommonUiEffect.ShowToast(it)) }
                )
            }
            // ... 其余逻辑同样分流委托
        }
    }
}
```

---

### 3.3 难点三：可观测性 APM 系统与分布式链路追踪 (`TraceManager` + `ApmLogger`)

源码位于 [`ListenArch/TraceManager.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/TraceManager.kt) 与 [`ListenArch/ApmLogger.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/ApmLogger.kt)。

#### 🔑 技术亮点
1. **短 TraceId 算法**：采用 `trace-UUID.take(8)` 生成短识别码，兼具全局随机性与控制台排版美观度；
2. **`inline fun <T> trace(...)` 高阶内联**：消除高频调用下的 Lambda 对象分配开销，零 GC 停顿；
3. **环形内存日志缓冲池 (Circular Ring Buffer)**：使用线程安全的 `CopyOnWriteArrayList`，维护最大 500 条日志上限，超出时自动剔除最旧日志（`removeAt(0)`），杜绝 OOM 内存泄露；
4. **响应式状态流输出**：通过 `logsFlow: StateFlow<List<ApmLogEntry>>`，可直接在开发者浮窗中实时滚动展示日志。

```kotlin
object TraceManager {
    fun newTraceId(): String = "trace-" + java.util.UUID.randomUUID().toString().take(8)

    /**
     * 内联追踪执行块，自动记录操作起点、成功耗时或失败堆栈
     */
    inline fun <T> trace(
        channel: ApmLogChannel = ApmLogChannel.APP,
        tag: String = "Trace",
        operationName: String,
        traceId: String = newTraceId(),
        block: (traceId: String) -> T
    ): T {
        val start = System.currentTimeMillis()
        ApmLogger.i(channel, tag, "[$traceId] Start: $operationName", traceId)
        return try {
            val result = block(traceId)
            val duration = System.currentTimeMillis() - start
            ApmLogger.i(channel, tag, "[$traceId] Success: $operationName (${duration}ms)", traceId)
            result
        } catch (e: Throwable) {
            val duration = System.currentTimeMillis() - start
            ApmLogger.e(channel, tag, "[$traceId] Failed: $operationName (${duration}ms) - ${e.message}", traceId, e)
            throw e
        }
    }
}
```

---

### 3.4 难点四：全局未捕获崩溃双写与平滑保护 (`CrashHandler`)

源码位于 [`ListenArch/CrashHandler.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/CrashHandler.kt)。

#### 🔑 核心实现机制
1. **责任链保留 (Chain of Responsibility)**：保存系统原本的默认处理器 `Thread.getDefaultUncaughtExceptionHandler()`，记录日志后再回传给系统，不破坏 Android 异常分发链；
2. **双写持久化策略 (Dual-Write)**：
   - 第一路写入内存 `ApmLogger.crash()`；
   - 第二路同步以追加模式（`appendText`）写入内部私有沙盒 `crash_logs.txt`；
3. **防二次崩溃静默保护**：`handleCrash` 内部全面使用 `try-catch` 包裹，严禁在崩溃处理器自身抛出任何异常，避免死循环闪退。

```kotlin
object CrashHandler {
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleCrash(thread, throwable, context.filesDir)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    fun handleCrash(thread: Thread, throwable: Throwable, targetDir: File?): String {
        return try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            // 1. 内存写入
            ApmLogger.crash("UncaughtException", "Crash in thread ${thread.name}: ${throwable.message}", throwable)

            // 2. 本地沙盒文件双写追加
            if (targetDir != null) {
                val file = File(targetDir, "crash_logs.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                file.appendText("--- CRASH [${sdf.format(Date())}] Thread: ${thread.name} ---
$stackTrace

")
            }
            stackTrace
        } catch (e: Exception) {
            // 防御二次崩溃：严禁在此阶段抛出任何异常
            ""
        }
    }
}
```

---

### 3.5 难点五：领域计算引擎纯函数式设计 (`TransactionCalculationEngine`)

源码位于 [`TransactionCalculationEngine.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/engine/TransactionCalculationEngine.kt)。

#### 🔑 核心优势
- 完全摆脱任何 Android 运行时依赖与 Context，纯 Kotlin 编写；
- 聚合计算算法（当月总支出、总收入、结余、预算执行比例、分类花销排行）全部为无副作用的**纯函数（Pure Functions）**；
- 单元测试执行速度极快（JVM 本地运行只需数十毫秒），逻辑覆盖率可轻松达到 100%。

---

## 4. 国际化与设计系统规范 (i18n & Design System)

### 4.1 动态双语国际化机制
- 采用集中枚举与字典模式（`AppStrings.kt` + `ExpenseStrings.kt`）；
- 通过扩展函数 `StringResKey.tr(lang: String)` 实现秒级双语切换；
- 严禁在 Composable 内部使用硬编码字符串（如直接写 `"保存账单"`）。

### 4.2 统一设计系统与样式组件 (`ListenUiComponent`)
- **颜色体系**：基于 `MaterialTheme.colorScheme`，支持用户动态定制 AccentColor（珊瑚红、极客绿、经典蓝、琥珀橙等）；
- **组件规范**：
  - 卡片：使用 `SurfaceCard(cornerRadius = AppDimens.CornerCard)`；
  - 按钮：使用 `CommonButton(style = CommonButtonStyle.Filled / Outlined)`；
  - 开关：使用 `CommonSwitchRow(title, subtitle, checked, onCheckedChange)`；
- 严禁在 UI 控件中硬编码颜色值（如 `Color(0xFF123456)`）。

---

## 5. 工程质量红线与 Custom Lint 规则速查表 (Quality Guardrails)

系统推行 16 条开发质量与架构底线红线（由自定义 Lint 引擎实时扫描阻断）：

| 规则编号 | 规则标识 (Issue ID) | 严重级别 | 违规禁止行为 (Bad Practice) | 正确工程做法 (Recommended Practice) |
| :---: | :--- | :---: | :--- | :--- |
| **01** | `ViewModelHoldsContextOrView` | 🔴 Error | ViewModel 内部持有 Activity/View/Context 强引用 | 仅允许持有 `Application`，或通过依赖注入解耦 |
| **02** | `DirectIoOnMainThread` | 🔴 Error | 在主线程或非 IO 协程中执行文件/数据库同步读写 | 必须使用 `withContext(Dispatchers.IO)` 调度 |
| **03** | `UncaughtExceptionInCompose` | 🔴 Error | 在 Composable 绘制过程中抛出未捕获异常 | 必须由 ViewModel 预先校验，UI 仅渲染状态 |
| **04** | `HardcodedColorInComposable` | 🟡 Warning | 在 UI 控件中硬编码 `Color(0xFF...)` | 使用 `MaterialTheme.colorScheme` 或 Design Token |
| **05** | `HardcodedStringInComposable`| 🟡 Warning | 在界面文字中直接硬编码中文/英文/日文 | 使用 `AppStrings.*.tr(lang)` 统一翻译函数 |
| **06** | `MissingTraceIdInCloudIo` | 🔴 Error | 云端备份与 REST API 调用未携带 TraceId | 必须由 `TraceManager.trace` 生成并透传 TraceId |
| **07** | `DirectLogPrintUsage` | 🔴 Error | 使用 `android.util.Log` 或 `println` | 统一调用 `ApmLogger.d / i / w / e / sync / db` |
| **08** | `FileSizeExceeds250Lines` | 🔴 Error | 单个 Kotlin 源码文件超过 250 行 | 采用 `Delegate` 模式或拆分成独立组件文件 |
| **09** | `MissingUnitTestForUtil` | 🟡 Warning | 核心计算引擎与工具函数缺少单元测试用例 | 在 `src/test/java` 中补充 100% 覆盖率单测 |
| **10** | `MermaidInMarkdownDoc` | 🔴 Error | 在工程文档中引入 ````mermaid` 代码块 | 统一采用 Unicode / ASCII 字符框线拓扑图与表格 |
| **11** | `MissingScreenOrientationConfig`| 🔴 Error | Activity 未限制竖屏或未适配横屏翻转 | 在 AndroidManifest 中配置明确的屏幕旋转规则 |
| **12** | `UnsafeBiometricKeyUsage` | 🔴 Error | 生物识别验证直接绕过加密密钥解密 | 必须配合 `CryptoObject` 或硬件 Keystore 校验 |
| **13** | `BlockingGetInFlow` | 🔴 Error | 在协程中滥用 `runBlocking` 读取 Flow | 使用 `Flow.first()` 或 `collectLatest` 异步收集 |
| **14** | `DirtyDataDriveUpload` | 🔴 Error | 数据无变动时强行向 Google Drive 发送上传请求 | 必须通过 SHA-256 脏数据校验比对无变动则跳过 |
| **15** | `MissingNetworkGuard` | 🔴 Error | 在蜂窝移动网络下自动执行耗电耗流的重型备份 | 检查 Wi-Fi 开关配置与 `NetworkCapabilities` |
| **16** | `MutablePublicStateFlow` | 🔴 Error | 向外部暴露公开的 `MutableStateFlow` | 必须暴露不可变的只读 `StateFlow.asStateFlow()` |

---

## 6. 本地开发、测试与持续集成流程 (Development & CI/CD Workflow)

### 6.1 常用 Gradle 工程命令速查

```powershell
# 1. 运行本地全量单元测试 (Unit Tests)
.\gradlew testDebugUnitTest

# 2. 执行自定义 Lint 质量与规范静态扫描
.\gradlew lintDebug

# 3. 本地 Release 打包构建 (生成安装包)
.\gradlew assembleRelease

# 4. 生成 Google Play 上传专用 AAB (Android App Bundle)
.\gradlew bundleRelease

# 5. 清理构建缓存与 KSP 生成代码
.\gradlew clean
```

### 6.2 Git 提交规范 (Conventional Commits)
为保持项目变更历史的清晰与自动化发布能力，提交信息必须严格采用规范前缀：
- `feat:` 新增功能（如添加周期账单、新增本地通知预警中枢）；
- `fix:` 缺陷修复（如修复手势防窥状态残留、修复横竖屏重组崩溃）；
- `refactor:` 代码重构（如提取 NavTab 枚举将文件拆减至 250 行内）；
- `docs:` 文档完善与架构说明升级（如完善开发指南、补充 ADR 决策记录）；
- `test:` 补充或重构单元测试用例；
- `style:` 格式调整（无代码语义变更）。
