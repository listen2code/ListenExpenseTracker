package com.listen.expensetracker.features.settings.viewmodel

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.listen.arch.i18n.tr
import com.listen.arch.sync.CloudSyncManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.listen.expensetracker.data.backup.TransactionBackupManager
import com.listen.expensetracker.data.cloud.GoogleDriveService
import com.listen.expensetracker.data.db.RecurringRuleDao
import com.listen.expensetracker.data.db.TransactionDao
import com.listen.expensetracker.data.engine.DemoDataEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Delegate managing data backup, cloud synchronization, demo seeding, and JSON file I/O for SettingsViewModel.
 */
class SettingsSyncDelegate(
    private val application: Application,
    private val dao: TransactionDao,
    private val recurringDao: RecurringRuleDao,
    private val prefManager: ExpenseDataStoreManager
) {
    suspend fun triggerCloudBackup(
        email: String?,
        lang: String,
        traceId: String,
        onOperating: (Boolean) -> Unit,
        onToast: (String) -> Unit
    ) {
        if (email.isNullOrBlank()) {
            onToast(AppStrings.LOGIN_GOOGLE_REQUIRED_TOAST.tr(lang))
            return
        }
        onOperating(true)
        try {
            val allList = dao.getAllTransactions()
            val payload = TransactionBackupManager.exportToJson(allList)
            val token = GoogleDriveService.getAccessToken(application, email)
            val driveResult = GoogleDriveService.uploadBackup(token, payload, traceId)
            driveResult.onSuccess {
                prefManager.setLastSyncTimestamp(System.currentTimeMillis())
                CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
                onToast("已成功备份至 Google Drive 云端硬盘 (${allList.size} 条)")
            }.onFailure { err ->
                CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
                onToast("Google Drive 上传异常: ${err.message}")
            }
        } catch (e: Throwable) {
            val allList = dao.getAllTransactions()
            val payload = TransactionBackupManager.exportToJson(allList)
            CloudSyncManager.backupToCloud(payload, allList.size, email, traceId)
            onToast("已备份至本地快照 (Drive 凭据待授权: ${e.message})")
        } finally {
            onOperating(false)
        }
    }

    suspend fun triggerCloudRestore(
        email: String?,
        lang: String,
        traceId: String,
        onOperating: (Boolean) -> Unit,
        onToast: (String) -> Unit
    ) {
        if (email.isNullOrBlank()) {
            onToast(AppStrings.LOGIN_GOOGLE_REQUIRED_TOAST.tr(lang))
            return
        }
        onOperating(true)
        try {
            val token = GoogleDriveService.getAccessToken(application, email)
            val driveResult = GoogleDriveService.downloadBackup(token, traceId)
            driveResult.onSuccess { payload ->
                val list = TransactionBackupManager.importFromJson(payload)
                if (list.isNotEmpty()) {
                    dao.insertTransactions(list)
                    prefManager.setLastSyncTimestamp(System.currentTimeMillis())
                    onToast("已从 Google Drive 成功恢复 ${list.size} 条账单")
                } else {
                    onToast(AppStrings.RESTORE_EMPTY_TOAST.tr(lang))
                }
            }.onFailure { driveErr ->
                val fallbackRes = CloudSyncManager.restoreFromCloud(email, traceId)
                fallbackRes.onSuccess { payload ->
                    val list = TransactionBackupManager.importFromJson(payload)
                    if (list.isNotEmpty()) {
                        dao.insertTransactions(list)
                        onToast("已从快照恢复 ${list.size} 条账单")
                    }
                }.onFailure {
                    onToast("云端恢复失败: ${driveErr.message}")
                }
            }
        } catch (e: Throwable) {
            val fallbackRes = CloudSyncManager.restoreFromCloud(email, traceId)
            fallbackRes.onSuccess { payload ->
                val list = TransactionBackupManager.importFromJson(payload)
                if (list.isNotEmpty()) {
                    dao.insertTransactions(list)
                    onToast("已从快照恢复 ${list.size} 条账单")
                }
            }.onFailure {
                onToast("恢复失败: ${e.message}")
            }
        } finally {
            onOperating(false)
        }
    }

    suspend fun seedDemoData(monthOffset: Int, lang: String, onToast: (String) -> Unit) {
        val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(monthOffset, lang)
        val count = dao.getTransactionCountInRange(startTs, endTs)
        if (count > 0) {
            onToast(AppStrings.SEED_MONTH_HAS_DATA_ERROR.tr(lang))
            return
        }
        val accountList = AccountRepository.getAllAccounts().map { it.key }
        val accounts = if (accountList.isEmpty()) listOf("CASH", "BANK", "CREDIT") else accountList
        val generated = DemoDataEngine.generate(monthOffset, lang, accounts)
        dao.insertTransactions(generated)
        val defaultRules = DemoDataEngine.generateDefaultRecurringRules(lang)
        val existingRules = recurringDao.getAllRules()
        defaultRules.forEach { demoRule ->
            if (existingRules.none { it.title == demoRule.title }) {
                recurringDao.insertRule(demoRule)
            }
        }
        onToast(AppStrings.SEED_MONTH_SUCCESS_TOAST.tr(lang).format(title, generated.size))
    }

    suspend fun clearAllData(lang: String, onToast: (String) -> Unit) {
        dao.deleteAll()
        recurringDao.deleteAll()
        onToast(AppStrings.CLEAR_ALL_SUCCESS_TOAST.tr(lang))
    }

    suspend fun exportJsonToFile(uri: Uri, lang: String, onToast: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val allList = dao.getAllTransactions()
            val json = TransactionBackupManager.exportToJson(allList)
            application.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(json.toByteArray(Charsets.UTF_8))
            }
            onToast(if (lang == "en") "Successfully exported ${allList.size} records to JSON file" else "已成功导出 ${allList.size} 条账单至 JSON 文件")
        } catch (e: Throwable) {
            onToast(if (lang == "en") "Export failed: ${e.message}" else "导出 JSON 文件失败: ${e.message}")
        }
    }

    suspend fun importJsonFromFile(uri: Uri, lang: String, onToast: (String) -> Unit) = withContext(Dispatchers.IO) {
        try {
            val json = application.contentResolver.openInputStream(uri)?.use { ins ->
                ins.bufferedReader(Charsets.UTF_8).readText()
            } ?: ""
            val list = TransactionBackupManager.importFromJson(json)
            if (list.isNotEmpty()) {
                dao.insertTransactions(list)
                onToast(if (lang == "en") "Successfully imported ${list.size} records" else "成功导入 ${list.size} 条账单数据")
            } else {
                onToast(if (lang == "en") "JSON content is empty or invalid" else "JSON 文件内容解析失败或为空")
            }
        } catch (e: Throwable) {
            onToast(if (lang == "en") "Import failed: ${e.message}" else "导入 JSON 文件失败: ${e.message}")
        }
    }

    suspend fun exportExcelToFile(
        uri: Uri,
        startTs: Long?,
        endTs: Long?,
        typeFilter: String,
        lang: String,
        onToast: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val allList = dao.getAllTransactions()
            val filtered = TransactionBackupManager.filterTransactions(allList, startTs, endTs, typeFilter)
            val bytes = TransactionBackupManager.exportToExcelCsv(filtered, lang)
            application.contentResolver.openOutputStream(uri)?.use { os ->
                os.write(bytes)
            }
            onToast(if (lang == "en") "Successfully exported ${filtered.size} records to Excel file" else "已成功导出 ${filtered.size} 条账单至 Excel 表格")
        } catch (e: Throwable) {
            onToast(if (lang == "en") "Export failed: ${e.message}" else "导出 Excel 失败: ${e.message}")
        }
    }

    suspend fun shareExcel(
        startTs: Long?,
        endTs: Long?,
        typeFilter: String,
        lang: String,
        onToast: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val allList = dao.getAllTransactions()
            val filtered = TransactionBackupManager.filterTransactions(allList, startTs, endTs, typeFilter)
            val bytes = TransactionBackupManager.exportToExcelCsv(filtered, lang)
            val exportDir = File(application.cacheDir, "exports").apply { mkdirs() }
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(exportDir, "lexpense_${timeStr}.csv")
            file.writeBytes(bytes)

            val shareUri = FileProvider.getUriForFile(application, "${application.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val title = if (lang == "en") "Share Excel Statement" else "分享账单表格"
            val chooser = Intent.createChooser(shareIntent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            application.startActivity(chooser)
        } catch (e: Throwable) {
            onToast(if (lang == "en") "Share failed: ${e.message}" else "分享账单失败: ${e.message}")
        }
    }
}
