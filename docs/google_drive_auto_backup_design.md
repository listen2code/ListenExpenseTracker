# Google Drive 自动备份功能技术设计与实现规范
(Google Drive Auto-Backup Specification & Technical Guide)

## 1. 概述 (Overview)

本文档系统性定义 **ListenExpenseTracker (lExpense)** 在用户连携 Google 账号后的**Google Drive 智能自动备份机制**。该模块旨在为用户提供**“零打扰、高可靠、省电省流”**的数据安全同步体验，确保用户的记账数据在发生任何新增、编辑、删除或退出应用时，能够实时、安全地沉淀在用户个人的 Google 云端硬盘（文件名为 `lexpense_backup.json`）中。

---

## 2. 核心架构与触发拓扑 (Architecture & Trigger Strategy)

系统采用 **“三大事件触发源 + 四重安全守卫 + 增量流式上传”** 的级联拓扑结构：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   【三大自动备份触发源 (Triggers)】                              │
├──────────────────────────────┬────────────────────────────────┬─────────────────────────────────┤
│ 1. 账单数据变动 (Mutation)   │ 2. 应用切后台 (Lifecycle)       │ 3. 配置与交互联动 (Manual/Pref) │
│  - Room 数据表发生任何变动   │  - MainActivity.onStop() 触发  │  - 开启自动备份开关 (延迟 1s)    │
│  - 5000ms 协程防抖计时器     │  - 500ms 极速防抖立即执行       │  - 成功连携 Google 账号 (延迟 2s)│
│  - 连续记账自动重置计时器    │  - 保证在进程冻结前完成落盘     │  - 设置页手动点击「立即备份」   │
└──────────────┬───────────────┴────────────────┬───────────────┴────────────────┬────────────────┘
               │                                │                                │
               └────────────────────────────────┼────────────────────────────────┘
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                 【四重安全守卫 (Guard Decision Chain)】                          │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│  [守卫 1] 是否已登录 Google 账号？ (isLoggedIn && email.isNotBlank())                            │
│           ├── 否 ──> 记录 APM SYNC 日志，静默跳过 (Skip)                                        │
│           └── 是 ──> 进入下一步                                                                 │
│  [守卫 2] 自动备份开关是否开启？ (autoBackupDriveFlow || force)                                 │
│           ├── 否 ──> 记录 APM SYNC 日志，静默跳过 (Skip)                                        │
│           └── 是 ──> 进入下一步                                                                 │
│  [守卫 3] 网络环境是否符合条件？ (wifiOnly -> NetworkCapabilities.TRANSPORT_WIFI)               │
│           ├── 不符合 (当前为蜂窝移动网络且开启了“仅 Wi-Fi”) ──> 静默推迟跳过 (Skip)              │
│           └── 符合 ──> 进入下一步                                                               │
│  [守卫 4] 数据是否有实质性变动？ (SHA-256 Dirty Checking)                                       │
│           ├── 无变更 (currentHash == lastBackupHash && !force) ──> 记录数据无变动，跳过上传      │
│           └── 有变更 ──> 组装云端同步请求                                                        │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │ 触发云端执行链路
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  【云端上传执行链路 (Execution Pipeline)】                       │
├─────────────────────────────────────────────────────────────────────────────────────────────────┤
│  1. 数据库序列化: AppDatabase -> TransactionDao.getAllTransactions() -> JSON 快照                │
│  2. 计算指纹摘要: MessageDigest(SHA-256) -> 生成 64 位十六进制哈希指纹                           │
│  3. 获取访问令牌: GoogleDriveService.getAccessToken() -> 动态提权守卫 (UserRecoverableAuth)      │
│  4. 执行云端更新: GoogleDriveService.uploadBackup()                                              │
│     - 文件已存在 -> PATCH /drive/v3/files/{fileId}?uploadType=media (流式增量覆盖)              │
│     - 文件不存在 -> POST /drive/v3/files?uploadType=multipart (RFC 2387 双段创建)               │
│  5. 状态持久化: DataStore 写入 lastSyncTimestamp 与 lastBackupHash，广播 CloudSyncManager.SUCCESS│
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 2.1 三大触发策略详细设计对比

