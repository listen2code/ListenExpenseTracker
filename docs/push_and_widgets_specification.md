# ListenExpenseTracker - 桌面 Widget 与通知系统规格说明

本文档描述 `ListenExpenseTracker` 的 **Android 原生桌面小组件体验 2.0 (AppWidget 2.0) 实现架构与本地定时记账提醒通知设计规划**。

---

## 1. 桌面小部件 2.0 (AppWidget 2.0) — 已全面落地实现 ✅

桌面小部件已从早期的单日极简卡片全面跃升为 **4x2 智能预算看板与闪电分类直达快捷记账系统**。

### 1.1 核心产品特性与布局规范
* **规格与尺寸**：标准 4x2 单元格尺寸（`minWidth="250dp"`, `minHeight="110dp"`, `targetCellWidth="4"`, `targetCellHeight="2"`），横向与纵向双向支持自适应缩放。
* **左侧仪表盘（4x2 Smart Budget Dashboard）**：
  * **月份与看板标题**：展示当前月份（如 `9月支出`）。
  * **当月总支出金额**：大字号即时展示当前自然月已发生支出（如 `￥3,540.00`）。
  * **剩余可用预算**：当设定预算时显示 `剩余 ￥1,460.00`；超支时显示 `已超支 ￥xxx.xx`；未设预算时显示 `未设置总预算`。
  * **水平圆角预算进度条**：使用原生 `ProgressBar` 动态反映 `spent / budget` 消耗比例。
  * **三态健康度微徽章**：
    - `正常`（绿色）：月度支出 < 80% 预算；
    - `预警`（琥珀色）：80% <= 月度支出 < 100% 预算；
    - `超支`（红色）：月度支出 >= 100% 预算。
  * **点击直达**：点击左侧看板区域，通过 `PendingIntent` 零延迟拉起 `MainActivity` 并聚焦于账单流水页。
* **右侧闪电分类记账矩阵（Quick Category Matrix）**：
  * **4 大高频彩色分类按钮**：
    1. 🍔 **餐饮** (`c_food`)
    2. 🚗 **交通** (`c_transport`)
    3. 🛍️ **购物** (`c_shopping`)
    4. 📦 **日用** (`c_other_exp`)
  * **通用记账主按钮**：`+ 记一笔`。

### 1.2 闪电记账路由与 DeepLink 通信
* **Intent / DeepLink 协议**：
  * 协议规范：`lexpense://quick_add?category={categoryId}&type={type}`
  * Extras 传递：`MainActivity.EXTRA_QUICK_ADD_CATEGORY` 与 `EXTRA_QUICK_ADD_TYPE`
* **路由处理链**：
  ```mermaid
  sequenceDiagram
      participant User as 用户桌面轻触图标
      participant RemoteViews as RemoteViews PendingIntent
      participant Activity as MainActivity (singleTop)
      participant AppState as ExpenseAppState
      participant Sheet as TransactionSheet
  
      User->>RemoteViews: 点击 🍔 餐饮
      RemoteViews->>Activity: Intent(ACTION_VIEW, lexpense://quick_add?category=c_food)
      Activity->>AppState: openQuickAdd("c_food", "EXPENSE")
      AppState->>Sheet: TransactionsDialog.AddTransaction(initialCategoryId="c_food")
      Sheet-->>User: 展开记账抽屉，分类默认选中“餐饮”，光标聚焦金额
  ```
* **分类别名规范化**：`MainActivity.normalizeCategoryId` 自动兼容别名（如 `cat_food` -> `c_food`, `cat_daily` -> `c_other_exp`），确保跨版本与外部调用的健壮性。

### 1.3 动态主题适配与 RemoteViews 规范
* **主题色彩体系**：定义 `res/values/colors_widget.xml`（浅色）与 `res/values-night/colors_widget.xml`（深色），在系统切换暗色主题时，小部件外壳、仪表盘背景、分类按键色彩自动匹配，视觉质感与主 App 深度统一。

### 1.4 变动驱动与能耗保护 (Zero-Polling)
* **零后台轮询**：`updatePeriodMillis="0"`，杜绝定时器频繁唤醒 CPU 导致的额外耗电。
* **响应式驱动**：由 `TransactionsViewModel` 观察 `Room` 数据库 `getAllTransactionsFlow()` 与 `ExpenseDataStoreManager` 偏好流，仅在账单发生增删改或预算、币种设置变动时，毫秒级主动推送到 `ListenExpenseAppWidgetProvider.updateFromTransactions(...)`。
* **冷启动与自愈机制**：小组件被添加到桌面或系统重启触发 `onUpdate` 时，异步从 Room 与 DataStore 一键还原最新数据。

---

## 2. 本地记账提醒通知系统 (Local Notification) — 规划中 ⏳

> [!IMPORTANT]
> 以下为设计规划，当前代码库中未包含 WorkManager、AlarmManager 或 NotificationManager 相关实现。

### 2.1 晚间定时记账提醒
* **触发机制**：每日 21:30 通过 `WorkManager` 或 `AlarmManager` 触发定时广播。
* **防骚扰策略**：
  * 若用户今日已有记账记录（`count(todayTransactions) > 0`），则自动静默跳过提醒。
  * 若今日无记录，弹出通知：*"今天还没有记账哦，点击快速记录今天的花销吧~"*。
* **点击行为**：点击通知直接拉起 `MainActivity` 并自动弹出 `TransactionSheet`。
