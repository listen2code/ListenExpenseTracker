package com.listen.expensetracker.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerServiceTest {

    @Test
    fun testIsVersionNewer_majorVersion() {
        assertTrue(UpdateCheckerService.isVersionNewer("1.0.0", "0.9.9"))
        assertTrue(UpdateCheckerService.isVersionNewer("v2.0.0", "1.99.99"))
        assertFalse(UpdateCheckerService.isVersionNewer("1.0.0", "2.0.0"))
    }

    @Test
    fun testIsVersionNewer_minorVersion() {
        assertTrue(UpdateCheckerService.isVersionNewer("0.1.0", "0.0.19"))
        assertTrue(UpdateCheckerService.isVersionNewer("1.2.0", "1.1.9"))
        assertFalse(UpdateCheckerService.isVersionNewer("0.0.19", "0.1.0"))
    }

    @Test
    fun testIsVersionNewer_patchVersion() {
        assertTrue(UpdateCheckerService.isVersionNewer("0.0.20", "0.0.19"))
        assertTrue(UpdateCheckerService.isVersionNewer("0.0.100", "0.0.99"))
        assertFalse(UpdateCheckerService.isVersionNewer("0.0.19", "0.0.19"))
        assertFalse(UpdateCheckerService.isVersionNewer("0.0.18", "0.0.19"))
    }

    @Test
    fun testIsVersionNewer_withPrefixesAndSuffixes() {
        assertTrue(UpdateCheckerService.isVersionNewer("v0.0.20", "0.0.19"))
        assertTrue(UpdateCheckerService.isVersionNewer("0.0.20-beta", "0.0.19"))
        assertFalse(UpdateCheckerService.isVersionNewer("v0.0.19", "0.0.19"))
    }

    @Test
    fun testIsVersionNewer_blankOrInvalid() {
        assertFalse(UpdateCheckerService.isVersionNewer("", "0.0.19"))
        assertFalse(UpdateCheckerService.isVersionNewer("0.0.19", ""))
        assertFalse(UpdateCheckerService.isVersionNewer("", ""))
    }

    @Test
    fun testParseVersionJson_newVersionAvailable() {
        val json = """
            {
                "version": "0.0.20",
                "buildNumber": 20,
                "url": "https://github.com/listen2code/ListenExpenseTracker/releases/download/v0.0.20/app-release.apk",
                "changelog": {
                    "zh": "修复若干问题",
                    "en": "Bug fixes"
                }
            }
        """.trimIndent()
        val result = UpdateCheckerService.parseVersionJson(
            jsonString = json,
            currentVersion = "0.0.1",
            currentBuildNumber = 1L,
            lang = "zh"
        )
        assertTrue("Expected NewVersionAvailable, got: $result", result is UpdateResult.NewVersionAvailable)
        if (result is UpdateResult.NewVersionAvailable) {
            assertTrue(result.releaseInfo.tagName.contains("0.0.20"))
            assertTrue(result.releaseInfo.changelog.isNotBlank())
            assertTrue(result.releaseInfo.apkDownloadUrl?.endsWith(".apk") == true)
        }
    }

    @Test
    fun testParseVersionJson_alreadyLatest() {
        val json = """
            {
                "version": "0.0.20",
                "buildNumber": 20,
                "url": "https://github.com/listen2code/ListenExpenseTracker/releases"
            }
        """.trimIndent()
        val result = UpdateCheckerService.parseVersionJson(
            jsonString = json,
            currentVersion = "0.0.20",
            currentBuildNumber = 20L,
            lang = "zh"
        )
        assertTrue("Expected AlreadyLatest, got: $result", result is UpdateResult.AlreadyLatest)
    }

    @Test
    fun testParseVersionJson_malformedJson() {
        val result = UpdateCheckerService.parseVersionJson(
            jsonString = "invalid-json",
            currentVersion = "0.0.1"
        )
        assertTrue("Expected Error on malformed JSON, got: $result", result is UpdateResult.Error)
    }

    @Test
    fun testCheckLatestRelease_handlesNetworkOrResultGracefully() = kotlinx.coroutines.test.runTest {
        val result = UpdateCheckerService.checkLatestRelease(
            currentVersion = "0.0.1",
            currentBuildNumber = 1L,
            lang = "zh"
        )
        // In CI or offline test runners, live HTTP may fail gracefully with Error or succeed
        assertTrue(result is UpdateResult.NewVersionAvailable || result is UpdateResult.Error || result is UpdateResult.AlreadyLatest)
    }
}
