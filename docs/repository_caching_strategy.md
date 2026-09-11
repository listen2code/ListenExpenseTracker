# ListenExpenseTracker - Repository 缓存与多级数据流控规范
(Repository Caching, Reactive Data Flow & Concurrency Specification)

本文档系统性定义 **ListenExpenseTracker (lExpense)** 及其底层支撑库 **`ListenArch`** 的 **Local-First (本地优先) 多级缓存架构、响应式数据流控、缓存失效协议与云端同步脏数据校验机制**。

---

## 1. Local-First 核心设计哲学 (Local-First Design Philosophy)

在移动端个人财务与记账应用中，用户对**“记账零阻力”与“数据绝对可靠”**有着严苛的要求：
1. **零延迟响应 (Instant Optimistic Visibility)**：
   - 用户的任何记账、编辑或删除操作必须在本地立即可见；
   - UI 层通过 Room 的响应式 `Flow` 与 DataStore `StateFlow` 驱动，绝不在主线程或 UI 渲染前等待任何网络同步或异步落盘响应；
2. **100% 全离线完好可用 (Offline-First Capability)**：
   - App 必须在飞行模式、地下车库、无网或弱网环境下保证 100% 功能完备；
   - 所有的业务数据持久化保存在本地 SQLite 数据库中，本地数据是唯一的权威事实来源（Single Source of Truth, SSOT）；
3. **安全同步降级与异步重试 (Graceful Cloud Sync Degradation)**：
   - Google Drive 云端同步仅作为后台次级自愈任务运行；
   - 网络超时或失败时记录 APM 审计日志并在下一次网络就绪时静默重试，绝不阻塞或阻断任何本地核心业务流程。

---

## 2. 三级缓存架构与数据流拓扑 (Three-Tier Caching Topology)

全系统的数据存取自顶向下构建了 **L1 内存缓存 $ightarrow$ L2 响应式持久化缓存 $ightarrow$ L3 云端快照与脏数据校验层**：

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               【表现层与 ViewModel (UI & Presentation)】                         │
│               TransactionsViewModel / StatisticsViewModel / SettingsViewModel                   │
└───────────────────────────────┬───────────────────────────────────────────┬─────────────────────┘
                                │ 1. 订阅响应式 Flow                         │ 2. 派发 CRUD 意图
                                ▼                                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                         【L1 级缓存：内存瞬时访问层 (In-Memory L1 Cache)】                       │
│  - CategoryRepository: 预编译分类列表 (expenseCategories / incomeCategories)                     │
│  - AccountRepository: 动态自定义账户内存缓存 (customAccounts: MutableList<Account>)             │
│  - FinancialInsightEngine: 当月洞察分析卡片瞬态计算缓存                                         │
└───────────────────────────────┬───────────────────────────────────────────┬─────────────────────┘
                                │ 内存未命中或持久化驱动                     │ 异步写入与序列化
                                ▼                                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                     【L2 级缓存：响应式磁盘持久化层 (Persistent L2 Cache)】                      │
│  - Room Database (AppDatabase): TransactionDao                                                  │
│    * InvalidationTracker 自动追踪表变动，零轮询主动推送 Flow<List<TransactionEntity>>           │
│  - Preferences DataStore (ExpenseDataStoreManager):                                             │
│    * stateIn(WhileSubscribed(5000)) 转换共享热流，消除重复磁盘 I/O 读取                          │
└───────────────────────────────┬───────────────────────────────────────────┬─────────────────────┘
                                │ 触发后台同步检测                           │ 发生数据变动
                                ▼                                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                 【L3 级缓存：云端同步与指纹脏检查层 (Remote L3 & Dirty-Checking)】              │
│  - GoogleDriveAutoBackupManager: 5000ms 协程防抖调度器                                           │
│  - SHA-256 Checksum Fingerprint: 比对 currentHash 与 lastBackupHash (无变动静默跳过)            │
│  - Google Drive REST API v3: 流式增量覆盖 (PATCH) / 多段创建 (POST)                              │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. 各层缓存策略与并发控制实现 (Caching Strategy & Implementation)

### 3.1 L1 内存缓存层 (In-Memory L1 Cache)

