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
 * Service that queries the public GitHub Releases API asynchronously
 * and determines whether a newer version is available.
 */
object UpdateCheckerService {
    private const val REPO_OWNER = "listen2code"
    private const val REPO_NAME = "ListenExpenseTracker"
    private const val GITHUB_API_URL = "https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest"

    /**
     * Checks the latest GitHub release asynchronously and compares it against current version.
     */
    suspend fun checkLatestRelease(currentVersion: String): UpdateResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val url = URL(GITHUB_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "lExpense-Android-App")
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val jsonString = reader.use { it.readText() }
                val json = JSONObject(jsonString)

                val tagName = json.optString("tag_name", "")
                val title = json.optString("name", tagName)
                val body = json.optString("body", "")
                val htmlUrl = json.optString("html_url", "https://github.com/$REPO_OWNER/$REPO_NAME/releases")

                // Find .apk download asset if available
                var apkUrl: String? = null
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url")
                            break
                        }
                    }
                }

                val cleanRemote = tagName.removePrefix("v").removePrefix("V").trim()
                val cleanLocal = currentVersion.removePrefix("v").removePrefix("V").trim()

                if (isVersionNewer(cleanRemote, cleanLocal)) {
                    UpdateResult.NewVersionAvailable(
                        ReleaseInfo(
                            tagName = tagName,
                            title = title,
                            changelog = body,
                            htmlUrl = htmlUrl,
                            apkDownloadUrl = apkUrl
                        )
                    )
                } else {
                    UpdateResult.AlreadyLatest(currentVersion)
                }
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                // No releases published yet
                UpdateResult.AlreadyLatest(currentVersion)
            } else {
                UpdateResult.Error("HTTP $responseCode: ${connection.responseMessage}")
            }
        } catch (e: Exception) {
            UpdateResult.Error(e.localizedMessage ?: "Unknown network error")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Compares remote and local semantic version numbers (major.minor.patch).
     * Returns true if remote is strictly greater than local.
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
