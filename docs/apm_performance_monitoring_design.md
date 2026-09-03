# ListenExpenseTracker - APM 性能监控与可观测性设计规范

本文档规范 `ListenExpenseTracker` 与基础 SDK (`ListenArch` / `ListenUiComponent`) 内置的 **APM (Application Performance Monitoring) 性能监控、日志浮窗与 TraceId 链路追踪体系**。

---

## 1. 核心设计哲学 (Core Philosophy)

1. **零外部强依赖**：APM 核心逻辑下沉于通用 SDK `ListenArch` (`com.listen.arch.apm`)，不强依赖 Firebase 或第三方性能监控平台，保证无网环境与私有化部署下的 100% 可用性。
2. **环形内存日志缓冲区 (In-Memory Ring Buffer)**：维护最多 500 条实时日志，避免内存泄漏，同时支持流式 `StateFlow<List<ApmLogEntry>>` 驱动 UI 实时渲染。
3. **全链路 TraceId 追踪**：为用户发起的每个 MVI `Intent` 分配唯一的短 UUID `traceId`（如 `trace-a1b2c3d4`），贯穿 ViewModel、Room SQLite 数据库与云端同步任务，实现精确到毫秒级的耗时打点。
4. **Crash Safe Mode 崩溃保护**：全局拦截未捕获的 Uncaught Exception，持久化崩溃堆栈至本地 `crash_logs.txt`，防止由于偶发空指针或数据库损坏导致 App 无休止闪退。

---

## 2. 架构分层设计

```mermaid
graph TD
    UI[Compose UI / User Interaction] -- 1. Trigger Intent (attach traceId) --> VM[ViewModel]
    VM -- 2. TraceManager.trace(...) --> ApmLog[ApmLogger (Ring Buffer 500)]
    VM -- 3. Execute DB/Network --> Dao[Room DAO / Cloud Sync]
    Dao -- 4. Log duration & SQL --> ApmLog
    ApmLog -- 5. StateFlow --> Inspector[LogInspectorSheet (UI Component)]
    GlobalCrash[UncaughtExceptionHandler] -- 6. Capture Crash --> CrashHandler[CrashHandler -> crash_logs.txt]
```

---

## 3. APM 日志分频道规范 (Log Channels)

| 频道名称 | 标识 (`ApmLogChannel`) | 采集内容与职责 |
| :--- | :--- | :--- |
| **APP** | `ApmLogChannel.APP` | ViewModel 调度、用户 Intent 触发、页面导航与状态更新 |
| **DB** | `ApmLogChannel.DB` | Room 数据库插入、批量插入、删除、清空与 SQL 执行耗时打点 |
| **SYNC** | `ApmLogChannel.SYNC` | Google Drive / 云端 REST API 同步、本地备份与数据反序列化 |
| **CRASH** | `ApmLogChannel.CRASH` | 全局捕获的未处理异常、崩溃线程与堆栈跟踪 |

---

## 4. TraceId 链路打点代码示例

```kotlin
// ViewModel 或 Repository 层使用示例：
TraceManager.trace(
    channel = ApmLogChannel.DB,
    tag = "RoomDB",
    operationName = "InsertTransaction",
    traceId = traceId
) {
    dao.insertTransaction(entity)
}
```

**日志输出格式样例**：
```text
[15:30:12.108][DB][INFO][RoomDB] [trace-f8a192c3] Start: InsertTransaction
[15:30:12.115][DB][INFO][RoomDB] [trace-f8a192c3] Success: InsertTransaction (7ms)
```

---

## 5. 技术难点与解决方案 (Technical Challenges & Implementation Details)

### 5.1 并发安全的环形缓冲区 (Concurrency-Safe Ring Buffer)
在多线程或协程中并发写入日志时，常规的 `ArrayList` 极易抛出 `ConcurrentModificationException`。
**解决方案**：在 `ApmLogger.kt` 中，日志存储采用 `CopyOnWriteArrayList<ApmLogEntry>`。当缓冲区达到 `MAX_LOG_SIZE`（500 条）时，移除首部元素以维持环形队列的性质。通过每次修改时直接更新底层数组，实现了高并发下的线程安全写入。

### 5.2 响应式流数据驱动与无缝 UI 渲染 (StateFlow Driven UI)
日志的实时渲染需要保证 UI 层始终展示最新的数据而不会产生不必要的重组或阻塞主线程。
**解决方案**：在 `ApmLogger` 中对外暴露 `logsFlow: StateFlow<List<ApmLogEntry>>`。每次缓冲池更新后，通过 `_logsFlow.value = buffer.toList()` 分发不可变的列表切片。UI 层通过 `collectAsState()` 直接观察流的变化。

### 5.3 Inspector 国际化支持 (Internationalization)
`LogInspectorSheet` 支持中英日多语言 (en, ja, zh)，可动态根据用户的应用内语言或系统语言展示对应的文案（如 "APM 性能与日志" / "APM Logs & Observability"），并内置了按频道过滤、关键字搜索的功能，所有这些逻辑均下沉至通用 UI 组件层，保证各宿主 App 的表现一致性。

---

## 6. 日志导出与系统分享

在 `LogInspectorSheet.kt` 中提供 **“导出 / 分享”** 与 **“清空 (Clear)”** 功能，通过 Android 原生 `Intent.ACTION_SEND` 将格式化的纯文本日志一键导出到邮件、微信或剪贴板，极大简化远程排查。
