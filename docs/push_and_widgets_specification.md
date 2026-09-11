# ListenExpenseTracker - 桌面小组件 (AppWidget 2.0) 与通知系统工程设计规范
(AppWidget 2.0 & Notification System Engineering Specification)

本文档系统性定义 **ListenExpenseTracker (lExpense)** 的 **Android 原生桌面小组件 2.0 (AppWidget 2.0) 架构设计、实现细节、RemoteViews 限制突围方案**，以及与 **本地智能通知预警中枢 (Notification Hub)** 的联动工程规范。

---

## 1. 架构总览与交互拓扑 (System Overview & Topology)

小组件系统与主应用采用 **“单向事件驱动 + 局部独立状态机 + 零轮询响应式刷新”** 架构：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  【桌面小部件层 (Desktop AppWidget)】                            │
├────────────────────────────────────────────────────────────────┬────────────────────────────────┤
│   【左侧：4x2 智能预算仪表盘】                                  │   【右侧：闪电记账矩阵】       │
│   - 应用图标、月份居中导航 (<- 2026年09月 ->)                  │   - 🍔 餐饮 (c_food)           │
│   - 小眼睛隐额切换 (••••)、健康度徽章 (正常/预警/超支)         │   - 🚗 交通 (c_transport)      │
│   - 月度总支出、可用/超支预算、三态彩色进度条                   │   - 🛍️ 购物 (c_shopping)       │
│   - 精准内容防误触热区 (点击打开 App)                           │   - 📦 日用 (c_other_exp)      │
└──────────────┬────────────────────────────────┬────────────────┴───────────────┬────────────────┘
               │ 1. 翻月/隐额操作广播           │ 2. 点击内容直达主页            │ 3. 点击分类快捷记账
               ▼                                ▼                                ▼
┌──────────────────────────────────────────────┐┌─────────────────────────────────────────────────┐
│ 【小部件局部状态机 (Local State Machine)】    ││     【深层链接与路由中枢 (DeepLink Router)】     │
│       ListenExpenseAppWidgetProvider         ││             MainActivity (singleTop)            │
├──────────────────────────────────────────────┤├─────────────────────────────────────────────────┤
│ - onReceive 拦截 ACTION_PREV/NEXT_MONTH      ││ - 解析 lexpense://quick_add?category=...        │
│ - onReceive 拦截 ACTION_TOGGLE_HIDE_AMOUNT   ││ - 兼容 Intent Extras 与 URI Query 双源数据      │
│ - 按 widgetId 隔离持久化 SharedPreferences   ││ - normalizeCategoryId 分类别名标准化兼容        │
│ - 重新读取 Room 触发 WidgetLayoutBinder 渲染 ││ - 调用 ExpenseAppState.openQuickAdd 展开抽屉    │
└──────────────────────▲───────────────────────┘└────────────────────────▲────────────────────────┘
                       │                                                 │
                       │ 4. 账单发生增删改时主动推送最新数据             │
                       │                                                 │
┌──────────────────────┴─────────────────────────────────────────────────┴────────────────────────┐
│                              【主应用数据与状态流 (Core Engine & VM)】                           │
│   Room Database (TransactionDao)  +  DataStore  +  TransactionsViewModel.observeTransactions()  │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1.1 核心设计哲学与能耗准则 (Zero-Polling Energy Guard)

1. **绝对零后台轮询 (`updatePeriodMillis="0"`)**：
   - 传统 Android 桌面小部件常配置每 30 分钟轮询一次，导致设备频繁唤醒 CPU，徒增耗电；
   - 本项目小部件将系统定期更新周期设为 0，**彻底杜绝任何后台 Alarm / 定时器唤醒**，待机状态能耗为 0；
2. **纯响应式数据驱动 (Reactive Data-Driven)**：
   - 依赖 `TransactionsViewModel.observeTransactions()` 监听 Room 的 `getAllTransactionsFlow()`；
   - 仅当用户在应用内完成记账（增删改、撤销、导入）或修改了预算/币种时，由主线程/IO 协程主动调用 `ListenExpenseAppWidgetProvider.updateFromTransactions(...)`，以毫秒级速度更新桌面小部件；
3. **冷启动自愈能力**：
   - 当手机重启、启动器（Launcher）崩溃恢复或用户首次添加小部件时，系统触发 `onUpdate`，小部件在后台独立开启 `CoroutineScope(Dispatchers.IO)` 从 SQLite 读取最新数据并自我填充，无需依赖主 App 处于活跃进程。

---

## 2. 桌面小部件 2.0 (AppWidget 2.0) 核心产品与视觉规范

