# ListenExpenseTracker - 全模块 API 与架构接口全景参考手册
(Comprehensive API & Architecture Interface Specification)

本文档系统性梳理 **ListenExpenseTracker (lExpense)** 及其底层支撑架构组件（**`ListenArch`** 与 **`ListenUiComponent`**）的所有核心公共类、接口、DAO、计算引擎、状态持有者、云端同步服务及桌面小部件 API。

每个模块均包含 **设计哲学 (Design Rationale)**、**架构实现要点 (Implementation Details)**、**完整 API 清单表格 (API Matrix)** 以及 **带逐行中文注释的关键代码解读 (Code Walkthrough)**。

---

## 1. 模块总体架构与通信拓扑 (System Architecture & Module Topology)

整个工程由底层通用架构库、UI 设计系统组件库以及顶层业务应用三部分构成，遵循 **Local-First（本地优先）**、**纯函数无状态计算** 与 **MVI 单向数据流** 原则：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                【应用表现层 (Application UI Layer)】                             │
│                  MainActivity / Navigation / Screens (Transactions, Statistics, Settings)       │
│                  Dialogs / Sheets (TransactionSheet, CategoryBudgetCenterDialog, AccountEdit)   │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ 订阅 StateFlow<State> / 发射 Intent
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                           【状态持有与编排层 (MVI ViewModels & Delegates)】                      │
│                  TransactionsViewModel | StatisticsViewModel | SettingsViewModel                 │
│                  SettingsStateHolder (Compose状态) | SettingsSyncDelegate (耗时同步委托)          │
└───────────────────────┬───────────────────────────────────────────┬─────────────────────────────┘
                        │ 驱动核心领域计算                          │ 调度云端与安全
                        ▼                                           ▼
┌───────────────────────────────────────────────┐ ┌───────────────────────────────────────────────┐
│      【领域核算引擎层 (Domain Engines)】       │ │       【云端同步与安全 (Cloud & Security)】    │
│  + TransactionCalculationEngine (月度收支计算) │ │  + GoogleDriveService (原生 REST API v3)      │
│  + CategoryBudgetEngine (分类预算健康核算)    │ │  + GoogleDriveAutoBackupManager (防抖自愈备份)│
│  + AnnualCalculationEngine (年度多维聚合)     │ │  + GoogleAuthManager (现代 CredentialManager) │
│  + FinancialInsightEngine (智能财务洞察协调)  │ │  + BiometricSecurityManager (硬件生物识别)    │
│  + FinancialInsightDetectors (行为习惯检测器) │ └───────────────────────┬───────────────────────┘
│  + RecurringTransactionEngine (周期履约基线)  │                         │
│  + AmountFormatExt (Rule 21 金额零截断扩展)   │                         │
└───────────────────────┬───────────────────────┘                         │
                        │ 读写本地持久化                                  │ 触发桌面同步
                        ▼                                                 ▼
┌───────────────────────────────────────────────┐ ┌───────────────────────────────────────────────┐
│      【本地持久化层 (Local Persistence)】      │ │       【桌面生态扩展 (Desktop Ecosystem)】    │
│  + Room: AppDatabase (TransactionDao, Rules)  │ │  + ListenExpenseAppWidgetProvider (广播调度)  │
│  + DataStore: ExpenseDataStoreManager (偏好)   │ │  + WidgetLayoutBinder (RemoteViews 动态渲染) │
└───────────────────────┬───────────────────────┘ └───────────────────────────────────────────────┘
                        │ 继承与消费底层 SDK
                        ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 【底层基础设施库 (Underlying SDKs)】                             │
