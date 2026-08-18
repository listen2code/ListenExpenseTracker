# ListenExpenseTracker - 错误收敛与 Result<T> 规范

本文档定义 `ListenExpenseTracker` 与基础架构 `ListenArch` 的 **统一错误收敛契约与 Kotlin 原生 Result<T> 错误模型**。

---

## 1. 错误收敛原则

1. **严禁在 UI / Composable 层抛出未捕获的运行时异常**。
2. **使用 Kotlin 原生 `Result<T>` 统一封装**：零外部三方依赖，结合 Kotlin 标准库的 `runCatching`、`safeCall {}` 与 `Flow.asResult()`。
3. **分层收口**：底层 I/O、SQL 或网络异常在 Data / ViewModel 层被安全捕获并转译为用户友好的提示信息或 MVI `ViewEffect`。

---

## 2. 核心扩展方法实现 ([ResultExtensions.kt](file:///C:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/mvi/ResultExtensions.kt))

```kotlin
// 安全调用并捕获异常转为 Result<T>
inline fun <T> safeCall(block: () -> T): Result<T> {
    return runCatching { block() }
}

// 将 Flow<T> 转换为 Flow<Result<T>>
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        .map { Result.success(it) }
        .catch { emit(Result.failure(it)) }
}
```

---

## 3. UI 交互映射规范

| 错误场景 | 表现行为 | 恢复建议 |
| :--- | :--- | :--- |
| **金额格式无效 / 小于等于 0** | 保持当前弹窗，禁用“完成记账”按键 | 提示用户输入正确金额 |
| **数据库读取异常** | `ApmLogger.db(...)` 记录日志，UI 展示 `EmptyStateView` | 下拉重试或联系客服 |
| **未捕获崩溃异常** | `CrashHandler` 捕获堆栈并写入 `crash_logs.txt` | 重启 App 并通过 APM 面板导出日志 |
