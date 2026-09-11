package com.listen.expensetracker.features.settings.viewmodel

import android.net.Uri
import com.listen.arch.sync.SyncState
import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.CategoryBudgetConfig
import com.listen.expensetracker.data.update.ReleaseInfo
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

class SettingsUiStateTest {

    @Test
    fun testDefaultValues() {
        val state = SettingsUiState()
        assertEquals("zh", state.language)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(AccentColor.EMERALD, state.accentColor)
        assertEquals("￥", state.currencySymbol)
        assertEquals(5000.0, state.monthlyBudget, 0.0)
        assertEquals(CategoryBudgetConfig.defaultRatios, state.categoryBudgetRatios)
        assertTrue(state.recurringRules.isEmpty())
        assertTrue(state.autoBackupDrive)
        assertFalse(state.autoBackupWifiOnly)
        assertNotNull(state.syncState)
        assertNull(state.googleAccountEmail)
        assertNull(state.googleDisplayName)
        assertNull(state.googleAvatarUrl)
        assertFalse(state.isLoggedIn)
        assertEquals(0L, state.lastSyncTimestamp)
        assertNull(state.activeDialog)
        assertFalse(state.isOperating)
        assertFalse(state.isDeveloperMode)
        assertFalse(state.isCheckingUpdate)
        assertFalse(state.biometricLockEnabled)
        assertEquals(0, state.lockTimeoutSeconds)
        assertTrue(state.recentAppsShieldEnabled)
        assertTrue(state.shakeToHideBalanceEnabled)
        assertFalse(state.isBiometricSupported)
        assertFalse(state.apmFloatingWindowEnabled)
        assertTrue(state.transactions.isEmpty())
    }

    @Test
    fun testCustomStateValuesAndCopy() {
        val rule = RecurringRuleEntity(
            id = "rule_1",
            title = "Test Rent",
            type = TransactionType.EXPENSE,
            categoryId = "c_shopping",
            categoryName = "Shopping",
            categoryIcon = "icon",
            categoryColorHex = "#EC4899",
            amount = 2500.0,
            accountType = "BANK",
            note = "Rent note",
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 1,
            startDate = 1000L,
            nextExecutionDate = 2000L,
            executionType = ExecutionType.AUTO_INSERT
        )
        val tx = TransactionEntity(
            id = "tx_1",
            type = TransactionType.EXPENSE,
            categoryId = "c_food",
            categoryName = "Food",
            categoryIcon = "icon",
            categoryColorHex = "#EF4444",
            amount = 50.0,
            timestamp = 1000L,
            note = "Lunch",
            accountType = "CASH"
        )

        val state = SettingsUiState(
            language = "en",
            themeMode = ThemeMode.DARK,
            accentColor = AccentColor.ROYAL_PURPLE,
            currencySymbol = "$",
            monthlyBudget = 8000.0,
            recurringRules = listOf(rule),
            isLoggedIn = true,
            googleAccountEmail = "user@gmail.com",
            googleDisplayName = "User Name",
            googleAvatarUrl = "https://avatar.png",
            isDeveloperMode = true,
            biometricLockEnabled = true,
            transactions = listOf(tx)
        )

        assertEquals("en", state.language)
        assertEquals(ThemeMode.DARK, state.themeMode)
        assertEquals(AccentColor.ROYAL_PURPLE, state.accentColor)
        assertEquals("$", state.currencySymbol)
        assertEquals(8000.0, state.monthlyBudget, 0.0)
        assertEquals(1, state.recurringRules.size)
        assertTrue(state.isLoggedIn)
        assertEquals("user@gmail.com", state.googleAccountEmail)
        assertEquals("User Name", state.googleDisplayName)
        assertEquals("https://avatar.png", state.googleAvatarUrl)
        assertTrue(state.isDeveloperMode)
        assertTrue(state.biometricLockEnabled)
        assertEquals(1, state.transactions.size)

        val copiedState = state.copy(language = "ja", isCheckingUpdate = true)
        assertEquals("ja", copiedState.language)
        assertTrue(copiedState.isCheckingUpdate)
        assertEquals(8000.0, copiedState.monthlyBudget, 0.0)
    }

