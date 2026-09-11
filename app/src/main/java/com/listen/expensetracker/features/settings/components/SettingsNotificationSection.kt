package com.listen.expensetracker.features.settings.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.notification.NotificationPermissionHelper
import com.listen.expensetracker.data.i18n.NotificationStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonSwitchRow
import com.listen.uicomponent.components.SurfaceCard

/**
 * 设置页「通知与提醒」高紧凑统一控制面板 (SettingsNotificationSection)。
 * 顶部 Header 融合全局主开关，展开后仅呈现 3 个高频业务开关（预算预警合并为单开关）。
 */
@Composable
fun SettingsNotificationSection(
    notificationsEnabled: Boolean,
    budgetAlertsEnabled: Boolean,
    recurringBillsAlertsEnabled: Boolean,
    appUpdatesAlertsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onToggleRecurringBillsAlerts: (Boolean) -> Unit,
    onToggleAppUpdatesAlerts: (Boolean) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasSystemPermission by remember {
        mutableStateOf(NotificationPermissionHelper.hasNotificationPermission(context))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasSystemPermission = NotificationPermissionHelper.hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceStandard,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            // 1. Header 与全局总开关融合为单行，未启用时极度紧凑 (~48dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = NotificationStrings.SETTINGS_NOTIFICATIONS_SECTION.tr(lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = NotificationStrings.SETTINGS_NOTIFICATIONS_ENABLE_DESC.tr(lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
            }

            // 系统未授权警告横幅
            if (!hasSystemPermission) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(AppDimens.CornerCard)
                        )
                        .padding(horizontal = AppDimens.SpaceMedium, vertical = AppDimens.SpaceSmall)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = NotificationStrings.SETTINGS_PERMISSION_DENIED_BANNER.tr(lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    CommonButton(
                        text = NotificationStrings.SETTINGS_PERMISSION_GRANT_BTN.tr(lang),
                        onClick = { NotificationPermissionHelper.openNotificationSettings(context) },
                        style = CommonButtonStyle.Primary,
                        modifier = Modifier.padding(start = AppDimens.SpaceSmall)
                    )
                }
            }

            // 2. 细分通知项列表（开启总开关后展开，仅 3 个清晰开关）
            AnimatedVisibility(
                visible = notificationsEnabled,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // 1. 预算预警与超支提醒（合并为单开关：80% 警戒与 100% 超支）
                    CommonSwitchRow(
                        title = NotificationStrings.SETTINGS_BUDGET_ALERTS.tr(lang),
                        subtitle = NotificationStrings.SETTINGS_BUDGET_ALERTS_DESC.tr(lang),
                        checked = budgetAlertsEnabled,
                        onCheckedChange = onToggleBudgetAlerts,
                        contentPadding = 0.dp
                    )

                    // 2. 周期账单自动入账提醒
                    CommonSwitchRow(
                        title = NotificationStrings.SETTINGS_RECURRING_ALERTS.tr(lang),
                        subtitle = NotificationStrings.SETTINGS_RECURRING_ALERTS_DESC.tr(lang),
                        checked = recurringBillsAlertsEnabled,
                        onCheckedChange = onToggleRecurringBillsAlerts,
                        contentPadding = 0.dp
                    )

                    // 3. 新版本发布更新提醒
                    CommonSwitchRow(
                        title = NotificationStrings.SETTINGS_UPDATE_ALERTS.tr(lang),
                        subtitle = NotificationStrings.SETTINGS_UPDATE_ALERTS_DESC.tr(lang),
                        checked = appUpdatesAlertsEnabled,
                        onCheckedChange = onToggleAppUpdatesAlerts,
                        contentPadding = 0.dp
                    )
                }
            }
        }
    }
}
