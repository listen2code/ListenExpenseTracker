package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.model.AppConstants

/**
 * Localized static mock data constants for demo & simulation transactions.
 * Extracted from DemoDataEngine to keep logic decoupled and clean.
 */
object DemoDataStrings {

    fun getSalaryNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Monthly Salary"
        AppConstants.Language.JA -> "毎月の給与"
        else -> "月度薪资发放"
    }

    fun getEarbudsNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Noise Canceling Earbuds"
        AppConstants.Language.JA -> "ノイズキャンセリングイヤホン"
        else -> "降噪无线耳机"
    }

    fun getConcertNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Concert Tickets"
        AppConstants.Language.JA -> "コンサートチケット"
        else -> "演唱会门票"
    }

    fun getApparelNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Apparel Accessories"
        AppConstants.Language.JA -> "衣類・アクセサリー"
        else -> "日常服饰配件"
    }

    fun getStreamingNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "${AppConstants.RECURRING_TAG} Streaming Subscription"
        AppConstants.Language.JA -> "${AppConstants.RECURRING_TAG} サブスクリプション"
        else -> "${AppConstants.RECURRING_TAG} 流媒体月度订阅"
    }

    fun getSukiyakiNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Japanese Sukiyaki"
        AppConstants.Language.JA -> "すき焼き"
        else -> "日式寿喜烧"
    }

    fun getLatteNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Starbucks Latte"
        AppConstants.Language.JA -> "スターバックスラテ"
        else -> "星巴克拿铁"
    }

    fun getBentoNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Lunch Bento"
        AppConstants.Language.JA -> "お弁当"
        else -> "便当午餐"
    }

    fun getSubwayNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Subway Commute"
        AppConstants.Language.JA -> "地下鉄通勤"
        else -> "地铁通勤"
    }

    fun getFruitTeaNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Fruit Tea"
        AppConstants.Language.JA -> "フルーツティー"
        else -> "下午茶果茶"
    }

    fun getBusNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "City Bus"
        AppConstants.Language.JA -> "市バス"
        else -> "公交出行"
    }

    fun getSnackNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Snack"
        AppConstants.Language.JA -> "お菓子"
        else -> "便利店零食"
    }

    fun getPastDiningNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Past Dining"
        AppConstants.Language.JA -> "先月の外食"
        else -> "上月日常餐饮"
    }

    fun getPastCommuteNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Past Commute"
        AppConstants.Language.JA -> "先月の交通費"
        else -> "上月交通出行"
    }

    fun getPastGroceriesNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Past Groceries"
        AppConstants.Language.JA -> "先月の日用品"
        else -> "上月日常百货"
    }

    fun getPastMovieNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Past Movie"
        AppConstants.Language.JA -> "先月の映画チケット"
        else -> "上月电影票"
    }

    fun getRentTitle(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Apartment Rent"
        AppConstants.Language.JA -> "家賃"
        else -> "住房租金"
    }

    fun getRentNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Monthly Rent"
        AppConstants.Language.JA -> "毎月1日の家賃"
        else -> "每月1日房租"
    }

    fun getNetflixTitle(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Netflix"
        AppConstants.Language.JA -> "Netflix 会員"
        else -> "Netflix 会员"
    }

    fun getNetflixNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Premium"
        AppConstants.Language.JA -> "プレミアムプラン"
        else -> "高级家庭套餐"
    }

    fun getSalaryTitle(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Monthly Salary"
        AppConstants.Language.JA -> "毎月の給与"
        else -> "每月薪资"
    }

    fun getSalaryRuleNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "Base Salary"
        AppConstants.Language.JA -> "基本給"
        else -> "固定工资发放"
    }

    fun getCloudTitle(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "iCloud Storage"
        AppConstants.Language.JA -> "iCloud ストレージ"
        else -> "iCloud 云存储"
    }

    fun getCloudNote(lang: String): String = when (lang.lowercase()) {
        AppConstants.Language.EN -> "200GB Plan"
        AppConstants.Language.JA -> "200GB プラン"
        else -> "200GB 空间"
    }
}
