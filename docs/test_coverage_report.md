# ListenExpenseTracker 单元测试覆盖率报告 (Jacoco Coverage Report)

**更新时间**: 2026-08-27  
**分析工具**: Jacoco 0.8.12 (`testDebugUnitTest` + `jacocoTestReport`)  
**测试结果**: **16 个单元测试套件 100% 全部通过 (BUILD SUCCESSFUL)**

---

## 📊 1. 整体覆盖率概览 (Overall Summary)

Jacoco 实测覆盖率数据：

| 维度 (Dimension) | 统计数值 (Count) | 覆盖率 (Coverage) | 评估状态 |
| :--- | :--- | :--- | :--- |
| **总可执行代码行 (Total Lines)** | **4,168** 行 | — | — |
| **已覆盖代码行 (Lines Covered)** | **870** 行 | **20.87%** | 🟢 逻辑层覆盖主导 |
| **核心数据与领域逻辑层 (Data & Domain Layer)** | **749 / 784** 行 | 🟢 **95.54%** | 🟢 **领域逻辑近乎完美覆盖** |
| **分支覆盖率 (Branch Coverage)** | **121 / 887** 分支 | **13.64%** | 🟡 逻辑分支绝大部分在引擎层 |
| **测试套件总数 (Test Suites)** | **16** 个测试类 | — | 🟢 涵盖引擎、备份、Repo、ViewModel State |

---

## 📦 2. 包路径 (Package-Level) 覆盖率明细

根据 Jacoco 导出的 `jacocoTestReport.csv` 精确统计：

| 包路径 (Package Name) | 行覆盖率 (Line Cov) | 已覆盖/总行数 | 分支覆盖率 (Branch Cov) | 包含类数量 | 核心功能与测试评价 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **`data.i18n`** | 🟢 **100.00%** | **401 / 401** | N/A | 1 | AppStrings & ExpenseStrings 国际化多语言与币种映射 |
| **`data.backup`** | 🟢 **100.00%** | **81 / 81** | **53.57%** (30/56) | 1 | JSON 与 CSV 数据导出/导入序列化解析与校验 |
| **`data.engine`** | 🟢 **97.95%** | **143 / 146** | **81.08%** (60/74) | 2 | 交易计算引擎、多维过滤、排序算法、趋势图与预算算子 |
| **`data.model`** | 🟢 **93.23%** | **124 / 133** | **70.45%** (31/44) | 5 | CategoryRepository, AccountRepository, Category, AppDimens |
| **`data.db`** | 🟡 **56.52%** | **13 / 23** | 0.00% (0/4) | 3 | TransactionEntity 实体属性映射与数据库构造 |
| **`features.statistics.viewmodel`**| 🟡 **22.92%** | **33 / 144** | 0.00% (0/14) | 21 | StatisticsUiState & StatisticsIntent 验证 |
| **`features.transactions.viewmodel`**| 🟡 **20.98%** | **47 / 224** | 0.00% (0/32) | 37 | TransactionsIntent & Effect 覆盖 |
| **`features.settings.viewmodel`**| 🔴 **6.02%** | **21 / 349** | 0.00% (0/128) | 41 | SettingsUiState & SettingsIntent 验证 |
| **`UI 组件与 Screen 层`** | 🔴 **0.00%** | **0 / 2,666** | 0.00% (0/535) | 50+ | Jetpack Compose 视图组件、Dialog、Widget 逻辑 |

---

## 🧪 3. 运行覆盖率测试命令

### 1. 执行单元测试并生成 Jacoco 报告
```bash
./gradlew testDebugUnitTest jacocoTestReport
```
生成报告位置：`app/build/reports/jacoco/jacocoTestReport/html/index.html` 与 `jacocoTestReport.csv`
