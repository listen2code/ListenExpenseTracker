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
  - [x] Google 账户登录状态、邮箱、用户名与头像 URL 持久化 Flow
- [x] **MVI 架构基类与错误收敛实装 (ListenArch)**
  - [x] `BaseViewModel<State, Intent, Effect>` 核心基类 (`handleIntent`, `updateState`, `emitEffect`)
  - [x] `ResultExtensions.kt` Kotlin 原生 `Result<T>` 函数式异常处理模型
  - [x] `LocaleManager` 多语言调度器与 `StringsRes` 字典系统
- [x] **通用 UI 组件与设计系统实装 (ListenUiComponent)**
  - [x] `AccentColor` & `Color.kt` 6+ 动态 Accent 调色盘 Token
  - [x] `ListenTheme` Material 3 主题包装器
  - [x] `NumericKeypad` 自定义算术数字键盘组件（含醒目“完成记账 ✓”按钮与最大金额保护）
  - [x] `DonutChart` Canvas 环形占比图
  - [x] `BarChart` Canvas 通用柱状图走势控件
  - [x] `SegmentedProgressBar` 通用分段比例条
  - [x] `SearchBarInput` 通用搜索输入框
  - [x] `SurfaceCard` 支持精确 `cornerRadius` 与 `contentPadding` 几何对齐
  - [x] `LogInspectorSheet` 调试浮窗（水平滑动 Chip、文字垂直居中）

---

## 阶段三：主 App 记账核心功能与页面实装 (Stage 3 - 100% Completed)

- [x] **记账与流水明细页面功能组装 (ListenExpenseTracker)**
  - [x] 接入 `ListenArch` 数据库与 `ListenUiComponent` 控件
  - [x] 顶部居中时间胶囊 (`[ < 2026年08月 > ]`) 与 `MonthPickerDialog` 年月网格直选
  - [x] 搜索关键字与账户类型过滤 Filter Chips，支持 `+` 管理账户
  - [x] 4 维流水排序引擎 (时间最新/时间最早/金额降序/金额升序)
  - [x] 明细列表按日分组 (`formatDayGroupHeader`) 与每日收支汇总 Header
  - [x] 长按流水条目呼出确认对话框 (`ConfirmDelete`) 进行安全删除
  - [x] 超薄响应式结余卡片与眼睛一键隐额切换
  - [x] 月度预算监控与剩余预算计算、进度条超支告警指示
- [x] **多维统计图表与排行榜 (StatisticsScreen)**
  - [x] 支出 / 收入双维度一键分析切换 (Segmented Toggle)
  - [x] 同步顶部年月胶囊切换 Header
  - [x] Canvas 环形占比图与分段比例条
  - [x] 月度日走势平滑折线图 (`LineChart`)
  - [x] 日均支出/收入、单笔最大支出/收入核心指标卡片
  - [x] 支出/收入分类排行榜
- [x] **偏好设置与数据运维 (SettingsScreen)**
  - [x] 5 大现代卡片分组架构（云端、个性化、预算、数据、系统）
  - [x] 深度接入 AndroidX CredentialManager 真实 Google 账户连携与状态持久化
  - [x] 多账户隔离云端快照同步与一键恢复
  - [x] 深浅主题与 6+ 强调色调色盘切换
  - [x] 币种弹窗切换与中英日多语言切换
  - [x] 月度预算自定义设定
  - [x] 19 条多周期全场景精细测试账单填充 (`SeedDemoData`)
  - [x] 2x2 响应式紧凑系统工具卡片与数据清空

---

## 阶段四：高阶体验、多账户云同步与自动化测试 (Stage 4 - 100% Completed)

- [x] **Google 账户连携与云端备份恢复 ([CloudSyncManager.kt](file:///C:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/sync/CloudSyncManager.kt))**
  - [x] `GoogleAuthManager` Google 登录意图调度与结果解析
  - [x] 账号级隔离云端快照与 MD5 校验和验证
  - [x] 实时 `CircularProgressIndicator` 备份/恢复进度指示与上次同步时间戳
  - [x] JSON 全量结构化备份导出与一键还原导入 (`ImportBackupSheet`)
  - [x] CSV 格式流水账单导出与系统分享
- [x] **APM 性能监控与链路可观测性**
  - [x] `ApmLogger` 500 条环形内存日志管理器（APP / DB / SYNC / CRASH）
  - [x] `TraceManager` 全链路毫秒耗时打点
  - [x] `CrashHandler` 全局未捕获异常保护
- [x] **15 套自动化单元测试全矩阵 (100% Pass，覆盖率超 60%)**
  - [x] `ApmLoggerTest`
  - [x] `TraceManagerTest`
  - [x] `CloudSyncManagerTest`
  - [x] `TransactionBackupManagerTest`
  - [x] `TransactionEntityTest`
  - [x] `BaseViewModelTest`
  - [x] `ResultExtensionsTest`
  - [x] `StringsResTest`
  - [x] `AccentColorTest`
  - [x] `ChartsModelTest`
  - [x] `CategoryRepositoryTest`
  - [x] `CategoryRepositoryComprehensiveTest`
  - [x] `AccountRepositoryTest`
  - [x] `TransactionCalculationEngineTest`
  - [x] `TransactionsIntentEffectTest`
- [x] **v0.0.9 体验与功能专项升级 (100% Completed)**
  - [x] 近 7 日图表重构升级为从当月 1 日至今的月度日走势平滑折线图 (`LineChart`)
  - [x] 流水页面移除左滑删除，改成长按条目呼出确认对话框 (`ConfirmDelete`) 进行安全删除
  - [x] 流水与统计画面年月导航胶囊支持左右滑动手势切换月份 (50px 灵敏阈值)
  - [x] 账户选择栏支持横向滑动，同时最右侧排序筛选按钮吸顶固定
  - [x] 资产账户管理 Dialog 现代化卡片式重构与美化，并实现账户在 DataStore 的持久化
  - [x] 演练数据生成升级为全随机化算法，每次点击生成不同真实场景账单

## 需求池
- [ ] **新增架构设计功能，展示当前APP的架构设计和技术栈**
- [ ] **增加push功能，APP升级后，可以收到推送，点击后跳转到setting画面，触发检查更新逻辑**
- [ ] **目前APM 日志浮窗 (Log Overlay Inspector)，目前还没实现**
- [ ] **CI/CD 增强：GitHub Actions `deploy` 任务执行成功后，自动发送邮件通知至 `listen2code@gmail.com`**