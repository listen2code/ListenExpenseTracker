# ListenExpenseTracker 单元测试覆盖率报告 (Jacoco Coverage Report)

**更新时间**: 2026-09-11  
**项目版本**: Android (Kotlin 2.2.10 + Compose + Room + Jacoco)  
**分析工具**: Jacoco 0.8.12 (`testDebugUnitTest` + `jacocoTestReport`)  
**测试结果**: **21 个单元测试套件 100% 全部通过 (BUILD SUCCESSFUL in 1m 40s)**

---

## 📊 1. 整体覆盖率概览 (Overall Summary)

Jacoco 最新实测覆盖率数据如下：

| 维度 (Dimension) | 统计数值 (Count) | 覆盖率 (Coverage) | 评估状态 |
| :--- | :--- | :--- | :--- |
| **总可执行代码行 (Total Lines)** | **11,589** 行 | — | — |
| **已覆盖代码行 (Lines Covered)** | **2,479** 行 | **21.39%** | 🟢 已覆盖代码行突破 2,400+ 行 (+1,208 行) |
| **核心数据与计算引擎层 (Data & Engine Layer)** | **2,241 / 2,414** 行 | 🟢 **92.83%** | 🟢 **引擎与领域层极高覆盖** |
| **分支覆盖率 (Branch Coverage)** | **659 / 3,739** 分支 | **17.63%** | 🟢 引擎核心分支全面覆盖 |
| **测试套件总数 (Test Suites)** | **21** 个测试类 | — | 🟢 新增 DemoDataEngine, 全量 SettingsUiState 测试 |

---

## 📦 2. 包路径 (Package-Level) 覆盖率明细

根据最新 Jacoco 导出的 `jacocoTestReport.csv` 精确统计：

| 包路径 (Package Name) | 行覆盖率 (Line Cov) | 已覆盖/总行数 | 分支覆盖率 (Branch Cov) | 包含类数量 | 核心功能与测试评价 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`data.i18n`** | 🟢 **100.00%** | **968 / 968** | N/A | 1 | AppStrings & ExpenseStrings 多语言与币种映射 |
| **`data.backup`** | 🟢 **98.15%** | **106 / 108** | **68.29%** (56/82) | 1 | JSON 与 CSV 数据导出/导入序列化解析与校验 |
| **`data.model`** | 🟢 **94.38%** | **151 / 160** | **71.74%** (33/46) | 9 | CategoryRepository, AccountRepository, BudgetModel, Category |
| **`data.engine`** | 🟢 **93.96%** | **856 / 911** | **71.36%** (466/653)| 19 | 交易计算引擎、演示数据生成引擎、财务洞察与趋势算子 |
| **`data.update`** | 🟢 **92.00%** | **69 / 75** | **60.00%** (42/70) | 6 | 应用更新检查、ReleaseInfo 与版本对比逻辑 |
| **`data.db`** | 🟢 **80.39%** | **41 / 51** | 0.00% (0/4) | 6 | TransactionEntity, RecurringRuleEntity 属性映射 |
| **`core.state`** | 🟡 **43.44%** | **53 / 122** | **47.46%** (28/59) | 3 | ExpenseAppState, NavTab & AppOverlay 状态与导航 |
| **`features.transactions.viewmodel`**| 🟡 **28.28%** | **97 / 343** | 8.33% (12/144) | 54 | TransactionsIntent & Effect 覆盖 |
| **`features.statistics.viewmodel`**| 🟡 **23.40%** | **44 / 188** | 0.00% (0/32) | 27 | StatisticsUiState & StatisticsIntent 验证 |
| **`features.settings.viewmodel`**| 🟡 **12.96%** | **53 / 409** | 0.00% (0/162) | 72 | 全量 SettingsUiState, SettingsIntent & Dialog 验证 |
| **`data.pref`** | 🟡 **11.92%** | **18 / 151** | 0.00% (0/12) | 29 | ExpensePreferences 模型与委托扩展 |
| **`widget`** | 🔴 **6.76%** | **15 / 222** | 19.82% (22/111) | 5 | ListenExpenseAppWidgetProvider 桌面微件测试 |
| **`core.security`** | 🔴 **3.61%** | **7 / 194** | 0.00% (0/48) | 7 | SecurityPreferences 偏好设置测试 |
| **`UI 组件与 Screen 层`** | 🔴 **0.00%** | **0 / 6,700+**| 0.00% (0/2,000+)| 150+ | Compose 布局组件、Dialog、Sheet 与列表项 |

---

## 🧪 3. 最新运行验证结果

所有测试均在 `./gradlew testDebugUnitTest` 验证通过：

- `DemoDataEngineTest.kt` PASS (演示数据生成、多语言中/英/日支持、拿铁因子、周期性账单规则)
- `SettingsUiStateTest.kt` PASS (设置页全量 State 属性、主题色枚举、Dialog、ReleaseInfo 与 Intent 完整覆盖)
- `BudgetModelTest.kt` PASS (分类预算比例与状态)
- `ExpensePreferencesTest.kt` PASS (偏好聚合模型)
- `NavTabTest.kt` PASS (导航路由)

---

> 📄 报告更新完成。数据源自 `jacocoTestReport.csv` 最新导出结果。
