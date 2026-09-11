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
     * @param force 是否强制跳过“防抖/无变动”检查直接上传
     */
    suspend fun performAutoBackup(context: Context, force: Boolean = false): Result<String> {
        val traceId = TraceManager.newTraceId()
        val prefManager = ExpenseDataStoreManager(context)

        // 守卫 1：账号登录状态校验
        val isLoggedIn = prefManager.isLoggedInFlow.first()
        val email = prefManager.userEmailFlow.first()
        if (!isLoggedIn || email.isBlank()) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "未登录 Google 账号，跳过自动备份",
                traceId = traceId
            )
            return Result.failure(IllegalStateException("User not logged into Google"))
        }

        // 守卫 2：自动备份全局开关校验
        val autoBackupEnabled = prefManager.autoBackupDriveFlow.first()
        if (!autoBackupEnabled && !force) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "用户已禁用 Google Drive 自动备份开关，跳过备份",
                traceId = traceId
            )
            return Result.failure(IllegalStateException("Auto backup disabled"))
        }

        // 守卫 3：网络环境守卫（仅在 Wi-Fi 下自动备份）
        val wifiOnly = prefManager.autoBackupWifiOnlyFlow.first()
        if (wifiOnly && !isWifiConnected(context)) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "已启用仅 Wi-Fi 自动备份，当前为蜂窝移动网络，跳过备份",
                traceId = traceId
            )
            return Result.failure(IllegalStateException("Wi-Fi not connected"))
        }

        return try {
            val db = AppDatabase.getInstance(context)
            val allList = db.transactionDao().getAllTransactions()
            val jsonPayload = TransactionBackupManager.exportToJson(allList)

            // 脏数据哈希校验：若本次序列化内容与上次成功备份哈希完全一致，则无需重复向云端上传
            val currentHash = computeHash(jsonPayload)
            val lastHash = prefManager.lastBackupHashFlow.first()

            if (!force && currentHash == lastHash && lastHash.isNotBlank()) {
                ApmLogger.sync(
                    tag = "AutoBackup",
                    message = "账单数据无变动 (Hash 一致: $currentHash)，无需上传，节省流量",
                    traceId = traceId
                )
                return Result.success("Skipped: data unchanged")
            }

            ApmLogger.sync(
                tag = "AutoBackup",
                message = "检测到数据变更，正在向 Google Drive 上传 ${allList.size} 条账单...",
                traceId = traceId
            )

            // 获取 OAuth 2.0 Bearer Token 并上传
            val token = GoogleDriveService.getAccessToken(context, email)
            val uploadRes = GoogleDriveService.uploadBackup(token, jsonPayload, traceId)

            uploadRes.onSuccess { fileId ->
                val now = System.currentTimeMillis()
                // 更新最后同步时间与数据指纹哈希
                prefManager.setLastSyncTimestamp(now)
                prefManager.setLastBackupHash(currentHash)
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(
                    tag = "AutoBackup",
                    message = "Google Drive 自动备份成功 (FileID: $fileId)",
                    traceId = traceId
                )
            }.onFailure { err ->
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(
                    tag = "AutoBackup",
                    message = "Google Drive 自动备份上传失败: ${err.message}",
                    traceId = traceId
                )
            }

            uploadRes
        } catch (e: Throwable) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "自动备份过程抛出未捕获异常: ${e.message}",
                traceId = traceId
            )
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

