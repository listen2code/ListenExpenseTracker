# ListenExpenseTracker - APM 性能监控与可观测性系统设计与实现规范
(Application Performance Monitoring & Observability Engineering Specification)

本文档系统性阐述 **ListenExpenseTracker (lExpense)** 及其底层架构组件（**`ListenArch`** 与 **`ListenUiComponent`**）内置的 **APM 性能监控、日志浮窗与 TraceId 全链路追踪体系**。

本文档包含业务背景、核心设计哲学、无锁并发环形缓冲区设计、全链路 TraceId 耗时计算、崩溃双写保护机制、UI 双模呈现（模态弹窗与全局可拖拽浮窗）以及带详尽逐行中文注释的重点源码剖析。

---

## 1. 核心设计哲学 (Core Philosophy)

### 1.1 背景与痛点
在移动客户端工程演进中，开发团队往往面临以下痛点：
1. **黑盒运行困境**：当用户或测试人员反馈“偶发同步失败”、“记账响应迟钝”或“偶尔闪退”时，离线环境下无法随时连接电脑打开 Android Studio 查看 Logcat；
2. **三方 SDK 沉重且存在隐私合规风险**：引入 Firebase Performance、Sentry 等重型商业 SDK 会增加数兆包体积，且在无网或内网私有化环境下完全瘫痪；
3. **日志过度膨胀引发 OOM**：粗暴地在内存中保存全部历史日志极易引发内存泄漏和 OutOfMemoryError；
4. **多线程并发读写冲突**：日志产生于主线程、协程池（Dispatchers.IO / Default）及各种异步回调，UI 渲染层同时并发读取，极易抛出 `ConcurrentModificationException`；
5. **日志缺乏关联性**：数十条数据库与网络日志混杂在一起，无法判断某条 SQL 是由哪个用户的点击操作触发的。

### 1.2 六大核心设计原则
```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 【核心设计原则 (Core Principles)】                                │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 1. 零外部强依赖 (Zero External SDK)      : 纯 Kotlin 基础设施下沉至 ListenArch，无网环境 100% 可用│
│ 2. 有界环形内存缓冲区 (Ring Buffer 500)  : CopyOnWriteArrayList 保证写安全，上限 500 条防 OOM     │
│ 3. 响应式快照分发 (Reactive StateFlow)  : buffer.toList() 产生不可变快照，UI 订阅渲染无并发脏读  │
│ 4. 8位短 TraceId 链路关联 (Short UUID)   : 4.3 亿组合兼顾排版宽度，贯穿“UI -> VM -> DB -> Cloud”   │
│ 5. Inline 零对象分配性能剖析 (Zero GC)   : inline 高阶函数消除热点路径下的 Lambda 堆内存分配       │
│ 6. 崩溃双写与责任链兜底 (Crash Safe Mode): 内存与本地文件 crash_logs.txt 双写，静默安全不闭锁系统 │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. 系统分层架构与通信拓扑 (System Architecture & Topology)

为了实现采集、存储、呈现与业务调用的完全解耦，整个 APM 体系划分为四个清晰层级：

### 2.1 架构分层框线图

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              【业务调用层 (Application Business Layer)】                         │
│       ViewModels (Transactions, Statistics, Settings) | AutoBackupManager | GoogleDriveService  │
│               - 调用 TraceManager.trace(...) 执行关键耗时监控                                   │
│               - 调用 ApmLogger.d/i/w/e/db/sync/crash 记录业务关键点                              │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ 写入日志 / 启动追踪打点
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              【底层采集与核心中枢 (ListenArch Core)】                            │
│                                      package com.listen.arch.apm                                │
├───────────────────────────────────────────────┬─────────────────────────────────────────────────┤
│ ApmLogger.kt                                  │ TraceManager.kt                                 │
│ - CopyOnWriteArrayList 环形缓冲区 (最大500条) │ - 生成 8 位短 TraceId (trace-a1b2c3d4)          │
│ - 状态流推送: StateFlow<List<ApmLogEntry>>    │ - inline trace() 耗时测量包装器                 │
│ - 纯文本结构化导出: exportPlainText()         │                                                 │
├───────────────────────────────────────────────┴─────────────────────────────────────────────────┤
│ CrashHandler.kt: 全局未捕获异常双写拦截 (ApmLogger.crash + crash_logs.txt)                     │
│ ApmLogEntry.kt : 不可变日志领域实体 (id, timestamp, level, channel, tag, traceId, stackTrace)   │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ StateFlow 响应式推送不可变快照
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              【双模 UI 呈现层 (Presentation UI Layer)】                         │
├───────────────────────────────────────────────┬─────────────────────────────────────────────────┤
│ 模式 A: 模态底部抽屉 (ListenUiComponent)      │ 模式 B: 全局可拖拽浮动气泡 (ListenExpenseTracker)│
│ LogInspectorSheet.kt                          │ ApmFloatingOverlay.kt                           │
│ - ModalBottomSheet 半展开全览                 │ - 屏幕边缘可自由拖拽的圆形气泡                  │
│ - 四大频道一键 FilterChip 过滤                │ - 12px 防误触判定 (点击展开 / 拖动位移)         │
│ - 关键词搜索与一键纯文本系统分享              │ - ApmFloatingInspectorCard 紧凑卡片排版         │
└───────────────────────────────────────────────┴─────────────────────────────────────────────────┘
```