### 2.1 4x2 智能看板布局与自适应设计
- **尺寸规格**：`minWidth="250dp"`, `minHeight="110dp"`, `targetCellWidth="4"`, `targetCellHeight="2"`；
- **自适应暗黑模式 (DayNight Adaptation)**：
  - 浅色模式资源定义在 `res/values/colors_widget.xml`，深色模式定义在 `res/values-night/colors_widget.xml`；
  - 小部件外壳背景（`widget_surface`）、卡片内层（`widget_card_bg`）、主副文字（`widget_text_primary` / `widget_text_secondary`）随系统暗色开关毫秒级无缝切换；
- **防误触热区架构 (Anti-mistouch Architecture)**：
  - 彻底解除 Root 布局容器的全局 `setOnClickPendingIntent`；
  - 翻月按钮与小眼睛按钮设置了广阔防误触靶心（40x34dp / 40x32dp）；
  - 打开 App 的跳转行为精准收拢于金额、可用预算、月份标题与图标区域，杜绝用户在桌面滑动切页时因靠近小部件边缘而意外启动 App。

### 2.2 预算三态健康度联动机制

| 预算消耗比例 ($R = rac{	ext{Spent}}{	ext{Budget}}$) | 健康状态枚举 | 徽章文案 | 进度条视觉呈现 | 业务指导意义 |
| :--- | :---: | :---: | :--- | :--- |
| **$R < 80\%$** | `NORMAL` | `正常`（绿底绿字） | 绿色圆角进度条 (`widget_progress_normal`) | 财务状况良好，支出节奏健康 |
| **$80\% \le R < 100\%$** | `WARNING` | `预警`（琥珀色） | 琥珀黄色进度条 (`widget_progress_warning`) | 临近预算红线，提示节制消费 |
| **$R \ge 100\%$** | `OVERBUDGET` | `超支`（红色） | 红色警戒进度条 (`widget_progress_over`) | 预算已超支，促使调整限额或止损 |

---

## 3. 核心技术难点与代码实现深度剖析 (Implementation Walkthrough)

### 3.1 难点一：RemoteViews 限制下的动态字号阶梯降级算法 (`WidgetLayoutBinder.kt`)

