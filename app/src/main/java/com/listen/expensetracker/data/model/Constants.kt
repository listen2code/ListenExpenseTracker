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
     * MIME types used for file system operations (Export/Import).
     */
    object MimeTypes {
        const val JSON = "application/json"
        const val CSV = "text/csv"
        const val ANY_TEXT = "text/*"
        const val ANY = "*/*"
    }

    /**
     * Constants related to Data Export and Backups.
     */
    object Export {
        const val BACKUP_FILE_PREFIX = "lexpense_backup_"
        const val BACKUP_DATE_FORMAT = "yyyyMMdd_HHmmss"
        const val DEFAULT_TYPE_FILTER = "ALL"
    }
}
