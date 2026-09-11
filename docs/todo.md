# ListenExpenseTracker - 演进路线图与任务追踪 (Roadmap & Todo)

本文档记录 `ListenExpenseTracker` 的功能迭代路线、目标态与任务落地记录。

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
    - [x] `BaseViewModel<State, Intent>` 核心基类 (`handleIntent`, `updateState`, `emitEffect`)，Effect 统一固化为 `CommonUiEffect`
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
- [x] **33 套自动化单元测试全矩阵 (126 项测试用例 100% Pass，ListenExpenseTracker 模块全覆盖)**
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
    - [x] `CategoryBudgetEngineTest`
    - [x] `FinancialInsightEngineTest`
    - [x] `FinancialInsightDetectorsTest`
    - [x] `RecurringTransactionEngineTest`
    - [x] `AnnualCalculationEngineTest`
    - [x] `AnnualTransactionEngineTest`
    - [x] `ExpenseAppStateTest`
    - [x] `NavTabTest`
    - [x] `SecurityPreferencesTest`
    - [x] `RecurringRuleEntityTest`
    - [x] `CompoundFilterCalculationTest`
    - [x] `AmountFormatExtTest`
    - [x] `TransactionSortOrderTest`
    - [x] `UpdateCheckerServiceTest`
    - [x] `BudgetModelTest`
    - [x] `AppDimensTest`
    - [x] `ListenExpenseAppWidgetProviderTest`

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
- [x] **设置页信息架构现代化重构 (Settings Redesign - 方案一)**
    - [x] 确立「财务规则偏好」与「数据中心」清晰二分法结构。
    - [x] 落地 `SettingsFinanceSection`：聚合月度预算、分类管理，并全新引入资产账户管理入口 (`SettingsDialog.AccountManage`)。
    - [x] 落地 `SettingsDataCenterSection`：统一收拢 Google Drive 云同步与本地 JSON 文件导入/导出数据处理。
    - [x] 落地 `GoogleAccountProfileCard`：独立解耦个人信息条与同步指示器，所有文件行数收敛至 230 行内，严格遵循 `PROMPTS.md` 规范。
