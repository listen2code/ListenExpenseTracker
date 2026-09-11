# ListenExpenseTracker - 系统架构设计与源码实现深度解析 (System Architecture & Code Walkthrough)

本文档系统性剖析 **ListenExpenseTracker** 的整体软件架构、分层拓扑、核心设计模式、关键技术难点及其源码实现细节。旨在为开发者提供全景式的设计思路阐述与深度教学指引。

---

## 1. 架构总览与分层拓扑 (Architecture Overview & Topology)

项目严格遵循 **Local-First (本地优先)、Privacy-First (隐私优先)、Serverless (无服务器)** 的现代化 Android 架构理念，全面拥抱 **Jetpack Compose + 纯 Kotlin MVI** 响应式数据流。

### 1.1 三层模块依赖拓扑

为彻底根治大型项目中常见的模块间“网状交叉依赖”与“业务逻辑下沉泄露”问题，全工程采用严格的**单向向下依赖**拓扑：

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                    ListenExpenseTracker (业务宿主 App 模块)                   │
│  - 业务领域模型 (TransactionEntity / RecurringRuleEntity / AccountType)       │
│  - 数据访问层 (Room Database / TransactionDao / ExpenseDataStoreManager)     │
│  - 纯计算引擎 (TransactionCalculationEngine / FinancialInsightEngine)        │
│  - 业务页面与状态机 (Transactions / Statistics / Settings Feature 模块)       │
└───────────────────────┬─────────────────────────────┬───────────────────────┘
                        │ 依赖                         │ 依赖
                        ▼                             ▼