### 2.2 跨模块职责划分矩阵

| 模块名称 | 所在包路径 | 包含核心文件 | 核心职责与边界 |
| :--- | :--- | :--- | :--- |
| **`ListenArch`** | `com.listen.arch.apm` | `ApmLogEntry.kt`<br/>`ApmLogger.kt`<br/>`TraceManager.kt`<br/>`CrashHandler.kt` | **底层采集与存储中枢**。纯 Kotlin 实现，绝无 Android UI 依赖，负责环形缓冲区管理、TraceId 生成、计时打点与崩溃拦截，供所有业务模块共享。 |
| **`ListenUiComponent`** | `com.listen.uicomponent.apm` | `LogInspectorSheet.kt`<br/>`LogInspectorComponents.kt`<br/>`LogEntryUi.kt` | **基础 UI 设计系统组件**。提供标准的 Material3 `ModalBottomSheet` 风格日志查看器，支持多语言、色彩高亮与日志分享。 |
| **`ListenExpenseTracker`** | `com.listen.expensetracker.core.apm` | `ApmFloatingOverlay.kt`<br/>`ApmFloatingInspectorCard.kt`<br/>`ApmFloatingLogRow.kt`<br/>`ApmInspectorComponents.kt` | **宿主专属全局悬浮窗**。在应用顶层注入悬浮气泡，支持全屏安全区域拖拽、实时未读错误徽标（Badge）与展开式卡片交互。 |

### 2.3 全链路调用与数据流转时序图

```text
[用户触发交互]          [ViewModel]        [TraceManager]         [ApmLogger]         [Room/Cloud]       [UI Inspector]
      │                      │                   │                     │                   │                  │
      │── 1. 派发 Intent ────>│                   │                     │                   │                  │
      │                      │── 2. trace() ────>│                     │                   │                  │
      │                      │                   │── 3. Start 日志 ───>│                   │                  │
      │                      │                   │                     │── 4. 写入 RingBuffer                 │
      │                      │                   │                     │── 5. 推送 StateFlow ────────────────>│ (重组渲染)
      │                      │<── 6. 注入 traceId│                     │                   │                  │
      │                      │                                         │                   │                  │
      │                      │── 7. 执行数据库/云端挂起操作 ──────────────────────────────>│                  │
      │                      │<── 8. 操作返回结果 ─────────────────────────────────────────│                  │
      │                      │                                         │                                      │
      │                      │── 9. 完成闭包 ───>│                     │                                      │
      │                      │                   │── 10. Success日志 ─>│ (记录耗时如 7ms)                     │
      │                      │                   │   (含 durationMs)   │── 11. 写入 RingBuffer                │
      │                      │                   │                     │── 12. 推送 StateFlow ───────────────>│ (刷新耗时)
```

---

## 3. 核心数据模型与契约 (Data Models & Domain Contracts)