- [x] **统计排行榜全新视觉重塑与全界面多语言横展开**
    - [x] 重塑 `RankingCategoryItem`：采用聚合卡片，引入金/银/铜领奖台排名徽章 (#1 🥇, #2 🥈, #3 🥉)，分类图标彩色光晕气泡，百分比胶囊标签，并嵌入全宽平滑补间进度条 (`animateFloatAsState`)。
    - [x] 修复 `LineChart` 趋势图 MAX 国际化缺陷：支持 `currencySymbol`、`maxLabel`、`totalLabel` 动态注入，完成横展开排查并消除 `MetricsSummaryCard` 等处的硬编码字符串。
    - [x] 智能按月生成演练数据 (`SeedDemoData`)：打通 `Transactions` 与 `Statistics` 月份联动体系，根据用户当前所浏览的具体月份生成全天候逼真收支模拟明细。
- [x] **Tab 切换滚动状态保持与图表动态刷新动效 (State Retention & Chart Animations)**
    - [x] 列表滑动位置记忆：在 `MainActivity` 中引入 `SaveableStateHolder` 配合 `rememberSaveable` 绑定各月份列表，彻底解决 Tab 切换时列表被重置置顶的问题。
    - [x] 图表数据刷新初启动画：升级 `DonutChart` (圆环扫掠)、`SegmentedProgressBar` (进度条伸展)、`LineChart` (曲线生长升起)、`BarChart` (柱状图升长) 与 `RankingCategoryItem`，统一接入 `Animatable` + `LaunchedEffect(data)` 驱动，在月份切换、收支切换、新增删除流水时均展示丝滑的入场微动效。

---

## 阶段六：异步更新机制与版本调度 (Stage 6 - 100% Completed)

- [x] **异步检查版本更新与更新引导 (In-App Version Checker)**
    - [x] 实现 `UpdateCheckerService`：异步请求静态配置 (`https://listen2code.github.io/ListenExpenseTracker/pages/version.json`)，毫秒级响应、免鉴权且无 API 频控限制。
    - [x] 语义化版本比对（SemVer）与构建号（BuildNumber）双重比对：识别新版本并完成单元测试覆盖 (`UpdateCheckerServiceTest`)。
    - [x] `AboutAppDialog` 接入更新检测与 Loading 状态，弹出 `UpdateAvailableDialog` 自动按中英日展示多语言 Changelog 并引导前往更新。

---

## 阶段七：严格分类预算管理与全功能交互体验升级 (Stage 7 - 100% Completed)

- [x] **分类预算核心体系与健康度模型 (Category Budget Architecture)**
    - [x] 核心模型：定义 `BudgetHealthStatus`（正常 `<80%`、预警 `80%~100%`、超支 `≥100%`）、`CategoryBudgetConfig` 与 `CategoryBudgetStatus`。
    - [x] 核算引擎：实现 `CategoryBudgetEngine.calculate()`，精准计算各分类月度支出、预算额、剩余/超支额及健康度，优先将超支与预警分类置顶排序。
    - [x] 数据持久化：扩展 DataStore `KEY_CATEGORY_BUDGETS`，支持分类比率的逗号分隔字符串序列化/反序列化与实时观察更新。
- [x] **分类预算管理中心与统一模态宿主 (CategoryBudgetModalDialog)**
    - [x] 单一宿主零闪烁：消除双 `AlertDialog` 切换导致的窗口销毁与蒙层闪烁，常驻同一 Dialog 窗口。
    - [x] 440dp 恒定高度锁定：外层容器绝对锁定 440dp 高度，分类列表采用 `weight(1f)` 自适应填充，彻底消灭页面切换时的高度拉伸与跳动感。
    - [x] 平滑推移与交叉淡入淡出动效：`AnimatedContent` 配合 `slideInHorizontally + fadeIn` 驱动看板与编辑模式平滑过渡。
    - [x] `HorizontalPager` 水平左右滑动手势切月：基于 `PAGER_BASE_INDEX` 接入 1:1 跟手滑动切月体验，双向无限滑动，联动顶部胶囊与平滑动画。
    - [x] 活跃月份记忆保持：在编辑比例与看板之间切换时，保持当前浏览的月份不变。
    - [x] 纯整数分区均分算法 (Integer Partitioning)：「均分剩余」采用基础商与余数分配，数学保证分配总额严格恒等于 100%，彻底消除 2%~3% 浮点舍入漂移。
- [x] **记账与分类管理交互细节修复**
    - [x] 修复 `CategoryManageDialog` 添加自定义新分类时未即时刷新列表问题（补全 `refreshKey++` 触发重组）。
    - [x] 修复连续点击“继续”添加账单时时间戳未递增导致的逆序倒挂问题（当天记账实时取值、历史记账自动递增 1 秒、Room DAO 引入 `rowid DESC` 次级排序加固）。
    - [x] 修复 `CommonEditText` 在带标签时单行高度过小导致金额数字底部被裁剪遮挡问题（`heightIn(min = 64.dp)`）。
    - [x] 移除看板中多余的「分类预算明细」标题，将三态健康徽章直接内嵌至总览卡片底部，界面通透干练。
- [x] **多维复合过滤器与复杂业务查询 (Compound Filter Engine)**
    - [x] 引擎侧扩展 `TransactionCalculationEngine` 支持收支类型 (`typeFilter`)、自定义金额区间 (`amountPreset`)、多分类交叉选择 (`selectedCategories`) 等多维度 AND 逻辑组合筛选。
    - [x] UI 侧落地 `TransactionFilterBottomSheet`，在抽屉内可视化呈现金额预设（如 `<50`、`50-500`）、分类 Tag 墙以及排序偏好，实时响应状态重组并高亮 Active Filter Chip。

---

## 阶段八：高优先级功能规划与深度体验跃升 (Stage 8 Roadmap & Todo)

> [!NOTE]
> 阶段八核心功能详细设计与实现规范已沉淀至 `docs/` 目录下，可点击对应链接查阅完整架构方案。

### 1. 周期性固定收支与订阅管理 (Recurring Transactions & Subscriptions) - [P0, 核心高频记账]
* **详细设计文档**：[recurring_transactions_and_subscriptions_design.md](recurring_transactions_and_subscriptions_design.md)
- [x] **周期性规则数据架构**
    - [x] 定义 `RecurringRuleEntity`：支持周期类型（每日 / 每周 / 每月固定日 / 每年）、收支类型、分类、金额、账户、备注、自动记账开关及下次执行时间戳。
    - [x] 实现 `RecurringRuleDao`：提供规则的增删改查及按状态过滤 Flow。
- [x] **后台触发与自动记账调度**
    - [x] 接入应用冷启动自检与后台自动记账引擎 (`RecurringTransactionEngine`)：在到达指定日期时，根据开关自动向 `TransactionDao` 写入流水记录并推进下次执行时间。
    - [x] 支持到期自动记账与提醒确认双模式。
- [x] **订阅管理与固定成本看板 UI**
    - [x] 在设置页「记账偏好与规则」增设「周期账单与订阅」入口 (`RecurringTransactionsDialog`)。
    - [x] 汇总计算每月固定生活成本 Baseline (`RecurringOverviewCard`，如“每月固定支出 ¥4,280，占月预算 42.8%”)，直观管理房租、宽带、流媒体等服务订阅状态。

### 2. 桌面小部件体验 2.0 (App Widget 2.0 - 快速记账与预算看板) - [P0, 极速记账触达]
* **详细设计文档**：[app_widget_2_0_design.md](app_widget_2_0_design.md)
- [x] **现代化 4x2 智能预算看板小部件**
    - [x] 桌面即时展示：当月总支出、剩余预算额度、收支进度条及三态健康度（正常 / 预警 / 超支）。
    - [x] 数据联动：接入 `ListenExpenseAppWidgetProvider`，在流水发生任何变动时实时刷新小部件。
- [x] **闪电分类直达快捷记账**
    - [x] 小部件内置 4 个高频快捷分类按钮（🍔 餐饮、🚗 交通、🛍️ 购物、📦 杂项）。
    - [x] 轻触图标通过 DeepLink / Intent 零延迟直达 App 并直接拉起记账弹窗，预选对应分类与账户，实现 2 秒内闪电记账。
- [x] **动态主题适配与抗截断渲染**
    - [x] 5 级阶梯动态字号平滑降级（18sp 至 9.5sp），彻底解决百万级大额金额截断。
    - [x] 3 态进度条互斥显隐，彻底解决部分 Android 定制系统 progressTint 失效缺陷。
    - [x] 防误触边界设计，消除桌面滑屏误触。

### 3. 智能财务洞察与深度环比分析 (Smart Financial Insights & MoM) - [P1, 数据价值挖掘]
* **详细设计文档**：[smart_financial_insights_design.md](smart_financial_insights_design.md)
- [x] **财务诊断与环比分析引擎**
    - [x] 实现 `FinancialInsightEngine`：9 大分析诊断规则（储蓄率健康度、月环比 MoM、消费斜率预测耗尽日、分类过度倾斜、突发异动、周末消费偏好、拿铁因子、单日峰值、自律天数）。
    - [x] 防未来日期误判边界保护：本月零支出天数严格以今天为上限，杜绝未发生日期的错误统计。
- [x] **统计页高阶视图增强**
    - [x] 统计页顶部轮播展示「财务洞察卡片 (Insight Cards)」。
    - [x] 增设「年度 12 个月收支走势 (Annual Overview)」柱状走势图，支持横向查看全年收支结余健康曲线。

### 4. 生物识别应用锁与隐私防窥模式 (Biometric App Lock & Privacy Shield) - [P1, 资产安全]
* **详细设计文档**：[biometric_security_and_privacy_design.md](biometric_security_and_privacy_design.md)
- [x] **生物识别指纹与面容安全锁**
    - [x] 接入 AndroidX `BiometricPrompt`：在设置页提供「开启应用锁」开关与硬件兼容性检测。
    - [x] 切换至后台超过设定时间（立即 / 1分钟 / 5分钟）后重新回到前台时，强制弹出指纹/面容解锁浮层 (`BiometricLockOverlay`)。
    - [x] 单调硬件时钟防护：采用 `SystemClock.elapsedRealtime()` 计算退后台真实时长，抵御用户修改系统时间漏洞。
- [x] **防窥与隐额模式增强**
    - [x] 在多任务切换器（Recent Apps）中隐藏敏感金额截图（`FLAG_SECURE` 动态注入）。
    - [x] 手势防窥：支持加速度传感器“摇一摇手机” (`ShakeDetector`) 或“双击结余区域”快速切换全局隐额模式。

### 5. 本地智能通知预警与提醒中枢 (Local Notification & Alert Hub) - [P1, 核心体验闭环]
* **详细设计文档**：[local_notification_system_design.md](local_notification_system_design.md)
- [ ] **统一通知底座与 Android 13+ 渠道矩阵**
    - [ ] 封装单例 `LocalNotificationManager`，统筹系统通知渠道分发与 PendingIntent 路由。
    - [ ] 注册渠道矩阵：`channel_budget_alerts` (高优先级)、`channel_recurring_bills` (默认优先级)、`channel_app_updates` (轻提醒)。
    - [ ] 接入 Android 13+ (`POST_NOTIFICATIONS`) 运行时动态权限鉴权、申请与未授权引导设置页。
- [ ] **预算超支与 80% 警戒线预警状态机**
    - [ ] 实现 `BudgetAlertGuard`：记账后检测当月总预算或分类预算是否跨越 80% 警戒或 100% 超支线。
    - [ ] 严格去重防骚扰 (Dedup)：维护 `yyyy_MM:targetId:LEVEL` 状态记录，同月同级仅通知 1 次。
    - [ ] DeepLink 穿透交互：点击通知直达分类预算管理模态弹窗 (`CategoryBudgetModalDialog`)。
- [ ] **周期账单自动履约入账通知**
    - [ ] 在 `RecurringTransactionEngine.processDueRules` 自动记账成功后捕获入账清单并发送提醒（单笔明细 / 多笔聚合）。
    - [ ] 点击通知直达流水页并聚焦定位当次自动记录的账单。
- [ ] **新版本发布静默检测与升级通知**
    - [ ] 联动 `UpdateCheckerService` 静默比对远端版本，带 3 天冷却防打扰。
    - [ ] 通知展示新版本亮点，点击直达应用内 `UpdateAvailableDialog`。
- [ ] **设置中心「通知与提醒」集中配置面板**
    - [ ] 设置页新增通知管理卡片，提供全局主开关与三大场景独立子开关。

### 6. 年月视图全维度联动、版本更新闭环与安全交互增强 - [Completed]
- [x] **跨 Tab 年月视图全维度联动与下钻保护**
    - [x] 在 `ExpenseAppState` 实现常规全局双向联动：跨 Tab 切换自动同步 `period` (MONTH / YEAR) 及对应的 `monthOffset` / `yearOffset`。
    - [x] 实现下钻返回保护 (`preserveStatisticsYearOnReturn`)：年视图图表点击月份下钻至月流水，返回切回统计时稳固保持年视图。
    - [x] 底部导航栏双击已置顶 Tab 时智能快速复位：根据当前视图模式自动归位当月或当年 (`offset = 0`)。
- [x] **开发者模式隐秘手势唤醒与数据操作防误触**
    - [x] 隐藏清空所有数据按钮，通过版本号/设置区域连续点击 5 次激活开发者模式，激活后永久保留。
    - [x] 统一“生成模拟数据”与“清空所有账单”按钮高度与文案规范。
    - [x] 模拟数据生成前校验当月是否已有真实账单，若存在则拦截生成并提示错误，杜绝脏数据污染。
- [x] **设置页版本校验与 Google Play 更新闭环**
    - [x] 设置页提供“检查更新”按钮，带 Loading 状态异步请求 GitHub `version.json` 进行版本比对。
    - [x] 有新版本呼出 `UpdateAvailableDialog` 展示更新说明，支持取消或一键拉起 Google Play 商店更新。
    - [x] 月预算与周期账单按钮优化为并排紧凑排布。

### 7. 人体工学操作优化、Widget 紧凑排布与交互细节打磨 - [Completed]
- [x] **设置页 Switch 点击区域全行标准化**
    - [x] 将数据中心云端同步与 Wi-Fi 限制开关全面迁移至 `CommonSwitchRow`，全行均可触发点击，彻底统一全 App 6 处开关操作手感。
- [x] **流水画面大拇指黄金热区化与长按极速重置**
    - [x] 顶部搜索栏回归全宽，账户横滑栏回归纯粹的账户切换功能，移除右上角及搜索框下方冗余入口。
    - [x] 右下角引入双 FAB：主记账 `+` 伴随次级筛选悬浮按钮，支持徽标状态感知与单手盲操。
    - [x] 引入复合手势：单击打开 `FilterSheet`，长按触发震动并一键瞬间清空所有生效筛选。
- [x] **桌面小部件 2.0 空间紧凑度优化**
    - [x] 优化左侧信息层次为双行结构：第一行排列 Icon、小眼睛与健康度状态；第二行排列左右切月箭头与月份标题，消除紧凑感。
- [x] **底部导航置顶防失效**
    - [x] 使用 `rememberUpdatedState(listState)` 修复月份切换导致 Effects 闭包引用过期失效的边界缺陷。
- [x] **为所有功能组件生成 @Preview 预览代码**
    - [x] 覆盖 features 目录下的核心 UI 页面与组件（`TransactionsScreen`、`StatisticsScreen`、`SettingsScreen`、`ExportOptionsSheet`、`AnnualOverviewMonthDetail`、`ApmInspectorComponents` 等），提升可视化开发与调试效率。

---

## 阶段九：深度财务管理、生态联动与数据自由 (Stage 9 Roadmap & Todo - 体验跃升规划)

### 1. 账单回收站与防误删自愈机制 (Recently Deleted Trash Bin) - [P0, 资产安全与防呆]
- [ ] **软删除与生命周期标记**
    - [ ] `TransactionEntity` 扩展 `isDeleted: Boolean = false` 与 `deletedAt: Long? = null` 字段。
    - [ ] 修改 `deleteTransaction` 默认行为为软删除标记，将数据移入回收站而非直接物理抹除。
- [ ] **回收站中心与一键自愈**
    - [ ] 设置页「数据中心」增设「账单回收站」管理入口，展示已删除账单列表与删除时间。
    - [ ] 支持单笔立即恢复、一键彻底清空与 30 天超期自动物理擦除机制。

### 2. 自定义账单起始日与发薪日周期 (Custom Billing & Salary Cycle) - [P0, 真实预算刚需]
- [ ] **发薪日周期偏移计算**
    - [ ] DataStore 新增 `salaryStartDayFlow: Flow<Int>`（支持 1~28 日自由配置，如每月 10 号发薪记为月预算首日）。
    - [ ] 重构 `TransactionCalculationEngine.getMonthRangeAndTitle`：支持基于自定义起始日切割时间区间（如 `2026.08.10 ~ 2026.09.09` 为 8 月统计周期）。
- [ ] **UI 联动与胶囊提示**
    - [ ] 顶部时间胶囊自适应展示自定义周期区间标签（如 `[ < 8月期 (8.10~9.09) > ]`）。
    - [ ] 分类预算与仪表盘无缝适配发薪日周期，真实反映工薪族每月资金流动周期。

### 3. 资产账户间转账与资金调拨 (Account Transfers & Balance Allocation) - [P0, 资产体系闭环]
- [ ] **转账数据模型与交易类型扩展**
    - [ ] `TransactionEntity` 扩展 `transferTargetAccountId: String? = null` 字段，支持标记「转账调拨」类型。
    - [ ] 转账记录在源账户计为扣减、在目标账户计为增加，但系统底层全局排除计入生活「收支」统计（净资产不发生变化）。
- [ ] **转账交互与流水呈现**
    - [ ] 记账数字键盘顶部增设「转账」切换 Tab，支持自由选择转出账户与转入账户（如“储蓄卡 ➔ 信用卡还款”）。
    - [ ] 流水列表中专属呈现转账卡片样式，清晰标示资金流动轨迹。

### 4. Android 桌面快捷方式与下拉控制中心磁贴 (App Shortcuts & Quick Settings Tile) - [P1, 极速记账生态]
- [ ] **系统 App Shortcuts (长按桌面图标直达)**
    - [ ] 配置 `shortcuts.xml` 静态快捷方式：直达记账键盘（Quick Add）、直达统计看板（Statistics）。
    - [ ] 动态快捷方式：根据用户最近 7 天最高频使用的 2 个分类动态注册快捷方式（如“直接记餐饮”、“直接记交通”）。
- [ ] **Android Quick Settings Tile (下拉控制中心磁贴)**
    - [ ] 实现 `ExpenseQuickTileService`，在系统下拉通知栏提供「闪电记账」磁贴。
    - [ ] 点击磁贴直接在系统任意界面上弹拉起半屏记账 Dialog，无需返回手机桌面。

### 5. 报销管理与垫资状态跟踪 (Reimbursement & Expense Tracking) - [P1, 职场与公私分明]
- [ ] **报销属性扩展**
    - [ ] `TransactionEntity` 扩展 `reimbursementStatus: String`（`NONE`, `PENDING_REIMBURSE`, `REIMBURSED`）。
    - [ ] 记账弹窗增设「待报销」轻量勾选开关。
- [ ] **报销管理专用看板**
    - [ ] 流水页顶部 Filter Chips 增设「待报销」过滤 Tag；
    - [ ] 汇总展示当前个人累计垫资总额（如“待报销 3 笔，共 ¥1,240.00”），到账后一键点击“标记已报销”，自动排除该笔对个人生活净支出的影响。

### 6. 多数据源开放备份支持 (WebDAV & 本地定时归档) - [P1, 数据主权]
- [ ] **WebDAV 私有云同步适配**
    - [ ] 抽象 `CloudSyncProvider` 接口，支持用户配置 WebDAV 服务器地址、账号与密码（完美适配坚果云、Nextcloud、群晖 Synology NAS）。
    - [ ] 实现增量快照上传与校验，为无 Google 移动服务 (GMS) 的设备提供平替云同步方案。
- [ ] **本地存储定期全量归档**
    - [ ] 增设「自动导出本地备份」开关，每周或每月自动向系统 `Documents/ListenExpenseTracker/` 目录归档带日期的加密 JSON 文件。

### 7. 主流第三方记账数据通用 CSV 导入与迁移引导 (Smart CSV Migration) - [P2, 低门槛迁入]
- [ ] **多格式 CSV 智能解析引擎**
    - [ ] 预置主流记账软件（网易有钱、钱迹、挖财、随手记等）导出的标准列结构模板。
    - [ ] 研发交互式字段映射对话框 (`CsvImportMappingDialog`)：允许用户手动指定“日期列”、“金额列”、“分类列”、“账户列”与“备注列”。
- [ ] **数据去重与安全注入**
    - [ ] 导入前计算每条记录的时间戳、金额与备注特征值，与本地现有账单进行相似度去重，避免重复导入。

### 8. 年度财务盘点与精美长图战报分享 (Annual Year-in-Review Shareable Cards) - [P2, 情绪价值沉淀]
- [ ] **年度财务指标高阶精算**
    - [ ] 计算年度总支出/收入、最忠诚分类 Top 1、单笔最大开销故事、零支出自律天数之最、全年月度开销平稳度指数。
- [ ] **Canvas 原生精美海报渲染**
    - [ ] 采用 Jetpack Compose Canvas 原生无外部渲染依赖绘制高分辨率长图海报（纯色极简风格）。
    - [ ] 调用系统原生分享面板 (`Intent.ACTION_SEND`)，支持一键将年度战报保存到相册或分享至社交圈。

---

## 需求池 (Backlog - 探索性功能备选)

- [ ] **账单多维标签管理与项目制复盘 (Multi-Tag System)**：支持单笔账单附加多标签（如 `#出差`、`#装修`、`#旅行`、`#宠物`），支持按 Tag 聚合汇总特定项目总开销。
- [ ] **大额支出分期付款管理 (Installment Payment Tracker)**：支持大件消费 3/6/12/24 期分期还款记录，自动按月扣减入账并跟踪剩余待还本息。
- [ ] **小票与发票凭证照片附件存根 (Receipt Photo Attachment)**：单笔记账支持拍照或从相册添加小票附件，本地沙盒加密缩略存储。
- [ ] **分类预算跨月结转机制 (Budget Rollover)**：支持将上月分类未用完的结余自动滚入下月可用额度。
- [ ] **AMOLED 纯黑夜间节能模式 (Pure Black Dark Mode)**：为 OLED 屏幕提供真正的 `#000000` 纯黑背景，结合深色主题达到极致省电。
- [ ] **自定义分类图标扩展包与拖拽排序**：支持用户自定义扩展矢量图标，并在分类管理对话框中支持长按拖拽自定义排列顺序。
- [ ] **多币种实时汇率换算与资产总览 (Multi-Currency Real-time FX)**：支持单笔账单标记原币种与入账汇率，自动折算为基准货币汇总。
- [ ] **架构设计全景可视化面板 (Architecture Visualizer)**：在开发者面板以图形化拓扑展示系统的 MVI 响应式流、Clean Architecture 与模块依赖解耦关系。
- [ ] **Google Drive 增量同步与冲突合并策略**：由目前的全量快照上传演进为版本向量驱动的增量差分合并。
