# ListenExpenseTracker (原生 Android 极简智能记账)

`ListenExpenseTracker (lExpense)` 是一款基于现代 Android 原生顶级技术栈打造的高性能、模块化解耦、隐私优先与本地优先（Local-First）的个人财务记账与智能资产管理应用。

项目采用 **Kotlin 2.2.10 + Jetpack Compose + 纯 Kotlin MVI 单向数据流 + Room SQLite + Google Credential Manager + Gradle Composite Build** 组合构建架构，全工程零引入第三方笨重图表库，实现极轻量、极速响应、深度安全防窥与全链路 APM 可观测性。

---

## 🌟 核心特性与产品矩阵 (Core Capabilities)

### 1. ⚡ 极速记账与大拇指黄金交互
- **2 秒闪电记账 (2-Second Flash Recording)**：
  - 桌面小部件直接外露 4 大高频消费场景（🍔 餐饮、🚗 交通、🛍️ 购物、📦 杂项），点击通过**双源 DeepLink** (`lexpense://quick_add?category=...`) 零延迟穿透唤起记账弹窗并自动预选对应分类与账户；
  - 流水页右下角布局双悬浮按钮（主记账 `+` 与复合筛选 FAB），单手大拇指舒适盲操；支持长按 500ms 震动一键重置所有已生效过滤。
- **人体工学数字算术键盘 (`NumericKeypad`)**：
  - 自研纯原生键盘，支持实时算术求值（加减混合输入）；
  - 配备物理微触觉反馈：普通键触发轻微按压震动，完成记账触发脉冲震动；
  - 遵循 Rule 22 动态截断拦截，严格限制输入金额至多 2 位小数。

### 2. 🛡️ 离线优先与多级缓存体系 (Local-First Architecture)
- **Room InvalidationTracker 响应式驱动**：本地 SQLite 数据库作为唯一的真实事实来源（SSOT），底层表变动自动触发 `Flow<List<TransactionEntity>>` 重发，实现零轮询 UI 响应；
- **三级缓存与数据流控**：
  - **L1 内存缓存**：分类列表与自定义账户常驻内存，纳秒级读取；
  - **L2 响应式磁盘持久化**：Room 与 DataStore (`stateIn(WhileSubscribed(5000))`) 共享热流；
  - **L3 云端快照**：增量上传前执行 **SHA-256 哈希指纹脏数据校验 (Dirty Checking)**，无变动静默跳过，绝不浪费网络流量与设备电量。

### 3. ☁️ Google 官方原生登录与云端无服务器备份 (Cloud Sync & Backup)
- **AndroidX `CredentialManager` 现代身份认证**：调起系统原生半屏 Google 账号选择器，免密安全授权；
- **Google Drive REST API v3 真实直连**：
  - 无需第三方私有服务器，直接与用户个人的 Google 云端硬盘通信，将结构化 JSON 快照安全存储于 `appDataFolder` 沙盒；
  - 支持 **5000ms 协程防抖自动备份**、切后台自动备份、仅 Wi-Fi 限制与一键云端还原；
  - 多用户账号级隔离，文件指纹比对杜绝多账号切换串号覆盖。

### 4. 📊 严格分类预算管理中心 (Category Budget Management)
- **单一模态零闪烁宿主 (`CategoryBudgetModalDialog`)**：
  - 在同一窗口内平滑推移与交叉淡入淡出（`AnimatedContent`）切换看板与编辑模式；
  - **440dp 恒定高度锁定**，彻底消灭切换视图时对话框的高度跳动；
  - 内置 `HorizontalPager` 手势跟手双向无限左右滑动切月。
- **纯整数分区均分算法 (Integer Partitioning)**：
  - 计算商与余数分配百分比，数学上确保总分配比例严格恒等于 100%，彻底消除浮点数加减导致的 2%~3% 漂移。
- **三态健康度诊断模型 (`BudgetHealthStatus`)**：
  - 正常 `NORMAL` ($<80\%$)、预警 `WARNING` ($80\% \sim 100\%$)、超支 `OVERBUDGET` ($\ge 100\%$) 动态徽章。

### 5. 📅 周期性固定账单与订阅管理 (Recurring & Subscriptions)
- **生活成本基线测算 (Baseline Cost)**：自动汇总房租、宽带、流媒体等服务，计算每月固定支出基线与占总预算比例；
- **冷启动自检与自动履约**：应用启动时后台自动比对当前日期与规则下次执行时间，自动向 Room 插入流水（带 `[周期]` 标识）并推进周期，支持闰年 2 月平滑过渡。

