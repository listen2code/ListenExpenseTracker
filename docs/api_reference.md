# ListenExpenseTracker - 全模块 API 与接口参考手册 (API Reference)

本文档整理 `ListenExpenseTracker` 以及底层 SDK (`ListenArch` 与 `ListenUiComponent`) 的公开核心类、接口、DAO、引擎与扩展函数接口清单。

---

## 1. `ListenArch` 核心接口与模块

### 1.1 `BaseViewModel<ViewState, UserIntent, ViewEffect>`
* **包名**：`com.listen.arch.mvi`
* **职责**：MVI 模式抽象基类，规范单向数据流。
* **方法**：
  * `val viewState: StateFlow<ViewState>`：只读 UI 状态流。
  * `val viewEffect: Flow<ViewEffect>`：只读单次副作用事件流（Toast、页面跳转、撤销 Snackbar）。
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
  * `isLoggedInFlow: Flow<Boolean>`
  * `userEmailFlow: Flow<String>`
  * `userDisplayNameFlow: Flow<String>`
  * `userAvatarUrlFlow: Flow<String>`
  * `lastSyncTimestampFlow: Flow<Long>`
* **更新方法**：
  * `suspend fun setLoggedIn(isLoggedIn: Boolean, userEmail: String, displayName: String, avatarUrl: String)`
  * `suspend fun setMonthlyBudget(budget: Double)`
  * `suspend fun setCurrencySymbol(symbol: String)`
  * `suspend fun setLanguage(langCode: String)`

### 1.4 `CloudSyncManager` (Google 账号隔离云同步引擎)
* **包名**：`com.listen.arch.sync`
* **方法**：
  * `val syncStateFlow: StateFlow<SyncState>`：当前云端同步状态（状态枚举、上次同步时间戳、当前账号、云端账单数）。
  * `suspend fun backupToCloud(transactions: List<TransactionEntity>, accountEmail: String, traceId: String): Result<Int>`：针对指定 Google 账号加密打包并上传快照，自动计算 MD5 校验和。
  * `suspend fun restoreFromCloud(accountEmail: String, traceId: String): Result<List<TransactionEntity>>`：从指定 Google 账号快照中拉取全量账单。

### 1.5 `ApmLogger` & `TraceManager`
* **包名**：`com.listen.arch.apm`
* **方法**：
  * `ApmLogger.d(tag, msg, traceId)` / `ApmLogger.i(...)` / `ApmLogger.e(...)` / `ApmLogger.db(...)` / `ApmLogger.sync(...)`
  * `TraceManager.newTraceId(): String`
  * `TraceManager.trace(channel, tag, operationName, traceId) { block }`

### 1.6 `StringsRes`
* **包名**：`com.listen.arch.i18n`
* **方法**：
  * `StringsRes.get(key: String, lang: String): String`

---

## 2. `ListenUiComponent` 核心组件与图表

| 组件名 | 包路径 | 核心入参 | 说明 |
| :--- | :--- | :--- | :--- |
| `NumericKeypad` | `com.listen.uicomponent.keypad` | `onKeyPress`, `onDeletePress`, `onDonePress` | 算术键盘（带醒目“完成记账 ✓”按钮） |
| `DonutChart` | `com.listen.uicomponent.charts` | `items: List<PieChartItem>`, `totalValue: Double` | Canvas 环形占比图（自适应中心指标） |
| `BarChart` | `com.listen.uicomponent.charts` | `items: List<BarChartItem>`, `height: Dp` | Canvas 垂直柱状走势图 |
| `SegmentedProgressBar` | `com.listen.uicomponent.components` | `segments: List<ProgressSegment>` | 分段比例条 |
| `SearchBarInput` | `com.listen.uicomponent.components` | `query: String`, `onQueryChange: (String) -> Unit` | 通用搜索输入框 |
| `SurfaceCard` | `com.listen.uicomponent.components` | `cornerRadius: Dp`, `contentPadding: Dp` | 统一卡片容器，支持精准圆角几何对齐 |
| `LogInspectorSheet` | `com.listen.uicomponent.apm` | `logs: List<LogEntryUi>`, `onClearLogs`, `onExportLogs` | APM 实时日志浮窗（支持水平滑动 Chip 与文本导出） |

---

## 3. `ListenExpenseTracker` 业务层核心类与引擎

### 3.1 `TransactionCalculationEngine`
* **包名**：`com.listen.expensetracker.data.engine`
* **职责**：纯函数式高阶计算引擎，将原始账单列表过滤、排序、分组并计算出所有统计指标。
* **方法**：
  ```kotlin
  fun filterAndCalculate(
      allList: List<TransactionEntity>,
      currentOffset: Int,
      query: String,
      accountFilter: String,
      budget: Double,
      sortOrder: TransactionSortOrder,
      currencySymbol: String
  ): CalculatedResult
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
  * `fun getAllAccounts(): List<AccountTypeItem>`
  * `fun getAccountName(key: String): String`
  * `fun addAccount(name: String): AccountTypeItem`
  * `fun deleteAccount(key: String): Boolean`

### 3.4 `GoogleAuthManager` (Google 授权管理)
* **包名**：`com.listen.expensetracker.auth`
* **方法**：
  * `fun getClient(context: Context): GoogleSignInClient`
  * `fun getLastSignedInAccount(context: Context): GoogleSignInAccount?`
  * `fun parseSignInResult(data: Intent?): Result<GoogleSignInAccount>`
  * `fun signOut(context: Context, onComplete: () -> Unit)`
