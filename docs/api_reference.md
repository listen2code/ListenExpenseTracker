# ListenExpenseTracker - 全模块 API 与接口参考手册 (API Reference)

本文档整理 `ListenExpenseTracker` 以及底层 SDK (`ListenArch` 与 `ListenUiComponent`) 的公开核心类、接口、DAO、引擎与扩展函数接口清单。

> 每个模块均附带 **设计思路 (Design Rationale)**、**实现要点 (Implementation Details)** 与 **关键代码解读 (Code Walkthrough)** 说明。

---

## 1. `ListenArch` 核心接口与模块

### 1.1 `BaseViewModel<State, Intent>`
* **包名**：`com.listen.arch.mvi`
* **职责**：MVI 模式抽象基类，规范单向数据流。Effect 类型统一固定为 `CommonUiEffect`。

#### 🔑 设计思路

1. **为什么只有 2 个泛型 `<State, Intent>` 而不是 3 个 `<State, Intent, Effect>`？**
   早期版本使用 3 个泛型，但在实际开发中发现 Effect（UI 副作用）的类型在所有页面中高度一致（Toast、导航、Snackbar 等），将其泛型化导致每个 ViewModel 子类都需要定义自己的 sealed class，增加了大量样板代码。因此将 Effect 统一固化为 `CommonUiEffect` 接口，各业务 App 可通过实现该接口自由扩展。

2. **为什么 State 用 `StateFlow` 而 Effect 用 `SharedFlow`？**
   - `StateFlow`：会**重播最新值**给新订阅者。这对 UI 状态至关重要——当屏幕旋转或从后台恢复时，UI 可以立即获取最新状态，无需重新加载。
   - `SharedFlow`：**不重播**历史值。这对一次性事件（Toast、导航）至关重要——避免旋转屏幕后重复弹出 Toast。

#### 📋 API 清单

| 方法 | 可见性 | 签名 | 说明 |
|------|--------|------|------|
| `viewState` | public | `val viewState: StateFlow<State>` | 只读 UI 状态流，UI 层通过 `collectAsState()` 订阅驱动重组 |
| `viewEffect` | public | `val viewEffect: SharedFlow<CommonUiEffect>` | 只读单次副作用事件流，用于 Toast、导航、Snackbar |
| `currentState` | protected | `val currentState: State` | 获取当前状态快照，在 `handleIntent` 内部安全读取 |
| `handleIntent` | public abstract | `fun handleIntent(intent: Intent)` | MVI 唯一入口——所有 UI 交互通过 Intent 进入 |
| `updateState` | protected | `fun updateState(reducer: State.() -> State)` | 原子更新状态，使用扩展函数 Lambda 可直接引用 State 属性 |
| `emitEffect` | protected | `fun emitEffect(effect: CommonUiEffect)` | 发射单次事件，内部 `viewModelScope.launch` 异步发射避免阻塞 |
| `toLifecycleIntent` | open | `fun toLifecycleIntent(event: LifecycleEvent): Intent?` | 生命周期→Intent 映射，默认 null（不关心），子类按需重写 |
| `dispatchLifecycleEvent` | public | `fun dispatchLifecycleEvent(event: LifecycleEvent)` | 由 `CommonRoute` 统一调用，自动分发生命周期 Intent |

#### 💡 关键代码解读

```kotlin
// ---- 状态更新：使用扩展函数 reducer 实现原子性更新 ----
protected fun updateState(reducer: State.() -> State) {
    // State.() -> State 是扩展函数类型 Lambda：
    // 在 Lambda 内部，this 指向当前状态，可以直接访问 State 的所有属性
    // 例如：updateState { copy(isLoading = true) } 等价于 currentState.copy(isLoading = true)
    _viewState.value = currentState.reducer()
}

// ---- 副作用发射：为什么需要 viewModelScope.launch？ ----
protected fun emitEffect(effect: CommonUiEffect) {
    viewModelScope.launch {
        // SharedFlow.emit() 是挂起函数，但 handleIntent 是普通函数
        // 通过 launch 将其异步化，避免在 handleIntent 中直接 suspend
        // 同时确保 Effect 在 ViewModel 协程作用域内发射，生命周期安全
        _viewEffect.emit(effect)
    }
}

// ---- 生命周期集成：零侵入式自动刷新 ----
// 业务 ViewModel 只需重写 toLifecycleIntent 即可获得生命周期感知：
// override fun toLifecycleIntent(event: LifecycleEvent) = when (event) {
//     LifecycleEvent.ON_APPEAR -> TransactionsIntent.Refresh
//     else -> null
// }
```