### 6. 💡 智能财务洞察与深度环比分析 (Smart Financial Insights)
- **9 大核心分析算法与诊断规则**：
  - 储蓄率健康度与收支赤字检测；
  - 月环比 (MoM) 消费增长/节流诊断；
  - 预算消耗速率预测（预计月底耗尽日）；
  - 分类过度倾斜与突发异动排查；
  - 周末报复性消费偏好检测；
  - 高频小额「拿铁因子」统计；
  - 单日开销最大峰值检测与零支出自律天数统计。
- **统计页顶部轮播展示洞察卡片**，支持一键直达对应分类或日期查看详情。

### 7. 🔒 生物识别安全锁与隐私防窥护盾 (Biometric Privacy Shield)
- **指纹/面容强认证**：接入 `BiometricSecurityManager`，优先强生物识别，优雅降级系统锁屏 PIN/密码；
- **单调硬件时钟防篡改**：采用 `SystemClock.elapsedRealtime()` 计算退后台时长，抵御用户回拨系统时间作弊；
- **系统多任务防窥 (`FLAG_SECURE`)**：动态注入安全窗口标志，彻底阻断 Recent Apps 多任务卡片截屏与录屏刺探；
- **物理手势一键隐额**：`ShakeDetector` 监听加速度计重力矢量差，1000ms 节流防抖，支持“摇一摇”或双击快速切换金额掩码 (`••••`)。

### 8. 📱 桌面小部件体验 2.0 (App Widget 2.0)
- **4x2 智能双模看板**：当月总支出、剩余预算额度、彩色进度条与三态健康度徽章；
- **RemoteViews 难点突围**：
  - **5 级动态阶梯字号降级**：自适应金额长度在 18sp 至 9.5sp 平滑过渡，彻底解决百万金额截断出现 `...`；
  - **三态彩色进度条互斥显隐**：预置 3 条彩色 ProgressBar，运行时互斥显隐，攻克部分厂商系统 Tint 失效缺陷；
  - **防误触设计**：解除根布局点击，仅在有效区域响应交互，消除桌面滑屏误触；
  - **零轮询能耗准则 (`updatePeriodMillis="0"`)**：待机能耗绝对为零。

### 9. 🔔 本地智能通知预警中枢 (Local Notification Hub)
- **Android 13+ 运行时权限与分级渠道矩阵**：
  - `channel_budget_alerts` (高优先级)：80% 警戒线与 100% 超支强提醒；
  - `channel_recurring_bills` (默认优先级)：周期账单自动入账履约明细与汇总提醒；
  - `channel_app_updates` (轻提醒)：静默检测新版本发布提醒。
- **防骚扰去重状态机**：以 `"${yearMonth}:${targetId}:STATUS"` 为去重键，同月同级仅通知 1 次；点击通知 DeepLink 精准穿透至对应弹窗或流水。

### 10. 🔭 全链路 APM 性能监控与可观测性
- **500 条环形内存日志 (`ApmLogger`)**：无锁线程安全滚动覆盖，零 GC 负担，划分 APP / DB / SYNC / CRASH 四大频道；
- **全局唯一 TraceId 链路打点 (`TraceManager`)**：毫秒级度量异步任务执行耗时；
- **防二次崩溃双写保护 (`CrashHandler`)**：拦截未捕获异常并同步导出崩溃现场快照。

---

## 🏗️ 系统三层架构拓扑 (Composite Build Architecture)

全工程基于 Gradle Composite Build (`includeBuild`) 建立清晰的单向向下依赖拓扑，底层 SDK 绝对零业务依赖，可独立作为通用底座复用：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                    【顶层业务宿主模块: ListenExpenseTracker (:app)】                              │
│  - 业务展示层: Transactions / Statistics / Settings Feature Screens & Dialogs                  │
│  - 业务状态机: ExpenseAppState (全局联动/下钻保护), ViewModels & Business Delegates             │
│  - 纯函数计算: TransactionCalculationEngine, CategoryBudgetEngine, FinancialInsightEngine     │
│  - 数据持久化: AppDatabase (TransactionDao, RecurringRuleDao), ExpenseDataStoreManager         │
│  - 扩展与生态: ListenExpenseAppWidgetProvider (桌面小组件 2.0), LocalNotificationManager (通知) │
└───────────────────────┬───────────────────────────────────────────┬─────────────────────────────┘
                        │ 依赖                                      │ 依赖
                        ▼                                           ▼
