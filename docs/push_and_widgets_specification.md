# ListenExpenseTracker - 桌面 Widget 与通知系统规格说明

本文档规划 `ListenExpenseTracker` 的 **Android 原生桌面小组件 (AppWidget) 与本地定时记账提醒通知设计规范**。

---

## 1. 桌面小组件 (AppWidget) 规划

### 1.1 今日结余与快捷记账组件 (4x2 / 2x2)
* **核心内容**：
  * 显示今日支出总额与当月结余进度。
  * 包含快捷入口按钮：`[+ 记一笔]`、`[扫码记账]`。
* **技术方案**：
  * 使用 Jetpack Glance (Compose-like DSL for AppWidgets)。
  * 通过 `TransactionDao` 获取今日数据并触发 `GlanceAppWidget.update(...)`。

---

## 2. 本地记账提醒通知系统 (Local Notification)

### 2.1 晚间定时记账提醒
* **触发机制**：每日 21:30 通过 `WorkManager` 或 `AlarmManager` 触发定时广播。
* **防骚扰策略**：
  * 若用户今日已有记账记录（`count(todayTransactions) > 0`），则自动静默跳过提醒。
  * 若今日无记录，弹出通知：*“今天还没有记账哦，点击快速记录今天的花销吧~”*。
* **点击行为**：点击通知直接拉起 `MainActivity` 并自动弹出 `AddTransactionSheet`。