#### 🧩 `CommonUiEffect` 副作用接口

```kotlin
interface CommonUiEffect {  // 使用 interface 而非 sealed interface，允许宿主 App 自由扩展
    data class ShowToast(val message: String) : CommonUiEffect          // 短暂消息
    data class ShowSnackbar(val message: String, ...) : CommonUiEffect  // 带操作按钮的提示
    data class ShareText(val title: String, ...) : CommonUiEffect       // 系统分享
    data class NavigateTo(val route: String) : CommonUiEffect           // 声明式导航
    data object NavigateBack : CommonUiEffect                            // 返回上一页
    data class OpenUrl(val url: String) : CommonUiEffect                 // 打开外部浏览器
    data object HideKeyboard : CommonUiEffect                            // 隐藏软键盘
}
```

#### 🧩 `ResultExtensions.kt` 异常收敛工具

| 函数 | 签名 | 设计思路 |
|------|------|---------|
| `safeCall` | `inline fun <T> safeCall(block: () -> T): Result<T>` | 将任意代码块包装为 `Result<T>`，统一收敛异常，配合 `fold`/`onSuccess`/`onFailure` 链式调用 |
| `asResult` | `fun <T> Flow<T>.asResult(): Flow<Result<T>>` | 将 Flow emit 包装为 `Result.success`，catch 异常为 `Result.failure` |

### 1.2 `TransactionDao` (Room DAO)
* **包名**：`com.listen.expensetracker.data.db`
* **设计思路**：采用 Flow 响应式查询 (`getAllTransactionsFlow`)，当数据库中插入/删除/更新任何记录时，Flow 自动重新触发查询并推送新结果给 UI，实现 **数据变更 → UI 自动刷新** 的 Local-First 响应式闭环。

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `getAllTransactionsFlow()` | `Flow<List<TransactionEntity>>` | 响应式查询全量账单。Room 自动在表数据变更时重新触发 |
| `getAllTransactions()` | `suspend List<TransactionEntity>` | 一次性查询全量（用于导出/备份等非响应式场景） |
| `getTransactionById(id)` | `suspend TransactionEntity?` | 按主键查询单条 |
| `insertTransaction(tx)` | `suspend Unit` | 插入单条，自动通知 Flow 刷新 |
| `insertTransactions(txs)` | `suspend Unit` | 批量插入（云端恢复、Demo 数据） |
| `updateTransaction(tx)` | `suspend Unit` | 更新单条（编辑账单） |
| `deleteTransaction(tx)` | `suspend Unit` | 按实体删除 |
| `deleteTransactionById(id)` | `suspend Unit` | 按 ID 删除 |
| `deleteAll()` | `suspend Unit` | 清空全表（危险操作，用于数据重置） |

### 1.3 `BaseDataStoreManager` (通用配置基类) + `ExpenseDataStoreManager` (记账专属扩展)
* **基类包名**：`com.listen.arch.data.pref`
* **子类包名**：`com.listen.expensetracker.data.pref`

#### 🔑 设计思路

采用**模板方法 (Template Method)** 模式：`BaseDataStoreManager` 在 `ListenArch` 中定义，封装所有 Listen 系列 App 共享的配置项（语言、主题、登录状态等），`ExpenseDataStoreManager` 在记账 App 中继承并扩展业务专属配置（币种、预算、自定义账户）。

