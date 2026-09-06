package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonSegmentedControl
import com.listen.uicomponent.components.CommonSwitchRow
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ListenTheme

/**
 * 设置页「安全与隐私」卡片组件 (SettingsSecuritySection)。
 * 涵盖生物识别应用锁开关、锁定时长选择、多任务防窥保护与摇一摇隐额手势。
 */
@Composable
fun SettingsSecuritySection(
    biometricLockEnabled: Boolean,
    lockTimeoutSeconds: Int,
    recentAppsShieldEnabled: Boolean,
    shakeToHideBalanceEnabled: Boolean,
    isBiometricSupported: Boolean,
    onToggleBiometricLock: (Boolean) -> Unit,
    onChangeLockTimeout: (Int) -> Unit,
    onToggleRecentAppsShield: (Boolean) -> Unit,
    onToggleShakeToHideBalance: (Boolean) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceLarge,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Security",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeLarge)
                )
                Text(
                    text = AppStrings.SETTINGS_SECURITY_TITLE.tr(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle
                )
            }

            // 1. 生物识别应用锁开关
            val biometricDesc = if (!isBiometricSupported) {
                AppStrings.SECURITY_BIOMETRIC_UNAVAILABLE.tr(lang)
            } else {
                AppStrings.SECURITY_BIOMETRIC_LOCK_DESC.tr(lang)
            }

            CommonSwitchRow(
                title = AppStrings.SECURITY_BIOMETRIC_LOCK_TITLE.tr(lang),
                checked = biometricLockEnabled && isBiometricSupported,
                onCheckedChange = { onToggleBiometricLock(it) },
                subtitle = biometricDesc,
                enabled = isBiometricSupported,
                contentPadding = 0.dp
            )

            // 2. 自动锁定时长选择器（仅开启应用锁时展示）
            if (biometricLockEnabled && isBiometricSupported) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = AppStrings.SECURITY_TIMEOUT_TITLE.tr(lang),
                        fontSize = AppDimens.TextSubtitle,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val timeoutOptions = listOf(
                        0 to AppStrings.SECURITY_TIMEOUT_IMMEDIATE.tr(lang),
                        60 to AppStrings.SECURITY_TIMEOUT_1MIN.tr(lang),
                        300 to AppStrings.SECURITY_TIMEOUT_5MIN.tr(lang)
                    )

                    CommonSegmentedControl(
                        items = timeoutOptions.map { it.second },
                        selectedIndex = timeoutOptions.indexOfFirst { it.first == lockTimeoutSeconds }.coerceAtLeast(0),
                        onIndexChange = { index -> onChangeLockTimeout(timeoutOptions[index].first) }
                    )
                }
            }

            // 3. 多任务后台防窥保护
            CommonSwitchRow(
                title = AppStrings.SECURITY_RECENT_APPS_TITLE.tr(lang),
                checked = recentAppsShieldEnabled,
                onCheckedChange = { onToggleRecentAppsShield(it) },
                subtitle = AppStrings.SECURITY_RECENT_APPS_DESC.tr(lang),
                contentPadding = 0.dp
            )

            // 4. 手势防窥（摇一摇/双击结余）
            CommonSwitchRow(
                title = AppStrings.SECURITY_GESTURE_TITLE.tr(lang),
                checked = shakeToHideBalanceEnabled,
                onCheckedChange = { onToggleShakeToHideBalance(it) },
                subtitle = AppStrings.SECURITY_GESTURE_DESC.tr(lang),
                contentPadding = 0.dp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsSecuritySectionPreview() {
    ExpenseStrings.init()
    ListenTheme {
        SettingsSecuritySection(
            biometricLockEnabled = true,
            lockTimeoutSeconds = 60,
            recentAppsShieldEnabled = true,
            shakeToHideBalanceEnabled = true,
            isBiometricSupported = true,
            onToggleBiometricLock = {},
            onChangeLockTimeout = {},
            onToggleRecentAppsShield = {},
            onToggleShakeToHideBalance = {},
            lang = "zh",
            modifier = Modifier.padding(16.dp)
        )
    }
}
