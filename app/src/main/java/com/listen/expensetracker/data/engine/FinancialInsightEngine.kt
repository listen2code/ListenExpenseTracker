package com.listen.expensetracker.data.engine

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import java.util.Calendar
import kotlin.math.abs

/**
 * 洞察严重等级枚举，对应卡片的情感化着色与优先级。
 */
enum class InsightSeverity {
    INFO,       // 信息提示
    POSITIVE,   // 积极向好 (翡翠绿)
    WARNING,    // 预警注意 (琥珀黄)
    DANGER      // 危险超支 (珊瑚红)
}

/**
 * 智能财务洞察项领域实体。
 */
data class FinancialInsightItem(
    val id: String,
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val categoryId: String? = null,
    val targetDay: Int? = null,
    val targetDateLabel: String? = null,
    val diffPercentage: Float? = null,
    val isCategoryAction: Boolean = false,
    val isBudgetAction: Boolean = false
)

/**
 * 年度单月度汇总模型。
 */
data class AnnualMonthSummary(
    val monthIndex: Int,
    val monthLabel: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double
)

/**
 * 智能财务洞察与深度环比诊断核心引擎 (FinancialInsightEngine)。
 * 负责当月 vs 上月环比分析、预算消耗速率预测、分类异动突增排查与全年收支总览计算。
 */
object FinancialInsightEngine {