#### 🔑 设计思路
- **高频读、低频写**的静态/半静态数据（如记账分类、资产账户类型）无需每次渲染均穿透至 SQLite 或 DataStore；
- `CategoryRepository` 采用 `List<Category>` 常驻内存，支持按 ID 纳秒级字典索引；
- `AccountRepository` 采用内存列表 `customAccounts` 作为一级缓存，所有增删改直接在内存中完成并瞬时通知观察者，随后异步将 JSON 字符串刷入 DataStore。

#### 💡 核心代码解读与逐行注释
```kotlin
package com.listen.expensetracker.data.model

import com.listen.arch.apm.ApmLogger
import org.json.JSONArray
import org.json.JSONObject

/**
 * 资产账户数据仓库与 L1 内存缓存管理器 (AccountRepository)。
 */
object AccountRepository {
    private const val TAG = "AccountRepository"

    // 1. 系统内置账户（静态不可变，内存常驻）
    val defaultAccounts = listOf(
        Account(id = "acc_cash", nameKey = "account_cash", isCustom = false),
        Account(id = "acc_card", nameKey = "account_card", isCustom = false),
        Account(id = "acc_credit", nameKey = "account_credit", isCustom = false)
    )

    // 2. L1 内存缓存：用户自定义账户列表
    // 使用 MutableList 在内存中维护最新状态，读取时实现绝对零 I/O 延迟
    private val customAccounts = mutableListOf<Account>()

    /**
     * 读取所有可用账户：合并系统内置账户与 L1 缓存中的自定义账户
     */
    fun getAllAccounts(): List<Account> {
        return defaultAccounts + customAccounts.toList()
    }

    /**
     * 更新 L1 内存缓存（从 DataStore 反序列化注入）
     */
    fun loadFromSerialized(jsonString: String) {
        customAccounts.clear()
        if (jsonString.isBlank()) return
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                customAccounts.add(
                    Account(
                        id = obj.getString("id"),
                        nameKey = obj.getString("name"),
                        isCustom = true
                    )
                )
            }
            ApmLogger.i(TAG, "L1 cache populated with ${customAccounts.size} custom accounts")
        } catch (e: Exception) {
            ApmLogger.e(TAG, "Failed to deserialize accounts into L1 cache: ${e.message}")
        }
    }

    /**
     * 将 L1 内存缓存序列化为持久化 JSON 字符串，供写入 L2 DataStore
     */
    fun serializeCustomAccounts(): String {
        val array = JSONArray()
        customAccounts.forEach { acc ->
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.nameKey)
            }
            array.put(obj)
        }
        return array.toString()
    }
}
```

---

### 3.2 L2 响应式持久化缓存层 (Room SQLite & DataStore)

#### 🔑 设计思路
1. **Room InvalidationTracker 自动失效机制**：
   - 本地 Room 数据库是交易流水的大盘真实事实来源（SSOT）；
   - `TransactionDao.getAllTransactionsFlow()` 返回的 `Flow` 会在 SQLite 数据表发生任何 `INSERT`、`UPDATE`、`DELETE` 事务提交时，由底层表变动监视器（`InvalidationTracker`）自动发出变更信号，触发重新查询并推送到下游 ViewModel；
   - 彻底摆脱了传统架构中手动调用 `refresh()` 的易错逻辑。
2. **DataStore 冷流转热流优化 (SharingStarted.WhileSubscribed)**：
   - 将底层基于磁盘读取的 Preferences DataStore `Flow` 通过 `stateIn` 转换为热流，设置 5000ms 宽限超时，在多个页面订阅时共享单份数据流，消除磁盘震荡。

#### 💡 核心代码解读与逐行注释
```kotlin
package com.listen.expensetracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 响应式交易流水数据访问接口 (TransactionDao)。
 * Room 框架会在底层为带有 Flow 返回值的方法自动绑定 InvalidationTracker。
 */
@Dao
interface TransactionDao {

    /**
     * 响应式监听全量账单数据流：
     * 任意线程向交易表插入/修改/删除数据时，InvalidationTracker 自动捕获并在 Dispatchers.IO 重新执行查询。
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactionsFlow(): Flow<List<TransactionEntity>>

    /**
     * 一次性快照挂起查询：供云备份、数据导出与单测在不建立持久监听的前提下读取。
     */
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    suspend fun getAllTransactions(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
```

---

### 3.3 L3 云端同步与 SHA-256 脏数据校验 (Remote L3 & Dirty Checking)

