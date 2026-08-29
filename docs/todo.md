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

- [x] **Google 账户连携与云端备份恢复 (CloudSyncManager.kt)**
  - [x] `GoogleAuthManager` Google 登录意图调度与结果解析
  - [x] 账号级隔离云端快照与 MD5 校验和验证
  - [x] 实时 `CircularProgressIndicator` 备份/恢复进度指示与上次同步时间戳
  - [x] JSON 全量结构化备份导出与一键还原导入 (`ImportBackupSheet`)
  - [x] CSV 格式流水账单导出与系统分享
- [x] **APM 性能监控与链路可观测性**
  - [x] `ApmLogger` 500 条环形内存日志管理器（APP / DB / SYNC / CRASH）
  - [x] `TraceManager` 全链路毫秒耗时打点
  - [x] `CrashHandler` 全局未捕获异常保护
- [x] **16 套自动化单元测试全矩阵 (100% Pass，覆盖率超 60%)**
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
  - [x] `TransactionsUiStateTest`

---

## 阶段五：体验精细化、CI/CD 自动化与架构组件解耦 (Stage 5 - 100% Completed)

- [x] **CI/CD 发布与邮件精准通知**
  - [x] GitHub Actions `deploy.yml` 仅在发布至 Google Play 生产环境成功时触发邮件通知至指定邮箱 (`listen2code@gmail.com`)。
- [x] **设置页版本号与开发者模式**
  - [x] 底部 `SettingsVersionFooter` 组件：展示当前 App 版本（优雅降级默认版本 `0.0.1`），加大点击触发热区。
  - [x] 快速连续点击版本号解锁「开发者模式」，动态展开「系统运维与测试」面板。
  - [x] `AboutAppDialog` 现代化改造：使用 App 专用矢量/栅格图标，移除冗余确认按钮，强化技术架构与 SDK 呈现。
- [x] **资产账户管理系统全面升级与重构**
  - [x] 结构分层：区分系统内置账户 (`现金`/`银行卡`/`信用卡`) 与用户自定义账户。
  - [x] 状态响应修复：引入 `refreshKey` 解决 Compose `remember` 引用相等导致添加/删除/编辑账户不即时刷新的问题。
  - [x] 长按删除能力全覆盖：在流水顶部过滤器、账户管理 Dialog、新增账单弹窗、编辑账单弹窗 4 大入口全量支持长按自定义账户调起删除二次确认。
  - [x] 遵循 `PROMPTS.md` 规范拆分抽取：
    - `AccountCardItem.kt`：单个账户行卡片与专属视觉着色。
    - `AccountEditDialog.kt`：账户添加与改名弹窗。
    - `AccountDeleteConfirmDialog.kt`：破坏性删除二次确认，遵循 Rule 15 使用 `CommonButtonStyle.Danger` 红色危险确认按钮。
    - `AccountManageDialog.kt`：精简至 220 行以内，专职调度编排。
- [x] **代码清理与多语言体验优化**
  - [x] 彻底清除微信/支付宝历史代码、文案及测试数据，规范化账户体系。
  - [x] 缩短添加账户输入框占位提示词 (`account_name_input`)，解决多语言在小屏设备折成 2 行的问题。
- [x] **UI / 动效细节微调与触觉反馈系统 (Polish & Haptics)**
  - [x] 物理级微触觉震动反馈：`NumericKeypad` 按键轻触 (`TextHandleMove`) 与完成记账脉冲 (`LongPress`)；过滤器切换与危险删除确认震动。
  - [x] 图表展开与平滑补间动效：`DonutChart` 顺时针 650ms 优雅扫开，`LineChart` 600ms 自底向上平滑拔起。
  - [x] 预算进度与收支切换过渡：`BalanceOverviewCard` 预算进度条 500ms 阻尼过渡，统计页收支切换 `AnimatedContent` 丝滑交叉淡入淡出。

---

## 需求池 (Backlog)

- [ ] **新增架构设计全景可视化功能**：在 App 关于/开发者面板展示当前架构设计与技术栈拓扑。
- [ ] **Push 本地推送与版本升级检查**：检测到新版本时推送本地通知，点击跳转设置页触发检查更新。
- [ ] **APM 日志全局悬浮窗 (Log Overlay Inspector)**：支持全局可拖拽悬浮球调起调试控制台。