    @Test
    fun testSettingsDialogVariants() {
        val monthlyDialog = SettingsDialog.MonthlyBudget
        val categoryDialog = SettingsDialog.CategoryManage
        val accountDialog = SettingsDialog.AccountManage
        val recurringDialog = SettingsDialog.RecurringManage
        val clearDialog = SettingsDialog.ClearConfirm
        val logoutDialog = SettingsDialog.LogoutConfirm
        val aboutDialog = SettingsDialog.AboutApp
        val exportExcelDialog = SettingsDialog.ExportExcelOptions

        val release = ReleaseInfo(
            tagName = "v1.1.0",
            title = "Version 1.1.0",
            changelog = "Bug fixes and improvements",
            htmlUrl = "https://github.com/release",
            apkDownloadUrl = "https://download.apk"
        )
        val updateDialog = SettingsDialog.UpdateAvailable(release)

        assertEquals("v1.1.0", updateDialog.releaseInfo.tagName)
        assertEquals("Version 1.1.0", updateDialog.releaseInfo.title)
        assertEquals("Bug fixes and improvements", updateDialog.releaseInfo.changelog)
        assertEquals("https://download.apk", updateDialog.releaseInfo.apkDownloadUrl)
        assertEquals(monthlyDialog, SettingsDialog.MonthlyBudget)
        assertEquals(categoryDialog, SettingsDialog.CategoryManage)
        assertEquals(accountDialog, SettingsDialog.AccountManage)
        assertEquals(recurringDialog, SettingsDialog.RecurringManage)
        assertEquals(clearDialog, SettingsDialog.ClearConfirm)
        assertEquals(logoutDialog, SettingsDialog.LogoutConfirm)
        assertEquals(aboutDialog, SettingsDialog.AboutApp)
        assertEquals(exportExcelDialog, SettingsDialog.ExportExcelOptions)
    }

    @Test
    fun testSettingsEffects() {
        assertEquals(SettingsEffect.LaunchGoogleSignIn, SettingsEffect.LaunchGoogleSignIn)
        assertEquals(SettingsEffect.ScrollToTop, SettingsEffect.ScrollToTop)
    }

    @Test
    fun testAllSettingsIntents() {
        // Preferences Intents
        val langIntent = SettingsIntent.ChangeLanguage("en")
        assertEquals("en", langIntent.langCode)

        val themeIntent = SettingsIntent.ChangeThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, themeIntent.mode)

        val accentIntent = SettingsIntent.ChangeAccentColor(AccentColor.OCEAN_BLUE)
        assertEquals(AccentColor.OCEAN_BLUE, accentIntent.accent)

        val budgetIntent = SettingsIntent.UpdateMonthlyBudget(6000.0)
        assertEquals(6000.0, budgetIntent.budget, 0.0)

        val customRatios = mapOf("c_food" to 0.4f, "c_transport" to 0.2f)
        val catBudgetsIntent = SettingsIntent.UpdateCategoryBudgets(7000.0, customRatios)
        assertEquals(7000.0, catBudgetsIntent.budget, 0.0)
        assertEquals(0.4f, catBudgetsIntent.ratios["c_food"] ?: 0f, 0.001f)

        // Recurring Rule Intents
        val mockRule = RecurringRuleEntity(
            id = "rule_2",
            title = "Gym",
            type = TransactionType.EXPENSE,
            categoryId = "c_fitness",
            categoryName = "Fitness",
            categoryIcon = "icon",
            categoryColorHex = "#F59E0B",
            amount = 300.0,
            accountType = "CREDIT",
            note = "Gym Note",
            frequency = RecurringFrequency.MONTHLY,
            dayOfPeriod = 15,
            startDate = 1000L,
            nextExecutionDate = 2000L,
            executionType = ExecutionType.AUTO_INSERT
        )
        val saveRuleIntent = SettingsIntent.SaveRecurringRule(mockRule)
        assertEquals("rule_2", saveRuleIntent.rule.id)

        val deleteRuleIntent = SettingsIntent.DeleteRecurringRule("rule_2")
        assertEquals("rule_2", deleteRuleIntent.ruleId)

