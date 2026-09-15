package com.listen.expensetracker.data.model

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized Design System Dimensions and UI Constants.
 * Prevents magic number hardcoding across Composable components.
 */
object AppDimens {
    // Corner Radii
    val CornerCard = 10.dp
    val CornerPill = 20.dp
    val CornerButton = 8.dp
    val CornerCircle = 50.dp

    // Spacing Tokens
    val SpaceExtraSmall = 2.dp
    val SpaceSmall = 4.dp
    val SpaceMedium = 6.dp
    val SpaceStandard = 8.dp
    val SpaceLarge = 12.dp
    val SpaceSection = 16.dp
    val SpaceBottomFab = 72.dp

    // Icon & Component Sizes
    val IconSizeSmall = 12.dp
    val IconSizeMedium = 16.dp
    val IconSizeLarge = 24.dp
    val ButtonHeightCompact = 36.dp
    val ChartHeightStandard = 136.dp
    val ChartBarWidth = 18.dp

    // Text Size Tokens
    val TextMicro = 9.sp
    val TextCaption = 10.sp
    val TextSmall = 11.sp
    val TextBody = 12.sp
    val TextSubtitle = 13.sp
    val TextTitle = 14.sp
    val TextHeader = 16.sp
    val TextDisplay = 18.sp
}

/**
 * Global App Constants including Deep Link routing and Security defaults.
 */
object AppConstants {
    const val DEFAULT_LANG = "zh"
    const val DEFAULT_CURRENCY = "￥"
    const val FILTER_ALL = "ALL"
    const val RECURRING_TAG = "[周期]"
    const val APP_NAME = "lExpense"

    object Language {
        const val ZH = "zh"
        const val EN = "en"
        const val JA = "ja"

        const val ZH_NAME = "简体中文"
        const val EN_NAME = "English"
        const val JA_NAME = "日本語"

        fun getDisplayName(lang: String): String = when (lang.lowercase()) {
            EN -> EN_NAME
            JA -> JA_NAME
            else -> ZH_NAME
        }
    }

    object DateFormat {
        const val ISO_DATE = "yyyy-MM-dd"
        const val YEAR_MONTH_CN = "yyyy年MM月"
        const val MONTH_YEAR_EN = "MMM yyyy"
        const val BACKUP_TIMESTAMP = "yyyyMMdd_HHmmss"
        const val TIME_DEFAULT = "HH:mm"
        const val DATETIME_SECONDS = "yyyy-MM-dd HH:mm:ss"
        const val DATETIME_MINUTES = "yyyy-MM-dd HH:mm"
        const val MONTH_DAY = "MM-dd"
        const val TIME_MILLIS = "HH:mm:ss.SSS"

        val MONTH_NAMES_EN = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        fun getMonthLabel(monthIndex: Int, lang: String): String {
            return if (lang.lowercase() == Language.EN) {
                MONTH_NAMES_EN.getOrElse(monthIndex) { "${monthIndex + 1}" }
            } else {
                "${monthIndex + 1}月"
            }
        }

        fun formatMonthDay(month: Int, day: Int, lang: String): String {
            return if (lang.lowercase() == Language.EN) {
                "$month/$day"
            } else {
                "${month}月${day}日"
            }
        }
    }

    object Account {
        const val CASH = "CASH"
        const val BANK = "BANK"
        const val CREDIT = "CREDIT"
    }

    object DeepLink {
        const val SCHEME = "lexpense"
        const val HOST_QUICK_ADD = "quick_add"
        const val HOST_BUDGET_CENTER = "budget_center"
        const val HOST_TRANSACTIONS = "transactions"
        const val HOST_UPDATE = "update"

        const val PARAM_FILTER = "filter"
        const val PARAM_VERSION = "version"
        const val VALUE_RECURRING = "recurring"
    }

    /**
     * Google Play Store market intent and web URLs.
     */
    object GooglePlay {
        const val MARKET_DETAILS_PREFIX = "market://details?id="
        const val WEB_DETAILS_PREFIX = "https://play.google.com/store/apps/details?id="
    }

    /**
     * MIME types used for file system operations (Export/Import).
     */
    object MimeTypes {
        const val JSON = "application/json"
        const val CSV = "text/csv"
        const val PLAIN_TEXT = "text/plain"
        const val ANY_TEXT = "text/*"
        const val ANY = "*/*"
    }

    /**
     * File extensions used throughout the app.
     */
    object FileExtension {
        const val JSON = ".json"
        const val CSV = ".csv"
        const val APK = ".apk"
    }

    /**
     * File storage paths and provider authorities.
     */
    object Storage {
        const val EXPORTS_DIR = "exports"
        const val FILE_PROVIDER_SUFFIX = ".fileprovider"
        const val GOOGLE_DRIVE_BACKUP_FILE_NAME = "lexpense_backup.json"
    }

    /**
     * APM logging channels and level constants.
     */
    object Apm {
        const val CHANNEL_APP = "APP"
        const val CHANNEL_DB = "DB"
        const val CHANNEL_SYNC = "SYNC"
        const val CHANNEL_CRASH = "CRASH"

        const val LEVEL_DEBUG = "DEBUG"
        const val LEVEL_INFO = "INFO"
        const val LEVEL_WARN = "WARN"
        const val LEVEL_ERROR = "ERROR"
    }

    /**
     * Constants related to Data Export and Backups.
     */
    object Export {
        const val BACKUP_FILE_PREFIX = "lexpense_backup_"
        const val BACKUP_DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val DEFAULT_TYPE_FILTER = "ALL"
    }

    /**
     * Centralized content descriptions for accessibility and UI icons.
     */
    object ContentDescription {
        const val APM = "APM"
        const val APM_BUBBLE = "APM Bubble"
        const val COLLAPSE = "Collapse"
        const val PREV = "Prev"
        const val NEXT = "Next"
        const val MINUS = "-"
        const val PLUS = "+"
        const val SELECT_YEAR = "Select Year"
        const val PREV_DECADE = "Prev Dec"
        const val NEXT_DECADE = "Next Dec"
        const val APP_ICON = "lExpense"
        const val ADD = "Add"
        const val DELETE = "Delete"
        const val EDIT = "Edit"
        const val SAVE = "Save"
        const val SHARE = "Share"
        const val GOOGLE_AVATAR = "Google Avatar"
        const val LOGOUT = "Logout"
        const val LOGIN = "Login"
        const val BACK = "Back"
        const val CLOSE = "Close"
        const val OPS = "Ops"
        const val SEED = "Seed"
        const val CLEAR = "Clear"
        const val THEME = "Theme"
        const val DATA_CENTER = "Data Center"
        const val BACKUP = "Backup"
        const val RESTORE = "Restore"
        const val EXPORT_EXCEL = "Export Excel"
        const val EXPORT_JSON = "Export JSON"
        const val IMPORT_JSON = "Import JSON"
        const val FINANCE_RULES = "Finance Rules"
        const val BUDGET = "Budget"
        const val RECURRING = "Recurring"
        const val CATEGORIES = "Categories"
        const val ACCOUNTS = "Accounts"
        const val NOTIFICATIONS = "Notifications"
        const val SECURITY = "Security"
        const val UPDATE = "Update"
        const val NEW_VERSION = "New Version"
        const val VIEW_IN_TRANSACTIONS = "View in Transactions"
        const val TOGGLE_AMOUNT = "Toggle Amount"
        const val TOGGLE_BALANCE = "Toggle Balance"
        const val EDIT_BUDGET = "Edit Budget"
        const val MANAGE_CATEGORIES = "Manage Categories"
        const val DATE = "Date"
    }
}