    fun generateInsights(
        allTransactions: List<TransactionEntity>,
        currentOffset: Int,
        monthlyBudget: Double,
        currencySymbol: String = "￥",
        lang: String = "zh"
    ): List<FinancialInsightItem> {
        val insights = mutableListOf<FinancialInsightItem>()

        val (currentStart, currentEnd, _) = TransactionCalculationEngine.getMonthRangeAndTitle(currentOffset, lang)
        val (prevStart, prevEnd, _) = TransactionCalculationEngine.getMonthRangeAndTitle(currentOffset - 1, lang)

        val currentExpenses = allTransactions.filter { it.timestamp in currentStart..currentEnd && it.type == TransactionType.EXPENSE }
        val prevExpenses = allTransactions.filter { it.timestamp in prevStart..prevEnd && it.type == TransactionType.EXPENSE }

        val currentTotal = currentExpenses.sumOf { it.amount }
        val prevTotal = prevExpenses.sumOf { it.amount }

        // 1. 月环比总支出对比 (MoM Total Expense Analysis)
        if (prevTotal > 0 && currentTotal > 0) {
            val diff = (currentTotal - prevTotal) / prevTotal
            val pctStr = "%.1f".format(abs(diff * 100))
            if (diff > 0.12) {
                insights.add(
                    FinancialInsightItem(
                        id = "insight_mom_increase",
                        title = AppStrings.INSIGHT_MOM_INC_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_MOM_INC_DESC.tr(lang).format(pctStr),
                        severity = InsightSeverity.WARNING,
                        diffPercentage = (diff * 100).toFloat()
                    )
                )
            } else if (diff < -0.12) {
                insights.add(
                    FinancialInsightItem(
                        id = "insight_mom_decrease",
                        title = AppStrings.INSIGHT_MOM_DEC_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_MOM_DEC_DESC.tr(lang).format(pctStr),
                        severity = InsightSeverity.POSITIVE,
                        diffPercentage = (diff * 100).toFloat()
                    )
                )
            }
        }

        // 2. 预算消耗速率预测 (Burn Rate Predictor)
        val nowCal = Calendar.getInstance()
        val currentDay = nowCal.get(Calendar.DAY_OF_MONTH)
        val maxDays = nowCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (currentOffset == 0 && monthlyBudget > 0 && currentDay in 3..(maxDays - 2)) {
            val dailyAvg = currentTotal / currentDay
            val estimatedTotal = dailyAvg * maxDays
            if (estimatedTotal > monthlyBudget && currentTotal < monthlyBudget) {
                val exhaustedDay = (monthlyBudget / dailyAvg).toInt().coerceIn(currentDay, maxDays)
                insights.add(
                    FinancialInsightItem(
                        id = "insight_burn_rate",
                        title = AppStrings.INSIGHT_BURN_RATE_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_BURN_RATE_DESC.tr(lang).format(
                            "$currencySymbol${dailyAvg.formatAmount()}",
                            exhaustedDay
                        ),
                        severity = InsightSeverity.WARNING,
                        isBudgetAction = true // 未来预测值不触发日期过滤，点击唤起月预算管理 (Rule 22)
                    )
                )
            }
        }

        // 3. 突发分类异动排查 (Category Spike Drilldown)
        val currentCatMap = currentExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } }
        val prevCatMap = prevExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } }
        for ((catId, amt) in currentCatMap) {
            val prevAmt = prevCatMap[catId] ?: 0.0
            if (prevAmt > 50.0 && amt > prevAmt * 1.8) {
                val catName = currentExpenses.firstOrNull { it.categoryId == catId }?.categoryName ?: catId
                val times = "%.1f".format(amt / prevAmt)
                insights.add(
                    FinancialInsightItem(
                        id = "insight_cat_jump_$catId",
                        title = AppStrings.INSIGHT_CAT_JUMP_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_CAT_JUMP_DESC.tr(lang).format(
                            catName,
                            times,
                            "$currencySymbol${amt.formatAmount()}"
                        ),
                        severity = InsightSeverity.INFO,
                        categoryId = catId,
                        isCategoryAction = true
                    )
                )
                break
            }
        }

        // 4. 单日开销最大峰值检测 (Peak Spending Day)
        val dayGroups = currentExpenses.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.DAY_OF_MONTH)
        }
        val maxDayEntry = dayGroups.maxByOrNull { entry -> entry.value.sumOf { it.amount } }
        if (maxDayEntry != null) {
            val peakDayAmount = maxDayEntry.value.sumOf { it.amount }
            if (peakDayAmount > 0 && currentTotal > 0 && peakDayAmount >= currentTotal * 0.35 && currentExpenses.size > 2) {
                val cal = Calendar.getInstance().apply { add(Calendar.MONTH, currentOffset) }
                val month = cal.get(Calendar.MONTH) + 1
                val peakDay = maxDayEntry.key
                val dateLabel = "${month}月${peakDay}日"
                insights.add(
                    FinancialInsightItem(
                        id = "insight_peak_day",
                        title = AppStrings.INSIGHT_PEAK_DAY_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_PEAK_DAY_DESC.tr(lang).format(
                            peakDay,
                            "$currencySymbol${peakDayAmount.formatAmount()}"
                        ),
                        severity = InsightSeverity.INFO,
                        targetDay = peakDay,
                        targetDateLabel = dateLabel
                    )
                )
            }
        }

        // 兜底提示卡片（当月数据平稳或记录较少时）
        if (insights.isEmpty()) {
            val isOver = currentTotal > monthlyBudget && monthlyBudget > 0
            val statusTitle = if (isOver) AppStrings.INSIGHT_OVER_TITLE.tr(lang) else AppStrings.INSIGHT_STEADY_TITLE.tr(lang)
            val statusDesc = if (isOver) {
                AppStrings.INSIGHT_OVER_DESC.tr(lang).format("$currencySymbol${(currentTotal - monthlyBudget).formatAmount()}")
            } else {
                AppStrings.INSIGHT_STEADY_DESC.tr(lang)
            }
            insights.add(
                FinancialInsightItem(
                    id = "insight_steady_state",
                    title = statusTitle,
                    description = statusDesc,
                    severity = if (isOver) InsightSeverity.DANGER else InsightSeverity.POSITIVE
                )
            )
        }

        return insights
    }

    fun calculateAnnualOverview(
        allTransactions: List<TransactionEntity>,
        currentOffset: Int,
        lang: String = "zh"
    ): List<AnnualMonthSummary> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, currentOffset)
        }
        val targetYear = cal.get(Calendar.YEAR)
        return AnnualCalculationEngine.calculateAnnualSummaries(allTransactions, targetYear, lang)
    }
}
