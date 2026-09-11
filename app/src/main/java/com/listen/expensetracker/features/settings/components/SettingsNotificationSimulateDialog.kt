package com.listen.expensetracker.features.settings.components

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.notification.LocalNotificationManager
import com.listen.expensetracker.core.notification.NotificationPermissionHelper
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.i18n.NotificationStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ListenTheme

/**
 * 开发者模式：本地系统通知快速演练与模拟对话框。
 * 支持一键派发 80% 警戒、100% 超支、周期账单自动入账与新版本更新四类系统通知。
 */
@Composable
fun SettingsNotificationSimulateDialog(
    onDismiss: () -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    fun dispatchSimulation(action: () -> Unit) {
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            Toast.makeText(
                context,
                NotificationStrings.SIMULATE_PERMISSION_REQUIRED_TOAST.tr(lang),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        LocalNotificationManager.createNotificationChannels(context, lang)
        action()
        Toast.makeText(
            context,
            NotificationStrings.SIMULATE_SENT_TOAST.tr(lang),
            Toast.LENGTH_SHORT
        ).show()
    }

    CommonDialog(
        onDismissRequest = onDismiss,
        title = NotificationStrings.SIMULATE_NOTIFICATIONS_TITLE.tr(lang),
        dismissButton = {
            CommonButton(
                text = AppStrings.BTN_CANCEL.tr(lang),
                onClick = onDismiss,
                style = CommonButtonStyle.Outlined
            )
        },
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
            CommonText(
                text = NotificationStrings.SIMULATE_NOTIFICATIONS_DESC.tr(lang),
                fontSize = AppDimens.TextCaption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(AppDimens.SpaceExtraSmall))

            // 1. 模拟 80% 预算警戒
            CommonButton(
                text = NotificationStrings.SIMULATE_BUDGET_WARNING.tr(lang),
                onClick = {
                    dispatchSimulation {
                        LocalNotificationManager.sendBudgetAlert(
                            context = context,
                            notificationId = LocalNotificationManager.ID_BUDGET_TOTAL_ALERT,
                            title = "⚠️ 月度总预算警戒线提醒",
                            content = "本月总支出已达总预算的 82.5%，剩余可用额度 ¥875.00。"
                        )
                    }
                },
                style = CommonButtonStyle.Primary,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp)) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 2. 模拟 100% 预算超支
            CommonButton(
                text = NotificationStrings.SIMULATE_BUDGET_OVERRUN.tr(lang),
                onClick = {
                    dispatchSimulation {
                        LocalNotificationManager.sendBudgetAlert(
                            context = context,
                            notificationId = LocalNotificationManager.ID_BUDGET_CATEGORY_ALERT_BASE + 1,
                            title = "🚨 餐饮分类预算已超支",
                            content = "本月餐饮分类支出已达 ¥1,820.00，超出分类预算 ¥320.00，请注意控制开支！",
                            categoryId = "c_food"
                        )
                    }
                },
                style = CommonButtonStyle.Danger,
                icon = { Icon(Icons.Default.ReportProblem, contentDescription = null, modifier = Modifier.size(18.dp)) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 3. 模拟周期账单入账
            CommonButton(
                text = NotificationStrings.SIMULATE_RECURRING_BILL.tr(lang),
                onClick = {
                    dispatchSimulation {
                        LocalNotificationManager.sendRecurringBillAlert(
                            context = context,
                            notificationId = LocalNotificationManager.ID_RECURRING_ALERT,
                            title = "📅 周期账单已自动记账入库 (2笔)",
                            content = "今日已自动履约记账 2 笔周期账单，合计支出 ¥3,650.00。点击查看流水。",
                            details = listOf(
                                "• 房租: ¥3,500.00 (住房 / 现金)",
                                "• 宽带光纤费: ¥150.00 (通讯 / 银行卡)"
                            )
                        )
                    }
                },
                style = CommonButtonStyle.Secondary,
                icon = { Icon(Icons.Default.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp)) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // 4. 模拟版本更新通知
            CommonButton(
                text = NotificationStrings.SIMULATE_APP_UPDATE.tr(lang),
                onClick = {
                    dispatchSimulation {
                        LocalNotificationManager.sendAppUpdateAlert(
                            context = context,
                            notificationId = LocalNotificationManager.ID_UPDATE_ALERT,
                            title = "🚀 发现 Listen 记账新版本 v1.5.0",
                            content = "全新功能就绪：本地通知预警中枢上线，支持点击直接穿透分类预算！",
                            versionName = "v1.5.0"
                        )
                    }
                },
                style = CommonButtonStyle.Secondary,
                icon = { Icon(Icons.Default.NewReleases, contentDescription = null, modifier = Modifier.size(18.dp)) },
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsNotificationSimulateDialogPreview() {
    ExpenseStrings.init()
    ListenTheme {
        SettingsNotificationSimulateDialog(onDismiss = {}, lang = "zh")
    }
}