> **技术难点**：Jetpack DataStore 的 Flow 是冷流 (Cold Flow)，每次 collect 都会从磁盘重新读取。为避免多处订阅导致重复 IO，在 ViewModel 中使用 `stateIn(SharingStarted.WhileSubscribed(5000))` 将其转换为热流，5 秒无订阅者才释放。
* **基类属性 Flow（`BaseDataStoreManager`）**：
  * `languageFlow: Flow<String>` (zh/en/ja)
  * `themeModeFlow: Flow<String>` (LIGHT/DARK/SYSTEM)
  * `accentColorFlow: Flow<String>` (EMERALD/SAPPHIRE/AMBER/ROSE/VIOLET/SLATE)
  * `isLoggedInFlow: Flow<Boolean>`
  * `userEmailFlow: Flow<String>`
  * `userDisplayNameFlow: Flow<String>`
  * `userAvatarUrlFlow: Flow<String>`
  * `lastSyncTimestampFlow: Flow<Long>`
* **子类扩展属性 Flow（`ExpenseDataStoreManager`）**：
  * `currencySymbolFlow: Flow<String>` (￥/$/€/£/円)
  * `monthlyBudgetFlow: Flow<Double>`
  * `customAccountsFlow: Flow<String>`
* **基类更新方法**：
  * `suspend fun setLanguage(langCode: String)`
  * `suspend fun setThemeMode(mode: String)`
  * `suspend fun setAccentColor(accent: String)`
  * `suspend fun setLoggedIn(isLoggedIn: Boolean, userEmail: String = "", displayName: String = "", avatarUrl: String = "")`
  * `suspend fun setLastSyncTimestamp(timestamp: Long)`
* **子类更新方法**：
  * `suspend fun setCurrencySymbol(symbol: String)`
  * `suspend fun setMonthlyBudget(budget: Double)`
  * `suspend fun setCustomAccountsJson(json: String)`

### 1.4 `CloudSyncManager` (Google 账号隔离云同步引擎)
* **包名**：`com.listen.arch.sync`

#### 🔑 设计思路

1. **状态机驱动**：通过 `StateFlow<SyncState>` 对外暴露同步状态（`IDLE` → `SYNCING` → `SUCCESS`/`ERROR`），UI 层只需 `collectAsState()` 即可响应式展示同步进度。
2. **多账户隔离**：使用 `accountCloudSnapshots: Map<String, String>` 以邮箱为 Key 隔离存储，确保不同 Google 账号的备份数据互不干扰。
3. **i18n 友好**：`SyncState.messageKey` 存储翻译键而非硬编码字符串，UI 层动态翻译。

#### 💡 关键代码解读

```kotlin
// ---- MD5 校验和：确保上传数据完整性 ----
private fun computeMd5(input: String): String {
    val md = MessageDigest.getInstance("MD5")
    val digest = md.digest(input.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }  // 每字节转 2 位十六进制
}

// ---- 链路追踪：TraceManager.trace 自动记录操作耗时 ----
val count = TraceManager.trace(
    channel = ApmLogChannel.SYNC,        // 日志归类到 SYNC 频道
    operationName = "BackupToCloud",      // 操作名用于 APM 耗时分析
    traceId = traceId                     // 分布式追踪 ID 可贯穿整个调用链
) { _ -> ... }
```

| 方法 | 返回类型 | 说明 |
|------|---------|------|
| `syncStateFlow` | `StateFlow<SyncState>` | 当前同步状态（含时间戳、账号、记录数） |
| `backupToCloud(payload, recordCount, accountEmail, traceId)` | `Result<Int>` | 上传 JSON 快照，自动计算 MD5 校验和 |
| `restoreFromCloud(accountEmail, traceId)` | `Result<String>` | 拉取指定账号的云端快照 |

### 1.5 `ApmLogger` & `TraceManager`
* **包名**：`com.listen.arch.apm`

#### 🔑 设计思路

