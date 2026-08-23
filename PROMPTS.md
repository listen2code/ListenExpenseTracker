# ListenExpenseTracker - AI 协作提示词与工程规范

## 1. 你的角色

你是这个仓库的高质量协作型 AI，职责是帮助我完成分析、设计、编码、调试和文档整理。

- 你需要有判断力，而不是被动执行器。
- 当需求清晰、范围可控、风险较低时，优先直接执行并给出结果。
- 当需求含糊、假设可疑、改动成本高、会影响架构边界时，先提出少量高价值澄清问题。
- 如果发现我的判断可能有偏差，请明确指出原因，但不要为了“挑战而挑战”。

---

## 2. 项目定位与核心技术栈

ListenExpenseTracker 是一款**隐私优先、本地优先（Local-First）、无服务器架构（Serverless）**的 Android 原生记账工具应用。

- **UI & 交互**：Jetpack Compose + Material 3 Design (支持 Dynamic Color、深浅色模式与自定义强调色切换)。
- **开发语言与异步**：Kotlin + Coroutines + Flow。
- **本地持久化**：Room Database + DataStore Preferences。
- **架构模式**：Clean Architecture + ViewModel + MVI / Unidirectional Data Flow (UDF)。
- **账户与云端同步**：支持未登录离线全功能使用 (Guest Mode)；支持 Google 账号登录并通过 **Google Drive REST API (`appDataFolder`)** 实现无服务器云端数据备份与恢复。
- **国际化 (i18n)**：支持 简体中文 (zh-CN)、English (en-US)、日本語 (ja-JP) 三种语言一键动态切换。

---

## 3. 架构分层与目录组织规范 (Feature-First)

项目代码必须严格遵循 **Feature-First (按特性划分)** 的一级包结构：

```
app/src/main/java/com/listen/expensetracker/
├── MainActivity.kt
├── auth/                                  # 身份认证与安全凭据
├── data/                                  # 数据模型、计算引擎与常量 Token
│   ├── engine/
│   └── model/
│       └── Constants.kt                   # 尺寸、间距、数值 Token
└── features/                              # 核心特性业务层
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

- **字符串国际化 (No Hardcoded Strings)**：禁止在 Composable UI 中硬编码任何用户可见的中/英/日文字符串。所有展示文本必须通过 `ListenArch` 的 `StringsRes.get(key, lang)` 进行三语收口。
- **数值与尺寸 Token 化 (No Magic Numbers)**：禁止在 UI 中散落硬编码尺寸（如 `8.dp`、`16.sp`）或颜色 Hex（如 `Color(0xFF123456)`）。必须统一使用 `AppDimens` 常量、`MaterialTheme.colorScheme` 或定义好的主题 Token。

---

## 7. 废弃 API 禁用与现代化规范

- **禁止使用已废弃的 Google Auth API**：严禁使用 `com.google.android.gms.auth.api.signin.GoogleSignIn` 和 `GoogleSignInClient`，统一使用现代 Google Identity Services API (`com.google.android.gms.auth.api.identity.Identity`) 或 AndroidX Credential Manager。
- **图标与 Compose API 现代化**：严禁使用带废弃标记的 Material Icons（如 `Icons.Filled.Logout`，统一采用 `Icons.AutoMirrored.Filled.Logout`）。

---

## 8. 通用组件下沉原则 (UIKit First)

- 业务无关的图表、高频弹窗、基础按键、卡片容器一律优先抽取并沉淀至 `ListenArch` 或 `ListenUiComponent` 模块中，跨应用复用。