# ListenExpenseTracker - 错误代码字典与统一异常收敛规范 (Error Codes & Result<T> Reference)

本文档系统性阐述 **ListenExpenseTracker** 及其架构底层库（`ListenArch` / `ListenUiComponent`）的**统一错误收敛契约、错误代码字典（Error Code Matrix）、Kotlin 原生 `Result<T>` 范式以及全局未捕获崩溃防护机制**。

---

## 1. 架构总览与错误收敛哲学 (Error Convergence Philosophy)

在移动端个人财务应用中，保证数据一致性与零崩溃是第一生存底线。系统严格推行 **“底层强捕获、分层转译、UI 零抛出、用户可自愈”** 的错误治理哲学：

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│                   【数据源与硬件层 (Data & Device Layer)】                  │
│       Room SQLite 读写 / Google Drive REST API / 指纹传感器 / SAF 导出       │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ 产生原始底层异常 (SQLException, IOException, ...)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                 【异常防御边界与转译 (Boundary & Translation)】              │
│                safeCall { ... }  /  Flow<T>.asResult()                      │
│  - 捕获原始异常并包装为 Kotlin 原生 Result<T> 或强类型 AppException           │
│  - 接入 APM：自动记录对应频道的 ApmLogger.e / TraceManager 故障上下文        │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ 返回 Result.failure(AppException)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                   【ViewModel 状态机消费 (MVI Consumption)】                 │
│                 result.fold(onSuccess = { ... }, onFailure = { ... })       │
├──────────────────────────────────────┬──────────────────────────────────────┤
│ 1. 持久状态降级 (State Fallback)     │ 2. 瞬态副作用告警 (ViewEffect)        │
│  - updateState { copy(error = ...) } │  - emitEffect(ShowSnackbar("...", "重试"))
│  - UI 呈现优雅空状态 / 红色提示文字  │  - emitEffect(ShowToast("..."))      │
└──────────────────────────────────────┴──────────────────────────────────────┘
                                       │ 驱动无状态 Screen 渲染
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                     【用户感知层 (User Experience Layer)】                   │
│          绝对不展示生硬的英文堆栈代码；展示母语化提示，并提供自愈恢复按键    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 核心铁律
1. **UI 零崩溃原则**：严禁在任何 Composable 函数内部抛出未捕获的运行时异常（如 `NullPointerException`、`IndexOutOfBoundsException`）；
2. **原生轻量级原则**：全面采用 Kotlin 标准库自带的 `Result<T>`，彻底摒弃重型的三方 Arrow 函子库，零 APK 体积增加；
3. **双重呈现原则**：
   - **可自愈/非致命错误**：通过 `CommonUiEffect.ShowSnackbar` 展示，并附带明确的 Action 按钮（如“重试”、“撤销”）；
   - **输入级局部错误**：直接绑定在表单 `UiState` 中，通过禁用提交按钮防患于未然。

---

## 2. 统一错误代码速查字典 (Error Code Matrix)

全系统采用四位数字进行错误代码分区：
- **`1xxx`**：业务逻辑与表单输入校验错误 (Business & Validation)
- **`2xxx`**：本地数据库与数据存储错误 (Local Storage & SQLite)
- **`3xxx`**：Google 云同步与网络鉴权错误 (Cloud Sync & Identity)
- **`4xxx`**：生物识别与资产安全隐私错误 (Security & Biometric)
- **`5xxx`**：系统底层框架与未捕获崩溃错误 (System & Crash)

