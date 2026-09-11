# 本地智能通知预警与提醒中枢设计规范 (Local Notification & Alert Hub Specification)

## 1. 概述 (Overview)

### 1.1 背景与痛点
在记账与个人财务管理应用中，通知是连接 App 与用户现实生活的核心桥梁。当前 **ListenExpenseTracker** 存在以下场景痛点：
1. **预算无法被动触达**：应用内虽有红黄绿三态健康度，但当用户离开 App 时，如果单笔大额支出或单日多笔花销打破了当月总预算或核心分类预算（如餐饮、娱乐），用户无法被动感知，容易造成持续性过度消费。
2. **周期账单静默入账无感知**：系统已实现周期性账单（Recurring Transaction）到达指定日期自动插入流水，但用户在后台静默扣款后完全不知情，无法核对自动入账是否准确、账目是否需要调整。
3. **版本更新无法及时传达**：目前仅支持在设置页手动点击“检查更新”，用户无法在第一时间获知新特性发布、性能提升或重要修复。

### 1.2 核心目标
构建一个轻量、可靠、低打扰、体验一致的**本地通知预警中枢**：
- **三大业务通知场景**：
  1. **预算预警与超支通知**：总预算及分类预算 80% 警戒线与 100% 超支即时警报；
  2. **自动周期账单履约通知**：周期账单后台自动记账后的明细与汇总提醒；
  3. **新版本发布升级通知**：静默检测发现 GitHub / Google Play 最新版本后的升级提醒；
- **严格防打扰机制 (Anti-Spam & Dedup Engine)**：同周期、同分类、同版本具备强去重逻辑，杜绝重复弹窗；
- **系统通知渠道分级 (Notification Channels Matrix)**：按 Android 8.0+ 规范划分独立 Channel，用户可按类别定制声音与振动；
- **深度路由穿透 (DeepLink Penetration)**：点击通知精准穿透到对应的业务操作界面（预算管理弹窗、流水高亮、更新弹窗）；
- **Android 13+ 运行时权限标准化适配**：无侵入权限检查、申请与降级引导。

---

## 2. 总体架构与时序图 (System Architecture & Sequence)

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                            1. 触发源 (Trigger Sources)                      │
├─────────────────────────┬─────────────────────────┬─────────────────────────┤
│    用户记账 / 批量改动   │   周期规则引擎自动履约   │   静默 / 启动版本检测   │
│  (Add/Edit Transaction) │(RecurringTransactionEngine)│  (UpdateCheckerService) │
└────────────┬────────────┴────────────┬────────────┴────────────┬────────────┘
             │                         │                         │
             ▼                         ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                      2. 决策与防骚扰引擎 (Business Guards)                   │
├─────────────────────────┬─────────────────────────┬─────────────────────────┤
│    BudgetAlertGuard     │RecurringBillNotification│AppUpdateNotification    │
│  80%警戒 / 100%超支警报 │  单笔明细 / 多笔批量聚合 │    远程版本对比 & 频控  │
│去重键: yyyy_MM:cat:LEVEL│  已入账条目去重与汇总   │  去重键: ver_tag:date   │
└────────────┬────────────┴────────────┬────────────┴────────────┬────────────┘
             │ (达到阈值 & 未提醒)    │ (有新入账)              │ (有新版本)
             └─────────────────────────┼─────────────────────────┘
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   3. 本地通知中枢 (LocalNotificationManager)                │
├─────────────────────────────────────────────────────────────────────────────┤
│  [1] NotificationPermissionHelper: Android 13+ (POST_NOTIFICATIONS) 权限鉴权│
│  [2] 渠道分流派发 (Channel Matrix):                                         │
│      ├─ channel_budget_alerts   [HIGH]    (震动 + 浮动横幅)                 │
│      ├─ channel_recurring_bills [DEFAULT] (温和提示音)                      │
│      └─ channel_app_updates     [DEFAULT] (静默 / 轻提醒)                   │
│  [3] PendingIntent 统一路由构造器 (携带 DeepLink 协议与页面参数)             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ (用户点击系统通知栏)
             ┌─────────────────────────┼─────────────────────────┐
             ▼                         ▼                         ▼