#### 🔑 设计思路
- **痛点**：若每次本地数据微小变动或切后台都直接发起 HTTP 请求上传 Google Drive，会导致流量激增、手机发热并频繁触发 Google Drive API 频控配额（Rate Limit 429）；
- **解决方案**：
  1. **5000ms 协程防抖 (Debounce Timer)**：连续记账时不断重置计时器，待用户操作停顿 5 秒后才触发同步评估；
  2. **SHA-256 内容摘要对比 (Dirty Checking)**：
     在上传前，将要上传的完整 JSON 字符串计算 SHA-256 哈希指纹，并与上次同步成功的 `lastBackupHash` 字符串进行对比：
     - 若 `currentHash == lastBackupHash`：判定为“数据实质无变化”，记录 APM 日志并静默跳过上传；
     - 若 `currentHash != lastBackupHash`：判定为“脏数据变动”，执行云端更新并刷新 `lastBackupHash`。

#### 💡 核心代码解读与逐行注释
```kotlin
package com.listen.expensetracker.data.cloud

import com.listen.arch.apm.ApmLogger
import java.security.MessageDigest

object BackupChecksumCalculator {

    /**
     * 计算备份文本内容的 SHA-256 十六进制摘要指纹。
     * 用于进行快速、高精度的脏数据检查 (Dirty Checking)。
     */
    fun calculateSha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 校验当前数据是否真正发生过实质性变更
     */
    fun isDirty(currentContent: String, lastBackupHash: String): Boolean {
        if (lastBackupHash.isBlank()) return true // 从未备份过，必须同步
        val currentHash = calculateSha256(currentContent)
        val dirty = currentHash != lastBackupHash
        if (!dirty) {
            ApmLogger.i("BackupChecksum", "Content hash matched ($currentHash), skipping cloud upload")
        }
        return dirty
    }
}
```

---

## 4. 缓存失效与数据一致性协议 (Cache Invalidation & Consistency)

| 触发场景 | L1 内存缓存动作 | L2 本地持久化动作 | L3 云端同步动作 | 一致性保障策略 |
| :--- | :--- | :--- | :--- | :--- |
| **新增/编辑账单** | — | `dao.insertTransaction()` | 触发 5000ms 协程防抖计时器 | 本地 Room 事务提交后，自动触发 `getAllTransactionsFlow()` 驱动 UI 刷新 |
| **删除账单** | — | `dao.deleteTransactionById()` | 触发 5000ms 协程防抖计时器 | 数据库级物理删除，UI 立即响应消失 |
| **新增自定义账户** | `customAccounts.add()` 立即刷新内存 | 异步写入 DataStore JSON | 纳入下次自动备份范围 | 双写保障：L1 优先满足即时渲染，L2 异步持久化防丢失 |
| **从云端恢复备份** | 清空并重新反序列化自定义账户 | `dao.deleteAll()` 然后批量 `dao.insertTransactions()` | 更新 `lastBackupHash` 与 `lastSyncTimestamp` | 事务级原子替换，保证数据不出现跨版本脏数据掺杂 |
| **测试数据一键填充** | — | `dao.insertTransactions(seedDemoData)` | 触发自动备份调度 | 校验当月是否存在真实数据，防止覆盖脏数据 |
| **清空所有数据** | 清空自定义账户列表 | `dao.deleteAll()` + DataStore 重置 | 触发云端空数据同步（可选） | 破坏性危险操作二次确认，APM 记录高危日志 |

---

## 5. 自动化测试与质量验证 (Testing & Verification)

全系统的缓存管理与数据流控均配备了严密的纯 JVM 单元测试，测试套件位于 `app/src/test/java/com/listen/expensetracker/`：

1. **`AccountRepositoryTest.kt`**：
   - 验证 L1 内存缓存对系统预置账户（现金/银行卡/信用卡）的不可篡改性；
   - 验证自定义账户在 JSON 序列化与反序列化过程中的字段保真性。
2. **`TransactionBackupManagerTest.kt`**：
   - 验证 JSON 全量账单导出、导入的数据格式完整性；
   - 验证损坏 JSON 格式导入时的拦截与自愈机制。
3. **`TransactionCalculationEngineTest.kt`**：
   - 验证从 L2 数据库提取的不可变原始数据列表在纯函数管道中的零副作用计算。