### 3.1 四大业务日志频道规范 (`ApmLogChannel`)
为了在繁杂的高频日志流中快速隔离目标信息，APM 明确定义了四大专属频道：

```kotlin
enum class ApmLogChannel {
    /** APP 频道：纯业务逻辑层面的日志，如 ViewModel 调度、用户 Intent、页面生命周期 */
    APP,
    /** DB 频道：数据库操作专属通道，记录 Room SQL、CRUD 事务及耗时统计 */
    DB,
    /** SYNC 频道：云端数据同步、Google Drive 上传/下载、网络请求与鉴权状态 */
    SYNC,
    /** CRASH 频道：未捕获异常（Uncaught Exception）和致命崩溃堆栈的专属避难所 */
    CRASH
}
```

### 3.2 四级日志严重程度与色彩编码 (`ApmLogLevel`)

| 级别 | 枚举值 | 语义说明 | UI 高亮配色方案 | 典型使用场景 |
| :---: | :--- | :--- | :--- | :--- |
| **DEBUG** | `ApmLogLevel.DEBUG` | 开发调试细节 | 沉稳灰 (`#6B7280`) | 局部变量跟踪、临时分支判定 |
| **INFO** | `ApmLogLevel.INFO` | 关键节点正常运行 | 翡翠绿 (`IncomeGreen` / `#10B981`) | 事务成功、同步开始、耗时记录 |
| **WARN** | `ApmLogLevel.WARN` | 潜在风险与非致命降级 | 琥珀黄 (`#F59E0B`) | 网络重试、防抖取消、降级处理 |
| **ERROR** | `ApmLogLevel.ERROR` | 业务失败与异常捕获 | 珊瑚红 (`ExpenseRed` / `#EF4444`) | SQL 失败、网络异常、全局崩溃 |

### 3.3 `ApmLogEntry` 领域实体
源码位于 [`ApmLogEntry.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/ApmLogEntry.kt)。

```kotlin
data class ApmLogEntry(
    // 默认使用 UUID 生成全局唯一标识符，作为 Compose LazyColumn 的 key 做极致高效的 Diff
    val id: String = java.util.UUID.randomUUID().toString(),
    // 毫秒级生成时间戳
    val timestamp: Long = System.currentTimeMillis(),
    // 严重等级 (DEBUG, INFO, WARN, ERROR)
    val level: ApmLogLevel = ApmLogLevel.INFO,
    // 所属业务频道 (APP, DB, SYNC, CRASH)
    val channel: ApmLogChannel = ApmLogChannel.APP,
    // 日志标签，用于快速识模块 (如 "RoomDB", "GoogleDrive", "AutoBackup")
    val tag: String = "APM",
    // 日志正文内容
    val message: String,
    // 可选分布式追踪 ID，用于关联单次操作链路
    val traceId: String? = null,
    // 异常堆栈跟踪信息 (仅在 ERROR 或 CRASH 级别时填充)
    val stackTrace: String? = null
)
```

---

## 4. 核心组件设计与关键算法深度剖析 (Deep-Dive & Annotated Code)

### 4.1 `ApmLogger`：环形缓冲区与响应式日志中枢

#### 🔑 设计挑战与解决方案
- **挑战 1：并发安全与锁争用**：
  若采用普通的 `ArrayList` + `synchronized`，在高频写入和 UI 遍历导出时会发生线程阻塞，甚至抛出 `ConcurrentModificationException`。
  **解决方案**：采用 `CopyOnWriteArrayList`。该容器在读取时不加锁，支持多线程安全并发迭代；写入时通过“写时复制”底层数组，虽然单次写开销稍大，但对于移动端每秒数十次以内的日志量完全无瓶颈，极大提升了 UI 渲染和导出时的读取流畅度。
- **挑战 2：内存无限膨胀 (OOM 隐患)**：
  长时间运行的应用可能累积数十万条日志。
  **解决方案**：实现严格的 **In-Memory Ring Buffer（环形缓冲区）**，设定容量硬上限 `MAX_LOG_SIZE = 500`。每次追加新日志前，若容器已满，主动执行 `buffer.removeAt(0)` 淘汰最旧数据。
- **挑战 3：UI 响应式数据一致性**：
  若直接对外暴露 `CopyOnWriteArrayList` 引用，Compose 无法感知内部条目的变动。
  **解决方案**：每次写入成功后，调用 `buffer.toList()` 生成**全新的不可变快照**赋值给 `_logsFlow: MutableStateFlow`。UI 层通过 `collectAsState()` 订阅即可实现响应式驱动。

#### 💡 关键代码实现与逐行教学注释
源码位于 [`ApmLogger.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/ApmLogger.kt)。