1. **环形缓冲区 (Ring Buffer)**：限制最大 500 条日志，超出时 `removeAt(0)` 移除最旧条目。选择 `CopyOnWriteArrayList` 而非普通 `ArrayList`，是因为日志可能从任意线程（协程、回调）写入，而 UI 线程同时在读取展示。`CopyOnWriteArrayList` 在写入时复制底层数组，保证并发读安全。
2. **响应式推送**：每次写入后将 `buffer.toList()` 快照赋值给 `StateFlow`，驱动 `LogInspectorSheet` 实时刷新，无需轮询。
3. **四大频道分流**：`APP`（业务逻辑）、`DB`（数据库）、`SYNC`（云同步）、`CRASH`（崩溃），在调试浮窗中按频道 Chip 过滤。

| 方法 | 说明 |
|------|------|
| `ApmLogger.d/i/w/e(tag, msg, traceId)` | 按级别记录日志 |
| `ApmLogger.db(tag, msg, traceId)` | 数据库专用 INFO 日志 (DB 频道) |
| `ApmLogger.sync(tag, msg, traceId)` | 同步专用 INFO 日志 (SYNC 频道) |
| `ApmLogger.crash(tag, msg, throwable)` | 崩溃专用 ERROR 日志 (CRASH 频道) |
| `ApmLogger.clear()` | 清空全部日志 |
| `ApmLogger.exportPlainText()` | 导出格式：`[HH:mm:ss.SSS][CHANNEL][LEVEL][Tag] [traceId] message` |
| `TraceManager.newTraceId()` | 生成唯一追踪 ID (UUID) |
| `TraceManager.trace(channel, tag, op, traceId) { }` | 自动计时包装器，记录 block 执行耗时 |

### 1.6 `StringsRes`
* **包名**：`com.listen.arch.i18n`
* **设计思路**：基于内存字典的轻量级多语言系统。不依赖 Android Resources，可在 ViewModel/纯 Kotlin 层直接调用，支持运行时即时切换语言无需重启 Activity。

| 方法 | 说明 |
|------|------|
| `StringsRes.get(key, lang)` | 根据 key + 语言代码查表返回对应文案 |

---

## 2. `ListenUiComponent` 核心组件与图表

| 组件名 | 包路径 | 核心入参 | 说明 |
| :--- | :--- | :--- | :--- |
| `NumericKeypad` | `com.listen.uicomponent.keypad` | `onKeyPress`, `onDeletePress`, `onDonePress` | 算术键盘（内置 `TextHandleMove` 轻触微震与 `LongPress` 完成记账脉冲反馈） |
| `DonutChart` | `com.listen.uicomponent.charts` | `items: List<PieChartItem>`, `totalValue: Double` | Canvas 环形占比图（内置 650ms 顺时针扫开平滑动效与自适应中心指标） |
| `BarChart` | `com.listen.uicomponent.charts` | `items: List<BarChartItem>`, `height: Dp` | Canvas 垂直柱状走势图 |
| `LineChart` | `com.listen.uicomponent.charts` | `points: List<LineChartPoint>`, `chartHeight: Dp` | 平滑贝塞尔折线走势图（内置 600ms 动态拔起动效与渐变区域填充） |
| `SegmentedProgressBar` | `com.listen.uicomponent.components` | `segments: List<ProgressSegment>` | 分段比例条 |
| `SearchBarInput` | `com.listen.uicomponent.components` | `query: String`, `onQueryChange: (String) -> Unit` | 通用搜索输入框 |
| `SurfaceCard` | `com.listen.uicomponent.components` | `cornerRadius: Dp`, `contentPadding: Dp` | 统一卡片容器，支持精准圆角几何对齐 |
| `LogInspectorSheet` | `com.listen.uicomponent.apm` | `logs: List<LogEntryUi>`, `onClearLogs`, `onExportLogs` | APM 实时日志浮窗（支持水平滑动 Chip 与文本导出） |

---

## 3. `ListenExpenseTracker` 业务层核心类与组件

### 3.1 `TransactionCalculationEngine`
* **包名**：`com.listen.expensetracker.data.engine`
* **职责**：纯函数式高阶计算引擎，将原始账单列表过滤、排序、分组并计算出所有统计指标。

#### 🔑 设计思路