源码位于 [`WidgetLayoutBinder.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/widget/WidgetLayoutBinder.kt)。

#### 🔑 技术难点与设计考量
Android 的 `RemoteViews` 是跨进程通过 IPC 由 SystemUI / Launcher 负责渲染的，**不支持在 XML 中声明 `app:autoSizeTextType`**。当用户支出金额达到几万、几十万甚至百万级，或者切换到英文（`$123,456.78`）时，固定字号会导致金额尾部被生硬截断显示为 `"..."`。

#### 💻 教学源码实现（含逐行中文注释）

```kotlin
// 动态字号计算 (Dynamic font size stepping):
// 根据格式化后的字符串长度与语言环境，实施多阶梯式降级，确保大金额在紧凑卡片中 100% 完整可视
val isEn = lang.equals("en", ignoreCase = true)
val targetSpentSp = when {
    formattedSpent.length <= 6 -> if (isEn) 16.5f else 18f   // ￥0 ~ ￥999 或 •••• (隐额)
    formattedSpent.length <= 8 -> if (isEn) 14.5f else 15.5f // ￥1,234
    formattedSpent.length <= 10 -> if (isEn) 12f else 13f    // ￥12,345
    formattedSpent.length <= 12 -> 10.5f                     // ￥123,456
    else -> 9.5f                                             // 百万级大额支出
}
views.setTextViewTextSize(R.id.widget_spent_amount, TypedValue.COMPLEX_UNIT_SP, targetSpentSp)

val targetRemainingSp = if (remainingText.length > 11) 9.5f else 11f
views.setTextViewTextSize(R.id.widget_budget_remaining, TypedValue.COMPLEX_UNIT_SP, targetRemainingSp)
```

---

### 3.2 难点二：三态彩色进度条互斥显隐设计 (`WidgetLayoutBinder.kt`)

#### 🔑 技术难点与设计考量
在常规 Activity 中，修改进度条颜色只需调用 `progressBar.progressTintList = ...`。然而 `RemoteViews` 提供的反射调用接口极其受限，在跨进程环境下**无法动态修改原生 ProgressBar 的进度染色（Progress Tint）**。

#### 💡 突围方案
在布局 XML 中预置 3 个不同颜色 Drawable 的原生 `ProgressBar`，在运行时通过 Triple 解构语法与互斥显隐（`View.VISIBLE` / `View.GONE`）实现零性能损耗的多色状态切换：

```kotlin
// 1. Triple 解构语法：在单行表达式中完成徽章文案、背景 Drawable、文本颜色的全方位映射
val (badgeText, badgeBg, badgeColor) = when (health) {
    BudgetHealthStatus.NORMAL -> Triple(AppStrings.BUDGET_STATUS_NORMAL.tr(lang), R.drawable.widget_badge_normal, R.color.widget_health_normal)
    BudgetHealthStatus.WARNING -> Triple(AppStrings.BUDGET_STATUS_WARNING.tr(lang), R.drawable.widget_badge_warning, R.color.widget_health_warning)
    BudgetHealthStatus.OVERBUDGET -> Triple(AppStrings.BUDGET_STATUS_OVER.tr(lang), R.drawable.widget_badge_over, R.color.widget_health_over)
}
views.setTextViewText(R.id.widget_health_badge, badgeText)
views.setInt(R.id.widget_health_badge, "setBackgroundResource", badgeBg)
views.setTextColor(R.id.widget_health_badge, ContextCompat.getColor(context, badgeColor))

// 2. 互斥控制三态进度条的显示与隐藏 (RemoteViews 无法动态染色 ProgressBar 的最佳工程替代方案)
views.setViewVisibility(R.id.widget_budget_progress_normal, if (health == BudgetHealthStatus.NORMAL) View.VISIBLE else View.GONE)
views.setViewVisibility(R.id.widget_budget_progress_warning, if (health == BudgetHealthStatus.WARNING) View.VISIBLE else View.GONE)
views.setViewVisibility(R.id.widget_budget_progress_over, if (health == BudgetHealthStatus.OVERBUDGET) View.VISIBLE else View.GONE)

val activeProgressBarId = when (health) {
    BudgetHealthStatus.NORMAL -> R.id.widget_budget_progress_normal
    BudgetHealthStatus.WARNING -> R.id.widget_budget_progress_warning
    BudgetHealthStatus.OVERBUDGET -> R.id.widget_budget_progress_over
}
views.setProgressBar(activeProgressBarId, 100, progressPercent, false)
```

---

### 3.3 难点三：基于 `widgetId` 的多实例局部状态机与隔离存储

源码位于 [`ListenExpenseAppWidgetProvider.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/widget/ListenExpenseAppWidgetProvider.kt)。

#### 🔑 技术难点与设计考量
用户可以在手机桌面上添加多个小组件（例如主屏放一个看“当月”，负一屏放一个看“上月”对比）。若使用全局单一变量存储月份偏移量与隐额状态，会导致所有小部件联动错乱。

#### 💡 隔离方案
在 `SharedPreferences` 的持久化 Key 中深度拼装 `widgetId` 后缀，实现多实例完全独立的状态隔离：

```kotlin
private const val PREFS_NAME = "listen_expense_widget_prefs"
private const val KEY_OFFSET_PREFIX = "widget_month_offset_"
private const val KEY_HIDE_PREFIX = "widget_hide_amount_"

fun getWidgetMonthOffset(context: Context, widgetId: Int): Int {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sp.getInt("$KEY_OFFSET_PREFIX$widgetId", 0)
}

fun setWidgetMonthOffset(context: Context, widgetId: Int, offset: Int) {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sp.edit().putInt("$KEY_OFFSET_PREFIX$widgetId", offset).apply()
}

fun getWidgetHideAmount(context: Context, widgetId: Int): Boolean {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sp.getBoolean("$KEY_HIDE_PREFIX$widgetId", false)
}

fun setWidgetHideAmount(context: Context, widgetId: Int, hide: Boolean) {
    val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sp.edit().putBoolean("$KEY_HIDE_PREFIX$widgetId", hide).apply()
}
```

---

### 3.4 难点四：唯一 RequestCode 与 DeepLink 路由双源解析

#### 🔑 技术难点与设计考量
1. **PendingIntent 覆盖碰撞问题**：若 4 个快捷记账按钮使用相同的 `requestCode` 构建 `PendingIntent`，系统会认为它们是同一个意图并执行参数覆盖，导致点击“餐饮”却总是打开“日用”；
2. **三方桌面启动器兼容性**：华为、小米、OPPO、原生 Pixel 启动器在派发桌面 PendingIntent 时，有的会丢失 URI Query 参数，有的会丢失 Bundle Extras。

