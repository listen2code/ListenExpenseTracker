package com.listen.expensetracker.data.i18n

import com.listen.arch.i18n.StringsRes

/**
 * 集中式本地通知多语言字典 (NotificationStrings)。
 * 涵盖通知渠道名、渠道描述、预算超支预警、周期账单自动入账、版本更新以及设置项文本。
 */
object NotificationStrings {

    const val CHANNEL_BUDGET_ALERTS_NAME = "channel_budget_alerts_name"
    const val CHANNEL_BUDGET_ALERTS_DESC = "channel_budget_alerts_desc"
    const val CHANNEL_RECURRING_BILLS_NAME = "channel_recurring_bills_name"
    const val CHANNEL_RECURRING_BILLS_DESC = "channel_recurring_bills_desc"
    const val CHANNEL_APP_UPDATES_NAME = "channel_app_updates_name"
    const val CHANNEL_APP_UPDATES_DESC = "channel_app_updates_desc"

    // 预算通知文案
    const val NOTIFY_BUDGET_OVERRUN_TOTAL_TITLE = "notify_budget_overrun_total_title"
    const val NOTIFY_BUDGET_OVERRUN_TOTAL_BODY = "notify_budget_overrun_total_body"
    const val NOTIFY_BUDGET_WARNING_TOTAL_TITLE = "notify_budget_warning_total_title"
    const val NOTIFY_BUDGET_WARNING_TOTAL_BODY = "notify_budget_warning_total_body"
    const val NOTIFY_BUDGET_OVERRUN_CAT_TITLE = "notify_budget_overrun_cat_title"
    const val NOTIFY_BUDGET_OVERRUN_CAT_BODY = "notify_budget_overrun_cat_body"
    const val NOTIFY_BUDGET_WARNING_CAT_TITLE = "notify_budget_warning_cat_title"
    const val NOTIFY_BUDGET_WARNING_CAT_BODY = "notify_budget_warning_cat_body"

    // 周期账单通知文案
    const val NOTIFY_RECURRING_SINGLE_TITLE = "notify_recurring_single_title"
    const val NOTIFY_RECURRING_SINGLE_BODY = "notify_recurring_single_body"
    const val NOTIFY_RECURRING_MULTI_TITLE = "notify_recurring_multi_title"
    const val NOTIFY_RECURRING_MULTI_BODY = "notify_recurring_multi_body"

    // 版本更新通知文案
    const val NOTIFY_UPDATE_TITLE = "notify_update_title"
    const val NOTIFY_UPDATE_BODY = "notify_update_body"

    // 设置项文本
    const val SETTINGS_NOTIFICATIONS_SECTION = "settings_notifications_section"
    const val SETTINGS_NOTIFICATIONS_ENABLE = "settings_notifications_enable"
    const val SETTINGS_NOTIFICATIONS_ENABLE_DESC = "settings_notifications_enable_desc"
    const val SETTINGS_BUDGET_ALERTS = "settings_budget_alerts"
    const val SETTINGS_BUDGET_ALERTS_DESC = "settings_budget_alerts_desc"
    const val SETTINGS_BUDGET_WARNING_LINE = "settings_budget_warning_line"
    const val SETTINGS_BUDGET_WARNING_LINE_DESC = "settings_budget_warning_line_desc"
    const val SETTINGS_RECURRING_ALERTS = "settings_recurring_alerts"
    const val SETTINGS_RECURRING_ALERTS_DESC = "settings_recurring_alerts_desc"
    const val SETTINGS_UPDATE_ALERTS = "settings_update_alerts"
    const val SETTINGS_UPDATE_ALERTS_DESC = "settings_update_alerts_desc"
    const val SETTINGS_PERMISSION_DENIED_BANNER = "settings_permission_denied_banner"
    const val SETTINGS_PERMISSION_GRANT_BTN = "settings_permission_grant_btn"

    // 运维与测试模拟通知
    const val SIMULATE_NOTIFICATIONS_BTN = "simulate_notifications_btn"
    const val SIMULATE_NOTIFICATIONS_TITLE = "simulate_notifications_title"
    const val SIMULATE_NOTIFICATIONS_DESC = "simulate_notifications_desc"
    const val SIMULATE_BUDGET_WARNING = "simulate_budget_warning"
    const val SIMULATE_BUDGET_OVERRUN = "simulate_budget_overrun"
    const val SIMULATE_RECURRING_BILL = "simulate_recurring_bill"
    const val SIMULATE_APP_UPDATE = "simulate_app_update"
    const val SIMULATE_SENT_TOAST = "simulate_sent_toast"
    const val SIMULATE_PERMISSION_REQUIRED_TOAST = "simulate_permission_required_toast"