```kotlin
package com.listen.arch.apm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

object ApmLogger {
    // 环形缓冲区上限容量：500 条足以覆盖最近数分钟的密集诊断，同时内存占用低于 1MB
    private const val MAX_LOG_SIZE = 500

    // 写时复制线程安全集合：确保多线程写入与 UI 线程读取互不干扰
    private val buffer = CopyOnWriteArrayList<ApmLogEntry>()

    // 响应式状态流：向 UI 暴露最新的不可变日志列表
    private val _logsFlow = MutableStateFlow<List<ApmLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<ApmLogEntry>> = _logsFlow.asStateFlow()

    /**
     * 底层统一写入入口
     */
    fun log(
        level: ApmLogLevel,
        channel: ApmLogChannel,
        tag: String,
        message: String,
        traceId: String? = null,
        throwable: Throwable? = null
    ) {
        // 1. 若携带异常对象，提取其完整的调用堆栈字符串
        val stackTrace = throwable?.let {
            val sw = StringWriter()
            it.printStackTrace(PrintWriter(sw))
            sw.toString()
        }

        // 2. 构造不可变日志实体
        val entry = ApmLogEntry(
            level = level,
            channel = channel,
            tag = tag,
            message = message,
            traceId = traceId,
            stackTrace = stackTrace
        )

        // 3. 环形缓冲区淘汰逻辑 (FIFO)：超出上限时剔除索引为 0 的最旧条目
        if (buffer.size >= MAX_LOG_SIZE) {
            buffer.removeAt(0)
        }
        buffer.add(entry)

        // 4. 生成不可变快照推送给 StateFlow，驱动 Compose UI 即时重组
        _logsFlow.value = buffer.toList()
    }

    // 快捷工具函数：APP 频道
    fun d(tag: String = "App", message: String, traceId: String? = null) =
        log(ApmLogLevel.DEBUG, ApmLogChannel.APP, tag, message, traceId)

    fun i(tag: String = "App", message: String, traceId: String? = null) =
        log(ApmLogLevel.INFO, ApmLogChannel.APP, tag, message, traceId)

    fun w(tag: String = "App", message: String, traceId: String? = null) =
        log(ApmLogLevel.WARN, ApmLogChannel.APP, tag, message, traceId)

    fun e(tag: String = "App", message: String, throwable: Throwable? = null, traceId: String? = null) =
        log(ApmLogLevel.ERROR, ApmLogChannel.APP, tag, message, traceId, throwable)

    // 快捷工具函数：专属频道
    fun db(tag: String = "RoomDB", message: String, traceId: String? = null) =
        log(ApmLogLevel.INFO, ApmLogChannel.DB, tag, message, traceId)

    fun sync(tag: String = "Sync", message: String, traceId: String? = null) =
        log(ApmLogLevel.INFO, ApmLogChannel.SYNC, tag, message, traceId)

    fun crash(tag: String = "Crash", message: String, throwable: Throwable? = null) =
        log(ApmLogLevel.ERROR, ApmLogChannel.CRASH, tag, message, null, throwable)

    /** 清空内存缓冲区 */
    fun clear() {
        buffer.clear()
        _logsFlow.value = emptyList()
    }

    /**
     * 纯文本结构化导出算法：
     * 格式形如：[HH:mm:ss.SSS][CHANNEL][LEVEL][Tag] [trace-id] message \n stackTrace
     */
    fun exportPlainText(): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return buffer.joinToString("\n") { entry ->
            val time = sdf.format(Date(entry.timestamp))
            val trace = if (entry.traceId != null) " [${entry.traceId}]" else ""
            val stack = if (entry.stackTrace != null) "\n${entry.stackTrace}" else ""
            "[$time][${entry.channel}][${entry.level}][${entry.tag}]$trace ${entry.message}$stack"
        }
    }
}
```