#### 💡 突围方案
1. 为 4 个按钮分配完全独立的静态 RequestCode（`201` 餐饮、`202` 交通、`203` 购物、`204` 日用）；
2. 建立双源解析机制（Dual-Source Parsing）：先解析 URI 参数，若缺失则降级读取 Intent Extras；
3. 分类别名规范化：兼容历史版本的 `cat_food` 与新版的 `c_food`。

```kotlin
// ListenExpenseAppWidgetProvider.kt 路由意图解析
fun parseQuickAddIntent(intent: Intent?): Pair<String, String>? {
    if (intent == null) return null
    var categoryId: String? = null
    var type: String? = null

    // 源 1: 优先解析标准 DeepLink (lexpense://quick_add?category=c_food&type=EXPENSE)
    val data = intent.data
    if (data != null && data.scheme == URI_SCHEME && data.host == URI_HOST_QUICK_ADD) {
        categoryId = data.getQueryParameter(PARAM_CATEGORY)
        type = data.getQueryParameter(PARAM_TYPE)
    }

    // 源 2: 降级解析 Intent Extras (防三方 Launcher 吞噬 URI query)
    if (categoryId.isNullOrBlank()) {
        categoryId = intent.getStringExtra(EXTRA_QUICK_ADD_CATEGORY)
    }
    if (type.isNullOrBlank()) {
        type = intent.getStringExtra(EXTRA_QUICK_ADD_TYPE)
    }

    val normalizedCat = normalizeCategoryId(categoryId) ?: return null
    val normalizedType = if (type.equals("INCOME", ignoreCase = true)) "INCOME" else "EXPENSE"
    return Pair(normalizedCat, normalizedType)
}
```

---

## 4. 本地智能通知预警中枢规范 (Notification Hub)

系统建立了完整的本地通知分发规范（详见 [`docs/local_notification_system_design.md`](local_notification_system_design.md)），与桌面小部件共同构成系统的桌面与系统级被动触达网络：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                            【系统通知渠道矩阵 (Notification Channel Matrix)】                   │
├──────────────────────────┬──────────┬──────────────┬────────────────────────────────────────────┤
│ 渠道 ID (Channel ID)     │ 重要级别 │ 提示形式     │ 承载业务场景                               │
├──────────────────────────┼──────────┼──────────────┼────────────────────────────────────────────┤
│ `channel_budget_alerts`   │ HIGH     │ 震动+横幅    │ 当月总预算/分类预算达到 80% 警戒与 100% 超支│
│ `channel_recurring_bills`│ DEFAULT  │ 温和提示音   │ 周期性账单后台自动履约入账通知 (单笔/汇总) │
│ `channel_app_updates`    │ DEFAULT  │ 轻提醒       │ 静默检测到新版本发布升级通知               │
│ `channel_daily_reminder` │ DEFAULT  │ 定时温和提示 │ 晚间 21:30 定时记账提醒 (今日无账单才触发) │
└──────────────────────────┴──────────┴──────────────┴────────────────────────────────────────────┘
```

### 4.1 核心防骚扰机制 (Anti-Spam Engine)
1. **去重键隔离 (Dedup Key Guard)**：
   - 预算预警去重键：`budget_alert_${yearMonth}_${categoryId}_${level}`，同月同分类同级别仅提醒 1 次；
   - 版本更新去重键：`update_notified_${versionCode}`，同一新版本只提醒 1 次；
2. **晚间记账智能防打扰**：
   - 每日 21:30 触发前，查询 SQLite 当日交易计数：`if (count(todayTransactions) > 0) return`；
   - 只要用户今天记过账，通知系统自动保持静默，绝不打扰。

---

## 5. 调试排错与验证手册 (Verification Cheat Sheet)

```powershell
# 1. 模拟点击小组件上一月 (Prev Month) 广播 (指定 widgetId，例如 12)
adb shell am broadcast -a com.listen.expensetracker.widget.ACTION_PREV_MONTH --ei appWidgetId 12

# 2. 模拟点击小组件下一月 (Next Month) 广播
adb shell am broadcast -a com.listen.expensetracker.widget.ACTION_NEXT_MONTH --ei appWidgetId 12

# 3. 模拟点击小眼睛一键隐额切换广播
adb shell am broadcast -a com.listen.expensetracker.widget.ACTION_TOGGLE_HIDE_AMOUNT --ei appWidgetId 12

# 4. 模拟闪电分类记账 DeepLink 拉起 (测试餐饮分类 c_food)
adb shell am start -a android.intent.action.VIEW -d "lexpense://quick_add?category=c_food&type=EXPENSE"

# 5. 查看桌面小组件实时生命周期日志
adb logcat -s "ListenExpenseAppWidgetProvider" "WidgetLayoutBinder"
```