┌───────────────────────────────────────────────┐ ┌───────────────────────────────────────────────┐
│  【通用 UI 与图表库 SDK: ListenUiComponent】   │ │       【底层架构底座 SDK: ListenArch】        │
│  - 纯原生 Canvas 图表 (DonutChart, LineChart) │ │  - MVI 状态机基类 (BaseViewModel<S, I>)       │
│  - 人体工学微触觉键盘 (NumericKeypad)         │ │  - 瞬态单次副作用通道 (CommonUiEffect)        │
│  - Material 3 统一设计系统 (ListenTheme)      │ │  - 高性能 APM (ApmLogger 环形队列, TraceManager)│
│  - 通用容器: SurfaceCard, CommonButton, etc.  │ │  - 全局崩溃保护 (CrashHandler 异常双写)       │
│  - 调试监控浮窗 (LogInspectorSheet)           │ │  - 偏好基类 (BaseDataStoreManager) & Result   │
└───────────────────────────────────────────────┘ └───────────────────────────────────────────────┘
```

---

## 📂 代码目录组织结构 (Feature-First)

```text
ListenExpenseTracker/
├── app/src/main/java/com/listen/expensetracker/
│   ├── MainActivity.kt                      # 单 Activity 宿主、导航与 DeepLink 路由中枢
│   ├── auth/                                # Google Credential Manager 原生身份认证
│   ├── core/                                # 核心系统层
│   │   ├── apm/                             # APM 悬浮窗宿主与日志提取
│   │   ├── effect/                          # CommonUiEffect 统一副作用调度
│   │   ├── route/                           # CommonRoute 双重生命周期派发路由
│   │   ├── security/                        # 生物识别应用锁、单调时钟、摇一摇手势防窥
│   │   └── state/                           # ExpenseAppState 全局状态持有者与下钻保护锁
│   ├── data/                                # 数据与领域模型层
│   │   ├── backup/                          # JSON 全量快照与 CSV (UTF-8 BOM) 导出/导入
│   │   ├── cloud/                           # Google Drive REST API v3 直连与自动备份管理器
│   │   ├── db/                              # Room 实体表 (Transaction, RecurringRule) 与 DAO
│   │   ├── engine/                          # 核心纯函数计算引擎 (流水、分类预算、洞察、周期)
│   │   ├── i18n/                            # ExpenseStrings 记账专属多语言字典
│   │   ├── model/                           # Category, Account, BudgetModel 领域数据类
│   │   ├── pref/                            # ExpenseDataStoreManager 偏好设置持久化
│   │   └── update/                          # UpdateCheckerService GitHub Pages 版本检测
│   ├── features/                            # 核心业务特性层 (Feature-First)
│   │   ├── budget/                          # 分类预算管理中心 (CategoryBudgetModalDialog)
│   │   ├── common/                          # 跨业务共享组件 (月份胶囊、Pager 常量)
│   │   ├── transactions/                    # 流水明细、4 维排序、复合过滤器与记账弹窗
│   │   ├── statistics/                      # 财务洞察卡片轮播、Canvas 环形图、折线走势图
│   │   └── settings/                        # 设置中心、数据中心、资产账户管理与开发者模式
│   └── widget/                              # 桌面小组件 2.0 (Provider 与 LayoutBinder)
├── ListenArch/                              # 独立架构底座 SDK (Composite Build)
└── ListenUiComponent/                       # 独立 UI 与 Canvas 图表库 SDK (Composite Build)
```

---

## 📚 规范与架构文档全景索引 (`docs/`)

`docs/` 目录下收录了 19 份详尽的系统设计、接口字典、算法推导与工程落地规范：

| 文档分类 | 规范文档路径 | 核心内容概述 |
| :--- | :--- | :--- |
| **工程开发准则** | [docs/project_development_guide.md](docs/project_development_guide.md) | 工程开发总方针、MVI 分层架构哲学、零 Mermaid 规范与单文件 $\le 250$ 行红线 |
| **系统架构总览** | [docs/architecture.md](docs/architecture.md) | 系统总体架构设计、模块隔离红线、单向数据流与计算模型解析 |
| **架构决策记录** | [docs/architecture_decision_records.md](docs/architecture_decision_records.md) | 33 项核心架构决策记录 (ADR-001 ~ ADR-033) 与技术选型权衡 |
| **全模块 API 手册** | [docs/api_reference.md](docs/api_reference.md) | 646 行全模块 API 接口字典，涵盖 7 大计算引擎、DAO、DataStore 与云同步 API |
| **演进与任务看板** | [docs/todo.md](docs/todo.md) | 系统演进路线图、各阶段（Stage 1~8）技术攻坚、难点代码剖析与 Backlog 需求池 |
| **Google 登录与云同步** | [docs/google_auth_and_drive_sync_guide.md](docs/google_auth_and_drive_sync_guide.md) | Credential Manager 原生登录与 Google Drive REST API v3 备份恢复全景指南 |
| **自动备份机制** | [docs/google_drive_auto_backup_design.md](docs/google_drive_auto_backup_design.md) | 自动备份三触发源、四重安全守卫、5s 协程防抖与 SHA-256 脏数据校验规范 |
| **桌面小部件 2.0** | [docs/app_widget_2_0_design.md](docs/app_widget_2_0_design.md) | 桌面小组件 2.0 规范，2 秒闪电记账、双源 DeepLink、5 级阶梯动态字号与防误触 |
| **小部件与通知规范** | [docs/push_and_widgets_specification.md](docs/push_and_widgets_specification.md) | 桌面小组件与系统通知渠道矩阵工程设计规范与调试手册 |
| **本地智能通知中枢** | [docs/local_notification_system_design.md](docs/local_notification_system_design.md) | 统一本地通知预警中枢主规范，超支警报、周期履约与版本升级三场景 |
| **超支预警专项规范** | [docs/budget_overrun_notification_design.md](docs/budget_overrun_notification_design.md) | 80% 警戒线与 100% 超支本地即时预警决策时序与防骚扰状态机 |
| **周期账单与订阅** | [docs/recurring_transactions_and_subscriptions_design.md](docs/recurring_transactions_and_subscriptions_design.md) | 周期性固定收支与订阅管理规范，冷启动自检推进算法与生活成本 Baseline |
| **智能财务洞察** | [docs/smart_financial_insights_design.md](docs/smart_financial_insights_design.md) | 智能财务诊断 9 大核心算法公式数学推导、阈值裁定与卡片呈现规范 |
| **生物识别与安全锁** | [docs/biometric_security_and_privacy_design.md](docs/biometric_security_and_privacy_design.md) | 生物识别应用锁、单调时钟防作弊、多任务 FLAG_SECURE 与摇一摇物理防窥 |
| **APM 性能监控** | [docs/apm_performance_monitoring_design.md](docs/apm_performance_monitoring_design.md) | APM 可观测性系统，500 条内存环形缓冲、短 TraceId 追踪与崩溃双写持久化 |
| **多级缓存与流控** | [docs/repository_caching_strategy.md](docs/repository_caching_strategy.md) | Local-First 三级缓存架构（内存 L1、Room L2、云端 L3）、失效协议与一致性保障 |
| **错误代码与异常收敛** | [docs/error_codes_reference.md](docs/error_codes_reference.md) | 1xxx~5xxx 统一错误代码矩阵、Kotlin 原生 Result<T> 范式与自愈策略 |
| **Custom Lint 规则** | [docs/custom_lint_rules.md](docs/custom_lint_rules.md) | 静态分析与 16 条 Custom Lint 代码质量红线与 UAST 规则实现规范 |
| **单元测试与覆盖率** | [docs/test_coverage_report.md](docs/test_coverage_report.md) | 单元测试覆盖率报告，涵盖全工程 33 个测试套件、126 项用例分析 |

---

## 🧪 自动化测试与质量保障体系 (Quality Assurance)

项目推行严格的自动化测试门禁与质量保障机制：
- **纯 JVM 快速测试**：核心领域计算引擎完全解耦 Android Framework Context，可在 JVM 上毫秒级执行；
- **测试覆盖状态**：
  - **33 个** 独立测试套件 (Test Suites / Classes)；
  - **126 项** 单元测试断言用例 (Test Cases / Methods)；
  - **100% 绿灯通过**，无任何 Regression 缺陷；
  - **57 项** Gradle 组合构建与验证任务执行一次性通过。

### 常用构建与测试指令

```powershell
# 1. 运行全量单元测试与断言校验 (日常回归推荐)
./gradlew testDebugUnitTest