┌───────────────────────────────────────┐   ┌─────────────────────────────────┐
│     ListenUiComponent (通用 UI 库)    │   │     ListenArch (架构底座 SDK)    │
│  - 纯视觉通用组件 (DonutChart, Line.. )│──>│  - MVI 状态机底座 (BaseViewModel│
│  - 通用交互组件 (NumericKeypad, Search│依赖│  - APM 监控与日志 (TraceManager) │
│  - 统一设计语言主题 (ListenTheme)      │   │  - 集中副作用 (CommonUiEffect)   │
└───────────────────────────────────────┘   └─────────────────────────────────┘
```

### 1.2 模块职责划分与隔离红线

| 模块名称 | 职责定位 | 包含的核心内容 | 依赖与隔离红线 |
| :--- | :--- | :--- | :--- |
| **`ListenArch`** | 底层架构技术底座 | MVI `BaseViewModel` 状态机、APM 内存日志分析、`TraceManager` 性能链路打点、`CrashHandler` 全局未捕获崩溃拦截、`BaseDataStoreManager`、`CommonUiEffect` 副作用分发、`StringsRes` 调度引擎 | **零业务耦合**。严禁包含任何特定业务实体（如记账、分类、货币等）、数据表或硬编码业务文案。可被任意 Android 应用独立引入。 |
| **`ListenUiComponent`** | 通用视觉与交互 UIKit | `DonutChart` 甜甜圈图表、`BarChart` 柱状图、`LineChart` 平滑折线图、`NumericKeypad` 人体工学数字键盘、`SurfaceCard`、`SearchBarInput`、`SegmentedProgressBar`、`BaseScreenScaffold`、`LogInspectorSheet` | **纯视觉组件库**。严禁包含任何业务领域模型或写死业务逻辑；所有入参均为通用基本类型或纯展示模型，保证 100% 可复用。 |
| **`ListenExpenseTracker`** | 业务宿主 App | `TransactionEntity`、`AppDatabase`、`ExpenseStrings` 多语言字典、流水/统计/设置 Feature 页面、桌面小部件、通知预警中枢、Google 云同步 | 承载记账业务的全部领域模型、业务计算、交互编排与持久化。 |

#### 🔑 架构隔离核心收益
- **独立编译隔离 (Composite Build)**：`ListenArch` 与 `ListenUiComponent` 采用 Gradle Composite Build (`includeBuild`) 引入，既能在单一工程中统一联调，又在物理层面阻止了向业务层的逆向依赖。
- **单元测试加速**：纯计算与架构底座不依赖 Android Framework Context，可在 JVM 上毫秒级运行单元测试。

---

## 2. 核心设计模式与重点源码实现 (Core Patterns & Walkthrough)

### 2.1 纯正 MVI 单向数据流 (Unidirectional Data Flow)

#### 🔑 设计思路
在复杂记账场景中（如筛选、搜索、换月、并发编辑），传统 MVVM 容易导致多处 `LiveData/MutableStateFlow` 状态碎片化，造成 UI 撕裂与竞争冒险。
本项目全量采用 **MVI (Model-View-Intent)** 模式：
- **`UiState`**：单一不可变状态快照（Single Source of Truth），UI 层仅需做无状态渲染；
- **`UiIntent`**：用户交互发出的单一事件意图，唯一合法的状态变更入口；
- **`CommonUiEffect`**：瞬态一次性副作用（Toast、Snackbar、页面跳转），与持久 UI 状态严格分离，防止屏幕旋转重组时重复触发。

#### 💡 重点代码解读：MVI 状态机基类 (`BaseViewModel`)
```kotlin
// ListenArch 中的通用 MVI 状态机基盘
abstract class BaseViewModel<S : Any, I : Any>(initialState: S) : ViewModel() {

    // ---- 1. 核心状态流：StateFlow 驱动不可变快照 ----
    // 使用 MutableStateFlow 保存单一真实源；对外暴露只读 StateFlow，彻底杜绝外部篡改
    private val _viewState = MutableStateFlow(initialState)
    val viewState: StateFlow<S> = _viewState.asStateFlow()

    // ---- 2. 一次性副作用通道：Channel 缓存瞬态事件 ----
    // 采用 Channel.BUFFERED 避免背压阻塞；转换为 Flow 供 UI 单次消费，消费后即消失
    private val _viewEffect = Channel<CommonUiEffect>(Channel.BUFFERED)
    val viewEffect: Flow<CommonUiEffect> = _viewEffect.receiveAsFlow()

    // ---- 3. 用户意图分发入口 (唯一合法变更入口) ----
    abstract fun handleIntent(intent: I)

    // ---- 4. 原子化状态减速器 (Reducer) ----
    // 强制通过原子 copy 产生全新不可变状态对象，触发 Compose 智能重组
    protected fun updateState(reducer: S.() -> S) {
        _viewState.update { current -> current.reducer() }
    }

    // ---- 5. 副作用发射器 ----
    protected fun emitEffect(effect: CommonUiEffect) {
        viewModelScope.launch {
            _viewEffect.send(effect)
        }
    }
}
```

---

### 2.2 泛型生命周期路由适配器 (`CommonRoute`)

#### 🔑 设计思路与技术难点
在 Compose 体系中，页面不仅受到 Android 原生 Activity 生命周期（如前后台切换 `ON_RESUME`/`ON_PAUSE`）的影响，还受到 Compose 自身组合生命周期（Tab 切换导致 Composable 挂载与 `onDispose` 卸载）的影响。
如果每个 Screen 都单独编写 `DisposableEffect` + `LifecycleEventObserver`，不仅会产生数百行样板代码，还极易遗漏注销导致内存泄露。
`CommonRoute` 是工程中**唯一的生命周期与 MVI 胶水适配器**。

#### 💡 重点代码解读：`CommonRoute.kt`
```kotlin
/**
 * 通用泛型 MVI 路由组件 (CommonRoute)
 *
 * @param S UiState 类型约束
 * @param I UiIntent 类型约束
 * @param VM BaseViewModel 具体子类，声明为 reified 泛型
 * @param viewModel 通过 viewModel() 在编译期自动推断解析出具体的 ViewModel 实例
 * @param content 使用 crossinline 修饰，防止 Lambda 内部发生非局部返回 (non-local return)
 */
@Composable
inline fun <S : Any, I : Any, reified VM : BaseViewModel<S, I>> CommonRoute(
    viewModel: VM = viewModel(),
    crossinline content: @Composable (state: S, onIntent: (I) -> Unit) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // ---- 双重生命周期统一派发机制 (Dual-Lifecycle Dispatch) ----
    // 解决痛点：系统退后台与 Tab 切换在底层是两套生命周期事件，这里统一映射为 MVI 生命周期 Intent
    DisposableEffect(lifecycleOwner, viewModel) {
        // 1. 系统级生命周期监听 (前后台切换 / 多任务防窥恢复)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_APPEAR)
                Lifecycle.Event.ON_PAUSE  -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DISAPPEAR)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // 2. Compose 树首次挂载生命周期
        viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_APPEAR)

        // 3. Compose 树离开/切换 Tab 卸载生命周期
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DISAPPEAR)
        }
    }

    // ---- 单向数据流收集与解耦渲染 ----
    val state by viewModel.viewState.collectAsState()
    content(state, viewModel::handleIntent) // 将纯状态与 Intent 方法引用传递给纯无状态 Screen
}
```

---

### 2.3 全类型安全应用编排器 (`ExpenseAppState`)

#### 🔑 设计思路与架构角色
`ExpenseAppState` 是全工程的**“总指挥家 (Conductor)”**。
- **它不是 ViewModel**：ViewModel 关注单一业务边界；而 `ExpenseAppState` 是顶层全局状态编排器，负责协调跨 Feature 的联动。
- **核心职责**：
  1. 集中持有 3 大 Feature ViewModel 的单一实例；
  2. 跨 Tab 时间状态双向同步与**下钻保护机制 (Drill-Down Protection)**；
  3. 双击底部导航置顶复位 (`triggerScrollToTop`)；
  4. 集中管理全局弹窗路由（Widget 快捷记账跳转、APM 调试面板等）。

#### 💡 重点与技术难点攻坚：跨 Tab 时间联动与下钻保护 (`syncTimeState`)
- **业务痛点**：用户在“统计”页面的“年度视图”中，点击了 8 月柱状图下钻到“流水”页面的“8月月视图”。当用户在底部导航栏再次切回“统计”页时，预期是**继续停留在刚才的年度视图**，而不是被流水页强制同步为月视图！
- **解决方案**：引入 `preserveStatisticsYearOnReturn` 状态保护锁与 `lastTimeTab` 追踪器。

```kotlin
class ExpenseAppState(
    val transactionsViewModel: TransactionsViewModel,
    val statisticsViewModel: StatisticsViewModel,
    val settingsViewModel: SettingsViewModel,
    val snackbarHostState: SnackbarHostState
) {
    var currentTab by mutableStateOf(NavTab.TRANSACTIONS)
        private set

    // ---- 核心难点 1：追踪“上一个非 Settings 的时间 Tab” ----
    // 场景：Settings Tab 本身无时间概念。从 Settings 切到 Statistics 时，
    // 同步源必须是更早之前的 Transactions Tab，不能造成状态混乱
    private var lastTimeTab: NavTab = NavTab.TRANSACTIONS

    // ---- 核心难点 2：下钻保护锁 (Drill-Down Protection Lock) ----
    // 当为 true 时，阻止下一次从 Transactions 切回 Statistics 时覆写统计页的年视图
    private var preserveStatisticsYearOnReturn = false

    private fun syncTimeState(fromTab: NavTab, toTab: NavTab) {
        if (fromTab == NavTab.TRANSACTIONS && toTab == NavTab.STATISTICS) {
            val stats = statisticsViewModel.viewState.value
            val tx = transactionsViewModel.viewState.value

            // 拦截器：如果统计页处于年度视图且命中保护锁，果断放行，保持年视图！
            if (preserveStatisticsYearOnReturn &&
                stats.period == StatisticsPeriod.YEAR &&
                tx.period == TransactionPeriod.MONTH
            ) {
                preserveStatisticsYearOnReturn = false // 消费保护锁
                return // 终止同步，保护用户的年视图不被重置！
            }

            // 常规同步：月视图下将流水的月份偏移量严格同步至统计页
            val targetPeriod = if (tx.period == TransactionPeriod.YEAR)
                StatisticsPeriod.YEAR else StatisticsPeriod.MONTH
            if (stats.period != targetPeriod) {
                statisticsViewModel.handleIntent(StatisticsIntent.ChangePeriod(targetPeriod))
            }
            if (targetPeriod == StatisticsPeriod.MONTH && stats.selectedMonthOffset != tx.selectedMonthOffset) {
                statisticsViewModel.handleIntent(StatisticsIntent.SetMonthOffset(tx.selectedMonthOffset))
            }
        }
        // ... (反向同步逻辑同理)
    }

    // ---- 下钻导航 API：精准置位保护锁 ----
    fun navigateToTransactionsMonth(year: Int, month: Int) {
        // 从年视图点击具体月份下钻时，开启保护锁
        preserveStatisticsYearOnReturn = true
        // 计算目标月份相对于当前真实月份的偏移量
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        val targetOffset = (year - currentYear) * 12 + (month - currentMonth)

        transactionsViewModel.handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
        transactionsViewModel.handleIntent(TransactionsIntent.SetMonthOffset(targetOffset))
        lastTimeTab = NavTab.TRANSACTIONS
        currentTab = NavTab.TRANSACTIONS
    }
}
```

#### 💡 重点与技术难点攻坚：双击 Tab 智能归位与防重复重放
```kotlin
// ---- 为什么使用 replay = 0, extraBufferCapacity = 1？ ----
// 1. replay = 0：绝对不能重放历史滚动事件！否则用户切换 Tab 时会莫名其妙触发自动滚动
// 2. extraBufferCapacity = 1：保证 tryEmit() 永远非阻塞执行，即使当前界面正在重组没有活跃 Collector 也不丢弃
private val _scrollToTopEvents = MutableSharedFlow<NavTab>(replay = 0, extraBufferCapacity = 1)
val scrollToTopEvents = _scrollToTopEvents.asSharedFlow()