├───────────────────────────────────────────────┬─────────────────────────────────────────────────┤
│  ListenArch (通用架构基础设施)                │  ListenUiComponent (原生 Canvas 图表与设计系统) │
│  - BaseViewModel<State, Intent>               │  - DonutChart (顺时针 650ms 扫开环形图)         │
│  - CommonUiEffect (Toast/Navigation/Snackbar) │  - BarChart (垂直柱状趋势图)                    │
│  - CloudSyncManager (账号隔离快照同步)        │  - LineChart (三次贝塞尔平滑折线走势图)         │
│  - ApmLogger & TraceManager (环形缓冲链路追踪)│  - NumericKeypad (带微震脉冲触觉的算术键盘)     │
│  - ResultExtensions (安全调用与异常收敛)      │  - SegmentedProgressBar / SurfaceCard / Search  │
└───────────────────────────────────────────────┴─────────────────────────────────────────────────┘
```

---

## 2. `ListenArch` 核心接口与模块详解

### 2.1 `BaseViewModel<State, Intent>` (MVI 状态机基类)
* **类路径**：`com.listen.arch.mvi.BaseViewModel`
* **职责**：规范应用单向数据流（Unidirectional Data Flow），统一管理 UI 状态（State）、交互动作（Intent）与一次性副作用（Effect）。

#### 🔑 设计思路
1. **为什么只有 2 个泛型 `<State, Intent>` 而非 `<State, Intent, Effect>`？**
   在标准 MVI 模式中，若引入第三个泛型参数，每个子类 ViewModel 都必须单独声明一个自定义的 sealed class Effect，导致大量样板代码重复。由于移动端单次副作用（如弹 Toast、路由导航、显示 Snackbar、收起软键盘）在所有业务场景中高度趋同，基类将 Effect 统一收敛为标准契约接口 `CommonUiEffect`，子类如有特殊需求可自由实现该接口扩展，极大精简了泛型签名。
2. **为什么 State 用 `StateFlow`，而 Effect 用 `SharedFlow`？**
   - **`StateFlow`（状态保鲜）**：具有粘性（Conflated），总是向新订阅者重播最新状态值。在屏幕旋转或 Activity 重建时，UI 可以即时恢复重组，无需重新触发网络或数据库查询；
   - **`SharedFlow`（单次消费）**：配置 `replay = 0`，不保留历史事件。保证 Toast 弹窗或导航跳转只消费一次，绝不会在屏幕配置发生变化后重复触发。

#### 📋 核心 API 清单

| 方法 / 属性 | 可见性 | 类型 / 签名 | 说明 |
| :--- | :--- | :--- | :--- |
| `viewState` | `public` | `StateFlow<State>` | 只读 UI 状态流，供 Compose 页面使用 `collectAsState()` 监听并触发重组 |
| `viewEffect` | `public` | `SharedFlow<CommonUiEffect>` | 只读单次事件流，供 UI 层的 `LaunchedEffect` 监听执行副作用 |
| `currentState` | `protected` | `State` | 当前状态的同步快照值，在内部逻辑中避免并发读脏数据 |
| `handleIntent` | `public abstract` | `fun handleIntent(intent: Intent)` | MVI 唯一交互入口，所有用户点击与系统事件均转为 Intent 传入 |
| `updateState` | `protected` | `fun updateState(reducer: State.() -> State)` | 状态更新唯一受控出口，通过扩展函数 Lambda 实现状态原子变更 |
| `emitEffect` | `protected` | `fun emitEffect(effect: CommonUiEffect)` | 在 `viewModelScope` 中异步发射一次性 UI 副作用事件 |
| `dispatchLifecycleEvent` | `public` | `fun dispatchLifecycleEvent(event: LifecycleEvent)` | 响应宿主生命周期（如 ON_APPEAR），自动派发对应的业务 Intent |
| `toLifecycleIntent` | `open` | `fun toLifecycleIntent(event: LifecycleEvent): Intent?` | 生命周期到业务 Intent 的映射挂钩，子类按需覆盖（默认返回 null） |

#### 💡 关键代码解读与逐行注释
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

abstract class BaseViewModel<State, Intent>(initialState: State) : ViewModel() {

    // 内部可变状态流，仅限 ViewModel 内部修改，禁止外部直接变更
    private val _viewState: MutableStateFlow<State> = MutableStateFlow(initialState)
    // 对外公开的不可变只读 StateFlow，确保单向数据流不可逆
    val viewState: StateFlow<State> = _viewState.asStateFlow()

    // 内部可变事件流：replay = 0 保证单次事件不重放，避免重组时重复弹出 Toast
    private val _viewEffect: MutableSharedFlow<CommonUiEffect> = MutableSharedFlow()
    val viewEffect: SharedFlow<CommonUiEffect> = _viewEffect.asSharedFlow()

    // 当前状态同步快照，便于在处理 Intent 时即时读取，免去 Flow 收集开销
    val currentState: State
        get() = _viewState.value

    // 核心契约：所有用户交互事件必须通过此抽象方法统一接入
    abstract fun handleIntent(intent: Intent)

    /**
     * 原子状态更新函数 (Reducer)
     * 使用高阶扩展函数 State.() -> State，使 Lambda 内部 this 直接指向当前状态
     * 示例：updateState { copy(isLoading = true, budget = 5000.0) }
     */
    protected fun updateState(reducer: State.() -> State) {
        _viewState.value = currentState.reducer()
    }

    /**
     * 发射单次副作用事件
     * 将挂起函数 emit 包装进 viewModelScope，避免外部必须挂起调用，保证调用方的轻量性
     */
    protected fun emitEffect(effect: CommonUiEffect) {
        viewModelScope.launch {
            _viewEffect.emit(effect)
        }
    }

    /**
     * 页面生命周期分发入口 (由 CommonRoute 或页面根组件在 DisposableEffect 中触发)
     */
    fun dispatchLifecycleEvent(event: LifecycleEvent) {
        toLifecycleIntent(event)?.let { intent ->
            handleIntent(intent)
        }
    }

    /**
     * 生命周期事件映射挂钩：子类若需在页面可见时自动刷新，只需重写此方法
     */
    open fun toLifecycleIntent(event: LifecycleEvent): Intent? = null
}
```

---

### 2.2 `CommonUiEffect` (跨模块通用副作用契约)
* **类路径**：`com.listen.arch.mvi.CommonUiEffect`
* **设计思路**：采用 `interface` 而非 `sealed interface`，既在架构层固化了常用的基础系统交互类型，又允许不同的宿主业务模块自由追加自定义 Effect，实现面向对象的设计开闭原则（OCP）。

```kotlin
interface CommonUiEffect {
    /** 弹出短暂轻量提示消息 (Toast) */
    data class ShowToast(val message: String) : CommonUiEffect

    /** 弹出带有操作按钮与撤销动作的悬浮条 (Snackbar) */
    data class ShowSnackbar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    ) : CommonUiEffect

    /** 调用系统原生分享面板 */
    data class ShareText(val title: String, val text: String) : CommonUiEffect

    /** 声明式路由导航到指定目标页面 */
    data class NavigateTo(val route: String) : CommonUiEffect

    /** 返回上一级页面 */
    data object NavigateBack : CommonUiEffect

    /** 打开外部默认浏览器 */
    data class OpenUrl(val url: String) : CommonUiEffect

    /** 收起系统软键盘 */
    data object HideKeyboard : CommonUiEffect
}
```