    fun init() {
        StringsRes.registerAppStrings("zh", zhMap)
        StringsRes.registerAppStrings("en", enMap)
        StringsRes.registerAppStrings("ja", jaMap)
    }

    private val zhMap = mapOf(
        CHANNEL_BUDGET_ALERTS_NAME to "预算超支与警戒预警",
        CHANNEL_BUDGET_ALERTS_DESC to "总预算及分类预算越过 80% 警戒或 100% 超支线时触发提醒",
        CHANNEL_RECURRING_BILLS_NAME to "周期账单自动入账",
        CHANNEL_RECURRING_BILLS_DESC to "周期账单到达执行日期并在后台自动记账入库后的提醒",
        CHANNEL_APP_UPDATES_NAME to "应用版本更新提醒",
        CHANNEL_APP_UPDATES_DESC to "检测到新版本可用时的静默与轻提醒",

        NOTIFY_BUDGET_OVERRUN_TOTAL_TITLE to "🚨 月度总预算已超支",
        NOTIFY_BUDGET_OVERRUN_TOTAL_BODY to "本月总支出已达 %s%s，超出总预算 %s%s，请注意控制开支！",
        NOTIFY_BUDGET_WARNING_TOTAL_TITLE to "⚠️ 月度总预算警戒线提醒",
        NOTIFY_BUDGET_WARNING_TOTAL_BODY to "本月总支出已达总预算的 %.1f%%，剩余可用额度 %s%s。",
        NOTIFY_BUDGET_OVERRUN_CAT_TITLE to "🚨 「%s」预算已超支",
        NOTIFY_BUDGET_OVERRUN_CAT_BODY to "本月该分类已支出 %s%s，超出预算额度 %s%s。",
        NOTIFY_BUDGET_WARNING_CAT_TITLE to "⚠️ 「%s」预算预警",
        NOTIFY_BUDGET_WARNING_CAT_BODY to "该分类支出已达设定预算的 %.1f%%，请合理规划后续消费。",

        NOTIFY_RECURRING_SINGLE_TITLE to "📅 周期账单已自动入账",
        NOTIFY_RECURRING_SINGLE_BODY to "已自动记录「%s」：%s%s（%s）",
        NOTIFY_RECURRING_MULTI_TITLE to "📅 自动记账提醒（共 %d 笔）",
        NOTIFY_RECURRING_MULTI_BODY to "已自动履约入账 %d 笔周期账单，合计支出 %s%s。点击查看流水明细。",

        NOTIFY_UPDATE_TITLE to "🚀 发现新版本 %s 已发布",
        NOTIFY_UPDATE_BODY to "全新版本已就绪！%s，点击查看更新详情。",

        SETTINGS_NOTIFICATIONS_SECTION to "通知与提醒",
        SETTINGS_NOTIFICATIONS_ENABLE to "启用应用通知",
        SETTINGS_NOTIFICATIONS_ENABLE_DESC to "预算预警、周期入账与新版提醒",
        SETTINGS_BUDGET_ALERTS to "预算预警与超支提醒",
        SETTINGS_BUDGET_ALERTS_DESC to "达 80% 警戒线或 100% 超支时提醒",
        SETTINGS_BUDGET_WARNING_LINE to "80% 警戒线预警",
        SETTINGS_BUDGET_WARNING_LINE_DESC to "支出达到 80% 时提前提醒",
        SETTINGS_RECURRING_ALERTS to "周期账单自动入账提醒",
        SETTINGS_RECURRING_ALERTS_DESC to "自动履约记账后通知核对",
        SETTINGS_UPDATE_ALERTS to "新版本发布更新提醒",
        SETTINGS_UPDATE_ALERTS_DESC to "检测到应用新版本时通知",
        SETTINGS_PERMISSION_DENIED_BANNER to "系统通知权限已关闭，无法接收提醒",
        SETTINGS_PERMISSION_GRANT_BTN to "去开启",

        SIMULATE_NOTIFICATIONS_BTN to "模拟测试系统通知",
        SIMULATE_NOTIFICATIONS_TITLE to "本地通知模拟演练",
        SIMULATE_NOTIFICATIONS_DESC to "点击下方按钮直接派发系统原生通知，测试横幅、声音振动、Inbox 样式与 DeepLink 直达。",
        SIMULATE_BUDGET_WARNING to "⚠️ 模拟总预算 80% 警戒提醒",
        SIMULATE_BUDGET_OVERRUN to "🚨 模拟分类预算 100% 超支提醒",
        SIMULATE_RECURRING_BILL to "🔁 模拟周期账单入账提醒 (多笔)",
        SIMULATE_APP_UPDATE to "🚀 模拟新版本发布提醒",
        SIMULATE_SENT_TOAST to "已派发模拟通知，请下拉通知栏体验！",
        SIMULATE_PERMISSION_REQUIRED_TOAST to "通知权限未开启，请先在上方开启通知权限"
    )

