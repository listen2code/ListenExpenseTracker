# ListenExpenseTracker - 全模块 API 与接口参考手册 (API Reference)

本文档整理 `ListenExpenseTracker` 以及底层 SDK (`ListenArch` 与 `ListenUiComponent`) 的公开核心类、接口、DAO 与扩展函数接口清单。

---

## 1. `ListenArch` 核心接口

### 1.1 `BaseViewModel<ViewState, UserIntent, ViewEffect>`
* **包名**：`com.listen.arch.mvi`
* **职责**：MVI 模式抽象基类，规范单向数据流。
* **方法**：
  * `val viewState: StateFlow<ViewState>`：只读 UI 状态流。
  * `val viewEffect: Flow<ViewEffect>`：只读单次副作用事件流（Toast、页面跳转、触感反馈）。
  * `fun handleIntent(intent: UserIntent)`：接收并处理外部意图。
  * `protected fun updateState(reducer: ViewState.() -> ViewState)`：原子更新状态。
  * `protected suspend fun emitEffect(effect: ViewEffect)`：发射单次事件。

### 1.2 `TransactionDao` (Room DAO)
* **包名**：`com.listen.arch.data.db`
* **方法**：
  * `fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>`
  * `fun getTransactionsByDateRangeFlow(start: Long, end: Long): Flow<List<TransactionEntity>>`
  * `suspend fun insertTransaction(transaction: TransactionEntity)`
  * `suspend fun insertTransactions(transactions: List<TransactionEntity>)`
  * `suspend fun deleteTransactionById(id: String)`
  * `suspend fun clearAll()`

### 1.3 `BaseDataStoreManager`
* **包名**：`com.listen.arch.data.pref`
* **属性 Flow**：
  * `languageFlow: Flow<String>` (zh/en/ja)
  * `themeModeFlow: Flow<String>` (LIGHT/DARK/SYSTEM)
  * `accentColorFlow: Flow<String>` (EMERALD/SAPPHIRE/AMBER/ROSE/VIOLET/SLATE)
  * `currencySymbolFlow: Flow<String>` (￥/$/€/£/円)
  * `monthlyBudgetFlow: Flow<Double>`
  * `lastSyncTimestampFlow: Flow<Long>`

### 1.4 `ApmLogger` & `TraceManager`
* **包名**：`com.listen.arch.apm`
* **方法**：
  * `ApmLogger.d(tag, msg, traceId)` / `ApmLogger.i(...)` / `ApmLogger.e(...)` / `ApmLogger.db(...)` / `ApmLogger.sync(...)`
  * `TraceManager.newTraceId(): String`
  * `TraceManager.trace(channel, tag, operationName, traceId) { block }`

### 1.5 `StringsRes`
* **包名**：`com.listen.arch.i18n`
* **方法**：
  * `StringsRes.get(key: String, lang: String): String`

---

## 2. `ListenUiComponent` 核心组件

| 组件名 | 包路径 | 核心入参 | 说明 |
| :--- | :--- | :--- | :--- |
| `NumericKeypad` | `com.listen.uicomponent.keypad` | `onKeyPress`, `onDeletePress`, `onDonePress` | 算术键盘（带醒目“完成记账 ✓”按钮） |
| `DonutChart` | `com.listen.uicomponent.charts` | `items: List<PieChartItem>`, `totalValue: Double` | Canvas 环形占比图 |
| `BarChart` | `com.listen.uicomponent.charts` | `items: List<BarChartItem>`, `height: Dp` | Canvas 垂直柱状走势图 |
| `SegmentedProgressBar` | `com.listen.uicomponent.components` | `segments: List<ProgressSegment>` | 分段比例条 |
| `SearchBarInput` | `com.listen.uicomponent.components` | `query: String`, `onQueryChange: (String) -> Unit` | 通用搜索输入框 |
| `LogInspectorSheet` | `com.listen.uicomponent.apm` | `logs: List<LogEntryUi>`, `onClearLogs`, `onExportLogs` | APM 实时日志浮窗 |