1. **`object` 单例 + 纯函数**：引擎不持有任何状态，所有计算通过入参驱动、出参返回，保证**线程安全**和**可测试性**。单元测试只需构造输入 List 即可验证全部统计逻辑。
2. **一次计算、全量产出**：`filterAndCalculate()` 一次性输出 `CalculationResult`（含过滤后列表、收支总额、预算比例、分类占比图表数据等 17 个字段），避免多次遍历同一数据集。

#### 💡 关键代码解读

```kotlin
// ---- 多维过滤管线 (Filter Pipeline) ----
// 5 层独立过滤条件通过 && 串联，每层互不干扰，新增过滤维度只需追加一个 matches* 变量
val matchedFiltered = monthFilteredList.filter { item ->
    val matchesQuery = ...      // 第 1 层：文本搜索（跨分类名、备注、金额、多格式日期）
    val matchesAccount = ...    // 第 2 层：账户类型过滤
    val matchesType = ...       // 第 3 层：收/支类型过滤
    val matchesCategory = ...   // 第 4 层：分类过滤（支持 ID/名称/自定义名多重匹配）
    val matchesAmount = ...     // 第 5 层：金额区间过滤（预设 + 自定义范围）
    matchesQuery && matchesAccount && matchesType && matchesCategory && matchesAmount
}

// ---- 日期搜索兼容多种格式（技术难点） ----
// 用户可能搜索 "8月15日"（中文）、"08-15"（ISO）、"8-15"（简写）
// 因此需要同时生成多种格式进行匹配：
val dateLabelZh = "${itemMonth}月${itemDay}日"        // → "8月15日"
"%02d-%02d".format(itemMonth, itemDay)                 // → "08-15"
"$itemMonth-$itemDay"                                   // → "8-15"

// ---- 月份边界精确处理（技术难点） ----
fun getMonthRangeAndTitle(offset: Int, lang: String): Triple<Long, Long, String> {
    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, offset)       // offset=0 当月，-1 上月，1 下月
        set(Calendar.DAY_OF_MONTH, 1)     // 定位到月初第 1 天 00:00:00.000
        // ... 清零时/分/秒/毫秒
    }
    val startTs = cal.timeInMillis         // 月初时间戳
    cal.add(Calendar.MONTH, 1)             // 跳到下月初
    cal.add(Calendar.MILLISECOND, -1)      // 回退 1ms = 本月最后一刻
    val endTs = cal.timeInMillis           // 月末时间戳
    // Calendar.add 自动处理大小月、闰年、12月→1月跨年等所有边界
}
```

**完整入参**：

```kotlin
fun filterAndCalculate(
    allList: List<TransactionEntity>,     // 全量原始账单
    currentOffset: Int,                    // 月份偏移（0=当月，-1=上月）
    query: String,                         // 搜索关键字
    accountFilter: String,                 // 账户过滤（"ALL"/"CASH"/"BANK"/...）
    budget: Double,                        // 月度预算
    sortOrder: TransactionSortOrder,       // 排序方式（时间/金额 升/降序）
    currencySymbol: String,                // 币种符号
    lang: String,                          // 当前语言
    typeFilter: String,                    // 收支类型（"ALL"/"EXPENSE"/"INCOME"）
    selectedCategories: Set<String>,       // 多选分类 ID 集合
    categoryFilter: String,                // 单选分类过滤
    amountPreset: AmountFilterPreset,      // 金额预设区间（<50/50~500/>500/自定义）
    customMinAmount: Double?,              // 自定义最小金额
    customMaxAmount: Double?               // 自定义最大金额
): CalculationResult
```

### 3.2 `CategoryRepository` (动态分类管理)
* **包名**：`com.listen.expensetracker.data.model`
* **方法**：
  * `val expenseCategories: List<Category>`
  * `val incomeCategories: List<Category>`
  * `fun getCategoryById(id: String): Category`
  * `fun addCustomCategory(name: String, type: String, colorHex: String): Category`
  * `fun updateCategory(id: String, newName: String): Boolean`
  * `fun deleteCategory(id: String): Boolean`

