package com.listen.expensetracker.features.settings.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Hub
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
import com.listen.expensetracker.data.i18n.ArchitectureStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.i18n.NotificationStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.*
import com.listen.uicomponent.theme.ListenTheme

/**
 * APM 运维可观测性、数据测试与架构全景可视化卡片 (SettingsApmSection)。
 * 涵盖全局浮窗开关、生成演示数据、清空数据暗门、模拟系统通知与架构全景入口。
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
    onOpenSimulateNotifications: () -> Unit = {},
    onOpenArchitectureVisualizer: () -> Unit = {}
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

            // Seed & Clear Buttons Row (长按“生成数据”按钮切换高危清空按钮的显隐)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val seedBtnText = if (targetMonthTitle.isNotBlank()) {
                    "${AppStrings.SEED_DATA_BTN.tr(lang)} ($targetMonthTitle)"
                } else {
                    AppStrings.SEED_DATA_BTN.tr(lang)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    color = Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = onSeedDemoData,
                            onLongClick = { toggleDangerZone() }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Seed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(AppDimens.IconSizeMedium)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        AutoResizeText(
                            text = seedBtnText,
                            maxLines = 1,
                            targetTextSize = 12.sp,
                            minTextSize = 8.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isDangerZoneVisible,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally(),
                    modifier = Modifier.weight(1f)
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
                                Icons.Default.DeleteSweep,
                                contentDescription = "Clear",
                                modifier = Modifier.size(AppDimens.IconSizeMedium)
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                    )
                }
            }

            // 开发者双通道按钮行：模拟通知与架构全景并列
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                CommonButton(
                    text = NotificationStrings.SIMULATE_NOTIFICATIONS_BTN.tr(lang),
                    onClick = onOpenSimulateNotifications,
                    style = CommonButtonStyle.Outlined,
                    icon = {
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )

                CommonButton(
                    text = ArchitectureStrings.TITLE.tr(lang),
                    onClick = onOpenArchitectureVisualizer,
                    style = CommonButtonStyle.Outlined,
                    icon = {
                        Icon(
                            Icons.Default.Hub,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                )
            }
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