---

### 2.3 `ResultExtensions.kt` (全局异常安全收敛)
* **类路径**：`com.listen.arch.result.ResultExtensions`
* **设计思路**：Kotlin 官方原生推荐的 `Result<T>` 异常收敛模式。避免在业务层四处抛出未捕获异常导致应用崩溃（Crash），将所有可能产生 I/O 或解析错误的代码块收敛为 `Result.success` 或 `Result.failure`。

```kotlin
/**
 * 安全执行任意代码块，将抛出的 Throwable 收敛为 Result.failure，正常执行收敛为 Result.success
 */
inline fun <T> safeCall(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Throwable) {
        Result.failure(e)
    }
}

/**
 * 将 Flow<T> 转换为安全的数据流 Flow<Result<T>>，在流上游发生异常时下游安全捕获
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this.map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
}
```

---

### 2.4 `TransactionDao` (Room 响应式数据持久化访问层)
* **接口路径**：`com.listen.expensetracker.data.db.TransactionDao`
* **职责**：作为本地数据库的核心持久化门户，深度利用 Room 框架内置的 `InvalidationTracker` 机制，提供全自动响应式查询与批量操作。

#### 🔑 设计思路
- **响应式响应与本地优先 (Local-First Reactive)**：`getAllTransactionsFlow()` 返回只读的 `Flow<List<TransactionEntity>>`。当任意后台线程、WorkManager 或前台页面插入、修改或删除记录时，Room 会感知到数据表变动，自动重新拉取数据并通过 Flow 推动 UI 刷新，实现**零轮询、零手动通知**的自愈机制。
- **一次性挂起与导出支持**：为备份、云同步及单测场景提供挂起函数 `getAllTransactions()`，无需建立持续监听通道，节省系统开销。

#### 📋 核心 API 清单

| 方法签名 | 返回值 | 说明 |
| :--- | :--- | :--- |
| `fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>` | `Flow<List<TransactionEntity>>` | 响应式监听全量账单，任何 CRUD 操作均触发流重发 |
| `suspend fun getAllTransactions(): List<TransactionEntity>` | `List<TransactionEntity>` | 一次性非响应式查询全量流水（适用于云备份与导出） |
| `suspend fun getTransactionById(id: String): TransactionEntity?` | `TransactionEntity?` | 根据 UUID 主键精确查找单条账单 |
| `suspend fun insertTransaction(transaction: TransactionEntity)` | `Unit` | 插入单条账单流水，冲突策略为 `REPLACE` |
| `suspend fun insertTransactions(transactions: List<TransactionEntity>)` | `Unit` | 批量事务插入账单（用于云端恢复与 Demo 数据一键生成） |
| `suspend fun updateTransaction(transaction: TransactionEntity)` | `Unit` | 更新已存在的账单记录 |
| `suspend fun deleteTransaction(transaction: TransactionEntity)` | `Unit` | 按实体对象删除记录 |
| `suspend fun deleteTransactionById(id: String)` | `Unit` | 按主键 ID 删除单条记录 |
| `suspend fun deleteAll()` | `Unit` | 清空全表账单（危险操作，用于数据彻底重置） |

---

### 2.5 `BaseDataStoreManager` 与 `ExpenseDataStoreManager` (配置持久化模板设计)
* **基类路径**：`com.listen.arch.data.pref.BaseDataStoreManager`
* **子类路径**：`com.listen.expensetracker.data.pref.ExpenseDataStoreManager`

#### 🔑 设计思路
1. **模板方法设计模式 (Template Method Pattern)**：
   `BaseDataStoreManager` 封装所有 Listen 系列 App 共享的基础偏好配置（多语言、明暗主题、强调色 AccentColor、登录态及云同步时间戳）；`ExpenseDataStoreManager` 继承基类并在此基础上拓展记账业务的专属配置项（货币符号、月度预算限额、自定义资产账户 JSON、Google Drive 自动备份策略开关）。