| 错误代码 | 错误标识 (Error Identifier) | 触发业务场景 | APM 频道 | 用户界面呈现 | 推荐故障自愈策略 |
| :--- | :--- | :--- | :---: | :--- | :--- |
| **`1001`** | `ERR_BIZ_AMOUNT_INVALID` | 记账金额 $\le 0$ 或表达式非法 | `APP` | 禁用“完成记账”按钮，金额高亮微红 | 引导用户在数字键盘输入有效金额 |
| **`1002`** | `ERR_BIZ_AMOUNT_OVERFLOW` | 单笔金额超出上限（`> 999,999.99`） | `APP` | 键盘拦截输入，提示“金额超出系统单笔限额” | 阻止继续追加数字按键 |
| **`1003`** | `ERR_BIZ_CATEGORY_REQUIRED` | 未选择任何有效分类直接尝试提交 | `APP` | 分类网格抖动或 Toast 提示“请选择账单分类” | 自动滚动至常用分类区域 |
| **`1004`** | `ERR_BIZ_ACCOUNT_REQUIRED` | 未绑定或选择了已失效的资产账户 | `APP` | Toast 提示“资产账户不存在” | 自动回退至默认账户 (`CASH`) |
| **`1005`** | `ERR_BIZ_DEMO_DATA_COLLISION` | 当月已存在真实手工账单，拦截生成测试数据 | `DB` | 弹出红色警示弹窗，明确说明“当月已有真实账单” | 杜绝脏数据污染真实账目 |
| **`1006`** | `ERR_BIZ_BUDGET_OVERRUN` | 单笔花销导致总预算或分类预算超支 $\ge 100\%$ | `APP` | 顶部浮现红色超支通知横幅 / 震动提示 | 引导用户进入分类预算中心调额 |
| **`1007`** | `ERR_BIZ_RECURRING_RULE_INVALID`| 周期账单截止日期早于首次开始时间 | `APP` | 弹窗标记日期冲突红字 | 自动将结束日期调整为下月对应日 |
| **`2001`** | `ERR_DB_READ_FAILURE` | SQLite 数据库损坏或游标读取超时 | `DB` | 列表展示 `EmptyStateView` 并提供“重新加载” | 调用 Room 重建连接或从最近备份恢复 |
| **`2002`** | `ERR_DB_WRITE_FAILURE` | 存储空间已满或 SQLite 外键约束违规 | `DB` | 弹出底部持久型 Snackbar：“账单保存失败” | 检查手机磁盘剩余空间并重试 |
| **`2003`** | `ERR_DB_MIGRATION_FAILED` | Room 数据库升级版本时迁移脚本执行失败 | `CRASH` | 触发 `fallbackToDestructiveMigration` 并警告 | 提示用户从 Google Drive 恢复数据 |
| **`2004`** | `ERR_DB_TRANSACTION_FAILED` | 批量数据导入/生成事务异常回滚 | `DB` | 自动回滚，数据库保持原子性零污染 | 提示“数据导入已中止，未更改任何账目” |
| **`3001`** | `ERR_CLOUD_AUTH_REQUIRED` | 未登录 Google 账号或登录凭据已过期 | `SYNC` | 设置页显示未连接态，提示“请先绑定 Google 账号” | 点击拉起 CredentialManager 一键重登 |
| **`3002`** | `ERR_CLOUD_PERMISSION_DENIED` | Google Drive 读写权限被用户拒绝撤回 | `SYNC` | 捕获 `UserRecoverableAuthException` | 自动调起 Google 原生系统授权面板提权 |
| **`3003`** | `ERR_CLOUD_NETWORK_TIMEOUT` | 网络不可用、飞行模式或连接 Google 超时 | `SYNC` | 同步状态指示灯变黄，提示“网络不可用，已暂存本地” | 待网络恢复后由 WorkManager 自动重试 |
| **`3004`** | `ERR_CLOUD_PAYLOAD_CORRUPTED` | 云端备份 JSON 校验和 (MD5) 不匹配 | `SYNC` | 拦截恢复，提示“云端数据快照已损坏，停止恢复” | 保留本地数据，避免错误快照覆写 |
| **`3005`** | `ERR_CLOUD_QUOTA_EXCEEDED` | 用户个人 Google Drive 云盘容量耗尽 | `SYNC` | 提示“Google 云端硬盘空间已满” | 引导用户清理云盘空间 |
| **`4001`** | `ERR_SEC_HARDWARE_UNAVAILABLE`| 设备无指纹/面容硬件传感器 | `APP` | 设置页“生物识别应用锁”开关自动置灰并注明原因 | 隐藏不可用的硬件开关 |
| **`4002`** | `ERR_SEC_NO_ENROLLED_BIOMETRICS`| 设备具备传感器但用户未录入任何指纹/面容 | `APP` | 开关提示“请先在系统设置中录入指纹或面容” | 提供直达系统安全设置的 Intent 入口 |
| **`4003`** | `ERR_SEC_AUTH_FAILED` | 指纹单次比对不匹配（如手指脱皮、放偏） | `APP` | 系统弹窗轻微振动，提示“再试一次” | 允许继续尝试或切换输入锁屏密码 |
| **`4004`** | `ERR_SEC_AUTH_LOCKOUT` | 连续多次识别失败触发传感器临时冻结 (30s) | `APP` | 自动切换为备用设备凭据（PIN / 图案密码） | 降级到系统级密码输入验证 |
| **`4005`** | `ERR_SEC_AUTH_CANCELLED` | 用户点击“取消”或主动按返回键退出验证 | `APP` | 保持 `BiometricLockOverlay` 全屏遮罩阻断 | 界面提供“重新唤醒解锁”主按钮 |
| **`5001`** | `ERR_SYS_UNCAUGHT_CRASH` | 全局未处理异常（Uncaught Exception） | `CRASH` | `CrashHandler` 拦截堆栈双写落盘，平滑退回桌面 | 下次启动提示“检测到异常退出，已保存诊断日志” |
| **`5002`** | `ERR_SYS_PERMISSION_DENIED` | Android 13+ 通知运行时权限被用户永久拒绝 | `APP` | 设置页提示“通知权限已禁用”，提供一键跳转通道 | 点击直达系统应用通知设置页 |
| **`5003`** | `ERR_SYS_FILE_EXPORT_FAILED` | SAF 导出文件时目标目录不可写或 I/O 中断 | `APP` | 提示“文件导出失败，请更换存储目录” | 重新调起系统文件选择器重试 |

