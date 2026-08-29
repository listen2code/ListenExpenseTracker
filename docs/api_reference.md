# ListenExpenseTracker - 全模块 API 与接口参考手册 (API Reference)

本文档整理 `ListenExpenseTracker` 以及底层 SDK (`ListenArch` 与 `ListenUiComponent`) 的公开核心类、接口、DAO、引擎与扩展函数接口清单。

---

## 1. `ListenArch` 核心接口与模块

### 1.1 `BaseViewModel<State, Intent, Effect>`
* **包名**：`com.listen.arch.mvi`
* **职责**：MVI 模式抽象基类，规范单向数据流。
* **方法**：
  * `val viewState: StateFlow<State>`：只读 UI 状态流。
  * `val viewEffect: SharedFlow<Effect>`：只读单次副作用事件流（Toast、页面跳转、撤销 Snackbar）。
  * `fun handleIntent(intent: Intent)`：接收并处理外部意图。
  * `protected fun updateState(reducer: State.() -> State)`：原子更新状态。
  * `protected fun emitEffect(effect: Effect)`：发射单次事件（内部通过 `viewModelScope.launch` 异步发射）。
  * `protected fun emitEffect(builder: () -> Effect)`：通过构建器 Lambda 发射单次事件。

### 1.2 `TransactionDao` (Room DAO)
* **包名**：`com.listen.expensetracker.data.db`
* **方法**：
  * `fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>`
  * `suspend fun getAllTransactions(): List<TransactionEntity>`
  * `suspend fun getTransactionById(id: String): TransactionEntity?`
  * `suspend fun insertTransaction(transaction: TransactionEntity)`
  * `suspend fun insertTransactions(transactions: List<TransactionEntity>)`
  * `suspend fun updateTransaction(transaction: TransactionEntity)`
  * `suspend fun deleteTransaction(transaction: TransactionEntity)`
  * `suspend fun deleteTransactionById(id: String)`
  * `suspend fun deleteAll()`

### 1.3 `BaseDataStoreManager` (通用配置基类) + `ExpenseDataStoreManager` (记账专属扩展)
* **基类包名**：`com.listen.arch.data.pref`
* **子类包名**：`com.listen.expensetracker.data.pref`
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
* **`AccountCardItem`** (`features.transactions.components`)：单个账户行卡片，含视觉图标、内置/自定义胶囊徽标、编辑与删除按钮。
* **`AccountEditDialog`** (`features.transactions.components`)：输入与编辑账户名称对话框。
* **`AccountDeleteConfirmDialog`** (`features.transactions.components`)：账户删除确认对话框，使用 `CommonButtonStyle.Danger` 红色危险确认按钮。
* **`SettingsFinanceSection`** (`features.settings.components`)：记账规则中枢卡片，集中管理月度预算、分类管理与资产账户管理入口。
* **`SettingsDataCenterSection`** (`features.settings.components`)：一体化数据中心，收拢 Google Drive 云端同步与本地 JSON 导出/导入。
* **`GoogleAccountProfileCard`** (`features.settings.components`)：Google 登录状态、头像名片与同步指示器组件。
* **`RankingCategoryItem`** (`features.statistics.components`)：现代分类排行榜行项，含领奖台名次勋章、图标光晕气泡、百分比胶囊与全宽平滑补间进度条。
* **`SettingsVersionFooter`** (`features.settings.components`)：设置页底部版本号展示与连击进入开发者模式触发器。
* **`AboutAppDialog`** (`features.settings.components`)：关于应用信息对话框，含 Dedicated App Icon 与技术栈展示。