    private val enMap = mapOf(
        CHANNEL_BUDGET_ALERTS_NAME to "Budget Overrun & Alerts",
        CHANNEL_BUDGET_ALERTS_DESC to "Alerts when monthly total or category budgets cross 80% or 100% threshold",
        CHANNEL_RECURRING_BILLS_NAME to "Recurring Bills Execution",
        CHANNEL_RECURRING_BILLS_DESC to "Notifications when due recurring bills are automatically recorded",
        CHANNEL_APP_UPDATES_NAME to "App Version Updates",
        CHANNEL_APP_UPDATES_DESC to "Notifications when a newer version is available",

        NOTIFY_BUDGET_OVERRUN_TOTAL_TITLE to "🚨 Monthly Budget Exceeded",
        NOTIFY_BUDGET_OVERRUN_TOTAL_BODY to "Total expense reached %s%s, exceeding budget by %s%s!",
        NOTIFY_BUDGET_WARNING_TOTAL_TITLE to "⚠️ Monthly Budget Warning",
        NOTIFY_BUDGET_WARNING_TOTAL_BODY to "Monthly expense reached %.1f%% of budget. Remaining: %s%s.",
        NOTIFY_BUDGET_OVERRUN_CAT_TITLE to "🚨 \"%s\" Budget Exceeded",
        NOTIFY_BUDGET_OVERRUN_CAT_BODY to "Category expense reached %s%s, exceeding budget by %s%s.",
        NOTIFY_BUDGET_WARNING_CAT_TITLE to "⚠️ \"%s\" Budget Warning",
        NOTIFY_BUDGET_WARNING_CAT_BODY to "Category expense reached %.1f%% of allocated budget.",

        NOTIFY_RECURRING_SINGLE_TITLE to "📅 Recurring Bill Auto-Recorded",
        NOTIFY_RECURRING_SINGLE_BODY to "Recorded \"%s\": %s%s (%s)",
        NOTIFY_RECURRING_MULTI_TITLE to "📅 Recurring Bills Auto-Recorded (%d)",
        NOTIFY_RECURRING_MULTI_BODY to "Auto-recorded %d recurring bills, total %s%s. Tap to review.",

        NOTIFY_UPDATE_TITLE to "🚀 New Version %s Available",
        NOTIFY_UPDATE_BODY to "New release is ready! %s. Tap to check updates.",

        SETTINGS_NOTIFICATIONS_SECTION to "Notifications & Alerts",
        SETTINGS_NOTIFICATIONS_ENABLE to "Enable App Notifications",
        SETTINGS_NOTIFICATIONS_ENABLE_DESC to "Budget alerts, recurring records and updates",
        SETTINGS_BUDGET_ALERTS to "Budget Alerts & Overrun",
        SETTINGS_BUDGET_ALERTS_DESC to "Alerts at 80% warning or 100% overrun",
        SETTINGS_BUDGET_WARNING_LINE to "80% Warning Line Alerts",
        SETTINGS_BUDGET_WARNING_LINE_DESC to "Early warning at 80% budget consumption",
        SETTINGS_RECURRING_ALERTS to "Recurring Bill Auto-Record",
        SETTINGS_RECURRING_ALERTS_DESC to "Notify after recurring transactions are recorded",
        SETTINGS_UPDATE_ALERTS to "New Version Updates",
        SETTINGS_UPDATE_ALERTS_DESC to "Notify when a new version is released",
        SETTINGS_PERMISSION_DENIED_BANNER to "Notification permission disabled in system settings",
        SETTINGS_PERMISSION_GRANT_BTN to "Enable",

        SIMULATE_NOTIFICATIONS_BTN to "Simulate System Notifications",
        SIMULATE_NOTIFICATIONS_TITLE to "Notification Simulation Hub",
        SIMULATE_NOTIFICATIONS_DESC to "Dispatch native notifications directly to test banners, vibration, inbox styles, and deeplinks.",
        SIMULATE_BUDGET_WARNING to "⚠️ Simulate 80% Budget Warning",
        SIMULATE_BUDGET_OVERRUN to "🚨 Simulate 100% Budget Overrun",
        SIMULATE_RECURRING_BILL to "🔁 Simulate Recurring Bills (Multi)",
        SIMULATE_APP_UPDATE to "🚀 Simulate New Version Update",
        SIMULATE_SENT_TOAST to "Notification dispatched! Swipe down status bar to check.",
        SIMULATE_PERMISSION_REQUIRED_TOAST to "Notification permission disabled. Please enable it first."
    )