---

### 4.2 `TraceManager`：无额外对象分配的高性能耗时追踪器

#### 🔑 设计亮点
1. **8 位短 TraceId 设计**：
   标准 UUID 长度达 36 字符（如 `9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d`），在移动端屏幕（宽度有限）日志中展示会导致正文严重换行甚至被截断。`TraceManager` 截取前 8 位形成形如 `trace-a1b2c3d4` 的紧凑 ID。16 进制 8 位具备 $16^8 \approx 42.9$ 亿种排列，在单机单会话内碰撞概率趋近于零，完美兼顾了**全局唯一性**与**排版紧凑性**。
2. **`inline` 零开销语法**：
   数据库事务与同步任务属于高频执行路径。将 `trace()` 声明为 `inline`，Kotlin 编译器会在编译期将高阶函数直接内联展开到调用点，**彻底消除了为 Lambda 创建匿名 Function 对象的堆内存分配与 GC 压力**。
3. **闭包异常安全透传**：
   在执行业务代码时若捕获到 `Throwable`，记录失败耗时与异常堆栈后，通过 `throw e` 原样向外重抛，**绝不静默吞掉异常**，确保业务上层的异常控制流（如事务回滚）正常运转。

#### 💡 关键代码实现与逐行教学注释
源码位于 [`TraceManager.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/TraceManager.kt)。

```kotlin
package com.listen.arch.apm

import java.util.UUID

object TraceManager {

    /**
     * 生成 8 位短 TraceId (例如 "trace-f8a192c3")
     */
    fun newTraceId(): String {
        return "trace-" + UUID.randomUUID().toString().take(8)
    }

    /**
     * 核心计时打点高阶函数
     *
     * @param channel 日志归属频道
     * @param tag 模块标签
     * @param operationName 正在追踪的操作名称 (如 "InsertTransaction", "BackupToCloud")
     * @param traceId 外部透传或新生成的追踪 ID
     * @param block 业务挂起或同步代码块，主动向 block 抛出生成的 traceId 供下游继续向下传递
     */
    inline fun <T> trace(
        channel: ApmLogChannel = ApmLogChannel.APP,
        tag: String = "Trace",
        operationName: String,
        traceId: String = newTraceId(),
        block: (traceId: String) -> T
    ): T {
        // 1. 记录开始时间戳 (毫秒级绝对时间，利于对照日志核验)
        val start = System.currentTimeMillis()
        ApmLogger.i(channel = channel, tag = tag, message = "[$traceId] Start: $operationName", traceId = traceId)

        return try {
            // 2. 执行真正的业务逻辑
            val result = block(traceId)
            // 3. 计算耗时并输出成功日志
            val duration = System.currentTimeMillis() - start
            ApmLogger.i(channel = channel, tag = tag, message = "[$traceId] Success: $operationName (${duration}ms)", traceId = traceId)
            result
        } catch (e: Throwable) {
            // 4. 捕获异常：记录耗时、错误消息与堆栈，并重新抛出
            val duration = System.currentTimeMillis() - start
            ApmLogger.e(
                channel = channel,
                tag = tag,
                message = "[$traceId] Failed: $operationName (${duration}ms) - ${e.message}",
                traceId = traceId,
                throwable = e
            )
            throw e
        }
    }
}
```

---

### 4.3 `CrashHandler`：崩溃拦截、持久化双写与责任链模式

#### 🔑 设计思路
1. **防死循环崩溃保护**：
   崩溃处理器内部的代码绝对不允许再抛出任何未经捕获的异常！如果在捕获未处理异常时又抛出异常，会导致 JVM 陷入无限死循环崩溃。因此 `handleCrash()` 的最外层必须使用兜底 `try-catch` 静默兜底。
2. **持久化与内存双写策略 (Dual-Write Strategy)**：
   - **写内存 (`ApmLogger.crash`)**：若应用崩溃后进程尚未被立即销毁，开发人员可在即时浮窗中查看；
   - **写文件 (`crash_logs.txt`)**：以 `appendText` 追加模式写入应用内部私有存储目录（`context.filesDir`）。下次冷启动时，应用可读取该文件获知上一次异常原因。
3. **责任链模式 (Chain of Responsibility)**：
   拦截全局异常前，先取出原有的 `Thread.getDefaultUncaughtExceptionHandler()` 并保存。在自定义持久化完成后，重新调用 `defaultHandler?.uncaughtException(thread, throwable)`，**确保 Android 系统的标准崩溃逻辑（如向系统展示崩溃提示、向 Google Play Vitals 上报）不会丢失**。

#### 💡 关键代码实现与逐行教学注释
源码位于 [`CrashHandler.kt`](file:///c:/Users/liste/Downloads/github/ListenArch/app/src/main/java/com/listen/arch/apm/CrashHandler.kt)。

```kotlin
package com.listen.arch.apm

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {
    // 幂等性标志位：防止重复注册导致责任链形成死循环嵌套
    private var isInitialized = false

    /**
     * 应用程序入口处初始化 (如在 Application.onCreate 或 MainActivity 中调用)
     */
    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true

        // 1. 保存系统默认的异常处理器
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        // 2. 注入我方责任链拦截器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 先执行我方双写持久化
            handleCrash(thread, throwable, context.filesDir)
            // 再交还给系统默认处理器继续派发
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 核心崩溃记录提取与双写
     */
    fun handleCrash(thread: Thread, throwable: Throwable, targetDir: File?): String {
        return try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()

            // 写入 1：内存环形日志 (供当前运行会话中的浮窗即时检索)
            ApmLogger.crash("UncaughtException", "Crash in thread ${thread.name}: ${throwable.message}", throwable)

            // 写入 2：文件系统持久化 (跨进程重启保留)
            if (targetDir != null) {
                val file = File(targetDir, "crash_logs.txt")
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val record = "--- CRASH [${sdf.format(Date())}] Thread: ${thread.name} ---\n$stackTrace\n\n"
                // appendText 追加写入，历史崩溃记录不会被清空覆盖
                file.appendText(record)
            }
            stackTrace
        } catch (e: Exception) {
            e.printStackTrace() // 内部静默失败，严禁向外再抛异常
            ""
        }
    }
}
```

---

### 4.4 `ApmFloatingOverlay`：全局可拖拽浮动气泡

#### 🔑 交互细节与手势识别算法
源码位于 [`ApmFloatingOverlay.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/core/apm/ApmFloatingOverlay.kt)。

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               【悬浮气泡防误触手势状态机】                                       │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│ 用户手指按下 (awaitFirstDown) -> 记录初始坐标 (startX, startY)                                    │
│       │                                                                                         │
│       ├─> 手指移动距离 hypot(dx, dy) >= 12px -> 标记 isDragging = true，动态更新气泡屏幕坐标      │
│       │                                                                                         │
│       └─> 手指抬起 (changedToUp) ───────────────────────────────────────────────────────────────┤
│             ├─> 若未达到 12px 阈值: 判定为轻触点击 (Tap) -> 展开 APM 悬浮大面板 (isExpanded=true)│
│             └─> 若已达到 12px 阈值: 判定为拖动结束 (Drag End) -> 保持气泡位置并消费手势         │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

