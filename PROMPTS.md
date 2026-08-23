# ListenExpenseTracker - AI 协作提示词与工程规范

## 1. 你的角色

你是这个仓库的高质量协作型 AI，职责是帮助我完成分析、设计、编码、调试和文档整理。

- 你需要有判断力，而不是被动执行器。
- 当需求清晰、范围可控、风险较低时，优先直接执行并给出结果。
- 当需求含糊、假设可疑、改动成本高、会影响架构边界时，先提出少量高价值澄清问题。
- 如果发现我的判断可能有偏差，请明确指出原因，但不要为了“挑战而挑战”。

---

## 2. 项目定位与架构矩阵

ListenExpenseTracker 是一款**隐私优先、本地优先（Local-First）、无服务器架构（Serverless）**的 Android 原生记账工具应用，属于 Listen 多 App 生态矩阵的一员。

### 模块职责与依赖边界 (Strict Boundary Isolation)
1. **`ListenArch` (架构底座 SDK)**：
   - 提供业务完全无关的底层技术设施：MVI `BaseViewModel` 状态机、APM 内存环形日志、`TraceManager` 链路打点、`CrashHandler` 崩溃防护、通用的 `BaseDataStoreManager`、通用的 Payload `CloudSyncManager`、通用的 `StringsRes` 调度引擎。
   - **严禁包含任何特定业务实体（如账单表、预算字段、记账文案等）**。
2. **`ListenUiComponent` (通用 UI 组件 SDK)**：
   - 提供无业务耦合的纯视觉与交互组件：`DonutChart` / `BarChart` 通用图表、`NumericKeypad` 通用数字键盘（支持自定义 `doneText`）、`SurfaceCard`、`SearchBarInput`（通用占位符）、`SegmentedProgressBar`、`LogInspectorSheet`。
   - **严禁写死任何业务文言或业务领域特定逻辑**。
3. **`ListenExpenseTracker` (业务宿主 App)**：
   - 承载所有的记账业务：`TransactionEntity` / `TransactionDao` / `AppDatabase`、`ExpenseDataStoreManager`、`ExpenseStrings` 业务多语言字典、`TransactionCalculationEngine`、流水/统计/设置 Feature 业务页面。

---

## 3. 架构分层与目录组织规范 (Feature-First)

项目代码必须严格遵循 **Feature-First (按特性划分)** 的一级包结构：

```
app/src/main/java/com/listen/expensetracker/
├── MainActivity.kt
├── auth/                                  # 现代 Google Identity 认证体系
├── data/                                  # 数据层（数据库、备份、偏好、多语言字典、计算引擎与常量）
│   ├── backup/                            # 业务账单 JSON / CSV 导出与导入引擎
│   ├── db/                                # Room 数据库单例、DAO 与 Entity 表结构
│   ├── engine/                            # 纯函数账单与多维统计计算引擎
│   ├── i18n/                              # ExpenseStrings 记账专属多语言字典
│   ├── model/                             # Category, Account 常量与 AppDimens Token
│   └── pref/                              # ExpenseDataStoreManager 偏好持久化
└── features/                              # 核心业务特性层
    ├── transactions/                      # 流水与记账
    │   ├── components/
    │   ├── ui/
    │   └── viewmodel/
    ├── statistics/                        # 多维统计与图表
    │   ├── components/
    │   └── ui/
    └── settings/                          # 偏好、云同步与系统运维
        ├── components/
        └── ui/
```

---

## 4. 单文件行数限制与单一职责规范 (CRITICAL RULE)

为了防止代码臃肿与逻辑腐化，制定以下硬性代码规模阈值：

1. **单文件行数限制**：
   - 单个 Kotlin / UI 文件代码行数**不得超过 200 ~ 250 行**。
   - 当单个文件行数逼近或超过 250 行时，**必须**按单一职责原则，将子区块、复杂卡片、弹窗对话框（Dialog / Sheet）或计算逻辑拆分为独立的组件文件（放入对应 Feature 的 `components/` 目录下）。
2. **单个 Composable 函数行数限制**：
   - 单个 Composable 函数**建议控制在 80 ~ 100 行以内**，复杂布局需分解为子 Composable，提高可读性与可测试性。
3. **ViewModel 与 UI 解耦**：
   - Screen 层只负责收集 State 和转发 Intent，不进行复杂的行级格式化与数据变换（由 CalculationEngine 或 Component 承接）。

---

## 5. 语言与注释规范

- **代码内注释 (In-code Comments)**：必须使用**详尽清晰的英文注释 (Detailed English Comments)**。重点解释 Compose 状态重组边界、MVI Intent 流转、生命周期避让、Insets 处理和异步协同原理，便于学习与规范统一。
- **文档与说明 (Documentation)**：所有 Markdown 说明文档（如 `README.md`、`docs/` 设计稿、`walkthrough.md`、`PROMPTS.md`）一律使用**中文**进行详实阐述。

---

## 6. 国际化与硬编码消灭规范

- **字符串国际化 (No Hardcoded Strings)**：禁止在 Composable UI 中硬编码任何用户可见的中/英/日文字符串。所有展示文本必须通过 `StringsRes.get(key, lang)` / `ExpenseStrings.get(key, lang)` 进行三语收口。
- **数值与尺寸 Token 化 (No Magic Numbers)**：禁止在 UI 中散落硬编码尺寸（如 `8.dp`、`16.sp`）或颜色 Hex（如 `Color(0xFF123456)`）。必须统一使用 `AppDimens` 常量、`MaterialTheme.colorScheme` 或定义好的主题 Token。

---

## 7. 废弃 API 禁用与现代化规范

- **禁止使用已废弃的 Google Auth API**：严禁使用 `com.google.android.gms.auth.api.signin.GoogleSignIn` 和 `GoogleSignInClient`，统一使用现代 Google Identity Services API (`com.google.android.gms.auth.api.identity.Identity`) 或 AndroidX Credential Manager。
- **图标与 Compose API 现代化**：严禁使用带废弃标记的 Material Icons（如 `Icons.Filled.Logout`，统一采用 `Icons.AutoMirrored.Filled.Logout`）。

---

## 8. 通用组件下沉与边界原则 (UIKit First)

- 业务无关的图表、基础按键、卡片容器一律抽取并沉淀至 `ListenArch` 或 `ListenUiComponent` 模块中；
- 沉淀至通用库时，**严禁引入宿主 App 的任何特定业务领域模型、专属数据表或写死文案**。