fun triggerScrollToTop(tab: NavTab) {
    _scrollToTopEvents.tryEmit(tab) // 触发 UI 层的 LazyList 滚动动画
    // 同时向 ViewModel 派发 ScrollToTop Intent，由业务状态机决定将 monthOffset 归零重置为当月
    when (tab) {
        NavTab.TRANSACTIONS -> transactionsViewModel.handleIntent(TransactionsIntent.ScrollToTop)
        NavTab.STATISTICS   -> statisticsViewModel.handleIntent(StatisticsIntent.ScrollToTop)
        NavTab.SETTINGS     -> settingsViewModel.handleIntent(SettingsIntent.ScrollToTop)
    }
}
```

---

### 2.4 两级弹窗与全局浮层宿主体系 (Host Architecture)

#### 🔑 设计思路
在声明式 Compose 中，如果将各个弹窗的 `openAddDialog: Boolean`、`openDeleteDialog: Boolean` 等状态随意散落在各个子组件中，会导致“状态地狱”，甚至产生多个弹窗重叠的严重 Bug。
工程推行**严格的两级宿主架构**：

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                          1. 根安全遮罩 (BiometricLockOverlay)               │
│               最高 Z-Index，生物识别未通过前完全阻断下层所有内容触控            │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ 认证解锁成功
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          2. 全局浮层宿主 (AppOverlayHost)                    │
│             常驻 APM 实时性能监控悬浮窗、内存泄露报警指示器、日志抽屉           │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   3. 页面级密封弹窗宿主 (TransactionsDialogHost 等)          │
│          由 UiState.activeDialog 密封接口驱动，互斥展示，天然杜绝多重弹窗       │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 💡 重点代码解读：声明式互斥弹窗宿主
```kotlin
// 1. 状态定义：使用密封接口 (Sealed Interface) 表达弹窗状态，当前只能存在 1 个或 0 个弹窗
sealed interface TransactionsDialog {
    data class AddTransaction(val initialCategoryId: String?, val initialType: String) : TransactionsDialog
    data class EditTransaction(val transaction: TransactionEntity) : TransactionsDialog
    data object MonthlyBudget : TransactionsDialog
    data object CategoryBudget : TransactionsDialog
    data class DeleteConfirm(val transaction: TransactionEntity) : TransactionsDialog
}

