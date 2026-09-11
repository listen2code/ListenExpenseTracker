package com.listen.expensetracker.data.cloud

import android.accounts.Account
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 原生 Google Drive REST API v3 云端同步服务 (轻量化无 SDK 架构)
 *
 * 核心设计优势：
 * 1. 零 SDK 依赖：彻底摒弃体积达数兆的 Google API Client，基于原生 HttpURLConnection 极速交互；
 * 2. 自愈鉴权守卫：拦截 UserRecoverableAuthException 并自动拉起系统授权面板，实现故障自愈；
 * 3. 增量覆盖与多部分上传：首次创建走 multipart/related，后续同步走媒体流式 PATCH，极致节省流量；
 * 4. 全链路 APM 追踪：在 SYNC 频道记录每次云端读写的 Trace ID、文件 ID 与耗时。
 */
object GoogleDriveService {

    private const val BACKUP_FILE_NAME = "lexpense_backup.json"
    private const val DRIVE_API_FILES = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_MULTIPART = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val DRIVE_UPLOAD_MEDIA = "https://www.googleapis.com/upload/drive/v3/files/%s?uploadType=media"
    private const val OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    /**
     * 获取具有 drive.file 作用域的有效 OAuth 2.0 访问令牌 (Bearer Token)
     * 若遇到权限未授予异常，自动拉起 Google 系统授权面板
     */
    suspend fun getAccessToken(context: Context, accountEmail: String): String = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, "com.google")
        try {
            GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            // 提权自愈：利用异常携带的 Intent 调起原生权限授予面板
            e.intent?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            throw IllegalStateException("请在弹出的 Google 授权窗口中点击「允许」，完成后再次点击备份")
        }
    }

    /**
     * 将加密或序列化后的 JSON 账单快照上传至 Google Drive
     */
    suspend fun uploadBackup(
        accessToken: String,
        jsonContent: String,
        traceId: String = TraceManager.newTraceId()
    ): Result<String> = withContext(Dispatchers.IO) {
        TraceManager.trace(ApmLogChannel.SYNC, "GoogleDrive", "UploadBackup", traceId) {
            try {
                val existingFileId = findBackupFileId(accessToken)
                val fileId = if (existingFileId != null) {
                    updateExistingFile(existingFileId, accessToken, jsonContent)
                    existingFileId
                } else {
                    createNewFile(accessToken, jsonContent)
                }
                ApmLogger.sync("GoogleDrive", "已成功上传备份至 Google Drive ($BACKUP_FILE_NAME, FileID: $fileId)", traceId)
                Result.success(fileId)
            } catch (e: Throwable) {
                ApmLogger.sync("GoogleDrive", "Google Drive 上传异常: ${e.message}", traceId)
                Result.failure(e)
            }
        }
    }

    /**
     * 从 Google Drive 下载最新的 JSON 备份数据流
     */
    suspend fun downloadBackup(
        accessToken: String,
        traceId: String = TraceManager.newTraceId()
    ): Result<String> = withContext(Dispatchers.IO) {
        TraceManager.trace(ApmLogChannel.SYNC, "GoogleDrive", "DownloadBackup", traceId) {
            try {
                val fileId = findBackupFileId(accessToken)
                    ?: return@trace Result.failure(IllegalStateException("未在 Google 云端硬盘中找到 $BACKUP_FILE_NAME 备份文件"))

                val downloadUrl = "$DRIVE_API_FILES/$fileId?alt=media"
                val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    connectTimeout = 15000
                    readTimeout = 15000
                }

                if (connection.responseCode !in 200..299) {
                    throw IllegalStateException("Google Drive 下载失败 (${connection.responseCode}): ${readStream(connection.errorStream)}")
                }

                val jsonPayload = readStream(connection.inputStream)
                ApmLogger.sync("GoogleDrive", "成功从 Google Drive 下载快照 ($BACKUP_FILE_NAME, ${jsonPayload.length} 字节)", traceId)
                Result.success(jsonPayload)
            } catch (e: Throwable) {
                ApmLogger.sync("GoogleDrive", "Google Drive 下载异常: ${e.message}", traceId)
                Result.failure(e)
            }
        }
    }

    /**
     * 检索用户 Drive 中是否存在未被移入回收站的有效备份文件
     */
    private fun findBackupFileId(accessToken: String): String? {
        val query = "name='$BACKUP_FILE_NAME' and trashed=false"
        val urlStr = "$DRIVE_API_FILES?q=${URLEncoder.encode(query, "UTF-8")}&fields=files(id,name,modifiedTime)"
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $accessToken")
            connectTimeout = 15000
            readTimeout = 15000
        }

        if (connection.responseCode in 200..299) {
            val responseText = readStream(connection.inputStream)
            val files = JSONObject(responseText).optJSONArray("files")
            if (files != null && files.length() > 0) {
                return files.getJSONObject(0).optString("id")
            }
        }
        return null
    }

    /**
     * 通过 multipart/related 方式在 Google Drive 中创建新备份文件
     */
    private fun createNewFile(accessToken: String, jsonContent: String): String {
        val boundary = "===lExpenseBoundary" + System.currentTimeMillis() + "==="
        val connection = (URL(DRIVE_UPLOAD_MULTIPART).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            connectTimeout = 15000
            readTimeout = 20000
        }

        val metadata = JSONObject().apply {
            put("name", BACKUP_FILE_NAME)
            put("mimeType", "application/json")
        }.toString()

        val outputStream: OutputStream = connection.outputStream
        outputStream.bufferedWriter().use { writer ->
            writer.write("--$boundary\r\n")
            writer.write("Content-Type: application/json; charset=UTF-8\r\n\r\n")
            writer.write(metadata)
            writer.write("\r\n--$boundary\r\n")
            writer.write("Content-Type: application/json\r\n\r\n")
            writer.write(jsonContent)
            writer.write("\r\n--$boundary--\r\n")
            writer.flush()
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Google Drive 创建文件失败 (${connection.responseCode}): ${readStream(connection.errorStream)}")
        }

        return JSONObject(readStream(connection.inputStream)).getString("id")
    }

    /**
     * 通过 PATCH uploadType=media 覆盖更新已存在的 Google Drive 备份文件
     */
    private fun updateExistingFile(fileId: String, accessToken: String, jsonContent: String) {
        val urlStr = DRIVE_UPLOAD_MEDIA.format(fileId)
        val connection = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $accessToken")
            setRequestProperty("Content-Type", "application/json")
            connectTimeout = 15000
            readTimeout = 20000
        }

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(jsonContent)
            writer.flush()
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Google Drive 更新文件失败 (${connection.responseCode}): ${readStream(connection.errorStream)}")
        }
    }

    /**
     * 安全读取字节流并转换为 UTF-8 文本
     */
    private fun readStream(inputStream: InputStream?): String {
        if (inputStream == null) return ""
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
    }
}