        val toggleRuleIntent = SettingsIntent.ToggleRecurringRule(mockRule, isEnabled = false)
        assertEquals(mockRule, toggleRuleIntent.rule)
        assertFalse(toggleRuleIntent.isEnabled)

        // Toggle Intents
        val autoBackupIntent = SettingsIntent.ToggleAutoBackupDrive(true)
        assertTrue(autoBackupIntent.enabled)

        val wifiOnlyIntent = SettingsIntent.ToggleAutoBackupWifiOnly(false)
        assertFalse(wifiOnlyIntent.enabled)

        val devModeIntent = SettingsIntent.ToggleDeveloperMode(true)
        assertTrue(devModeIntent.enabled)

        val bioLockIntent = SettingsIntent.ToggleBiometricLock(true)
        assertTrue(bioLockIntent.enabled)

        val timeoutIntent = SettingsIntent.ChangeLockTimeout(60)
        assertEquals(60, timeoutIntent.seconds)

        val shieldIntent = SettingsIntent.ToggleRecentAppsShield(false)
        assertFalse(shieldIntent.enabled)

        val shakeIntent = SettingsIntent.ToggleShakeToHideBalance(true)
        assertTrue(shakeIntent.enabled)

        val apmFloatingIntent = SettingsIntent.ToggleApmFloatingWindow(true)
        assertTrue(apmFloatingIntent.enabled)

        // Auth & Sync Intents
        assertEquals(SettingsIntent.TriggerGoogleSignIn, SettingsIntent.TriggerGoogleSignIn)
        assertEquals(SettingsIntent.ScrollToTop, SettingsIntent.ScrollToTop)

        val linkIntent = SettingsIntent.LinkGoogleAccount("test@test.com", "Test User", "http://avatar")
        assertEquals("test@test.com", linkIntent.email)
        assertEquals("Test User", linkIntent.displayName)
        assertEquals("http://avatar", linkIntent.avatarUrl)

        assertEquals(SettingsIntent.UnlinkGoogleAccount, SettingsIntent.UnlinkGoogleAccount)
        assertEquals(SettingsIntent.TriggerCloudBackup, SettingsIntent.TriggerCloudBackup)
        assertEquals(SettingsIntent.TriggerCloudRestore, SettingsIntent.TriggerCloudRestore)

        // Data Management Intents
        val seedIntent = SettingsIntent.SeedDemoData(1)
        assertEquals(1, seedIntent.monthOffset)

        assertEquals(SettingsIntent.ClearAllData, SettingsIntent.ClearAllData)

        val mockUri = mock(Uri::class.java)
        val exportJsonIntent = SettingsIntent.ExportJsonToFile(mockUri)
        assertEquals(mockUri, exportJsonIntent.uri)

        val importJsonIntent = SettingsIntent.ImportJsonFromFile(mockUri)
        assertEquals(mockUri, importJsonIntent.uri)

        val exportExcelIntent = SettingsIntent.ExportExcelToFile(mockUri, 1000L, 2000L, "EXPENSE")
        assertEquals(mockUri, exportExcelIntent.uri)
        assertEquals(1000L, exportExcelIntent.startTs)
        assertEquals(2000L, exportExcelIntent.endTs)
        assertEquals("EXPENSE", exportExcelIntent.typeFilter)

        val shareExcelIntent = SettingsIntent.ShareExcel(null, null, "ALL")
        assertNull(shareExcelIntent.startTs)
        assertNull(shareExcelIntent.endTs)
        assertEquals("ALL", shareExcelIntent.typeFilter)

        // Dialog & Update Intents
        val openDialogIntent = SettingsIntent.OpenDialog(SettingsDialog.MonthlyBudget)
        assertEquals(SettingsDialog.MonthlyBudget, openDialogIntent.dialog)

        assertEquals(SettingsIntent.DismissDialog, SettingsIntent.DismissDialog)

        val checkUpdateIntent = SettingsIntent.CheckForUpdates("1.0.0")
        assertEquals("1.0.0", checkUpdateIntent.currentVersion)
    }
}