┌─────────────────────────┐┌─────────────────────────┐┌─────────────────────────┐
│  分类预算管理弹窗        ││  交易流水页 / 高亮账单  ││  版本更新说明弹窗       │
│(CategoryBudgetModal... )││  (TransactionsScreen)   ││ (UpdateAvailableDialog) │
│ 直达调整预算或查明细     ││ 快速核对自动账单 / 撤销 ││ 查看 Changelog / 去更新 │
└─────────────────────────┘└─────────────────────────┘└─────────────────────────┘
```

### 2.1 架构流转矩阵表 (Markdown Native Pipeline Table)

| 流转阶段 | 涉及组件 / 引擎 | 职责说明 | 输入与触发条件 | 产出与目标流转 |
| :--- | :--- | :--- | :--- | :--- |
| **阶段 1：业务触发源** | `TransactionsViewModel` | 捕获单笔/批量记账改动 | 用户手动添加/编辑账单 | 提交当月健康汇总 $\to$ 阶段 2 |
| | `RecurringTransactionEngine` | 到期周期规则自动履约记账 | 启动/切页触发履约逻辑 | 自动生成流水列表 $\to$ 阶段 2 |
| | `UpdateCheckerService` | 异步网络请求版本文件 | 冷启动/设置页触发版本比对 | 远端 `ReleaseInfo` $\to$ 阶段 2 |
| **阶段 2：防骚扰决策** | `BudgetAlertGuard` | 80% 警戒线与 100% 超支判定 | 预算支出比率 $\ge 80\%$ 且未通知 | 预警通知载荷 $\to$ 阶段 3 |
| | `RecurringNotificationHelper` | 自动入账单笔明细 / 批量聚合 | 履约入账条目数 $> 0$ | 账单通知载荷 $\to$ 阶段 3 |
| | `UpdateNotificationHelper` | 新版本比对与 3 天频控冷却 | 远端版本高于本地且未提醒 | 更新通知载荷 $\to$ 阶段 3 |
| **阶段 3：通知派发中枢** | `LocalNotificationManager` | Android 13+ 鉴权、渠道分流与投递 | 收到业务通知载荷 | 调用系统通知服务展示 |
| **阶段 4：界面穿透路由** | `MainActivity` | DeepLink 意图路由解析与页面调度 | 用户点击系统通知栏条目 | 唤起预算弹窗 / 流水 / 更新弹窗 |


---

## 3. 三大通知场景详细设计

### 3.1 场景一：预算超支与 80% 警戒线预警 (Budget Alerts)

#### 1. 警戒线判定规则
- **月度总预算 (Monthly Total Budget)**：
  - **超支警报 (OVERBUDGET)**：当月累计支出 $\ge$ 月度总预算（对应系统枚举 `BudgetHealthStatus.OVERBUDGET`）；
  - **警戒预警 (WARNING)**：当月累计支出 $\ge$ 月度总预算 $\times 80\%$ 且 $<$ 月度总预算（对应系统枚举 `BudgetHealthStatus.WARNING`）。
- **分类月度预算 (Category Monthly Budget)**：
  - 对设定了有效预算额度（$> 0$）的单一分类分别做超支（`OVERBUDGET`）与 80% 预警（`WARNING`）判定。

#### 2. 去重防骚扰状态机 (Anti-Spam Dedup State Machine)
去重键格式：`"${yearMonth}:${targetId}:${statusLevel}"`
- 示例 1：`"2026_09:TOTAL:WARNING"`（2026年9月总预算80%已通知）
- 示例 2：`"2026_09:c_food:OVERBUDGET"`（2026年9月餐饮分类超支已通知）
- **持久化方案**：保存在 `NotificationPreferences` 中，跨进程与重启持久生效；跨月时自动清理 2 个月前的过期历史记录。
- **状态单向升级原则**：若当月先触发了 WARNING，后续继续消费导致 OVERBUDGET，OVERBUDGET 属于新级别，允许再次通知；但在已是 OVERBUDGET 的情况下，后续消费不会再反复触发通知。

#### 3. 文案规范
- **总预算超支**：
  - 标题：`🚨 月度总预算已超支`
  - 内容：`本月总支出已达 ¥{spent}，超出总预算 ¥{overrun}，请注意控制开支！`
- **总预算预警**：
  - 标题：`⚠️ 月度总预算警戒线提醒`
  - 内容：`本月总支出已达总预算的 {ratio}%，剩余可用额度 ¥{remaining}。`
- **分类预算超支**：
  - 标题：`🚨 「{categoryName}」预算已超支`
  - 内容：`本月该分类已支出 ¥{spent}，超出预算额度 ¥{overrun}。`
- **分类预算预警**：
  - 标题：`⚠️ 「{categoryName}」预算预警`
  - 内容：`该分类支出已达设定预算的 {ratio}%，请合理规划后续消费。`

#### 4. 路由穿透
- 点击 Intent Action：`lexpense://budget_center?categoryId={catId}`
- 用户点击后直接呼出 `CategoryBudgetModalDialog`，高亮聚焦该分类，支持立即调增/调整预算。

