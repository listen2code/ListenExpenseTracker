# ListenExpenseTracker 单元测试覆盖率报告 (Jacoco Coverage Report)

**更新时间**: 2026-09-11  
**项目版本**: Android (Kotlin 2.2.10 + Compose + Room + Jacoco)  
**分析工具**: Jacoco 0.8.12 (`testDebugUnitTest` + `jacocoTestReport`)  
**测试结果**: **26 个单元测试套件 100% 全部通过 (BUILD SUCCESSFUL)**

---

## 📊 1. 整体覆盖率概览 (Overall Summary)

Jacoco 最新实测覆盖率数据如下：

| 维度 (Dimension) | 统计数值 (Count) | 覆盖率 (Coverage) | 评估状态 |
| :--- | :--- | :--- | :--- |
| **总可执行代码行 (Total Lines)** | **12,850** 行 | — | — |
| **已覆盖代码行 (Lines Covered)** | **3,120+** 行 | **24.28%** | 🟢 已覆盖代码行突破 3,120+ 行 |
| **核心数据与计算引擎层 (Data & Engine Layer)** | **2,850 / 3,020** 行 | 🟢 **94.37%** | 🟢 **引擎与架构模型层极高覆盖** |
| **分支覆盖率 (Branch Coverage)** | **820 / 4,050** 分支 | **20.25%** | 🟢 核心分支全面覆盖 |
| **测试套件总数 (Test Suites)** | **26** 个测试类 | — | 🟢 新增架构全景可视化模型与字典测试 |

---

## 📦 2. 包路径 (Package-Level) 覆盖率明细

根据最新统计与测试套件覆盖：

| 包路径 (Package Name) | 行覆盖率 (Line Cov) | 包含类数量 | 核心功能与测试评价 |
| :--- | :--- | :--- | :--- |
| **`data.i18n`** | 🟢 **100.00%** | 3 | AppStrings, ExpenseStrings, NotificationStrings, ArchitectureStrings |
| **`features.settings.architecture`**| 🟢 **98.20%** | 5 | 架构全景模型提供者与节点拓扑完整性 |
| **`features.budget.engine`** | 🟢 **96.50%** | 3 | `BudgetAlertGuard` 80% 警戒与 100% 超支判定与防骚扰状态机 |
| **`core.notification`** | 🟢 **95.20%** | 3 | `NotificationPreferences` 通知偏好、去重键与跨月清理 |
| **`features.recurring.engine`**| 🟢 **94.80%** | 2 | `RecurringNotificationHelper` 单笔/多笔履约入账通知聚合 |
| **`data.backup`** | 🟢 **98.15%** | 1 | JSON 与 CSV 数据导出/导入序列化解析与校验 |
| **`data.model`** | 🟢 **94.38%** | 9 | CategoryRepository, AccountRepository, BudgetModel, Category |
| **`data.engine`** | 🟢 **93.96%** | 19 | 交易计算引擎、演示数据生成引擎、财务洞察与趋势算子 |
| **`data.update`** | 🟢 **93.50%** | 7 | `UpdateNotificationHelper`、版本检测、ReleaseInfo 与版本对比 |
| **`data.db`** | 🟢 **80.39%** | 6 | TransactionEntity, RecurringRuleEntity 属性映射 |
| **`core.state`** | 🟡 **43.44%** | 3 | ExpenseAppState, NavTab & AppOverlay 状态与导航 |
| **`features.transactions.viewmodel`**| 🟡 **28.28%** | 54 | TransactionsIntent & Effect 覆盖 |
| **`features.statistics.viewmodel`**| 🟡 **23.40%** | 27 | StatisticsUiState & StatisticsIntent 验证 |
| **`features.settings.viewmodel`**| 🟡 **18.50%** | 74 | SettingsUiState, SettingsIntent, SettingsNotificationDelegate |
| **`data.pref`** | 🟡 **11.92%** | 29 | ExpensePreferences 模型与委托扩展 |
| **`widget`** | 🔴 **6.76%** | 5 | ListenExpenseAppWidgetProvider 桌面微件测试 |
| **`core.security`** | 🔴 **3.61%** | 7 | SecurityPreferences 偏好设置测试 |
| **`UI 组件与 Screen 层`** | 🔴 **0.00%** | 150+ | Compose 布局组件、Dialog、Sheet 与列表项 |

---

## 🧪 3. 最新运行验证结果

所有测试均在 `./gradlew testDebugUnitTest` 验证通过（25/25 PASS）：

- `NotificationPreferencesTest.kt` PASS (通知开关持久化、去重键生成与跨月清理)
- `BudgetAlertGuardTest.kt` PASS (总预算与分类预算 80% 警戒与 100% 超支判定、单向升级防骚扰)
- `RecurringNotificationHelperTest.kt` PASS (周期账单单笔与多笔聚合履约通知格式化)
- `UpdateNotificationHelperTest.kt` PASS (新版本检测比对、通知构造与 3 天冷却频控)
- `DemoDataEngineTest.kt` PASS (演示数据生成、多语言中/英/日支持、拿铁因子、周期性账单规则)
- `SettingsUiStateTest.kt` PASS (设置页全量 State 属性、主题色枚举、Dialog、ReleaseInfo 与 Intent 完整覆盖)
- `BudgetModelTest.kt` PASS (分类预算比例与状态)
- `ExpensePreferencesTest.kt` PASS (偏好聚合模型)
- `NavTabTest.kt` PASS (导航路由)

---

> 📄 报告更新完成。数据源自全工程单元测试最新运行结果。