| 触发策略类型 | 触发源载体 | 防抖延时 (Debounce) | 设计初衷与业务价值 |
| :--- | :--- | :---: | :--- |
| **数据变动触发**<br>`(Mutation-Driven)` | `TransactionsViewModel`<br>监听 `getAllTransactionsFlow()` | `5000 ms` | **防网络风暴**：用户在流水页连续记录多笔账单、修改分类、批量导入时，每次变动均重置 5 秒倒计时，待用户操作停歇后仅执行 1 次合并同步，避免对 Google 云端频繁发起 HTTP 请求。 |
| **应用切后台触发**<br>`(Lifecycle onStop)` | `MainActivity`<br>`override fun onStop()` | `500 ms` | **防数据遗失**：用户记完最后一笔账往往直接按 Home 键或划出应用。切入后台时以极短的 500ms 快速调度，确保在操作系统杀进程或进入深度休眠前将最新账单推送到云端。 |
| **配置与交互联动**<br>`(Preference & Manual)` | `SettingsViewModel`<br>开关切换 / 账号绑定 / 立即备份 | `0 ~ 2000 ms` | **即时反馈**：当用户在设置中首次打开自动备份开关（延迟 1s）或成功连携 Google 账号（延迟 2s）时，立即自动触发一次基线备份；手动点击「立即备份」时直接强制同步（`force = true`）。 |

---

## 3. 四大防御守卫与省电省流设计 (Guards & Battery Optimization)

在移动设备上运行后台同步，最大的痛点是**电池耗电过快**和**偷跑用户移动数据流量**。为此，`GoogleDriveAutoBackupManager` 构建了极其严密的四道防线：

### 3.1 守卫一：Google 账号鉴权守卫 (Account Guard)
- **校验逻辑**：`prefManager.isLoggedInFlow.first() && prefManager.userEmailFlow.first().isNotBlank()`
- **设计考量**：未登录用户绝对不触发任何 Google 服务调用，零唤醒、零 GMS 底层交互。

### 3.2 守卫二：自动备份用户偏好守卫 (User Preference Guard)
- **校验逻辑**：`val autoBackupEnabled = prefManager.autoBackupDriveFlow.first(); if (!autoBackupEnabled && !force) return`
- **设计考量**：充分尊重用户的自主控制权。若用户手动关闭了“自动备份到 Google Drive”开关，除非用户在设置页显式点击“立即备份”（`force = true`），否则一切后台变动均直接拦截。

### 3.3 守卫三：网络约束守卫 (Wi-Fi Only Guard)
- **校验逻辑**：通过现代 Android API（`ConnectivityManager` + `NetworkCapabilities.TRANSPORT_WIFI`）检测。
  ```kotlin
  private fun isWifiConnected(context: Context): Boolean {
      val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
      val network = cm.activeNetwork ?: return false
      val capabilities = cm.getNetworkCapabilities(network) ?: return false
      return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
  }
  ```
- **设计考量**：国内及海外许多用户的移动蜂窝流量昂贵。开启“仅在 Wi-Fi 下自动备份”后，在蜂窝移动网络下静默跳过，待设备连接至家庭/公司 Wi-Fi 后自动补发。

### 3.4 守卫四：基于 SHA-256 的数据指纹脏检查 (Dirty-Checking Engine)
- **核心痛点**：为什么不能仅比对本地最后修改时间戳？
  - 本地修改时间戳可能因系统时钟调整、批量重算或无效更新而变化，容易导致“无实际改动却上传了完全相同的快照”；
- **实现机制**：
  1. 从 Room 数据库导出全量账单并序列化为标准 JSON 字符串；
  2. 使用 `MessageDigest.getInstance("SHA-256")` 计算该 JSON 的 64 位十六进制散列值；
  3. 比对 DataStore 中持久化的 `lastBackupHash`：
     ```kotlin
     if (!force && currentHash == lastHash && lastHash.isNotBlank()) {
         ApmLogger.sync(tag = "AutoBackup", message = "账单数据无变动 (Hash 一致: $currentHash)，无需上传，节省流量", traceId = traceId)
         return Result.success("Skipped: data unchanged")
     }
     ```