---

### 3.2 场景二：自动周期账单履约入账通知 (Recurring Bills Execution)

#### 1. 触发时机
在每次 `RecurringTransactionEngine.processDueRules` 触发且有记录成功被自动插入（`ExecutionType.AUTO_INSERT`）后触发。
- 启动时校验；
- 每次进入流水页面（`TransactionsIntent.ScreenAppear`）时的补偿校验；
- 日常后台调度检查。

#### 2. 消息合并与聚合策略
- **单笔执行入账**：
  - 标题：`📅 周期账单已自动入账`
  - 内容：`已自动记录「{ruleTitle}」：-¥{amount}（{accountName}）`
- **多笔并发执行入账**（如月初同时执行房租、订阅、宽带等多笔）：
  - 标题：`📅 自动记账提醒（共 {count} 笔）`
  - 内容：`已自动履约入账 {count} 笔周期账单，合计支出 ¥{totalExpense}。点击查看流水明细。`
  - 使用 `NotificationCompat.InboxStyle` 或展开式长文本展示各项明细。

#### 3. 路由穿透
- 点击 Intent Action：`lexpense://transactions?filter=recurring&timestamp={latestTimestamp}`
- 用户点击后直接跳转流水主页面，并高亮定位或通过 Snackbar 提示“已自动记录 X 笔周期账单”，支持用户一键核对或撤销。

---

### 3.3 场景三：新版本发布与升级通知 (App Version Update)

#### 1. 触发时机与检测流程
- 在后台冷启动或定期触发 `UpdateCheckerService.checkLatestRelease()`；
- 当返回 `UpdateResult.NewVersionAvailable(releaseInfo)` 时，触发决策。

#### 2. 频控与去重策略
- 去重键格式：`"${releaseInfo.tagName}:${todayDate}"`
- 同一版本号：
  - 首次检测到立即推送通知；
  - 若用户未更新，冷却周期为 3 天，避免频繁打扰用户；
  - 支持在通知栏提供「暂不提醒」或「立即查看」快捷 Action。

#### 3. 文案规范
- 标题：`🚀 发现新版本 {versionName} 已发布`
- 内容：`全新版本已就绪！{changelogSummary}，点击查看更新详情。`

#### 4. 路由穿透
- 点击 Intent Action：`lexpense://update?version={versionName}`
- 用户点击后直接拉起应用内的 `UpdateAvailableDialog` 模态弹窗，展示完整更新日志，支持直接唤起 Google Play 商店或下载更新。

---

## 4. 通知渠道矩阵 (Notification Channel Matrix)

严格遵循 Android 8.0+ (Oreo) 及 Android 13+ (Tiramisu) 权限规范：