2. **冷流 (Cold Flow) 转热流 (Hot Flow) 的性能优化**：
   DataStore 默认暴露的 `data.map { ... }` 属于冷流。若多个 ViewModel 或 UI 组件直接订阅同一个 DataStore 属性，每次订阅都会重新触发磁盘 I/O。在实际工程中，利用 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), default)` 将其转换为共享热流，并设定 5 秒无订阅者超时策略，防止内存泄漏的同时消除冗余 I/O。

#### 📋 核心配置项矩阵

| 配置属性 Flow | 类型 | 归属 | 默认值 / 允许范围 |
| :--- | :--- | :--- | :--- |
| `languageFlow` | `Flow<String>` | Base | `"zh"` (支持 `"zh"`, `"en"`, `"ja"`) |
| `themeModeFlow` | `Flow<String>` | Base | `"SYSTEM"` (支持 `"LIGHT"`, `"DARK"`, `"SYSTEM"`) |
| `accentColorFlow` | `Flow<String>` | Base | `"EMERALD"` (支持翡翠绿/宝石蓝/琥珀黄/玫瑰红等 6 色) |
| `isLoggedInFlow` | `Flow<Boolean>` | Base | `false` (Google 登录状态) |
| `userEmailFlow` | `Flow<String>` | Base | `""` (当前登录的 Google 邮箱地址) |
| `currencySymbolFlow` | `Flow<String>` | Expense | `"￥"` (支持 `"￥"`, `"$"`, `"€"`, `"£"`, `"円"`) |
| `monthlyBudgetFlow` | `Flow<Double>` | Expense | `5000.0` (用户月度预算上限) |
| `customAccountsFlow` | `Flow<String>` | Expense | `""` (自定义资产账户序列化 JSON) |
| `autoBackupDriveFlow`| `Flow<Boolean>` | Expense | `true` (Google Drive 自动备份总开关) |
| `autoBackupWifiOnlyFlow`| `Flow<Boolean>`| Expense | `true` (是否限制仅在 Wi-Fi 下自动备份) |

---

### 2.6 `CloudSyncManager` (账号隔离快照同步引擎)
* **类路径**：`com.listen.arch.sync.CloudSyncManager`
* **职责**：作为多账户安全的云端同步调度中枢，维护云端快照数据流，保证多账号间的数据物理隔离与校验和完整性。

#### 🔑 设计思路
1. **响应式状态机驱动**：通过 `syncStateFlow: StateFlow<SyncState>` 实时呈现同步全流程状态：
   `IDLE` (空闲) $\rightarrow$ `SYNCING` (同步中) $\rightarrow$ `SUCCESS` (成功) / `ERROR` (异常)，UI 仅需监听即可完成进度条与状态展示。
2. **多账号物理隔离**：基于 `accountCloudSnapshots: ConcurrentHashMap<String, String>`，以用户 Google 邮箱为隔离键独立存储云端快照，防止多账号切换时数据串号覆盖。
3. **MD5 / SHA 校验和比对**：上传前实时计算快照哈希，下载时完成哈希校验，防止数据传输损坏。

---

### 2.7 `ApmLogger` & `TraceManager` (高性能应用监控与链路追踪)
* **类路径**：`com.listen.arch.apm.ApmLogger` / `TraceManager`

#### 🔑 设计思路
1. **线程安全有界环形缓冲区 (Bounded Ring Buffer)**：
   应用运行期间可能在多条子协程与主线程中高频写入日志。`ApmLogger` 内部采用 `CopyOnWriteArrayList<LogEntry>` 维护最大 500 条的有界缓冲区，超出上限时自动淘汰最旧条目（FIFO）。`CopyOnWriteArrayList` 在写入时保证写安全，读取时生成无需加锁的只读快照，彻底消除了并发并发修改异常（`ConcurrentModificationException`）。
2. **四大频道分流**：划分 `APP`（UI 逻辑）、`DB`（数据库事务）、`SYNC`（云端交互）与 `CRASH`（严重异常）频道，支持在调试浮窗中按 Chip 标签瞬时过滤。
3. **分布式 TraceID 链路追踪**：
   利用 `TraceManager.trace()` 闭包自动为每一次跨越“ViewModel $\rightarrow$ Engine $\rightarrow$ Room $\rightarrow$ Cloud”的复杂流程生成全局唯一的 UUID Trace ID，并在操作结束时自动统计执行耗时，上报性能日志。

```kotlin
// 使用范式：一行代码完成耗时测量与链路跟踪
val fileId = TraceManager.trace(
    channel = ApmLogChannel.SYNC,
    tag = "GoogleDrive",
    operationName = "UploadBackup",
    traceId = traceId
) {
    // 执行耗时 I/O 逻辑...
}
```

---

## 3. `ListenUiComponent` 核心组件与 Canvas 图表详解

本模块为纯 Compose 实现的原生轻量级设计系统，不依赖第三方大型图表库（如 MPAndroidChart），彻底避免原生 View 嵌入 Compose 带来的布局测量颠簸（Layout Jitter）。

### 3.1 `DonutChart` (顺时针扫开 Canvas 环形图)
* **组件路径**：`com.listen.uicomponent.charts.DonutChart`
* **数学与绘制原理**：
  - 基于 Canvas 的 `drawArc()` API 绘制同心圆环；
  - 启动时通过 `Animatable` 驱动 650ms `FastOutSlowInEasing` 缓动，将扫过角度从 $0^\circ$ 顺时针插值至 $360^\circ$；
  - 环形内径掏空，中心自适应呈现总支出金额与“支出总览”多语言标签。

```kotlin
@Composable
fun DonutChart(
    items: List<PieChartItem>,            // 分类切片数据 (label, value, percentage, colorHex)
    totalValue: Double,                   // 聚合总金额
    modifier: Modifier = Modifier,
    currencySymbol: String = "￥",        // 货币符号
    donutWidth: Dp = 22.dp                // 圆环厚度
)
```

---

### 3.2 `LineChart` (三次贝塞尔曲线平滑走势图)
* **组件路径**：`com.listen.uicomponent.charts.LineChart`
* **数学与绘制原理**：
  - **三次贝塞尔平滑插值 (Cubic Bézier Interpolation)**：相邻两点 $(P_0, P_1)$ 之间不使用生硬的折线直连，而是计算控制点 $(C_1, C_2)$ 调用 `path.cubicTo()` 生成丝滑曲线：
    $$C_1 = \left( x_0 + \frac{x_1 - x_0}{2}, y_0 \right), \quad C_2 = \left( x_0 + \frac{x_1 - x_0}{2}, y_1 \right)$$
  - **闭合渐变区域填充**：曲线绘制完成后，Path 自动从最后一个点向下连线到底部轴线，并闭合回原点，使用 `Brush.verticalGradient` 绘制轻透明的主题色渐变阴影；
  - **600ms 动态拔起动效**：通过 `Animatable(0f)` 驱动点位纵坐标从轴底向目标高度动态拔起。

---

### 3.3 `NumericKeypad` (带微震脉冲触觉反馈的记账算术键盘)
* **组件路径**：`com.listen.uicomponent.keypad.NumericKeypad`
* **设计亮点**：
  - **触觉微震体验**：普通数字键在按下时触发系统轻微震动反馈（`HapticFeedbackType.TextHandleMove`），给用户即时的实体物理按键质感；
  - **完成记账脉冲**：右下角“完成”大按钮在长按或点击成功时触发高优先级脉冲震动（`HapticFeedbackType.LongPress`），明确告知用户账单已入库；
  - **内置实时算术求值**：支持直接输入 `+` 和 `-` 符号，完成输入时自动计算出最终金额，避免在记账前必须切换到计算器应用。

---

## 4. `ListenExpenseTracker` 核心计算引擎 (`data.engine`)

### 4.1 `TransactionCalculationEngine` (月度收支与流水过滤管道)
* **类路径**：`com.listen.expensetracker.data.engine.TransactionCalculationEngine`
* **职责**：负责账单流水的全量查询、5 层条件组合过滤、多语言多格式模糊日期匹配及 17 项统计学指标一次性聚合。

#### 🔑 设计思路
1. **多维过滤管道 (Filter Pipeline Pattern)**：
   将过滤逻辑拆分为 5 个独立的布尔判断：
   - 第 1 层：文本搜索（支持分类名、备注、账户类型、金额浮点数与四类不同日期的多重模糊查询）；
   - 第 2 层：资产账户筛选（`CASH`, `BANK`, `CREDIT` 等）；
   - 第 3 层：收支类型过滤（`EXPENSE`, `INCOME`, `ALL`）；
   - 第 4 层：分类层级匹配（支持 ID、多语言 NameKey 及用户自定义分类名）；
   - 第 5 层：金额预设区间（`<50`, `50~500`, `>500` 及自定义区间）。
2. **月度时间戳毫秒边界精算法**：
   使用 `Calendar` 进行月份边缘计算时，先将目标定位到当月 1 号 00:00:00.000，再向后平移 1 个月并回退 1 毫秒（`cal.add(Calendar.MILLISECOND, -1)`），精准获取当月最后一刻 23:59:59.999，自动抵御平年、闰年与大小月份的天数差异。

#### 💡 关键代码解读与逐行注释
```kotlin
object TransactionCalculationEngine {