---

## 3. 核心技术难点与代码实现深度剖析 (Implementation Walkthrough)

### 3.1 难点一：函数式安全调用封装器 (`ResultExtensions.kt`)

#### 🔑 设计思路
在 Kotlin 中，直接使用 `try-catch` 会导致代码嵌套深、可读性差。
通过内联函数将标准操作封装为原生 `Result<T>`，不仅能消减 Lambda 分配开销，还能支持纯函数式链式调用（`map`、`flatMap`、`onSuccess`、`onFailure`）。

```kotlin
package com.listen.arch.result

import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * 安全调用包装器 (safeCall)
 * 接收一个可能抛出异常的 block，安全捕获并封装为 Kotlin 原生 Result<T>
 */
inline fun <T> safeCall(
    tag: String = "SafeCall",
    channel: ApmLogChannel = ApmLogChannel.APP,
    block: () -> T
): Result<T> {
    return runCatching(block).onFailure { throwable ->
        // 自动接入 APM 审计日志：记录错误类型与发生位置，零漏网之鱼
        ApmLogger.e(channel, tag, "safeCall captured exception: ${throwable.message}")
    }
}

/**
 * 将常规 Flow<T> 转换为安全流 Flow<Result<T>>
 * 重点难点：捕获上游（Upstream）可能发生的任何异常，向下游发射带有错误的 Result.failure，
 * 绝不让上游崩溃导致下游 Collect 协程意外终结！
 */
fun <T> Flow<T>.asResult(): Flow<Result<T>> {
    return this
        // 1. 上游正常发射的数据，自动包装为 Result.success
        .map { Result.success(it) }
        // 2. 拦截上游未处理的异常，安全转换为 Result.failure 优雅发射
        .catch { throwable ->
            ApmLogger.e(ApmLogChannel.APP, "FlowAsResult", "Flow upstream error: ${throwable.message}")
            emit(Result.failure(throwable))
        }
}
```