#### 关键技术点：
1. **屏幕安全区域边界钳位 (Bounds Clamping)**：
   计算 `(constraints.maxWidth - bubbleSize)` 与 `(constraints.maxHeight - bubbleSize)`，使用 `.coerceIn(0f, maxX)` 确保气泡无论如何拖动，绝不会滑出屏幕可视区域或被系统刘海屏/导航栏遮挡；
2. **实时错误气泡徽标 (BadgedBox)**：
   监听日志流中级别为 `ERROR` 的计数，若 `errorCount > 0`，在气泡右上角渲染高亮红色小徽标，即使气泡处于收起状态，也能醒目提示开发者存在潜在异常；
3. **流畅缩放与阻尼过渡 (Spring Transitions)**：
   气泡展开为面板时，采用 `scaleIn + fadeIn` 与 `TransformOrigin(0.5f, 0.5f)` 弹簧动效，视觉体验丝滑。

---

## 5. 日志采集实战与各频道规范 (Logging Best Practices)

### 5.1 `APP` 频道：ViewModel Intent 触发与导航打点
在 MVI ViewModel 的 `handleIntent` 入口处推荐打点，记录用户触发的动作意图：
```kotlin
override fun handleIntent(intent: TransactionsIntent) {
    val traceId = TraceManager.newTraceId()
    ApmLogger.i(
        channel = ApmLogChannel.APP,
        tag = "TransactionsVM",
        message = "收到用户交互意图: ${intent.javaClass.simpleName}",
        traceId = traceId
    )
    when (intent) {
        is TransactionsIntent.Search -> { ... }
        // ...
    }
}
```