    /**
     * 月度流水过滤与统计综合计算 (一次遍历，全量产出)
     */
    fun filterAndCalculate(
        allList: List<TransactionEntity>,      // 原始全量账单
        currentOffset: Int,                     // 目标月份偏移量 (0 为本月，-1 为上月)
        query: String,                          // 用户搜索关键字
        accountFilter: String,                  // 账户过滤
        budget: Double,                         // 用户设置的月度预算
        sortOrder: TransactionSortOrder,        // 排序规则 (日期/金额)
        currencySymbol: String = "￥",          // 货币符号
        lang: String = "zh",                   // 当前语言
        typeFilter: String = "ALL",             // 类型过滤 (EXPENSE/INCOME)
        selectedCategories: Set<String> = emptySet(), // 选中的分类集合
        amountPreset: AmountFilterPreset = AmountFilterPreset.ALL, // 金额区间预设
        customMinAmount: Double? = null,        // 自定义最小金额
        customMaxAmount: Double? = null         // 自定义最大金额
    ): CalculationResult {
        // 1. 获取目标月份的精准起止毫秒时间戳与本地化标题
        val (startTs, endTs, title) = getMonthRangeAndTitle(currentOffset, lang)
        // 2. 先通过时间区间过滤出目标月的全量子集
        val monthFilteredList = allList.filter { it.timestamp in startTs..endTs }

        // 3. 执行 5 层过滤管道
        val matchedFiltered = monthFilteredList.filter { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            val itemMonth = itemCal.get(Calendar.MONTH) + 1
            val itemDay = itemCal.get(Calendar.DAY_OF_MONTH)
            val dateLabelZh = "${itemMonth}月${itemDay}日"

            // 第 1 层：搜索匹配 (同时覆盖分类名、备注、金额、中文日期、ISO 日期与短横线日期)
            val cleanQuery = query.trim().lowercase()
            val matchesQuery = cleanQuery.isEmpty() ||
                item.categoryName.lowercase().contains(cleanQuery) ||
                item.note.lowercase().contains(cleanQuery) ||
                item.accountType.lowercase().contains(cleanQuery) ||
                "%.2f".format(item.amount).contains(cleanQuery) ||
                item.amount.toLong().toString() == cleanQuery ||
                dateLabelZh.contains(cleanQuery) ||
                "%02d-%02d".format(itemMonth, itemDay).contains(cleanQuery) ||
                "$itemMonth-$itemDay".contains(cleanQuery)

            // 第 2~5 层：账户、类型、分类与金额范围比对
            val matchesAccount = accountFilter == "ALL" || item.accountType == accountFilter
            val matchesType = typeFilter == "ALL" || item.type.equals(typeFilter, ignoreCase = true)
            val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains("ALL") ||
                selectedCategories.any { cat -> item.categoryId.equals(cat, true) || item.categoryName.equals(cat, true) }
            val matchesAmount = when (amountPreset) {
                AmountFilterPreset.ALL -> true
                AmountFilterPreset.SMALL_LT_50 -> item.amount < 50.0
                AmountFilterPreset.MEDIUM_50_500 -> item.amount in 50.0..500.0
                AmountFilterPreset.LARGE_GT_500 -> item.amount > 500.0
                AmountFilterPreset.CUSTOM -> (customMinAmount == null || item.amount >= customMinAmount) &&
                                             (customMaxAmount == null || item.amount <= customMaxAmount)
            }
            matchesQuery && matchesAccount && matchesType && matchesCategory && matchesAmount
        }

        // 4. 排序与多维指标计算
        val sorted = when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> matchedFiltered.sortedByDescending { it.timestamp }
            TransactionSortOrder.DATE_ASC -> matchedFiltered.sortedBy { it.timestamp }
            TransactionSortOrder.AMOUNT_DESC -> matchedFiltered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> matchedFiltered.sortedBy { it.amount }
        }

