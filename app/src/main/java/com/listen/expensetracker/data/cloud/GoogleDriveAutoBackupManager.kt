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
 * Intelligent Google Drive Auto-Backup Orchestrator.
 * Provides mutation-driven debounced sync, app lifecycle background sync,
 * dirty-data hash checking, and Wi-Fi network guarding.
 */
object GoogleDriveAutoBackupManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingDebounceJob: Job? = null

    /**
     * Schedules a debounced auto-backup job (default delay: 5000ms).
     * Automatically resets if new transactions or mutations occur rapidly.
     */
    fun scheduleAutoBackup(context: Context, delayMs: Long = 5000L) {
        pendingDebounceJob?.cancel()
        pendingDebounceJob = scope.launch {
            delay(delayMs.milliseconds)
            performAutoBackup(context.applicationContext, force = false)
        }
    }

    /**
     * Immediately executes the auto-backup to Google Drive if conditions are met.
     */
    suspend fun performAutoBackup(context: Context, force: Boolean = false): Result<String> {
        val traceId = TraceManager.newTraceId()
        val prefManager = ExpenseDataStoreManager(context)

        val isLoggedIn = prefManager.isLoggedInFlow.first()
        val email = prefManager.userEmailFlow.first()
        if (!isLoggedIn || email.isBlank()) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "User not signed into Google, skipping auto-backup",
                traceId = traceId
            )
            return Result.failure(IllegalStateException("User not logged into Google"))
        }

        val autoBackupEnabled = prefManager.autoBackupDriveFlow.first()
        if (!autoBackupEnabled && !force) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "Auto-backup disabled by user preference, skipping",
                traceId = traceId
            )
            return Result.failure(IllegalStateException("Auto backup disabled"))
        }

        val wifiOnly = prefManager.autoBackupWifiOnlyFlow.first()
        if (wifiOnly && !isWifiConnected(context)) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "Wi-Fi only enabled but currently on cellular, skipping auto-backup",
                traceId = traceId
            )
            return Result.failure(IllegalStateException("Wi-Fi not connected"))
        }

        return try {
            val db = AppDatabase.getInstance(context)
            val allList = db.transactionDao().getAllTransactions()
            val jsonPayload = TransactionBackupManager.exportToJson(allList)

            // Dirty checking with SHA-256 digest
            val currentHash = computeHash(jsonPayload)
            val lastHash = prefManager.lastBackupHashFlow.first()

            if (!force && currentHash == lastHash && lastHash.isNotBlank()) {
                ApmLogger.sync(
                    tag = "AutoBackup",
                    message = "Data unchanged (Hash matches: $currentHash), skipping upload",
                    traceId = traceId
                )
                return Result.success("Skipped: data unchanged")
            }

            ApmLogger.sync(
                tag = "AutoBackup",
                message = "Data mutation detected. Uploading backup (${allList.size} records) to Google Drive...",
                traceId = traceId
            )

            val token = GoogleDriveService.getAccessToken(context, email)
            val uploadRes = GoogleDriveService.uploadBackup(token, jsonPayload, traceId)

            uploadRes.onSuccess { fileId ->
                val now = System.currentTimeMillis()
                prefManager.setLastSyncTimestamp(now)
                prefManager.setLastBackupHash(currentHash)
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(
                    tag = "AutoBackup",
                    message = "Successfully auto-backed up to Google Drive (FileID: $fileId)",
                    traceId = traceId
                )
            }.onFailure { err ->
                CloudSyncManager.backupToCloud(jsonPayload, allList.size, email, traceId)
                ApmLogger.sync(
                    tag = "AutoBackup",
                    message = "Google Drive auto-backup upload failed: ${err.message}",
                    traceId = traceId
                )
            }

            uploadRes
        } catch (e: Throwable) {
            ApmLogger.sync(
                tag = "AutoBackup",
                message = "Auto-backup exception encountered: ${e.message}",
                traceId = traceId
            )
            Result.failure(e)
        }
    }

    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun computeHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