// 2. 宿主实现：统一在 Screen 末尾一行接入，页面代码精简至 150 行以内
@Composable
fun TransactionsDialogHost(
    dialog: TransactionsDialog?,
    onIntent: (TransactionsIntent) -> Unit
) {
    when (dialog) {
        null -> Unit // 无弹窗，零 Compose 节点开销
        is TransactionsDialog.AddTransaction -> {
            TransactionSheet(
                initialCategoryId = dialog.initialCategoryId,
                onDismiss = { onIntent(TransactionsIntent.CloseDialog) },
                onConfirm = { tx -> onIntent(TransactionsIntent.SaveTransaction(tx)) }
            )
        }
        is TransactionsDialog.DeleteConfirm -> {
            CommonConfirmDialog(
                title = "确认删除账单",
                onConfirm = { onIntent(TransactionsIntent.ConfirmDelete(dialog.transaction)) },
                onDismiss = { onIntent(TransactionsIntent.CloseDialog) }
            )
        }
        // ...
    }
}
```

---

### 2.5 全局集中副作用流收集 (`CollectCommonUiEffects`)

#### 🔑 设计思路与时序
传统开发中，每个页面都要维护自己的 `LaunchedEffect` 来弹出 Toast 或 Snackbar，多页面重复注册会导致监听混乱。
`CollectCommonUiEffects` 部署在 `MainActivity` 中，**一行代码收敛全 App 所有 ViewModel 的通用副作用**。

```text
[用户触发操作 (如删除账单)]
           │
           ▼