        val totalExp = sorted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalInc = sorted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        return CalculationResult(
            filteredTransactions = sorted,
            totalExpense = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
            monthlyBudget = budget,
            remainingBudget = (budget - totalExp).coerceAtLeast(0.0),
            budgetUsageRatio = if (budget > 0) (totalExp / budget).toFloat() else 0f,
            isOverBudget = totalExp > budget,
            categoryShares = calculateCategoryShares(sorted.filter { it.type == TransactionType.EXPENSE }, totalExp),
            // ... 其余图表走势与极值字段全量输出
            monthTitle = title
        )
    }
}
```

---

### 4.2 `CategoryBudgetEngine` (分类预算健康核算引擎)
* **类路径**：`com.listen.expensetracker.data.engine.CategoryBudgetEngine`
* **职责**：依据用户为各细分品类设置的预算配比（`categoryRatios: Map<String, Float>`），对当月各分类支出进行健康度核算。

#### 🔑 设计思路
- **3 态健康度诊断模型 (`BudgetHealthStatus`)**：
  1. `OVERBUDGET` (超支)：实际开销突破了该类别的额度，或该类别未分配预算（额度为0）却产生了支出；
  2. `WARNING` (预警)：实际开销已消耗达到或超过该类别预算的 $80\%$（`usageRatio >= 0.8f`）；
  3. `NORMAL` (正常)：在预算安全线内理性消费。
- **复合优先级降序排列**：返回列表优先将超支类别置顶，其次排布预警类别，最后按消耗金额降序，引导用户优先关注财务风险点。

---

### 4.3 `AnnualCalculationEngine` & `AnnualTransactionEngine` (年度维度数据聚合)
* **类路径**：`com.listen.expensetracker.data.engine.AnnualCalculationEngine` / `AnnualTransactionEngine`
* **职责**：
  - `AnnualCalculationEngine`：负责计算目标年份的全年毫秒起止区间（1月1日 00:00:00.000 至 12月31日 23:59:59.999），并将全年拆分为 12 个自然月，生成各月收支、净结余、年度月均开销及 12 个月的月度趋势折线图点位。
  - `AnnualTransactionEngine`：专门处理统计页年维度下的账单列表检索、关键字匹配及年度预算（月预算 $\times 12$）执行比率计算。

---

### 4.4 `FinancialInsightEngine` & `FinancialInsightDetectors` (智能财务洞察与生活方式诊断)
* **类路径**：`com.listen.expensetracker.data.engine.FinancialInsightEngine` / `FinancialInsightDetectors`
* **职责**：基于用户真实消费流水，挖掘生活方式与收支异常，生成具备情绪化色彩的轮播诊断卡片。

#### 📋 10 大核心检测规则矩阵

| 规则名称 | 检测函数 | 触发数学阈值条件 | 严重等级 | 交互动作 |
| :--- | :--- | :--- | :---: | :--- |
| **健康储蓄率** | `detectSavingsRate` | 净结余 $\ge 20\% \times$ 总收入 | `POSITIVE` | - |
| **收支赤字警报** | `detectSavingsRate` | 总支出 $>$ 总收入 (结余 $< 0$) | `DANGER` | - |
| **月度支出激增** | `Engine.generateInsights` | 当月支出较上月同期上涨 $> 12\%$ | `WARNING` | - |
| **月度支出节流** | `Engine.generateInsights` | 当月支出较上月同期节省 $> 12\%$ | `POSITIVE` | - |
| **烧钱率耗尽预警** | `Engine.generateInsights` | 日均外推月末将超预算且天数在 `[3..max-2]` | `WARNING` | 唤起预算修改弹窗 |
| **中旬节流勋章** | `Engine.generateInsights` | 已过 $\ge 8$ 天且预估全月开支 $\le 70\%$ 预算 | `POSITIVE` | 查看预算消耗进度 |
| **单分类过度倾斜** | `Engine.generateInsights` | 单分类开支 $\ge 45\% \times$ 当月总支出 | `WARNING` | 穿透筛选该分类流水 |
| **突发分类异动** | `Engine.generateInsights` | 单分类较上月环比增长 $> 1.8\times$ 且基数 $> 50$ 元 | `INFO` | 穿透筛选该分类流水 |
| **周末消费倾斜** | `detectWeekendSpendingShift` | 周末日均 $\ge 1.6\times$ 工作日日均且周末总额 $> 100$ 元 | `INFO` | - |
| **拿铁因子累积** | `detectLatteFactor` | 单笔 $\le 35$ 元的小额微支出笔数 $\ge 6$ 笔 | `INFO` | - |
| **单日最大峰值** | `Engine.generateInsights` | 单日消费 $\ge 35\% \times$ 当月总支出且总笔数 $> 2$ | `INFO` | 一键定位该日期流水 |
| **零支出自律天数** | `detectNoSpendDays` | 当月未花钱的自律天数 $\ge 3$ 天（截止今日） | `POSITIVE` | - |
| **平稳运行兜底** | `Engine.generateInsights` | 全月数据平稳、未触发任何异常时的兜底卡片 | `POSITIVE` | - |

---

### 4.5 `RecurringTransactionEngine` (周期履约与固定成本精算)
* **类路径**：`com.listen.expensetracker.data.engine.RecurringTransactionEngine`
* **职责**：负责周期性规则（日/周/月/年）的下次履约时间递推、月度刚性生活成本（Baseline）归一化折算以及到期账单自动化履约入库。

#### 🔑 设计思路
1. **月末安全递推算法**：
   若用户设置每月 31 号扣款，当遇到 2 月（平年 28 天/闰年 29 天）或 4/6/9/11 月（30 天）时，算法调用 `Calendar.getActualMaximum(Calendar.DAY_OF_MONTH)` 自动平滑对齐到该月最后一天，**绝不会发生日期跳月溢出**；
2. **多频次归一化月度成本基线**：
   - 每日规则（DAILY）：$\text{月成本} = \text{金额} \times 30$
   - 每周规则（WEEKLY）：$\text{月成本} = \text{金额} \times \frac{52}{12}$
   - 每月规则（MONTHLY）：$\text{月成本} = \text{金额} \times 1$
   - 每年规则（YEARLY）：$\text{月成本} = \text{金额} \div 12$
   由此折算出用户每月刚性固定支出与收入底线。

---

### 4.6 `AmountFormatExt.kt` (全局金额与百分比格式化规范扩展)
* **类路径**：`com.listen.expensetracker.data.engine.AmountFormatExt`
* **核心规范 (Rule 21)**：
  对所有面向用户的金额展示，若格式化后末尾为 `.00` 或 `.0`，自动剔除小数点和无效零转换为整数形式；若包含有效小数，去除多余后缀零并最多保留两位小数：
  - `100.00` $\rightarrow$ `"100"`
  - `50.0` $\rightarrow$ `"50"`
  - `12.50` $\rightarrow$ `"12.5"`
  - `12.34` $\rightarrow$ `"12.34"`

```kotlin
fun Double.formatAmount(): String {
    if (this == 0.0 || this == -0.0) return "0"
    val str = String.format(Locale.US, "%.2f", this)
    return when {
        str.endsWith(".00") -> str.removeSuffix(".00")
        str.endsWith(".0") -> str.removeSuffix(".0")
        str.contains(".") -> str.trimEnd('0').trimEnd('.')
        else -> str
    }
}

