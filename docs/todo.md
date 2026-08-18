# ListenExpenseTracker - 演进路线图与任务追踪 (Roadmap & Todo)

本文档记录 `ListenExpenseTracker` 的功能迭代路线、目标态与落地记录。

---

## 阶段一：核心应用与库架构落地 (Stage 1 - 100% Completed)

- [x] **前期功能设计与 UI 画面规划**
- [x] **PROMPTS.md 针对 Android 原生架构的改版**
- [x] **创建根目录 README.md 与模块依赖说明**
- [x] **创建 ListenArch 独立 SDK 项目并配置 Composite Build**
- [x] **创建 ListenUiComponent 独立 SDK 项目并配置 Composite Build**
- [x] **沉淀项目开发指南 (docs/project_development_guide.md)**
- [x] **维护 Git 提交与版本控制记录**

---

## 阶段二：底层 SDK 核心与通用组件实装 (Stage 2 - 100% Completed)

- [x] **Room 数据库实装 (ListenArch)**
  - [x] `TransactionEntity` 账单数据表定义
  - [x] `TransactionDao` Flow 响应式 SQL 增删改查、日期区间查询与批量插入
  - [x] `AppDatabase` Room 单例数据库
- [x] **DataStore 偏好设置实装 (ListenArch)**
  - [x] `BaseDataStoreManager` 语言 (zh/en/ja)、主题模式 (Light/Dark/System)、AccentColor、多币种符号 (￥/$/€/£/円)、月度预算持久化 Flow
- [x] **MVI 架构基类与错误收敛实装 (ListenArch)**
  - [x] `BaseViewModel<ViewState, UserIntent, ViewEffect>` 核心基类 (`handleIntent`, `updateState`, `emitEffect`)
  - [x] `ResultExtensions.kt` Kotlin 原生 `Result<T>` 函数式异常处理模型
  - [x] `LocaleManager` 多语言调度器
- [x] **通用 UI 组件与设计系统实装 (ListenUiComponent)**
  - [x] `AccentColor` & `Color.kt` 6+ 动态 Accent 调色盘 Token
  - [x] `ListenTheme` Material 3 主题包装器
  - [x] `NumericKeypad` 自定义算术数字键盘组件（含醒目“完成记账 ✓”按钮）
  - [x] `DonutChart` Canvas 环形占比图
  - [x] `BarChart` Canvas 通用柱状图走势控件
  - [x] `SegmentedProgressBar` 通用分段比例条
  - [x] `SearchBarInput` 通用搜索输入框
  - [x] `SurfaceCard`, `IconBadge`, `EmptyStateView`, `LoadingView` 单一职责基础控件

---

## 阶段三：主 App 记账核心功能与页面实装 (Stage 3 - 100% Completed)

- [x] **记账与流水明细页面功能组装 (ListenExpenseTracker)**
  - [x] 接入 `ListenArch` 数据库与 `ListenUiComponent` 控件
  - [x] 顶部月份与日期切换选择
  - [x] 搜索关键字与账户类型 (微信/支付宝/银行卡/现金) 过滤 Filter Chips
  - [x] 4 维流水排序引擎 (时间最新/时间最早/金额降序/金额升序)
  - [x] 明细列表按日期分组与 Swipe-to-Delete 滑动删除
  - [x] 记账与编辑原生 `DatePickerDialog` 日期精准回填选择
  - [x] 账单点击全要素编辑修改 (`EditTransactionSheet`)
  - [x] 收支净结余卡片与眼睛一键隐额切换
  - [x] 月度预算监控与已用百分比进度条、超支告警指示
- [x] **多维统计图表与排行榜 (StatisticsScreen)**
  - [x] 支出 / 收入双维度一键分析切换 (Segmented Toggle)
  - [x] Canvas 环形占比图与分段比例条
  - [x] 7 日消费趋势柱状图走势
  - [x] 日均支出/收入、单笔最大支出/收入核心指标卡片
  - [x] 支出/收入分类排行榜
- [x] **偏好设置与数据运维 (SettingsScreen)**
  - [x] 深浅主题与 6+ 强调色调色盘切换
  - [x] 多币种符号切换 (`￥`, `$`, `€`, `£`, `円`)
  - [x] 中英日多语言实时切换
  - [x] 月度预算自定义设定与弹窗
  - [x] 一键填充演示测试账单 (`SeedDemoData`)
  - [x] 清空所有账单二次确认弹窗 (`ClearAllData`)

---

## 阶段四：可观测性与云端同步备份 (Stage 4 - 100% Completed)

- [x] **APM 日志浮窗与调试面板 (ListenArch / ListenUiComponent)**
  - [x] `ApmLogger` 500 条环形内存日志管理器
  - [x] APP / DB / SYNC / CRASH 4 频道分类过滤
  - [x] `LogInspectorSheet` 实时日志浮窗与一键分享/导出
- [x] **TraceId 链路追踪系统**
  - [x] `TraceManager` 生成全局唯一 `traceId`
  - [x] ViewModel Intent 到 Room DB 读写全链路毫秒级耗时打点
- [x] **Crash Safe Mode 崩溃保护**
  - [x] `CrashHandler` 全局未捕获异常拦截与 `crash_logs.txt` 持久化
- [x] **云端同步与多端备份恢复 ([CloudSyncManager.kt](file:///C:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/sync/CloudSyncManager.kt))**
  - [x] 响应式 `SyncStatus` 与上次同步时间记录
  - [x] 云端备份与云端还原 API 数据流
  - [x] JSON 全量备份导出与一键还原导入弹窗 (`ImportBackupSheet`)
  - [x] CSV 格式流水账单导出与系统分享

---

## 阶段五：自动化测试与规范矩阵 (Stage 5 - 100% Completed)

- [x] **自动化单元测试全覆盖 (`src/test/`)**
  - [x] `TransactionBackupManagerTest`：JSON 序列化与 CSV 导出验证 (100% Pass)
  - [x] `ResultExtensionsTest`：`safeCall` 与 `Flow.asResult()` 错误流控验证 (100% Pass)
  - [x] `CategoryRepositoryTest`：分类检索与默认降级验证 (100% Pass)
- [x] **📚 7 部核心规范文档矩阵矩阵 (`docs/`)**
  - [x] `docs/project_development_guide.md`：项目开发与架构设计指南
  - [x] `docs/apm_performance_monitoring_design.md`：APM 性能监控与日志浮窗设计
  - [x] `docs/repository_caching_strategy.md`：数据源缓存与云端同步降级规范
  - [x] `docs/error_codes_reference.md`：错误码与 Result<T> 统一收敛规范
  - [x] `docs/custom_lint_rules.md`：静态分析与 Lint 代码审查规范
  - [x] `docs/push_and_widgets_specification.md`：桌面 Widget 与通知系统规格
  - [x] `docs/todo.md`：演进路线图与任务追踪 (100% 完成)