[ViewModel.handleIntent(Delete)]
           │
           ├─ 1. 更新持久状态 ──> updateState { copy(items = ...) } ──> UI 重绘
           │
           └─ 2. 发射瞬态副作用 ──> emitEffect(ShowSnackbar("已删除", action="撤销"))
                                              │
                                              ▼
                               [MainActivity: CollectCommonUiEffects]
                                              │
                                              ├─ collectLatest { effect }
                                              ▼
                                [snackbarHostState.showSnackbar()]
                                              │
                       ┌──────────────────────┴──────────────────────┐
                       │ 点击“撤销”按钮                               │ 超时未点击
                       ▼                                             ▼
            [effect.onAction?.invoke()]                       [自动淡出关闭]
                       │
                       ▼
         [ViewModel.handleIntent(Restore)]
```

#### 💡 重点代码解读：`CommonUiEffectCollector.kt`
```kotlin
@Composable
fun CollectCommonUiEffects(
    vararg viewModels: BaseViewModel<*, *>, // 接收全量 ViewModel 实例列表
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    viewModels.forEach { vm ->
        // ---- 独立协程生命周期绑定 ----
        // 以 vm 实例作为 key，只有当 ViewModel 发生变化时才重启协程
        LaunchedEffect(vm) {
            // ---- collectLatest 关键优化 ----
            // 如果用户连续快速删除账单，前一个 Snackbar 还没结束就来了新的，
            // collectLatest 会立即取消上一个挂起，直接展示最新的 Snackbar，杜绝消息队列堆积延迟
            vm.viewEffect.collectLatest { effect ->
                when (effect) {
                    is CommonUiEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is CommonUiEffect.ShowSnackbar -> {
                        // 挂起直到用户操作或超时
                        val result = snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = effect.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            effect.onAction?.invoke() // 安全执行撤销回调
                        }
                    }
                    is CommonUiEffect.HideKeyboard -> {
                        keyboardController?.hide()
                    }
                    is CommonUiEffect.ShareText -> {
                        shareSystemText(context, effect.content, effect.title)
                    }
                    else -> Unit // 特殊业务副作用留给各子 Screen 独立消化
                }
            }
        }
    }
}
```

---

## 3. 本地存储与纯计算引擎架构 (Local-First Engine Architecture)

### 3.1 五层纯函数式计算管线 (`TransactionCalculationEngine`)

#### 🔑 设计思路与技术难点
记账应用的核心瓶颈在于：用户在频繁切换月份、切换账户、搜索关键字、切换收支 Tab 时，列表计算若处理不当极易引发主线程卡顿（Jank）。
系统设计了 `TransactionCalculationEngine`：
1. **纯 Kotlin Object**：不依赖 Android 任何上下文，纯 JVM 极速执行；
2. **五层过滤流水线 (5-Layer Pipeline)**：逐层剪枝，最小化中间对象创建；
3. **单次遍历多指标聚合**：在一次线性遍历中同步计算总支出、总收入、结余与各分类占比。

```text
全量本地账单池 (All Transactions: List<TransactionEntity>)
           │
           ▼
 [第 1 层: 时间区间切片 (Date Range)] ─── 过滤指定年/月时间戳范围
           │
           ▼
 [第 2 层: 账户维度筛选 (Account Filter)] ─── 微信/支付宝/银行卡/现金...
           │
           ▼
 [第 3 层: 分类维度筛选 (Category Filter)] ─── 餐饮/交通/娱乐/购物...
           │
           ▼
 [第 4 层: 收支类型分流 (Type Filter)] ─── 支出 (EXPENSE) / 收入 (INCOME)
           │
           ▼
 [第 5 层: 文本关键词匹配 (Keyword Search)] ─── 备注/分类名模糊匹配
           │
           ▼
输出: 过滤后列表 + 现金流统计 + 分类预算健康度模型 (CalculationResult)
```

#### 💡 重点代码解读：带详细注释的计算流水线
```kotlin
object TransactionCalculationEngine {