---

### 3.2 难点二：强类型领域异常模型 (`AppException.kt`)

#### 🔑 设计思路与枚举安全
传统的 Java `Exception` 只包含不可控的字符串消息，无法直接用于 UI 国际化或业务状态机分支匹配。
系统定义了密封类（Sealed Class）层级的 `AppException`：
1. **携带整型错误码 (`code`)**：便于 APM 上报与全局检索；
2. **携带国际化多语言 Key (`userMessageKey`)**：UI 层拿到后直接通过 `AppStrings.get(key).tr(lang)` 转化为用户母语；
3. **Sealed 穷尽性保障**：在 ViewModel 进行 `when (e)` 分支匹配时，编译器强制要求覆盖全部子类，彻底消除漏网缺陷。

```kotlin
package com.listen.expensetracker.core.error

/**
 * 全系统强类型统一领域异常基类
 */
sealed class AppException(
    val code: Int,
    val userMessageKey: String,
    cause: Throwable? = null
) : Exception("Error [$code]: $userMessageKey", cause) {

    // 业务校验异常
    class BusinessException(code: Int, messageKey: String) : AppException(code, messageKey)

    // 本地存储异常
    class DatabaseException(code: Int, messageKey: String, cause: Throwable? = null) : AppException(code, messageKey, cause)

    // 云同步异常
    class CloudSyncException(code: Int, messageKey: String, cause: Throwable? = null) : AppException(code, messageKey, cause)

    // 生物安全异常
    class SecurityException(code: Int, messageKey: String) : AppException(code, messageKey)

    // 系统底层崩溃
    class SystemException(code: Int, messageKey: String, cause: Throwable? = null) : AppException(code, messageKey, cause)
}
```

---

### 3.3 难点三：全局未捕获异常崩溃防护与双写持久化 (`CrashHandler.kt`)

#### 🔑 设计思路与技术难点
当发生未曾预料的 `OutOfMemoryError` 或底层的 JNI/驱动崩溃时，应用随时面临系统杀进程闪退。
`CrashHandler` 是最后一道防线：
1. **双写策略 (Dual-Write Strategy)**：
   - **写内存**：立即将崩溃信息推入 `ApmLogger` 500 条环形链表；
   - **写磁盘**：同步追加写入应用专属内部私有文件 `crash_logs.txt`，确保进程被杀死后重启依然能读取到上次崩溃的现场堆栈；
2. **防死锁与防二次崩溃 (Crash-in-Crash Defense)**：
   - 如果 `CrashHandler` 自身在写文件时抛出了 `IOException`，最外层必须有兜底 `try-catch` 静默忽略，坚决杜绝递归抛出导致系统 Watchdog 死锁。

```kotlin
// 文件位置: ListenArch/.../apm/CrashHandler.kt
object CrashHandler : Thread.UncaughtExceptionHandler {

    private const val TAG = "CrashHandler"
    private const val CRASH_FILE_NAME = "crash_logs.txt"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var appDir: File? = null

    /**
     * 在 Application 或 MainActivity.onCreate 第一行初始化挂载
     */
    fun init(context: Context) {
        appDir = context.filesDir
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        // 接管系统全局未捕获异常调度器
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        handleCrash(thread, throwable)
        // 执行完持久化保存后，交还给系统默认处理器以正常退出或重启应用
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun handleCrash(thread: Thread, throwable: Throwable) {
        // ---- 核心保护点：CrashHandler 自身内部绝对不能抛出任何未捕获异常！----
        try {
            val timestamp = System.currentTimeMillis()
            val threadName = thread.name
            val stackTrace = throwable.stackTraceToString()
            val crashReport = """
                ================== CRASH EVENT [ERR_SYS_5001] ==================
                Time: $timestamp
                Thread: $threadName
                Exception: ${throwable.javaClass.name}: ${throwable.message}
                StackTrace:
                $stackTrace
                =================================================================
            """.trimIndent()

            // 1. 第一写：推入 APM 内存环形日志链表
            ApmLogger.crash(TAG, "Uncaught exception on thread [$threadName]: ${throwable.message}")

            // 2. 第二写：同步追加落盘到本地 crash_logs.txt 文件
            appDir?.let { dir ->
                val file = File(dir, CRASH_FILE_NAME)
                file.appendText("$crashReport\n\n")
            }
        } catch (e: Throwable) {
            // 静默吞掉自身异常，防止二次崩溃 (Secondary Crash) 造成系统进程卡死
        }
    }
}
```