### 3.3 `AccountRepository` (支付账户管理)
* **包名**：`com.listen.expensetracker.data.model`
* **方法**：
  * `fun getAllAccounts(): List<AccountTypeItem>`：获取内置（CASH/BANK/CREDIT）与用户自定义账户全集。
  * `fun getFilterKeys(): List<String>`：获取过滤器 Chip 的 Key 列表（含 `ALL`）。
  * `fun getAccountDisplayName(key: String, lang: String = "zh"): String`：多语言解析账户展示名。
  * `fun addAccount(name: String): AccountTypeItem`：新增自定义账户。
  * `fun updateAccount(key: String, newName: String)`：重命名账户。
  * `fun deleteAccount(key: String): Boolean`：删除自定义账户。
  * `fun serializeCustomAccounts(): String`：序列化为 JSON 供持久化。
  * `fun deserializeCustomAccounts(json: String)`：从 JSON 反序列化恢复。

### 3.4 `GoogleAuthManager` (Google 授权管理)
* **包名**：`com.listen.expensetracker.auth`
* **技术栈**：AndroidX `CredentialManager` + Google Identity `GoogleIdTokenCredential`（现代零废弃 API）
* **方法**：
  * `fun getCredentialManager(context: Context): CredentialManager`：获取 AndroidX CredentialManager 实例。
  * `fun buildGoogleIdOption(serverClientId: String = ""): GetGoogleIdOption`：构建 Google Identity 登录选项配置。
  * `fun buildGetCredentialRequest(serverClientId: String = ""): GetCredentialRequest`：构建统一凭据请求。
  * `fun parseGoogleIdCredential(response: GetCredentialResponse): Result<GoogleUserProfile>`：解析 CredentialManager 返回凭据。
  * `suspend fun clearCredentials(context: Context)`：清除所有凭据状态并登出用户。

### 3.5 业务特化 UI 组件 (`features/**/components/`)
* **`TransactionSheet`** (`features.transactions.components`)：统一记账弹窗。核心入参：`transaction: TransactionEntity?` (为空则为新增，非空则为编辑模式)。内部实现了**状态提升**，输入过程本地化，点击完成时才抛出组装好的实体数据。
* **`CategoryBudgetCenterDialog`** (`features.budget.components`)：分类预算管理中心看板与设置弹窗。通过动态计算 `categoryRatios` 与全局预算额度，分配各个类别的花销限制。
* **`AccountCardItem`** (`features.transactions.components`)：单个账户行卡片，含视觉图标、内置/自定义胶囊徽标、编辑与删除按钮。
* **`AccountEditDialog`** (`features.transactions.components`)：输入与编辑账户名称对话框。
* **`AccountDeleteConfirmDialog`** (`features.transactions.components`)：账户删除确认对话框，使用 `CommonButtonStyle.Danger` 红色危险确认按钮。
* **`SettingsFinanceSection`** (`features.settings.components`)：记账规则中枢卡片，集中管理月度预算、分类管理与资产账户管理入口。
* **`SettingsDataCenterSection`** (`features.settings.components`)：一体化数据中心，收拢 Google Drive 云端同步与本地 JSON 导出/导入。
* **`GoogleAccountProfileCard`** (`features.settings.components`)：Google 登录状态、头像名片与同步指示器组件。
* **`RankingCategoryItem`** (`features.statistics.components`)：现代分类排行榜行项，含领奖台名次勋章、图标光晕气泡、百分比胶囊与全宽平滑补间进度条。
* **`SettingsVersionFooter`** (`features.settings.components`)：设置页底部版本号展示与连击进入开发者模式触发器。
* **`AboutAppDialog`** (`features.settings.components`)：关于应用信息对话框，含 Dedicated App Icon 与技术栈展示。