# 2. 编译并校验 Kotlin 语法规范
./gradlew compileDebugKotlin

# 3. 运行静态代码分析与 Lint 质量检查
./gradlew lintDebug

# 4. 生成 Jacoco 单元测试覆盖率报告
./gradlew testDebugUnitTest jacocoTestReport

# 5. 构建生产环境 Release APK 安装包
./gradlew assembleRelease
```

---

## 🚀 快速上手与本地编译 (Quick Start)

### 1. 环境要求
- **Android Studio**: Ladybug (2024.2.1) 或更高版本；
- **JDK 版本**: OpenJDK 17 或 21；
- **Gradle 版本**: 8.11+；
- **Kotlin 版本**: 2.2.10；
- **最低 SDK 版本**: Android 8.0 (API 26)；目标 SDK 版本: Android 15 (API 35)。

### 2. 克隆与工程导入
```bash
git clone https://github.com/listen2code/ListenExpenseTracker.git
cd ListenExpenseTracker
```
使用 Android Studio 直接打开该目录，Gradle 会自动识别根目录 `settings.gradle.kts` 中的 Composite Build 配置并完成三模块协同索引。

---

## 📄 开源与版权说明

Copyright © 2026 ListenExpenseTracker Contributors.  
Licensed under the Apache License, Version 2.0.
