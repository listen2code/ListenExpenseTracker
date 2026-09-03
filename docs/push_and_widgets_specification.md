# ListenExpenseTracker - 桌面 Widget 与通知系统规格说明

本文档描述 `ListenExpenseTracker` 的 **Android 原生桌面小组件 (AppWidget) 实现现状与本地定时记账提醒通知设计规划**。

---

## 1. 桌面小组件 (AppWidget) — 已实现 ✅

### 1.1 今日结余与快捷记账组件
* **核心内容**：
  * 仅显示今日支出总额（实时从数据库拉取今日的 Expense 加总）。
  * 包含快捷入口按钮（拉起 `MainActivity`）。
* **技术方案**：
  * 使用传统 Android `AppWidgetProvider` + `RemoteViews` 实现（对应 `ListenExpenseAppWidgetProvider`）。
  * 每次账单数据变更后，通过 `updateFromTransactions(...)` 过滤 `todayStart` 之后的开销，重新渲染 Widget 内容。

---

## 2. 本地记账提醒通知系统 (Local Notification) — 规划中 ⏳

> [!IMPORTANT]
> 以下为设计规划，**尚未实现**。当前代码库中未包含 WorkManager、AlarmManager 或 NotificationManager 相关实现。

### 2.1 晚间定时记账提醒
* **触发机制**：每日 21:30 通过 `WorkManager` 或 `AlarmManager` 触发定时广播。
* **防骚扰策略**：
  * 若用户今日已有记账记录（`count(todayTransactions) > 0`），则自动静默跳过提醒。
  * 若今日无记录，弹出通知：*"今天还没有记账哦，点击快速记录今天的花销吧~"*。
* **点击行为**：点击通知直接拉起 `MainActivity` 并自动弹出 `TransactionSheet`。