| 渠道 ID | 渠道名称 | 重要性 (Importance) | 提醒方式 | 适用场景 |
|---------|---------|--------------------|---------|---------|
| `channel_budget_alerts` | 预算超支与警戒预警 | `IMPORTANCE_HIGH` | 响铃 + 振动 + 状态栏浮动横幅 | 总预算及分类预算越过 80% / 100% 警戒线 |
| `channel_recurring_bills` | 周期账单自动入账 | `IMPORTANCE_DEFAULT` | 响铃或微振动 | 周期账单自动生效记账入库 |
| `channel_app_updates` | 应用版本更新提醒 | `IMPORTANCE_LOW` / `DEFAULT` | 无声静默通知或轻提示 | 发现可用新版本发布 |

---

## 5. 设置中心管理与权限联动 (Notification Settings)

### 5.1 设置层级与配置项
在设置页采用高度紧凑且层级分明的「通知与提醒」卡片，Master Switch 直接融于 Header：
```
┌─────────────────────────────────────────────────────────────┐
│ 🔔 通知与提醒                                     [Switch]  │  <-- Master Switch 融于顶栏
│    预算预警、周期入账与新版提醒                             │
├─────────────────────────────────────────────────────────────┤ (展开后仅 3 行，极简紧凑)
│  预算预警与超支提醒                               [Switch]  │  <-- 2合1合并项 (覆盖 80% 与 100%)
│  达 80% 警戒线或 100% 超支时提醒                            │
│                                                             │
│  周期账单自动入账提醒                             [Switch]  │
│  自动履约记账后通知核对                                     │
│                                                             │
│  新版本发布更新提醒                               [Switch]  │
│  检测到应用新版本时通知                                     │
└─────────────────────────────────────────────────────────────┘
```

### 5.2 Android 13+ 运行时权限联动流程
1. 用户在设置页打开任何通知开关时，检测 `ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)`；
2. 若未授权，通过系统标准 `ActivityResultContracts.RequestPermission` 拉起授权弹窗；
3. 若用户拒绝，设置卡片顶部显示警告横幅「系统通知权限已关闭，无法接收提醒」，点击「去开启」按钮一键直达当前 App 的系统通知设置页 (`NotificationPermissionHelper.openNotificationSettings`)。

---

## 6. 模块划分与落地代码实现清单 (Delivered Implementation)

| 序号 | 模块 / 文件 | 职责说明 | 落地行数 (<= 250) |
|:---:|------------|---------|:-------:|
| 1 | `core/notification/LocalNotificationManager.kt` | 单例通知分发中心，负责 Channel 矩阵初始化、通知构建、PendingIntent 路由与安全发送 | 207 行 |
| 2 | `core/notification/NotificationPermissionHelper.kt` | Android 13+ 运行时权限检查与系统设置跳转辅助 | 68 行 |
| 3 | `core/notification/NotificationPreferences.kt` | 通知开关偏好持久化、去重键（`notifiedKeys`）与过期清理 | 143 行 |
| 4 | `data/i18n/NotificationStrings.kt` | 中/英/日三语集中式通知渠道名、通知标题/内容及设置文案映射 | 219 行 |
| 5 | `features/budget/engine/BudgetAlertGuard.kt` | 预算超支 (100%) 与预警 (80%) 决策引擎与防骚扰状态机 | 207 行 |
| 6 | `features/recurring/engine/RecurringNotificationHelper.kt` | 周期账单自动履约后单笔/多笔聚合通知构造与派发 | 76 行 |
| 7 | `data/update/UpdateNotificationHelper.kt` | 版本更新比对结果通知派发与 3 天防打扰频控 | 53 行 |
| 8 | `features/settings/components/SettingsNotificationSection.kt` | 设置页通知面板 UI，Header 融合总开关，三项紧凑布局 | 181 行 |
| 9 | `features/settings/viewmodel/SettingsNotificationDelegate.kt` | 设置中心通知状态管理与版本检测委托代理 | 113 行 |
| 10 | `features/settings/components/SettingsNotificationSimulateDialog.kt` | 开发者模式 APM 系统通知全链路模拟演练弹窗 | 179 行 |
| 11 | `MainActivity.kt` | 扩展 DeepLink 路由解析（预算中心、周期流水、版本弹窗） | 167 行 |