fun Double.formatPercentage(): String {
    if (this == 0.0 || this == -0.0) return "0"
    val str = String.format(Locale.US, "%.1f", this)
    return when {
        str.endsWith(".00") -> str.removeSuffix(".00")
        str.endsWith(".0") -> str.removeSuffix(".0")
        str.contains(".") -> str.trimEnd('0').trimEnd('.')
        else -> str
    }
}
```

---

## 5. 云端同步与安全架构模块 (`data.cloud` & `core.security`)

### 5.1 `GoogleDriveService` (轻量化原生 REST API v3 同步服务)
* **类路径**：`com.listen.expensetracker.data.cloud.GoogleDriveService`
* **职责**：基于原生 `HttpURLConnection` 实现与 Google Drive REST API v3 的直接通信。

#### 🔑 设计思路
1. **零重量级 SDK 依赖**：
   彻底放弃引入体积数兆的官方 Google Drive Client 依赖库，直接通过标准 HTTPS 请求与 `https://www.googleapis.com/drive/v3/files` 交互，将 APK 安装包体积精简数兆；
2. **最小权限原则 (OAuth Scope)**：
   仅申请 `https://www.googleapis.com/auth/drive.file` 权限。应用**只能访问由其自身创建的文件**，对用户网盘内的其他私人文件无任何读取与修改权限，最大限度保护用户数据隐私并顺利通过 Google 安全合规审计；
3. **自愈提权机制 (`UserRecoverableAuthException`)**：
   在获取访问令牌时，若拦截到用户尚未向应用授权该权限域的异常，自动提取异常携带的系统 PendingIntent 调起系统授权弹窗，用户点击同意后即可无感重试。

---

### 5.2 `GoogleDriveAutoBackupManager` (智能自动备份编排调度器)
* **类路径**：`com.listen.expensetracker.data.cloud.GoogleDriveAutoBackupManager`
* **四大前置守卫 (Four Guards)**：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   【自动备份四大前置守卫校验】                                    │
├───────────────────────────────┬───────────────────────────────┬─────────────────────────────────┤
│ 守卫 1: 登录态守卫             │ 守卫 2: 全局开关守卫          │ 守卫 3: 网络环境守卫            │
│ isLoggedIn && email.isNotBlank│ autoBackupEnabled == true     │ wifiOnly ? isWifiConnected : true│
├───────────────────────────────┴───────────────────────────────┴─────────────────────────────────┤
│ 守卫 4: 脏数据 SHA-256 哈希校验 (Dirty Checking)                                                 │
│ hash(currentJson) != lastSuccessHash (若账单内容未发生任何变更，直接放弃上传，节省电量与流量)   │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

