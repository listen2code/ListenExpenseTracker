# ListenExpenseTracker - 桌面 Widget 与通知系统规格说明

本文档描述 `ListenExpenseTracker` 的 **Android 原生桌面小组件 (AppWidget) 实现现状与本地定时记账提醒通知设计规划**。

---

## 1. 桌面小组件 (AppWidget) — 已实现 ✅

### 1.1 今日结余与快捷记账组件 (4x2 / 2x2)
* **核心内容**：
  * 显示今日支出总额与当月结余进度。
  * 包含快捷入口按钮：`[+ 记一笔]`、`[扫码记账]`。
* **技术方案**：
  * 使用传统 Android `AppWidgetProvider` + `RemoteViews` 实现。
  * 通过 `ListenExpenseAppWidgetProvider.updateAllWidgets(...)` 在账单数据变更时（如新增/删除交易）自动刷新 Widget 显示内容。

---

## 2. 本地记账提醒通知系统 (Local Notification) — 规划中 ⏳

> [!IMPORTANT]
> 以下为设计规划，**尚未实现**。当前代码库中未包含 WorkManager、AlarmManager 或 NotificationManager 相关实现。

### 2.1 晚间定时记账提醒
* **触发机制**：每日 21:30 通过 `WorkManager` 或 `AlarmManager` 触发定时广播。
* **防骚扰策略**：
  * 若用户今日已有记账记录（`count(todayTransactions) > 0`），则自动静默跳过提醒。
  * 若今日无记录，弹出通知：*"今天还没有记账哦，点击快速记录今天的花销吧~"*。
* **点击行为**：点击通知直接拉起 `MainActivity` 并自动弹出 `AddTransactionSheet`。