    /**
     * 高性能计算核心入口：纯函数设计，完全无副作用
     */
    fun calculate(
        allTransactions: List<TransactionEntity>,
        filter: TransactionFilterConfig,
        categoryBudgets: Map<String, Double>
    ): CalculationResult {
        // 1. 链式过滤剪枝
        val filteredList = allTransactions.asSequence()
            // 第 1 层：时间窗口筛选
            .filter { tx -> tx.timestamp in filter.startTimestamp..filter.endTimestamp }
            // 第 2 层：账户筛选（ALL 表示不限制）
            .filter { tx -> filter.accountType == null || tx.accountType == filter.accountType }
            // 第 3 层：分类筛选
            .filter { tx -> filter.categoryId == null || tx.categoryId == filter.categoryId }
            // 第 4 层：收支类型筛选
            .filter { tx -> filter.transactionType == null || tx.type == filter.transactionType }
            // 第 5 层：搜索关键词匹配（备注或分类名匹配）
            .filter { tx ->
                filter.keyword.isNullOrBlank() ||
                tx.note.contains(filter.keyword, ignoreCase = true) ||
                tx.categoryName.contains(filter.keyword, ignoreCase = true)
            }
            .toList()

        // 2. 单次循环汇总统计（避免多次 sumOf 造成多重循环迭代）
        var totalExpense = 0.0
        var totalIncome = 0.0
        val categoryExpenseMap = mutableMapOf<String, Double>()

        for (tx in filteredList) {
            if (tx.type == TransactionType.EXPENSE) {
                totalExpense += tx.amount
                categoryExpenseMap[tx.categoryId] = (categoryExpenseMap[tx.categoryId] ?: 0.0) + tx.amount
            } else {
                totalIncome += tx.amount
            }
        }

        // 3. 计算预算健康度三态状态机
        // NORMAL (<80%) | WARNING (80%~100%) | OVERBUDGET (>=100%)
        val categoryStatuses = categoryBudgets.map { (catId, budget) ->
            val spent = categoryExpenseMap[catId] ?: 0.0
            val ratio = if (budget > 0) spent / budget else 0.0
            val status = when {
                ratio >= 1.0 -> BudgetHealthStatus.OVERBUDGET
                ratio >= 0.8 -> BudgetHealthStatus.WARNING
                else         -> BudgetHealthStatus.NORMAL
            }
            CategoryBudgetStatus(
                category = CategoryRepository.getCategoryById(catId),
                budgetAmount = budget,
                spentAmount = spent,
                ratio = (budget / totalBudget).toFloat(),
                usageRatio = ratio.toFloat(),
                remainingAmount = budget - spent,
                status = status
            )
        }

        return CalculationResult(
            transactions = filteredList,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            netBalance = totalIncome - totalExpense,
            categoryStatuses = categoryStatuses
        )
    }
}
```

---

## 4. 生物识别与多任务防窥安全架构 (Security & Privacy)

### 4.1 三重安全防护矩阵

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                            1. 生物识别强认证底座                            │
│     基于 androidx.biometric.BiometricPrompt，适配指纹、3D人脸与凭据 PIN 密码     │
├─────────────────────────────────────────────────────────────────────────────┤
│                         2. 单调时钟后台超时锁屏状态机                       │
│    采用 SystemClock.elapsedRealtime()，彻底杜绝用户通过篡改系统时间绕过锁屏   │
├─────────────────────────────────────────────────────────────────────────────┤
│                          3. 多任务界面防窥截屏防护                          │
│     动态注入/清除 WindowManager.LayoutParams.FLAG_SECURE，阻断系统缩略图快照 │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 💡 重点与技术难点攻坚：为什么防超时必须使用单调时钟？
```kotlin
class AppSecurityCoordinator(private val onShakeTriggered: () -> Unit) {
    var isAppLocked by mutableStateOf(false)

    // ---- 为什么绝对不能用 System.currentTimeMillis()？ ----
    // currentTimeMillis() 读取的是手机墙上时间（Wall Clock）。
    // 如果恶意用户切到后台后，进入系统设置把手机时钟往前调 1 小时，
    // currentTimeMillis() 计算出的时间差就会变成负数，从而瞬间绕过锁屏超时判定！
    // elapsedRealtime() 是 CPU 从开机起算的单调硬件时钟，不可篡改，即便手机睡眠也不会停滞！
    private var backgroundTimestamp = 0L

    fun onStop() {
        backgroundTimestamp = SystemClock.elapsedRealtime() // 记录退后台的硬件时间戳
    }

    fun onStart(
        activity: FragmentActivity,
        biometricEnabled: Boolean,
        timeoutSeconds: Int,
        lang: String,
        shieldEnabled: Boolean
    ) {
        if (!biometricEnabled) return

        val elapsedSeconds = if (backgroundTimestamp == 0L) {
            Long.MAX_VALUE / 1000 // 首次冷启动：强制锁定
        } else {
            (SystemClock.elapsedRealtime() - backgroundTimestamp) / 1000
        }

        if (elapsedSeconds >= timeoutSeconds) {
            isAppLocked = true // 越过安全阈值，切入锁定状态机
            promptUnlock(activity, lang, shieldEnabled)
        }
    }

