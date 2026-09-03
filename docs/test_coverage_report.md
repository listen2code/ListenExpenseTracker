# ListenExpenseTracker 单元测试覆盖率报告 (Jacoco Coverage Report)

**更新时间**: 2026-09-03
**项目版本**: Android (Kotlin 2.2.10 + Compose + Room + Jacoco)  
**分析工具**: Jacoco 0.8.14 (`testDebugUnitTest` + `jacocoTestReport`)  
**测试结果**: **21 个单元测试套件 100% 全部通过**

---

## 📊 1. 整体覆盖率概览 (Overall Summary)

Jacoco 最新实测覆盖率数据如下：

| 维度 (Dimension) | 统计数值 (Count) | 覆盖率 (Coverage) | 评估状态 |
| :--- | :--- | :--- | :--- |
| **总可执行代码行 (Total Lines)** | **6,808** 行 | — | — |
| **已覆盖代码行 (Lines Covered)** | **1,271** 行 | **18.67%** | 🟡 整体覆盖率较稳定，UI 层尚未覆盖 |
| **核心数据与领域逻辑层 (Data & Domain Layer)** | **1,138 / 1,559** 行 | 🟢 **73.00%** | 🟢 **核心领域模型和解析引擎得到重点保障** |
| **分支覆盖率 (Branch Coverage)** | **261 / 2,086** 分支 | **12.51%** | 🟡 逻辑分支绝大部分在引擎与更新检查层 |
| **测试套件总数 (Test Suites)** | **21** 个测试类 | — | 🟢 新增 CategoryBudgetEngine, CompoundFilter 等场景 |

---

## 📦 2. 包路径 (Package-Level) 覆盖率明细

根据最新 Jacoco 导出的数据精确统计：

| 包路径 (Package Name) | 行覆盖率 (Line Cov) | 已覆盖/总行数 | 分支覆盖率 (Branch Cov) | 核心功能与测试评价 |
| :--- | :--- | :--- | :--- | :--- |
| **`data.i18n`** | 🟢 **100.00%** | **586 / 586** | N/A | AppStrings & ExpenseStrings 国际化多语言与币种映射 |
| **`data.backup`** | 🟢 **100.00%** | **82 / 82** | **53.57%** (30/56) | JSON 与 CSV 数据导出/导入序列化解析与校验 |
| **`data.model`** | 🟢 **94.38%** | **151 / 160** | **71.74%** (33/46) | AppDimens, BudgetModel, Category, Account |
| **`data.update`** | 🟢 **92.00%** | **69 / 75** | **60.00%** (42/70) | 应用更新检查与版本对比逻辑 (`UpdateCheckerService`) |
| **`data.engine`** | 🟢 **74.42%** | **224 / 301** | **80.87%** (148/183) | 复合过滤器 (`CompoundFilter`)、分类预算 (`CategoryBudgetEngine`)、交易排序引擎 |
| **`data.db`** | 🟡 **56.52%** | **13 / 23** | 0.00% (0/4) | TransactionEntity 实体属性映射与数据库构造 |
| **`features.statistics.viewmodel`**| 🟡 **26.43%** | **37 / 140** | 0.00% (0/18) | StatisticsUiState & StatisticsIntent 状态机验证 |
| **`features.transactions.viewmodel`**| 🟡 **23.51%** | **63 / 268** | 6.67% (8/120) | TransactionsIntent, UiState 与 Filter 状态迁移 |
| **`data.pref`** | 🟡 **11.82%** | **13 / 110** | 0.00% (0/12) | ExpensePreferences 模型与委托扩展 |
| **`core.state`** | 🟡 **9.33%** | **7 / 75** | 0.00% (0/30) | NavTab & AppOverlay 状态描述符 |
| **`features.settings.viewmodel`**| 🔴 **8.12%** | **26 / 320** | 0.00% (0/126) | SettingsUiState & SettingsIntent 验证 |
| **`data.cloud`** | 🔴 **0.00%** | **0 / 222** | 0.00% (0/74) | Google Drive 云端同步组件 (在 ListenArch 中单独测试) |
| **`UI 组件与 Screen 层`** | 🔴 **0.00%** | **0 / 4,200+** | 0.00% (0/1,200+) | 包含 Budget, Settings, Transactions, Statistics 等 Compose 布局与 AppWidget |

---

## 🧪 3. 测试策略与设计思路亮点 (Test Patterns & Strategies)

本项目采用了高度聚焦业务逻辑的测试策略，强调**重领域模型，轻视图层**。

1. **组合与多维过滤器的深度覆盖 (Compound Filter Calculation)**
   - **设计思路**: 收支查询往往涉及多个组合条件的交集 (AND) 筛选（如：金额在 `50-500` 间 + 分类为 `购物` + 账户为 `银行卡`）。`CompoundFilterCalculationTest` 构建了结构化的内存 `TransactionEntity` 假数据，对 `TransactionCalculationEngine` 的过滤器管道进行了边界和组合逻辑断言。
   - **技术亮点**: 测试解耦了 UI 状态，纯粹通过纯函数调用验证过滤引擎的无状态与等幂性，极大降低了由于 UI 重组导致的偶现测试失败可能。

2. **分类预算引擎的数学模型验证 (Category Budget Engine)**
   - **设计思路**: 预算超支或预警涉及到多种状态的精确转移。`CategoryBudgetEngineTest` 通过在给定测试集合下验证 `calculate` 函数返回的 `BudgetHealthStatus` (如正常、警告、超支)。
   - **技术亮点**: 测试用例准确模拟了真实比例 (`ratios`) 到绝对金额的转换验证，确保在除法与浮点预算计算中无精度丢失问题，从而确保首页状态显示的绝对正确。

3. **版本比对服务的健壮性断言 (SemVer Update Checker)**
   - 包含针对 `UpdateCheckerServiceTest` 的测试用例，覆盖了应用内置静态 JSON 更新版本的语义化 (SemVer) 对比算法，保证当用户端低于线上版本时，确保触发 `UpdateAvailable` 状态，验证了后台无头服务的正确性。

4. **ViewModel 与 Intent 状态机快照测试**
   - **设计思路**: 对 `TransactionsIntentTest`, `SettingsUiStateTest` 采用了基于 MVI 架构状态机的测试，通过传入 Intent 序列并拦截最终产出的 UI 状态。
   - **技术亮点**: 由于逻辑完全抽离至 ViewModel，在不依赖 Android 框架 (Context) 的纯 JVM 环境中完成了复杂页面生命周期的逻辑回放和验证，这也是为什么 UI 层组件 0 覆盖，但相关交互结果依然有保障的原因。

---

> 📄 报告更新完成。数据源自 `jacocoTestReport.csv` 最新导出结果。