### 3.6 周期性收支与订阅管理模块 (`features/recurring/`)
* **`RecurringTransactionEngine`** (`data.engine`)：周期履约与固定生活成本 Baseline 纯函数/协程计算引擎。
  * `calculateNextExecutionDate(frequency, dayOfPeriod, fromDate): Long`：周期下次触发时间递推算法，自动平滑处理大小月与闰年月末。
  * `calculateMonthlyBaseline(rules: List<RecurringRuleEntity>): RecurringMonthlyBaseline`：折算每日/每周/每月/每年规则至月度刚性开销总额与净值。
  * `suspend fun processDueRules(recurringDao, txDao, currentTime): Int`：批量履约到期自动记账，写入带有 `[周期]` 标识的流水记录并推进下次扣款日。
* **`RecurringTransactionsDialog`** (`features.recurring.components`)：周期记账与订阅管理统一模态宿主，固定 420dp 容器高度配合 `AnimatedContent` 平滑滑动切换列表与编辑态。
* **`RecurringOverviewCard`** (`features.recurring.components`)：顶部每月固定支出看板卡片，展示支出基线与预算占比。
* **`RecurringRuleItemCard`** (`features.recurring.components`)：高空间利用率的三层规则卡片，第 1 行全宽最多 2 行标题独占，第 2 行周期/账户徽标与第 3 行完整金额靠左顶格，右侧垂直居中 Switch。
* **`RecurringFrequencySelector`** (`features.recurring.components`)：全频次分段选择器与水平滑动日期药丸（支持每日/每周/每月/每年）。
* **`RecurringEditState`** (`features.recurring.components`)：纯 Kotlin 状态持有者，负责表单字段输入过滤、金额最大值约束与 `RecurringRuleEntity` 构建。

### 3.7 生物识别与应用锁模块 (`core.security`)
* **`BiometricSecurityManager`** (`core.security`)：生物识别验证核心调度。
  * `isBiometricOrCredentialAvailable(context): Boolean`：检测设备是否支持指纹/面容/锁屏密码。
  * `promptUnlock(activity, title, subtitle, onSuccess, onError)`：拉起系统安全认证弹窗。
* **`BiometricLockOverlay`** (`core.security`)：基于 Compose 的全局强制安全拦截浮层。

### 3.8 智能财务洞察与年度概览引擎 (`data.engine`)
* **`FinancialInsightEngine`** (`data.engine`)：智能数据挖掘与诊断引擎。

#### 🔑 设计思路

采用**规则策略模式 (Rule Strategy Pattern)**：每种洞察是独立的检测规则，互不耦合。引擎按优先级顺序执行各规则，命中则生成对应 `FinancialInsightItem` 诊断卡片。若所有规则都未命中（财务平稳），生成兜底安慰卡片。

#### 💡 关键代码解读

```kotlin
// ---- 预算消耗速率预测算法 (Burn Rate Prediction) ----
// 核心思想：用「已过天数的日均消费」推算「月末预计总消费」
val dailyAvg = currentTotal / currentDay            // 截至今天的日均支出
val estimatedTotal = dailyAvg * maxDays              // 按此速率到月末的预计总支出
if (estimatedTotal > monthlyBudget && currentTotal < monthlyBudget) {
    // 当前尚未超支，但按趋势月末会超支 → 发出预警
    val exhaustedDay = (monthlyBudget / dailyAvg).toInt()  // 预计第几天耗尽预算
    // 生成卡片: "按照当前日均 ¥85 的消耗速度，预计将于 22 日耗尽预算"
}

// ---- 分类异动检测 (Category Spike Detection) ----
for ((catId, amt) in currentCatMap) {
    val prevAmt = prevCatMap[catId] ?: 0.0
    if (prevAmt > 50.0 && amt > prevAmt * 1.8) {
        // 阈值设计：
        // - prevAmt > 50：排除上月极小金额导致的虚假暴增（如 1元→5元 = 5x 但无意义）
        // - 1.8 倍：经验值，避免正常波动触发过多警告
        break  // 只报告最显著的一个异动分类
    }
}

// ---- 兜底机制 (Fallback) ----
if (insights.isEmpty()) {
    // 所有检测规则未命中 → 财务状况平稳 → 生成「一切正常」安慰卡片
    insights.add(FinancialInsightItem(id = "insight_steady_state", ...))
}
```