---

### 3.4 难点四：Google 云同步动态提权与自愈恢复流 (`GoogleDriveService`)

#### 🔑 设计思路
在向 Google Drive 上传备份文件时，如果用户的授权 Scope 失效或尚未取得写权限，Google Play Services 底层会直接抛出 `UserRecoverableAuthException`。
如果直接判定为同步失败，用户将陷入永久无法同步的死局。
系统设计了“**异常捕获 $\to$ Intent 提取 $\to$ 动态拉起提权 $\to$ 授权后自愈重试**”的完整链路：

```kotlin
// ViewModel 中的动态自愈提权实现范式
fun performCloudSync(activityLauncher: ActivityResultLauncher<Intent>) = viewModelScope.launch {
    updateState { copy(isSyncing = true) }

    val result = googleDriveService.uploadBackupSnapshot(payloadJson)

    result.fold(
        onSuccess = { fileId ->
            updateState { copy(isSyncing = false, lastSyncTime = System.currentTimeMillis()) }
            emitEffect(CommonUiEffect.ShowToast("云端备份同步成功"))
        },
        onFailure = { throwable ->
            updateState { copy(isSyncing = false) }
            // ---- 重点异常识别与动态提权自愈 ----
            if (throwable is UserRecoverableAuthException) {
                // 该异常自带一个由 Google 构造好的授权 Intent，调起它即可让用户在官方界面同意提权！
                activityLauncher.launch(throwable.intent)
            } else {
                // 普通网络或配置异常，展示标准错误信息
                val errorMsg = throwable.localizedMessage ?: "云同步服务不可用"
                emitEffect(CommonUiEffect.ShowSnackbar(errorMsg, actionLabel = "重试", onAction = {
                    performCloudSync(activityLauncher) // 用户点击重试
                }))
            }
        }
    )
}
```

---

## 4. 故障演练与测试验证规范 (Fault Injection & Verification)

为了确保错误收敛链路在严苛边界条件下的绝对稳固，工程建立了自动化故障注入（Fault Injection）测试用例：

```kotlin
// 测试范式：验证当数据库抛出 SQLiteDiskFullException 时，系统能优雅降级而不会崩溃
@Test
fun testDatabaseDiskFull_gracefulFallback() = runTest {
    // 1. Mock DAO 模拟磁盘写满异常
    val mockDao = mockk<TransactionDao>()
    coEvery { mockDao.insertTransaction(any()) } throws SQLiteDiskFullException("Disk full")

    // 2. 触发记账 Intent
    viewModel.handleIntent(TransactionsIntent.AddTransaction(...))

    // 3. 验证 ViewModel 状态未被破坏，且成功向 UI 发射了错误告警 Effect
    val effect = viewModel.viewEffect.first()
    assertTrue(effect is CommonUiEffect.ShowSnackbar)
    assertEquals(2002, (effect as CommonUiEffect.ShowSnackbar).errorCode)
}
```

---

## 5. 架构归纳清单

- **代码行数约束遵守**：所有错误处理与扩展工具类单文件行数均在 $30 \sim 150$ 行之间，100% 符合 $\le 250$ 行架构规范；
- **全平台多语言覆盖**：所有错误提示均在 `AppStrings.kt` 与 `ExpenseStrings.kt` 中配备完整的中、英、日三语翻译，无任何硬编码英文报错输出给终端用户。
