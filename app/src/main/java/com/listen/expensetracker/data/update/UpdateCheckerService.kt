package com.listen.expensetracker.data.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val title: String,
    val changelog: String,
    val htmlUrl: String,
    val apkDownloadUrl: String?
)

sealed interface UpdateResult {
    data class NewVersionAvailable(val releaseInfo: ReleaseInfo) : UpdateResult
    data class AlreadyLatest(val currentVersion: String) : UpdateResult
    data class Error(val message: String) : UpdateResult
}

/**
 * Service that queries the latest version metadata from GitHub Pages version.json asynchronously.
 */
object UpdateCheckerService {
    const val VERSION_URL = "https://listen2code.github.io/ListenExpenseTracker/pages/version.json"

    /**
     * Checks the latest release metadata asynchronously from version.json.
     */
    suspend fun checkLatestRelease(
        currentVersion: String,
        currentBuildNumber: Long = 0L,
        lang: String = "zh"
    ): UpdateResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(VERSION_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "lExpense-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val jsonString = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val json = JSONObject(jsonString)

                val version = json.optString("version", "") ?: ""
                val buildNumber = json.optLong("buildNumber", 0L)
                val targetUrl = json.optString("url", "") ?: ""

                val changelogObj = json.optJSONObject("changelog")
                val changelogText = changelogObj?.optString(lang)?.takeUnless { it.isBlank() }
                    ?: changelogObj?.optString("en")?.takeUnless { it.isBlank() }
                    ?: changelogObj?.optString("zh")?.takeUnless { it.isBlank() }
                    ?: ""

                val cleanRemote = version.removePrefix("v").removePrefix("V").trim()
                val cleanLocal = currentVersion.removePrefix("v").removePrefix("V").trim()

                val isNewer = if (currentBuildNumber > 0L && buildNumber > 0L) {
                    buildNumber > currentBuildNumber
                } else {
                    isVersionNewer(cleanRemote, cleanLocal)
                }

                if (isNewer) {
                    UpdateResult.NewVersionAvailable(
                        ReleaseInfo(
                            tagName = "v$cleanRemote",
                            title = "v$cleanRemote",
                            changelog = changelogText,
                            htmlUrl = targetUrl,
                            apkDownloadUrl = if (targetUrl.endsWith(".apk", ignoreCase = true)) targetUrl else null
                        )
                    )
                } else {
                    UpdateResult.AlreadyLatest(currentVersion)
                }
            } else {
                UpdateResult.Error("HTTP $responseCode: ${connection.responseMessage}")
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.localizedMessage ?: "Network error")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares remote and local semantic version numbers (major.minor.patch).
     */
    fun isVersionNewer(remote: String, local: String): Boolean {
        if (remote.isBlank() || local.isBlank()) return false
        val remoteParts = parseVersionParts(remote)
        val localParts = parseVersionParts(local)

        val maxLen = maxOf(remoteParts.size, localParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val l = localParts.getOrElse(i) { 0 }
            if (r > l) return true
            if (r < l) return false
        }
        return false
    }

    private fun parseVersionParts(version: String): List<Int> {
        return version.split(".")
            .map { part ->
                part.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
            }
    }
}