    // ---- 多任务界面截屏防窥 (FLAG_SECURE 动态治理) ----
    // Android 系统在用户切多任务卡片时，会自动对当前 Activity 窗口截屏作为缩略图。
    // FLAG_SECURE 会强行命令系统渲染引擎在合成多任务卡片时将其涂黑/留白，
    // 同时阻断一切录屏与截屏软件刺探敏感资产数据！
    fun applyRecentAppsShield(activity: Activity, shieldEnabled: Boolean) {
        if (shieldEnabled || isAppLocked) {
            activity.window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
```

---

## 5. Google 云同步与增量快照设计 (Cloud Sync & Backup)

### 5.1 无服务器架构直连 Google Drive REST API v3

1. **零服务器维护**：应用不部署任何后端服务器，杜绝数据在第三方中转服务器落盘的安全合规风险；
2. **CredentialManager 现代鉴权**：使用 Android 官方最新的凭据管理器完成 Google 账号一键无感授权；
3. **动态 OAuth2 提权处理**：在调用 Drive API 遇到权限不足时，拦截 `UserRecoverableAuthException` 并拉起系统授权 Intent：

```kotlin
// 捕获权限不足异常并拉起用户交互授权弹窗
try {
    googleDriveService.uploadBackup(backupJson)
} catch (e: UserRecoverableAuthException) {
    // 动态提权：调起系统专属授权面板，用户授权后自动重试
    activityResultLauncher.launch(e.intent)
}
```

---

## 6. 本地智能通知预警中枢架构 (Local Notification Hub)

### 6.1 渠道分流矩阵与防骚扰机制

| 渠道 ID | 渠道名称 | 重要性 (Importance) | 提醒方式 | 适用业务场景 |
| :--- | :--- | :--- | :--- | :--- |
| `channel_budget_alerts` | 预算超支与警戒预警 | `IMPORTANCE_HIGH` | 响铃 + 振动 + 浮动横幅 | 月度总预算或分类预算跨越 80% 警戒或 100% 超支 |
| `channel_recurring_bills`| 周期账单自动入账 | `IMPORTANCE_DEFAULT` | 提示音 / 轻微振动 | 周期规则到期自动插入流水，提醒用户对账 |
| `channel_app_updates` | 应用版本更新提醒 | `IMPORTANCE_LOW`/`DEFAULT` | 静默或轻微提示 | 发现 GitHub / Google Play 新版本发布 |

#### 🔑 严格防骚扰状态机 (Anti-Spam Dedup)
- **去重 Key 格式**：`"${yearMonth}:${categoryId}:${alertLevel}"`
- **单向升级原则**：同月、同分类、同等级仅发送 1 次通知；允许从 80% 警戒通知单向升级为 100% 超支通知，但超支后后续消费不再重复打扰。

---

## 7. 桌面小部件 2.0 架构 (AppWidget 2.0)

### 7.1 RemoteViews 极简渲染与跨进程唤醒
1. **内存极简主义**：小部件在系统桌面宿主进程（`Launcher`）中渲染，完全通过标准 `RemoteViews` 组装布局，内存占用常驻 $< 2\text{MB}$；
2. **DeepLink 闪电唤起**：桌面小部件提供“快速记餐饮”、“快速记交通”直达入口。通过构建标准 `PendingIntent`，携带深链参数零级直达记账弹窗：

```kotlin
// 构造带参数的直达 PendingIntent
val intent = Intent(context, MainActivity::class.java).apply {
    action = Intent.ACTION_VIEW
    data = Uri.parse("lexpense://quick_add?category=food&type=EXPENSE")
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
}
val pendingIntent = PendingIntent.getActivity(
    context,
    requestCode,
    intent,
    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // 兼容 Android 12+ 安全规范
)
```

---

## 8. CI/CD 自动化构建与发布体系 (Release Pipeline)

```text
代码提交 / 打 Git Tag (git push --tags)
                 │
                 ▼
       GitHub Actions 自动化流水线
                 │
                 ├─ 1. JDK 17 环境初始化与 Gradle 依赖校验
                 ├─ 2. R8 代码深度混淆与无用资源缩减 (minify & shrink)
                 ├─ 3. Google Play App Signing 密钥签名
                 ├─ 4. 生成高度切片的 AAB (Android App Bundle) 交付物
                 │
                 ▼
       自动化发版至 Google Play 生产轨道
                 │
                 ▼
       发版成功邮件通知系统负责人 (listen2code@gmail.com)
```

---

## 9. 核心架构源码文件对照清单 (Source Code Index)

| 源码文件路径 | 行数 | 架构核心职责 |
| :--- | :--- | :--- |
| `core/state/ExpenseAppState.kt` | 214 行 | 应用顶层全局状态编排器、跨 Tab 时间同步状态机、双击快速置顶 |
| `core/state/NavTab.kt` | 23 行 | 类型安全的底部导航 Tab 枚举定义 |
| `core/route/CommonRoute.kt` | 61 行 | 泛型 MVI 路由适配器、双重生命周期派发统一收口点 |
| `core/effect/CommonUiEffectCollector.kt` | 104 行 | 全局集中副作用流调度器（Toast、Snackbar、分享、键盘） |
| `core/security/AppSecurityCoordinator.kt` | 127 行 | 安全生命周期协调器、单调时钟超时状态机、`FLAG_SECURE` 防窥治理 |
| `core/security/BiometricSecurityManager.kt` | 80 行 | 系统生物识别强认证适配与设备能力探测工具类 |
| `core/overlay/AppOverlayHost.kt` | 26 行 | 全局浮层声明式宿主（APM 悬浮窗） |
| `data/engine/TransactionCalculationEngine.kt` | 138 行 | 纯 Kotlin 五层无状态计算引擎、收支聚合与预算健康度状态机 |
| `data/engine/RecurringTransactionEngine.kt` | 144 行 | 周期账单调度与自动履约记账引擎 |
| `data/update/UpdateCheckerService.kt` | 137 行 | 异步网络版本检测服务与 SemVer 版本号对比算法 |

---

## 10. 核心架构难点攻坚对照表 (Technical Challenges & Solutions)

| # | 核心技术难点 | 产生原因与潜在风险 | 终极解决方案 | 核心落地模块 |
| :---: | :--- | :--- | :--- | :--- |
| **1** | **跨 Tab 时间维度不同步** | 用户在流水页切到 8 月，切到统计页时仍是当月，造成认知割裂 | `syncTimeState` 双向同步联动 | `ExpenseAppState` |
| **2** | **年视图下钻月流水后返回被重置** | 年视图点击月份下钻月流水，再点底部导航返回统计页时，传统同步会把年视图覆写为月视图 | 引入 `preserveStatisticsYearOnReturn` 下钻保护状态锁 | `ExpenseAppState` |
| **3** | **中立 Tab 造成时间源污染** | 设置页没有时间概念，直接从设置页切统计页会导致时间源丢失 | `lastTimeTab` 追踪上一个有效的时间 Tab | `ExpenseAppState.switchTab()` |
| **4** | **双击 Tab 造成意外重复滚动** | `SharedFlow` 默认重放机制会导致切 Tab 时误触自动滚顶 | `replay = 0, extraBufferCapacity = 1` 消除历史重放 | `_scrollToTopEvents` |
| **5** | **多 ViewModel 生命周期的重复模板代码** | 每个页面都手写监听，易导致注销遗漏与代码冗余 | `CommonRoute` 泛型内联双重生命周期收口 | `CommonRoute.kt` |
| **6** | **多 ViewModel 副作用事件冲突** | 各页面分散弹 Snackbar，消息队列竞争且难以处理撤销操作 | `CollectCommonUiEffects` 统一注册，`collectLatest` 保证最新优先 | `CommonUiEffectCollector.kt` |
| **7** | **篡改系统时钟绕过生物识别锁屏** | 手机墙上时钟 `currentTimeMillis()` 可被用户手动回调伪造未超时 | 使用硬件单调时钟 `SystemClock.elapsedRealtime()` 测算间隔 | `AppSecurityCoordinator` |
| **8** | **多任务界面泄露敏感资产截图** | 操作系统切多任务卡片时会对当前 Activity 强制拍照存底 | 动态注入与清除 `WindowManager.LayoutParams.FLAG_SECURE` | `applyRecentAppsShield()` |
| **9** | **Activity 结果回调导致内存泄露** | `ActivityResultLauncher` 依赖 Activity 实例，不能直接放入 ViewModel | 采用 `StateHolder` 模式隔离 Compose 框架级状态与纯业务状态 | `SettingsStateHolder.kt` |
| **10**| **复杂多维过滤引发主线程卡顿** | 频繁输入搜索、切换账户、换月时多重嵌套循环遍历 | `TransactionCalculationEngine` 五层管线化纯函数单次遍历汇总 | `TransactionCalculationEngine` |