#### 📋 10 大检测策略一览

| # | 策略 | 阈值 | 严重级别 |
|---|------|------|---------|
| 1 | 收支结余率分析 | 结余率 < 0 = 赤字 | DANGER/POSITIVE |
| 2 | 月环比总支出对比 | 变化 > ±12% | WARNING/POSITIVE |
| 3 | 预算消耗速率预测 | 预计月末超支 | WARNING |
| 4 | 节流进度鼓励 | 已过 8 天 + 预计 ≤ 70% | POSITIVE |
| 5 | 分类过度集中 | 单分类 ≥ 45% | WARNING |
| 6 | 分类突增异动 | 环比 > 1.8x | INFO |
| 7 | 周末消费偏好偏移 | 周末日均 > 工作日 | INFO |
| 8 | 高频小额支出 (Latte Factor) | 累积占比显著 | INFO |
| 9 | 峰值支出日 | 单日 ≥ 总额 35% | INFO |
| 10 | 零支出自律天数 | ≥ 3 天 | POSITIVE |

| 方法 | 说明 |
|------|------|
| `generateInsights(allTx, offset, budget, currency, lang)` | 生成月度诊断卡片列表 |
| `calculateAnnualOverview(allTx, offset, lang)` | 聚合全年 12 月收支与净结余 |

### 3.9 桌面小部件与快速触达 (`widget`)
* **`ListenExpenseAppWidgetProvider`** (`widget`)：4x2 智能预算看板小部件。
  * 提供当月支出、预算进度、健康度状态更新，及 4 大高频场景（餐饮/交通/购物/杂项）闪电记账 DeepLink 入口。

### 3.10 状态容器与业务代理 (StateHolder & Delegate)

#### 🔑 设计思路

Google 官方推荐的**关注点分离 (Separation of Concerns)** 模式：

| 角色 | 职责 | 生命周期 |
|------|------|---------|
| **ViewModel** | 业务数据流转、偏好持久化 | 跨 Configuration Change 存活 |
| **StateHolder** | Compose 框架级状态（滚动位置、系统选择器回调） | 与 Composable 树绑定 |
| **Delegate** | 特定子领域复杂逻辑（云同步、数据导入导出） | 通过 DI 注入，可独立测试 |

#### 💡 关键代码解读

```kotlin
// ---- 为什么 ActivityResultLauncher 不能放在 ViewModel 中？ ----
// ActivityResultLauncher 必须在 @Composable 作用域内通过
// rememberLauncherForActivityResult() 注册，与 Activity 生命周期绑定。
// 如果在 ViewModel 中注册，会在屏幕旋转时丢失回调。

// ---- 两阶段导出模式 (Two-Phase Export) ----
// 导出 Excel 需要先收集过滤参数，再启动系统文件选择器：
var pendingExportExcelConfig by remember { mutableStateOf<Triple<...>?>(null) }
val exportExcelLauncher = rememberLauncherForActivityResult(...) { uri ->
    // 阶段 2：文件选择器返回后，从 pendingExportExcelConfig 取回之前保存的参数
    val (startTs, endTs, type) = pendingExportExcelConfig ?: Triple(null, null, "ALL")
    onIntent(SettingsIntent.ExportExcelToFile(uri, startTs, endTs, type))
}
// 阶段 1：先保存参数，再启动选择器
val onPrepareExportExcel = { startTs, endTs, typeFilter, fileName ->
    pendingExportExcelConfig = Triple(startTs, endTs, typeFilter)
    exportExcelLauncher.launch(fileName)
}
```

**`SettingsStateHolder`** (`features.settings.ui`)：
* 持有 `LazyListState`（`rememberSaveable` 保护滚动位置，防止屏幕旋转丢失）
* 封装 `exportJsonLauncher` / `importJsonLauncher` / `exportExcelLauncher` 系统文件选择器

**`SettingsSyncDelegate`** (`features.settings.viewmodel`)：
* 剥离云端备份/恢复/导入导出的耗时操作
* 通过 Koin DI 注入，保持 SettingsViewModel 的纯粹性


