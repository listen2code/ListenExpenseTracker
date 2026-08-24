package com.listen.expensetracker.data.cloud

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.ApmLogger
import com.listen.arch.apm.TraceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Native Google Drive REST API v3 Integration Service.
 * Provides direct, lightweight cloud file upload, query, and restore operations.
 */
object GoogleDriveService {

    private const val BACKUP_FILE_NAME = "lexpense_backup.json"
    private const val DRIVE_API_FILES = "https://www.googleapis.com/drive/v3/files"
    private const val DRIVE_UPLOAD_MULTIPART = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
    private const val DRIVE_UPLOAD_MEDIA = "https://www.googleapis.com/upload/drive/v3/files/%s?uploadType=media"
    private const val OAUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file"

    /**
     * Acquires a valid OAuth 2.0 Access Token with Google Drive scopes.
     *
     * @param context Context reference
     * @param accountEmail The authenticated user's Google email
     * @return Valid Bearer OAuth token
     */
    suspend fun getAccessToken(context: Context, accountEmail: String): String = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, "com.google")
        try {
            GoogleAuthUtil.getToken(context, account, OAUTH_SCOPE)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            e.intent?.let { intent ->
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
            throw IllegalStateException("请在弹出的 Google 授权窗口中点击「允许」，完成后再次点击备份")
        }
    }

    /**
     * Uploads the backup JSON payload to Google Drive, updating an existing file or creating a new one.
     *
     * @param accessToken Valid OAuth 2.0 Bearer token
     * @param jsonContent Serialized transaction JSON data
     * @param traceId Distributed APM tracing identifier
     * @return Result containing the uploaded Google Drive File ID
     */
    suspend fun uploadBackup(
        accessToken: String,
        jsonContent: String,
        traceId: String = TraceManager.newTraceId()
    ): Result<String> = withContext(Dispatchers.IO) {
        TraceManager.trace(
            channel = ApmLogChannel.SYNC,
            tag = "GoogleDrive",
            operationName = "UploadBackup",
            traceId = traceId
        ) {
            try {
                val existingFileId = findBackupFileId(accessToken)
                val fileId = if (existingFileId != null) {
                    updateExistingFile(existingFileId, accessToken, jsonContent)
                    existingFileId
                } else {
                    createNewFile(accessToken, jsonContent)
                }

                ApmLogger.sync(
                    tag = "GoogleDrive",
                    message = "Successfully uploaded backup ($BACKUP_FILE_NAME, FileID: $fileId) to Google Drive",
                    traceId = traceId
                )
                Result.success(fileId)
            } catch (e: Throwable) {
                ApmLogger.sync(
                    tag = "GoogleDrive",
                    message = "Failed to upload to Google Drive: ${e.message}",
                    traceId = traceId
                )
                Result.failure(e)
            }
        }
    }

    /**
     * Downloads and returns the raw JSON backup payload from Google Drive.
     *
     * @param accessToken Valid OAuth 2.0 Bearer token
     * @param traceId Distributed APM tracing identifier
     * @return Result containing the raw JSON content
     */
    suspend fun downloadBackup(
        accessToken: String,
        traceId: String = TraceManager.newTraceId()
    ): Result<String> = withContext(Dispatchers.IO) {
        TraceManager.trace(
            channel = ApmLogChannel.SYNC,
            tag = "GoogleDrive",
            operationName = "DownloadBackup",
            traceId = traceId
        ) {
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

                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    val errorBody = readStream(connection.errorStream)
                    throw IllegalStateException("Google Drive 下载失败 ($responseCode): $errorBody")
                }

                val jsonPayload = readStream(connection.inputStream)
                ApmLogger.sync(
                    tag = "GoogleDrive",
                    message = "Successfully downloaded backup ($BACKUP_FILE_NAME, ${jsonPayload.length} bytes) from Google Drive",
                    traceId = traceId
                )
                Result.success(jsonPayload)
            } catch (e: Throwable) {
                ApmLogger.sync(
                    tag = "GoogleDrive",
                    message = "Failed to download from Google Drive: ${e.message}",
                    traceId = traceId
                )
                Result.failure(e)
            }
        }
    }

    /**
     * Searches for an existing active backup file in Google Drive.
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
            val json = JSONObject(responseText)
            val files = json.optJSONArray("files")
            if (files != null && files.length() > 0) {
                return files.getJSONObject(0).optString("id")
            }
        }
        return null
    }

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

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val err = readStream(connection.errorStream)
            throw IllegalStateException("Google Drive 创建文件失败 ($responseCode): $err")
        }

        val response = JSONObject(readStream(connection.inputStream))
        return response.getString("id")
    }

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

        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val err = readStream(connection.errorStream)
            throw IllegalStateException("Google Drive 更新文件失败 ($responseCode): $err")
        }
    }

    private fun readStream(inputStream: java.io.InputStream?): String {
        if (inputStream == null) return ""
        return BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { it.readText() }
    }
}
