package com.listen.expensetracker.features.settings.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.listen.uicomponent.components.AutoResizeText
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonSwitchRow
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ListenTheme

/**
 * APM Observability, Testing Seeds, and About App Section Card.
 * 方案 A：默认隐藏高危的“清空所有账单”按钮，支持长按“生成数据”或卡片标题隐秘唤出。
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
            // Header（支持长按标题作为备用暗门）
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

            // APM Floating Window Switch (Rule 22: 控制全局可拖拽调试悬浮球)
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

            // Notification Simulation Trigger
            CommonButton(
                text = NotificationStrings.SIMULATE_NOTIFICATIONS_BTN.tr(lang),
                onClick = onOpenSimulateNotifications,
                style = CommonButtonStyle.Outlined,
                icon = {
                    Icon(
                        Icons.Default.NotificationsActive,
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