### 5.2 `DB` 频道：Room 数据库操作
在对数据库执行读写时，使用 `TraceManager.trace` 包裹，记录操作耗时：
```kotlin
suspend fun insertTransaction(entity: TransactionEntity) {
    TraceManager.trace(
        channel = ApmLogChannel.DB,
        tag = "RoomDB",
        operationName = "InsertTransaction"
    ) { traceId ->
        db.transactionDao().insertTransaction(entity)
        ApmLogger.db("RoomDB", "已成功插入流水 [${entity.categoryName}] 金额: ${entity.amount}", traceId)
    }
}
```

### 5.3 `SYNC` 频道：Google Drive 自动备份
在云端同步引擎中记录网络请求阶段与耗时：
```kotlin
suspend fun performAutoBackup(context: Context): Result<String> {
    val traceId = TraceManager.newTraceId()
    return TraceManager.trace(ApmLogChannel.SYNC, "GoogleDrive", "AutoBackup", traceId) {
        ApmLogger.sync("GoogleDrive", "开始上传备份数据流...", traceId)
        val fileId = GoogleDriveService.uploadBackup(accessToken, jsonPayload, traceId).getOrThrow()
        ApmLogger.sync("GoogleDrive", "备份完成，云端文件 ID: $fileId", traceId)
        fileId
    }
}
```

---

## 6. 技术难点与避坑指南 (Technical Challenges & FAQ)

| # | 技术难点 | 潜在风险 | 解决方案与架构决策 |
|---|---|---|---|
| 1 | **多线程并发写入日志** | 抛出 `ConcurrentModificationException` | 使用 `CopyOnWriteArrayList` 保证写时安全、读不加锁 |
| 2 | **内存溢出 (OOM) 风险** | 长时间记账产生海量日志撑爆堆内存 | 严格限定 500 条环形缓冲区，超出时主动 `removeAt(0)` 淘汰最旧数据 |
| 3 | **UI 收集日志导致界面卡顿** | 频繁重组阻塞主线程渲染 | 写入后生成不可变列表快照（`buffer.toList()`）赋值给 `StateFlow` |
| 4 | **TraceId 在小屏幕下换行截断** | 36 位完整 UUID 占据过多宽度 | 截取前 8 位短 UUID（`trace-a1b2c3d4`），42.9 亿组合兼顾唯一与美观 |
| 5 | **热点路径耗时测量性能衰减** | 每次测量创建闭包对象增加 GC 频次 | `TraceManager.trace()` 采用 `inline` 声明，编译期原地展开零对象分配 |
| 6 | **崩溃处理器自身的稳定性** | 处理异常时若抛出异常会导致死循环闪退 | 外层包裹严格 try-catch，并使用责任链模式把异常还给系统默认处理器 |
| 7 | **悬浮气泡拖拽与点击手势冲突** | 拖动气泡松手时误触发面板展开 | 设定 12px 欧氏距离阈值判定（`hypot(dx, dy) >= 12px` 为拖拽，否则为轻触） |
| 8 | **屏幕旋转时气泡位置飞出可视区** | 横竖屏切换后原坐标超出新屏幕边界 | `BoxWithConstraints` 动态获取当前安全约束，坐标值实时通过 `coerceIn` 钳位 |
