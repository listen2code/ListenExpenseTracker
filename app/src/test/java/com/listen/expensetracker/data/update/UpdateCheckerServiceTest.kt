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
    fun testCheckLatestRelease_liveVersionJson() = kotlinx.coroutines.test.runTest {
        val result = UpdateCheckerService.checkLatestRelease(
            currentVersion = "0.0.1",
            currentBuildNumber = 1L,
            lang = "zh"
        )
        assertTrue("Expected new version available from version.json, got: $result", result is UpdateResult.NewVersionAvailable)
        if (result is UpdateResult.NewVersionAvailable) {
            assertTrue(result.releaseInfo.tagName.contains("0.0.20"))
            assertTrue(result.releaseInfo.changelog.isNotBlank())
        }
    }

    @Test
    fun testCheckLatestRelease_alreadyLatest() = kotlinx.coroutines.test.runTest {
        val result = UpdateCheckerService.checkLatestRelease(
            currentVersion = "0.0.20",
            currentBuildNumber = 20L,
            lang = "zh"
        )
        assertTrue("Expected already latest, got: $result", result is UpdateResult.AlreadyLatest)
    }
}