- **5 秒防抖机制**：当用户高频连续记账或批量导入时，每次变动均取消前序挂起的协程任务（`pendingDebounceJob?.cancel()`），并在最后一次记账 5 秒后静默向云端执行单次备份。

---

### 5.3 `GoogleAuthManager` (现代化 AndroidX Credential Manager)
* **类路径**：`com.listen.expensetracker.auth.GoogleAuthManager`
* **技术演进**：彻底摒弃已遭 Google 废弃的旧版 `GoogleSignInClient`，全量拥抱 AndroidX `CredentialManager` 与 Google Identity `GetGoogleIdOption`，实现无密码 Passkey 与 Google 账号一键无感登录。

---

### 5.4 `BiometricSecurityManager` & `BiometricLockOverlay` (生物识别安全架构)
* **类路径**：`com.listen.expensetracker.core.security.BiometricSecurityManager`
* **技术栈**：基于 AndroidX `BiometricPrompt` 封装。
* **双模认证支持**：配置 `Authenticators.BIOMETRIC_STRONG or Authenticators.DEVICE_CREDENTIAL`。当设备不支持指纹/面容或录入失败时，自动平滑回退至锁屏 PIN 码或图形密码解锁，绝不闭锁用户。
* **Compose 全局强制隐私安全遮罩**：当用户切至后台或息屏再返回时，UI 层瞬时覆盖一层纯暗色无内容泄露的防护屏障，直到通过生物识别认证才予淡出解除。

---

## 6. 桌面小部件与生态扩展 (`widget`)

### 6.1 `ListenExpenseAppWidgetProvider` (桌面小组件 2.0)
* **类路径**：`com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider`
* **核心架构设计**：
  - 作为系统 `BroadcastReceiver` 运行于独立的进程上下文，无 ViewModel 生命周期；内部启动 `CoroutineScope(Dispatchers.IO)` 异步从 Room 和 DataStore 读取数据并构建 RemoteViews；
  - **组件局部状态机 (Widget-Local State Machine)**：
    支持用户直接在桌面上点击切换上月/下月（`ACTION_PREV_MONTH` / `ACTION_NEXT_MONTH`）以及切换隐藏/显示金额明细（`ACTION_TOGGLE_HIDE_AMOUNT`），并通过 SharedPreferences 存储小部件维度独立的显示状态；
  - **4 大高频场景闪电记账 DeepLink**：
    包含餐饮 (`c_food`)、交通 (`c_transport`)、购物 (`c_shopping`) 与日常 (`c_other_exp`) 四个预设分类按钮，点击通过显式 Intent 拉起应用并自动预填分类打开记账弹窗。

---

### 6.2 `WidgetLayoutBinder` (RemoteViews 动态字号与健康度绑定)
* **类路径**：`com.listen.expensetracker.widget.WidgetLayoutBinder`
* **两项工程难题突破**：
  1. **超大金额字号动态降级**：
     RemoteViews 不支持 Compose 的自适应 Text 测量。本渲染器依据格式化后的金额字符长度动态切换 20sp、16sp 与 14sp 字号，杜绝千万元级大额数字在桌面上发生截断或换行换位；
  2. **3 态互斥进度条渲染**：
     为解决不同 Android 版本 RemoteViews `setProgressTintList` 兼容性偶发失效的系统缺陷，布局预先放置 3 个并列的不同色彩原生 ProgressBar（超支红、预警黄、正常蓝），依据 `BudgetHealthStatus` 动态切换可见性（`VISIBLE` 与 `GONE`），保障各 Android 版本的视觉绝对一致性。

---

## 7. 业务状态持有者与路由架构 (StateHolder & Delegate)

按照 Google 架构推荐规范，将复杂页面的状态管理划分为三个清晰维度：

```text
┌───────────────────────────────┬───────────────────────────────┬─────────────────────────────────┐
│ 角色                          │ 核心职责                      │ 生命周期契约                    │
├───────────────────────────────┼───────────────────────────────┼─────────────────────────────────┤
│ 1. SettingsViewModel          │ 业务数据流转、偏好设置持久化  │ 存活于 ViewModelStoreOwner      │
│ 2. SettingsStateHolder        │ Compose 框架状态 (LazyList,   │ 与 Composable 组合树绑定        │
│                               │ 两阶段 ActivityResultLauncher)│ (rememberSaveable 跨旋转恢复)   │
│ 3. SettingsSyncDelegate       │ 剥离耗时云端同步、数据导入导出│ 通过依赖注入解耦，支持独立测试  │
└───────────────────────────────┴───────────────────────────────┴─────────────────────────────────┘
```

### 7.1 两阶段文件导出模式 (Two-Phase Export Pattern)
由于 Android 规范要求 `rememberLauncherForActivityResult()` 必须在 Composable 可组合函数初始化阶段注册，无法在普通 ViewModel 异步流程中临时创建。`SettingsStateHolder` 采用优雅的两阶段导出模式：
1. **阶段 1 (暂存配置并启动)**：用户点击导出时，先将时间戳区间与过滤条件暂存于 `pendingExportExcelConfig`，随后调用 `launcher.launch(fileName)` 调起系统存储访问框架（SAF）；
2. **阶段 2 (回调提取并派发 Intent)**：系统文件选择器返回所选目标的 `Uri` 后，从暂存配置中取回过滤参数，组装为 `SettingsIntent.ExportExcelToFile` 派发给 ViewModel 启动后台流式写入。