    private val jaMap = mapOf(
        CHANNEL_BUDGET_ALERTS_NAME to "予算超過・警告通知",
        CHANNEL_BUDGET_ALERTS_DESC to "月間総予算またはカテゴリー予算が80%または100%を超えた場合の通知",
        CHANNEL_RECURRING_BILLS_NAME to "定期収支の自動記帳",
        CHANNEL_RECURRING_BILLS_DESC to "定期ルールが期日に自動的に記帳された際の通知",
        CHANNEL_APP_UPDATES_NAME to "アプリのアップデート通知",
        CHANNEL_APP_UPDATES_DESC to "新しいバージョンが利用可能になった際の通知",

        NOTIFY_BUDGET_OVERRUN_TOTAL_TITLE to "🚨 月間予算を超過しました",
        NOTIFY_BUDGET_OVERRUN_TOTAL_BODY to "今月の総支出が%s%sに達し、予算を%s%s超過しました！",
        NOTIFY_BUDGET_WARNING_TOTAL_TITLE to "⚠️ 月間予算の警告ライン",
        NOTIFY_BUDGET_WARNING_TOTAL_BODY to "今月の総支出が予算の%.1f%%に達しました。残り: %s%s。",
        NOTIFY_BUDGET_OVERRUN_CAT_TITLE to "🚨 「%s」の予算超過",
        NOTIFY_BUDGET_OVERRUN_CAT_BODY to "このカテゴリーの支出が%s%sに達し、予算を%s%s超過しました。",
        NOTIFY_BUDGET_WARNING_CAT_TITLE to "⚠️ 「%s」の予算警告",
        NOTIFY_BUDGET_WARNING_CAT_BODY to "このカテゴリーの支出が設定予算の%.1f%%に達しました。",

        NOTIFY_RECURRING_SINGLE_TITLE to "📅 定期収支が自動記帳されました",
        NOTIFY_RECURRING_SINGLE_BODY to "「%s」を自動記帳しました: %s%s（%s）",
        NOTIFY_RECURRING_MULTI_TITLE to "📅 自動記帳のお知らせ（合計 %d 件）",
        NOTIFY_RECURRING_MULTI_BODY to "%d 件の定期収支を自動記帳しました（合計 %s%s）。タップして確認。",

        NOTIFY_UPDATE_TITLE to "🚀 新バージョン %s がリリースされました",
        NOTIFY_UPDATE_BODY to "新バージョンが利用可能です！%s。タップして詳細を確認。",

        SETTINGS_NOTIFICATIONS_SECTION to "通知とリマインダー",
        SETTINGS_NOTIFICATIONS_ENABLE to "アプリ通知を有効にする",
        SETTINGS_NOTIFICATIONS_ENABLE_DESC to "予算警告、定期自動記帳、アップデート通知",
        SETTINGS_BUDGET_ALERTS to "予算警告・超過通知",
        SETTINGS_BUDGET_ALERTS_DESC to "80%警戒または100%超過時に通知",
        SETTINGS_BUDGET_WARNING_LINE to "80% 警戒ライン通知",
        SETTINGS_BUDGET_WARNING_LINE_DESC to "予算が80%に達した時点で事前警告",
        SETTINGS_RECURRING_ALERTS to "定期自動記帳の通知",
        SETTINGS_RECURRING_ALERTS_DESC to "定期収支が自動記帳された後に通知",
        SETTINGS_UPDATE_ALERTS to "新バージョン更新通知",
        SETTINGS_UPDATE_ALERTS_DESC to "新機能やバグ修正版がリリースされた際に通知",
        SETTINGS_PERMISSION_DENIED_BANNER to "システムの通知権限が無効になっています",
        SETTINGS_PERMISSION_GRANT_BTN to "設定を開く",

        SIMULATE_NOTIFICATIONS_BTN to "通知のシミュレーション",
        SIMULATE_NOTIFICATIONS_TITLE to "通知テストセンター",
        SIMULATE_NOTIFICATIONS_DESC to "通知を直接送信し、バナー、バイブレーション、ディープリンクの動作をテストします。",
        SIMULATE_BUDGET_WARNING to "⚠️ 予算80%警告通知をテスト",
        SIMULATE_BUDGET_OVERRUN to "🚨 予算100%超過通知をテスト",
        SIMULATE_RECURRING_BILL to "🔁 定期自動記帳通知をテスト (複数)",
        SIMULATE_APP_UPDATE to "🚀 アップデート通知をテスト",
        SIMULATE_SENT_TOAST to "通知を送信しました。通知バーをプルダウンして確認してください！",
        SIMULATE_PERMISSION_REQUIRED_TOAST to "通知権限が無効です。先に通知権限を有効にしてください。"
    )
}
