# ListenExpenseTracker - 静态分析与 Lint 代码审查规范

本文档定义 `ListenExpenseTracker` 与基础架构 SDK (`ListenArch` / `ListenUiComponent`) 的 **代码质量红线、静态分析基线与 Custom Lint 规则规范**。

---

## 1. 核心质量红线 (Quality Ground Rules)

1. **零警告编译基线 (Zero-Warning Baseline)**：
   - 严禁忽略 Gradle 编译 Warning、未处理废弃 API 或不安全的类型转换。
   - 所有 Kotlin 代码需保证类型安全与空安全。
2. **单一事实来源 (Single Source of Truth)**：
   - 全局数据以 Room SQLite / DataStore 为准，ViewModel 严禁维护私有不可恢复的状态副本。
3. **架构解耦红线**：
   - `ListenArch` 必须 100% 独立于业务逻辑，严禁引用 `com.listen.listenexpensetracker.*`。
   - `ListenUiComponent` 必须纯 UI 化，严禁引用 Room 数据库或平台具体业务实体。

---

## 2. 静态分析检查规则清单

| 规则 ID | 级别 | 规则描述 | 违规范例 | 修正方案 |
| :--- | :--- | :--- | :--- | :--- |
| **LINT_001** | ERROR | ViewModel 严禁持有 Context/View 引用 | `val ctx: Context` in ViewModel | 使用 AndroidViewModel(application) 或依赖注入 |
| **LINT_002** | ERROR | UI 控件严禁硬编码颜色值 | `Color(0xFFEF4444)` | 使用 `MaterialTheme.colorScheme` 或 `AccentColor` Token |
| **LINT_003** | WARNING | Compose 字符串严禁硬编码 | `Text("确定")` | 通过 `LocaleManager` 统一多语言调度 |
| **LINT_004** | ERROR | 禁止在主线程执行 I/O 或 DB 读写 | `dao.getAll()` blocking call | 使用 `Flow<List<T>>` 或 `withContext(Dispatchers.IO)` |
| **LINT_005** | WARNING | 扩展函数必须包含单元测试 | 新增 Extension 无对应 Test | 在 `src/test/` 下补充单元测试 |