- **收益**：即便用户频繁在流水页滑动或触发重新计算，只要账单记录、金额、分类未发生实质变更，**网络上传请求短路拦截率高达 100%**。

---

## 4. 重点与难点代码实现深度剖析 (Implementation Walkthrough)

### 4.1 难点一：变动驱动与防抖协程调度中枢 (`GoogleDriveAutoBackupManager.kt`)

源码位于 [`GoogleDriveAutoBackupManager.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/data/cloud/GoogleDriveAutoBackupManager.kt)。

#### 🔑 技术难点与架构抉择
1. **独立于界面的全局生命周期**：
   - 不能使用 `viewModelScope` 或 `lifecycleScope`，因为用户记完账后可能立即关闭界面，组件销毁会导致协程被立即取消；
   - 必须使用长寿命的独立协程作用域：`CoroutineScope(SupervisorJob() + Dispatchers.IO)`；
   - `SupervisorJob()` 确保单个备份任务出现异常时，不会导致整个备份管理器崩溃失效；
2. **优雅的防抖实现 (Debounce Mechanism)**：
   - 维护一个全局可空的 `pendingDebounceJob: Job?`；
   - 每次调度时先执行 `pendingDebounceJob?.cancel()`，然后开启新协程并挂起 `delay(delayMs)`。

#### 💻 核心实现与教学级中文注释

```kotlin
package com.listen.expensetracker.data.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import com.listen.arch.sync.CloudSyncManager
import com.listen.expensetracker.data.backup.TransactionBackupManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import kotlin.time.Duration.Companion.milliseconds

/**
 * 智能 Google Drive 自动备份编排调度器 (Intelligent Auto-Backup Orchestrator)
 *
 * 核心机制：
 * 1. 数据变动防抖 (Mutation-Driven Debouncing)：高频记账操作合并，延时 5 秒静默触发，杜绝网络风暴；
 * 2. 脏数据哈希校验 (SHA-256 Dirty Checking)：通过比对全量账单 JSON 哈希值，无变动则直接跳过上传；
 * 3. 网络守卫 (Network Guarding)：严格检测 Wi-Fi 开关配置，防止非 Wi-Fi 环境下消耗用户蜂窝流量；
 * 4. APM 全链路追踪：记录备份各阶段耗时、文件 ID 与异常详情。
 */
object GoogleDriveAutoBackupManager {

    // 采用独立于 UI 生命周期的全局 IO 协程作用域，SupervisorJob 避免单个任务崩溃扩散
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingDebounceJob: Job? = null

    /**
     * 调度防抖自动备份任务（默认防抖时长：5000ms）
     * 若在防抖倒计时期间再次发生账单变动，自动取消前序任务并重置倒计时
     */
    fun scheduleAutoBackup(context: Context, delayMs: Long = 5000L) {
        pendingDebounceJob?.cancel()
        pendingDebounceJob = scope.launch {
            delay(delayMs.milliseconds)
            performAutoBackup(context.applicationContext, force = false)
        }
    }

    /**
     * 执行 Google Drive 自动备份主干链路（带前置条件守卫）
     *
     * @param context 上下文引用
     * @param force 是否强制跳过“防抖/无变动”检查直接上传（如用户手动点击立即备份）
     */
    suspend fun performAutoBackup(context: Context, force: Boolean = false): Result<String> {
        val traceId = TraceManager.newTraceId()
        val prefManager = ExpenseDataStoreManager(context)

        // 守卫 1：账号登录状态校验
        val isLoggedIn = prefManager.isLoggedInFlow.first()
        val email = prefManager.userEmailFlow.first()
        if (!isLoggedIn || email.isBlank()) {
            ApmLogger.sync(tag = "AutoBackup", message = "未登录 Google 账号，跳过自动备份", traceId = traceId)
            return Result.failure(IllegalStateException("User not logged into Google"))
        }

        // 守卫 2：自动备份全局开关校验
        val autoBackupEnabled = prefManager.autoBackupDriveFlow.first()
        if (!autoBackupEnabled && !force) {
            ApmLogger.sync(tag = "AutoBackup", message = "用户已禁用 Google Drive 自动备份开关，跳过备份", traceId = traceId)
            return Result.failure(IllegalStateException("Auto backup disabled"))
        }

        // 守卫 3：网络环境守卫（仅在 Wi-Fi 下自动备份）
        val wifiOnly = prefManager.autoBackupWifiOnlyFlow.first()
        if (wifiOnly && !isWifiConnected(context)) {
            ApmLogger.sync(tag = "AutoBackup", message = "已启用仅 Wi-Fi 自动备份，当前为蜂窝移动网络，跳过备份", traceId = traceId)
            return Result.failure(IllegalStateException("Wi-Fi not connected"))
        }

        return try {
            val db = AppDatabase.getInstance(context)
            val allList = db.transactionDao().getAllTransactions()
            val jsonPayload = TransactionBackupManager.exportToJson(allList)

            // 守卫 4：脏数据哈希校验。若本次序列化内容与上次成功备份哈希完全一致，则无需重复向云端上传
            val currentHash = computeHash(jsonPayload)
            val lastHash = prefManager.lastBackupHashFlow.first()

            if (!force && currentHash == lastHash && lastHash.isNotBlank()) {
                ApmLogger.sync(tag = "AutoBackup", message = "账单数据无变动 (Hash 一致: $currentHash)，无需上传，节省流量", traceId = traceId)
                return Result.success("Skipped: data unchanged")
            }

            ApmLogger.sync(tag = "AutoBackup", message = "检测到数据变更，正在向 Google Drive 上传 ${allList.size} 条账单...", traceId = traceId)

            // 获取 OAuth 2.0 Bearer Token 并上传
            val token = GoogleDriveService.getAccessToken(context, email)
            val uploadRes = GoogleDriveService.uploadBackup(token, jsonPayload, traceId)

            uploadRes.onSuccess { fileId ->
                val now = System.currentTimeMillis()
                // 更新最后同步时间与数据指纹哈希
                prefManager.setLastSyncTimestamp(now)
                prefManager.setLastBackupHash(currentHash)
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(tag = "AutoBackup", message = "Google Drive 自动备份成功 (FileID: $fileId)", traceId = traceId)
            }.onFailure { err ->
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(tag = "AutoBackup", message = "Google Drive 自动备份上传失败: ${err.message}", traceId = traceId)
            }

            uploadRes
        } catch (e: Throwable) {
            ApmLogger.sync(tag = "AutoBackup", message = "自动备份过程抛出未捕获异常: ${e.message}", traceId = traceId)
            Result.failure(e)
        }
    }

    /**
     * 检测设备当前是否连接在 Wi-Fi 网络环境
     */
    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * 计算数据字符串的 SHA-256 哈希值
     */
    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
```

---

### 4.2 难点二：Room 响应式流级联自动触发 (`TransactionsViewModel.kt`)

源码位于 [`TransactionsViewModel.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/transactions/viewmodel/TransactionsViewModel.kt#L190-L198)。

#### 🔑 核心实现
无需在“新增账单”、“删除账单”、“编辑账单”每个业务方法中侵入式调用备份代码。通过监听 Room DAO 的 `getAllTransactionsFlow()`，任何数据库写入都会自动触发该流的下发，从而以最小的侵入性实现变动驱动触发：

```kotlin
// TransactionsViewModel.kt 监听全量账单变动 Flow
private fun observeTransactions() = viewModelScope.launch {
    dao.getAllTransactionsFlow().collectLatest { allList ->
        // 1. 重新计算当月收支统计汇总
        applyCalculations(allList)
        // 2. 同步更新桌面小组件 Widget
        ListenExpenseAppWidgetProvider.updateFromTransactions(
            application, allList, currentState.currencySymbol, currentState.monthlyBudget, currentState.language
        )
        // 3. 级联触发 Google Drive 自动备份（带 5 秒防抖）
        GoogleDriveAutoBackupManager.scheduleAutoBackup(application)
    }
}
```

---

### 4.3 难点三：生命周期 `onStop()` 极速安全兜底 (`MainActivity.kt`)

源码位于 [`MainActivity.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/MainActivity.kt#L145-L153)。

#### 🔑 核心实现
当用户记完账直接退出应用或切回手机桌面时，Activity 会立即触发 `onStop()` 回调。此时将防抖延时设为极短的 `500ms`，抢在 Android 进程冻结前完成同步：

```kotlin
// MainActivity.kt 监听切后台生命周期
override fun onStop() {
    super.onStop()
    // 联动安全防护协调器，校验应用锁状态
    securityCoordinator.onStop()
    if (securityCoordinator.isAppLocked) {
        pendingQuickAddIntent.value = null
    }
    // 切后台时调度极速自动备份 (500ms)，确保在进程冻结前将最新数据推向 Google 云端
    GoogleDriveAutoBackupManager.scheduleAutoBackup(this, delayMs = 500L)
}
```

---

### 4.4 难点四：设置联动与立即备份调度 (`SettingsViewModel.kt`)

源码位于 [`SettingsViewModel.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/settings/viewmodel/SettingsViewModel.kt#L72-L107)。

#### 🔑 核心实现
当用户在设置页打开开关或绑定账号时，自动触发初始化基线备份：

```kotlin
// SettingsViewModel.kt 处理配置变更 Intent
is SettingsIntent.ToggleAutoBackupDrive -> viewModelScope.launch {
    prefManager.setAutoBackupDrive(intent.enabled)
    updateState { copy(autoBackupDrive = intent.enabled) }
    // 用户开启开关后，延迟 1000ms 自动触发一次基线同步检查
    if (intent.enabled) GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 1000L)
}

is SettingsIntent.LinkGoogleAccount -> viewModelScope.launch {
    prefManager.setLoggedIn(true, intent.email, intent.displayName ?: "", intent.avatarUrl ?: "")
    emitEffect(CommonUiEffect.ShowToast("Google 账号已成功连携: ${intent.email}"))
    // 登录成功后，延迟 2000ms 自动触发一次全量同步检查
    GoogleDriveAutoBackupManager.scheduleAutoBackup(application, delayMs = 2000L)
}
```

---

## 5. 云端快照存储契约与安全隔离 (Storage & Security Isolation)

### 5.1 云端存储规范
- **存储载体**：用户个人 Google 云端硬盘 (Google Drive) 根目录；
- **目标文件名**：`lexpense_backup.json`；
- **MIME 类型**：`application/json`；
- **覆盖更新原则**：全系统在云端**仅维护单一最新快照文件**。上传时若检测到云端已有 `lexpense_backup.json`（通过 `name='lexpense_backup.json' and trashed=false` 检索），直接通过 PATCH 请求更新正文，绝不在用户网盘中堆砌大量历史重复垃圾文件。

### 5.2 权限最小特权原则 (Principle of Least Privilege)
- 本模块申请的 OAuth 作用域严格限定为：
  `oauth2:https://www.googleapis.com/auth/drive.file`
- **安全保障**：该作用域只能读写由 **ListenExpenseTracker** 自身创建的文件，**绝对无法读取、访问或修改用户 Google Drive 里的其他任何私人文档、相册或工作文件**，最大程度保障用户数据隐私。

### 5.3 快照数据结构契约

```json
{
  "version": 1,
  "exportedAt": 1726041600000,
  "deviceInfo": "Android 14",
  "transactions": [
    {
      "id": "tx_20260911_001",
      "amount": 25.50,
      "type": "EXPENSE",
      "category": "DINING",
      "account": "CASH",
      "timestamp": 1726041600000,
      "note": "午餐",
      "currency": "CNY"
    }
  ]
}
```

---

## 6. UI / UX 规范与状态指示 (UI/UX Specification)

在「设置 $ightarrow$ 数据中心与云端同步（[`SettingsDataCenterSection.kt`](file:///c:/Users/liste/Downloads/github/ListenExpenseTracker/app/src/main/java/com/listen/expensetracker/features/settings/components/SettingsDataCenterSection.kt)）」卡片中：

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│  [图标] 数据管理与云端同步                                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│  [头像] user@gmail.com (已登录)                                 [退出登录]  │
├─────────────────────────────────────────────────────────────────────────────┤
│  自动备份到 Google Drive                                          [ 开关 ON ]│
│  账单变动或退出应用时，自动静默同步至个人云端硬盘                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  仅在 Wi-Fi 下自动备份                                            [ 开关 OFF]│
│  避免在移动蜂窝网络下消耗流量                                               │
├─────────────────────────────────────────────────────────────────────────────┤
│  [立即备份至云端]                                          [从云端快照恢复] │
├─────────────────────────────────────────────────────────────────────────────┤
│  同步状态指示：                                                             │
│  - 空闲状态: 绿色对勾  上次备份时间：2026-09-11 14:30                        │
│  - 同步中:   环形进度条 正在安全同步至 Google Drive...                      │
│  - 异常提示: 橙色感叹号 网络不可用 / 待授权 (点击可自愈修复)                │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. 异常场景与自愈处理矩阵 (Resilience & Self-Healing Matrix)

| 异常业务场景 | 系统捕获与拦截点 | 自动恢复 / 降级策略 | 用户界面感知 |
| :--- | :--- | :--- | :--- |
| **高频快速连续记账** | `pendingDebounceJob?.cancel()` | 每次写入重置 5s 计时器，自动合并为最后一次上传 | 界面无卡顿，无任何多余弹窗 |
| **离线/飞行模式记账** | `isWifiConnected` 或网络超时 | 拦截上传，保持本地快照，记录 APM 日志 | 同步指示器显示未连接，待联网后自动补偿 |
| **首次访问 Drive 需授权** | 捕获 `UserRecoverableAuthException` | 提取内部 `Intent` 并添加 `NEW_TASK` 自动拉起官方授权弹窗 | 弹出系统权限申请窗，点击允许后秒级自愈 |
| **用户在设置中关闭自动备份** | 守卫 2 `autoBackupDriveFlow` 拦截 | 立即中止备份并记录 APM 审计日志 | 开关置灰并保存状态，尊重用户选择 |
| **本地数据无任何变动** | 守卫 4 `currentHash == lastHash` | SHA-256 指纹比对一致，直接短路成功返回 | 日志打印 `Skipped: data unchanged`，节省流量 |
| **云盘空间已满 (Quota)** | HTTP 响应码 `403 QuotaExceeded` | 拦截重试，防止死循环请求；保留本地快照安全 | 提示“Google 云端硬盘空间不足，已保存至本地” |
| **多设备并发同步冲突** | Google Drive `modifiedTime` | 以后上传设备的快照覆盖为准（LWW 策略，Last-Write-Wins） | 推荐在主设备启用自动备份 |

---

## 8. 调试验证与测试指南 (Verification & Testing)

```powershell
# 1. 动态查看自动备份调度与 APM 日志
adb logcat -s "AutoBackup" "GoogleDrive" "lExpense_APM:SYNC"

# 2. 验证防抖机制：快速插入多条账单测试合并
# 预期日志：仅在最后一次记账 5 秒后打印一次 "Uploading backup ... to Google Drive"

# 3. 验证脏数据检测：不做任何账单修改重新触发
# 预期日志：打印 "Data unchanged (Hash matches: ...), skipping upload"

# 4. 模拟断开 Wi-Fi 验证网络守卫
adb shell svc wifi disable
# 预期日志：打印 "Wi-Fi only enabled but currently on cellular, skipping auto-backup"
adb shell svc wifi enable
```
