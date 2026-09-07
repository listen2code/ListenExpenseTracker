package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.uicomponent.charts.LineChartPoint
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import java.util.Calendar

/**
 * 年度维度计算结果模型。
 */
data class AnnualCalculationResult(
    val filteredTransactions: List<TransactionEntity>,
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double,
    val monthlyAverageExpense: Double,
    val monthlyAverageIncome: Double,
    val categoryShares: List<PieChartItem>,
    val progressSegments: List<ProgressSegment>,
    val incomeCategoryShares: List<PieChartItem>,
    val incomeProgressSegments: List<ProgressSegment>,
    val monthlyTrendPoints: List<LineChartPoint>,
    val annualSummaries: List<AnnualMonthSummary>,
    val maxExpenseTransaction: TransactionEntity?,
    val maxIncomeTransaction: TransactionEntity?,
    val yearTitle: String,
    val year: Int
)

/**
 * 年度财务数据分析与多维指标聚合引擎 (AnnualCalculationEngine)。
 * 专注年维度的毫秒区间换算、全年各大分类占比、12个月月度走势与年度月均收支计算。
 */
object AnnualCalculationEngine {

    /**
     * 计算指定年份偏移量的毫秒时间戳起止区间及显示标题。
     * @param yearOffset 相对当前年份的偏移量，0 表示今年，-1 表示去年
     */
    fun getYearRangeAndTitle(yearOffset: Int, lang: String = "zh"): Triple<Long, Long, String> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.YEAR, yearOffset)
        }
        val targetYear = cal.get(Calendar.YEAR)

        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTs = startCal.timeInMillis

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, Calendar.DECEMBER)
            set(Calendar.DAY_OF_MONTH, 31)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        val endTs = endCal.timeInMillis

        val title = if (yearOffset == 0) {
            when (lang.lowercase()) {
                "en" -> "This Year ($targetYear)"
                "ja" -> "今年 (${targetYear}年)"
                else -> "今年 (${targetYear}年)"
            }
        } else {
            when (lang.lowercase()) {
                "en" -> "$targetYear"
                "ja" -> "${targetYear}年"
                else -> "${targetYear}年"
            }
        }

        return Triple(startTs, endTs, title)
    }

    /**
     * 针对年度区间进行账单筛选与统计聚合运算。
     */
    fun filterAndCalculateYear(
        allList: List<TransactionEntity>,
        yearOffset: Int,
        lang: String = "zh"
    ): AnnualCalculationResult {
        val (startTs, endTs, title) = getYearRangeAndTitle(yearOffset, lang)
        val cal = Calendar.getInstance().apply { add(Calendar.YEAR, yearOffset) }
        val targetYear = cal.get(Calendar.YEAR)

        val yearFiltered = allList.filter { it.timestamp in startTs..endTs }
            .sortedByDescending { it.timestamp }

        val totalExp = yearFiltered.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalInc = yearFiltered.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        val expenseShares = TransactionCalculationEngine.calculateCategoryShares(
            yearFiltered.filter { it.type == TransactionType.EXPENSE }, totalExp
        )
        val expenseSegments = expenseShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val incomeShares = TransactionCalculationEngine.calculateCategoryShares(
            yearFiltered.filter { it.type == TransactionType.INCOME }, totalInc
        )
        val incomeSegments = incomeShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val maxExpenseTx = yearFiltered.filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amount }
        val maxIncomeTx = yearFiltered.filter { it.type == TransactionType.INCOME }.maxByOrNull { it.amount }

        val monthlyAvgExp = totalExp / 12.0
        val monthlyAvgInc = totalInc / 12.0

        val summaries = calculateAnnualSummaries(allList, targetYear, lang)

        val trendPoints = summaries.map { summary ->
            LineChartPoint(
                label = summary.monthLabel,
                value = summary.totalExpense,
                subLabel = "${targetYear}-${summary.monthIndex}"
            )
        }

        return AnnualCalculationResult(
            filteredTransactions = yearFiltered,
            totalExpense = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
            monthlyAverageExpense = monthlyAvgExp,
            monthlyAverageIncome = monthlyAvgInc,
            categoryShares = expenseShares,
            progressSegments = expenseSegments,
            incomeCategoryShares = incomeShares,
            incomeProgressSegments = incomeSegments,
            monthlyTrendPoints = trendPoints,
            annualSummaries = summaries,
            maxExpenseTransaction = maxExpenseTx,
            maxIncomeTransaction = maxIncomeTx,
            yearTitle = title,
            year = targetYear
        )
    }

    /**
     * 计算目标年份全 12 个月的月度收支汇总，用于生成柱状图及趋势点位。
     */
    fun calculateAnnualSummaries(
        allTransactions: List<TransactionEntity>,
        targetYear: Int,
        lang: String = "zh"
    ): List<AnnualMonthSummary> {
        return (0..11).map { monthIdx ->
            val monthCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, targetYear)
                set(Calendar.MONTH, monthIdx)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTs = monthCal.timeInMillis
            val maxDay = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            monthCal.set(Calendar.DAY_OF_MONTH, maxDay)
            monthCal.set(Calendar.HOUR_OF_DAY, 23)
            monthCal.set(Calendar.MINUTE, 59)
            monthCal.set(Calendar.SECOND, 59)
            monthCal.set(Calendar.MILLISECOND, 999)
            val endTs = monthCal.timeInMillis

            val monthTxs = allTransactions.filter { it.timestamp in startTs..endTs }
            val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

            val label = when (lang.lowercase()) {
                "en" -> when (monthIdx) {
                    0 -> "Jan"; 1 -> "Feb"; 2 -> "Mar"; 3 -> "Apr"; 4 -> "May"; 5 -> "Jun"
                    6 -> "Jul"; 7 -> "Aug"; 8 -> "Sep"; 9 -> "Oct"; 10 -> "Nov"; else -> "Dec"
                }
                "ja" -> "${monthIdx + 1}月"
                else -> "${monthIdx + 1}月"
            }

            AnnualMonthSummary(
                monthIndex = monthIdx + 1,
                monthLabel = label,
                totalExpense = exp,
                totalIncome = inc,
                netBalance = inc - exp
            )
        }
    }

    /**
     * 过滤指定年份与目标分类的账单，并计算收支总额与年份标题。
     */
    fun filterAnnualCategory(
        allList: List<TransactionEntity>,
        year: Int,
        categoryName: String,
        categoryId: String?,
        lang: String = "zh"
    ): AnnualCategoryFilterResult {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val (startTs, endTs, yearTitle) = getYearRangeAndTitle(year - currentYear, lang)
        val yearTxs = allList.filter { it.timestamp in startTs..endTs }
        val catFilter = categoryId ?: categoryName
        val matched = yearTxs.filter {
            it.categoryName.equals(catFilter, true) || it.categoryId.equals(catFilter, true) ||
            it.categoryName.equals(categoryName, true)
        }.sortedByDescending { it.timestamp }
        val totalExp = matched.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalInc = matched.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        return AnnualCategoryFilterResult(matched, totalExp, totalInc, yearTitle)
    }
}

/**
 * 年度分类过滤结果模型。
 */
data class AnnualCategoryFilterResult(
    val transactions: List<TransactionEntity>,
    val totalExpense: Double,
    val totalIncome: Double,
    val yearTitle: String
)
