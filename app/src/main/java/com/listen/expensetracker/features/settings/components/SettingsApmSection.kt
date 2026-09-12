package com.listen.expensetracker.features.settings.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.i18n.NotificationStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.*
import com.listen.uicomponent.theme.ListenTheme

/**
 * APM 运维可观测性与数据测试卡片 (SettingsApmSection)。
 * 涵盖全局浮窗开关、生成演示数据、清空数据暗门与模拟系统通知入口。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsApmSection(
    apmFloatingWindowEnabled: Boolean,
    onToggleApmFloatingWindow: (Boolean) -> Unit,
    onSeedDemoData: () -> Unit,
    onConfirmClearAll: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier,
    targetMonthTitle: String = "",
    onOpenSimulateNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var isDangerZoneVisible by remember { mutableStateOf(false) }

    fun toggleDangerZone() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        isDangerZoneVisible = !isDangerZoneVisible
        val tip = if (isDangerZoneVisible) {
            AppStrings.DANGER_ZONE_UNLOCKED_TOAST.tr(lang)
        } else {
            AppStrings.DANGER_ZONE_LOCKED_TOAST.tr(lang)
        }
        Toast.makeText(context, tip, Toast.LENGTH_SHORT).show()
    }

    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceStandard,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)) {
            // Header（长按标题作为备用暗门）
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { toggleDangerZone() }
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Ops",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(AppDimens.IconSizeMedium)
                )
                Text(
                    text = AppStrings.SETTINGS_SYSTEM_OPS.tr(lang),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // APM Floating Window Switch
            CommonSwitchRow(
                title = AppStrings.APM_FLOATING_WINDOW_TITLE.tr(lang),
                checked = apmFloatingWindowEnabled,
                onCheckedChange = onToggleApmFloatingWindow,
                subtitle = AppStrings.APM_FLOATING_WINDOW_DESC.tr(lang),
                contentPadding = 0.dp
            )

            val seedBtnText = if (targetMonthTitle.isNotBlank()) {
                "${AppStrings.SEED_DATA_BTN.tr(lang)} ($targetMonthTitle)"
            } else {
                AppStrings.SEED_DATA_BTN.tr(lang)
            }

            // 1. 生成本月数据按钮 (全宽，长按切换高危清空按钮的显隐)
            CommonButton(
                text = seedBtnText,
                onClick = onSeedDemoData,
                onLongClick = { toggleDangerZone() },
                style = CommonButtonStyle.Outlined,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = "Seed",
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )

            // 2. 清空数据高危暗门按钮 (展开时全宽显示)
            AnimatedVisibility(
                visible = isDangerZoneVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                CommonButton(
                    text = AppStrings.CLEAR_ALL.tr(lang),
                    onClick = {
                        isDangerZoneVisible = false
                        onConfirmClearAll()
                    },
                    style = CommonButtonStyle.Danger,
                    icon = {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            modifier = Modifier.size(AppDimens.IconSizeMedium)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                )
            }

            // 3. 模拟系统通知全宽按钮
            CommonButton(
                text = NotificationStrings.SIMULATE_NOTIFICATIONS_BTN.tr(lang),
                onClick = onOpenSimulateNotifications,
                style = CommonButtonStyle.Outlined,
                icon = {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsApmSectionPreview() {
    ExpenseStrings.init()
    ListenTheme {
        SettingsApmSection(
            apmFloatingWindowEnabled = true,
            onToggleApmFloatingWindow = {},
            onSeedDemoData = {},
            onConfirmClearAll = {},
            lang = "zh"
        )
    }
}
