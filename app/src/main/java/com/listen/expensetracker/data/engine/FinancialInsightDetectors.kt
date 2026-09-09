package com.listen.expensetracker.data.engine

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.i18n.AppStrings
import java.util.Calendar
import kotlin.math.abs

/**
 * 财务行为习惯与生活方式洞察规则检测器 (FinancialInsightDetectors)。
 * 负责周末消费偏好、小额高频拿铁因子、零支出自律天数以及储蓄率/赤字分析。
 */
internal object FinancialInsightDetectors {

    fun detectSavingsRate(
        currentIncomeTotal: Double,
        currentTotal: Double,
        currencySymbol: String,
        lang: String
    ): FinancialInsightItem? {
        if (currentIncomeTotal <= 0 || currentTotal <= 0) return null
        val net = currentIncomeTotal - currentTotal
        val savingsRate = net / currentIncomeTotal
        return if (savingsRate >= 0.20) {
            FinancialInsightItem(
                id = "insight_savings_rate",
                title = AppStrings.INSIGHT_SAVINGS_HEALTHY_TITLE.tr(lang),
                description = AppStrings.INSIGHT_SAVINGS_HEALTHY_DESC.tr(lang).format(
                    "$currencySymbol${currentIncomeTotal.formatAmount()}",
                    "$currencySymbol${currentTotal.formatAmount()}",
                    (savingsRate * 100).formatPercentage()
                ),
                severity = InsightSeverity.POSITIVE,
                diffPercentage = (savingsRate * 100).toFloat()
            )
        } else if (net < 0) {
            FinancialInsightItem(
                id = "insight_deficit",
                title = AppStrings.INSIGHT_DEFICIT_TITLE.tr(lang),
                description = AppStrings.INSIGHT_DEFICIT_DESC.tr(lang).format("$currencySymbol${abs(net).formatAmount()}"),
                severity = InsightSeverity.DANGER
            )
        } else null
    }

    fun detectWeekendSpendingShift(
        currentExpenses: List<TransactionEntity>,
        currencySymbol: String,
        lang: String
    ): FinancialInsightItem? {
        if (currentExpenses.size < 4) return null
        val calCheck = Calendar.getInstance()
        var weekendSum = 0.0; var weekdaySum = 0.0
        val weekendDays = mutableSetOf<Int>(); val weekdayDays = mutableSetOf<Int>()
        for (tx in currentExpenses) {
            calCheck.timeInMillis = tx.timestamp
            val dow = calCheck.get(Calendar.DAY_OF_WEEK); val dom = calCheck.get(Calendar.DAY_OF_MONTH)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                weekendSum += tx.amount; weekendDays.add(dom)
            } else {
                weekdaySum += tx.amount; weekdayDays.add(dom)
            }
        }
        if (weekendDays.isEmpty() || weekdayDays.isEmpty()) return null
        val weekendAvg = weekendSum / weekendDays.size
        val weekdayAvg = weekdaySum / weekdayDays.size
        if (weekendAvg >= weekdayAvg * 1.6 && weekendSum > 100.0) {
            return FinancialInsightItem(
                id = "insight_weekend_shift",
                title = AppStrings.INSIGHT_WEEKEND_SHIFT_TITLE.tr(lang),
                description = AppStrings.INSIGHT_WEEKEND_SHIFT_DESC.tr(lang).format(
                    "$currencySymbol${weekendAvg.formatAmount()}",
                    "$currencySymbol${weekdayAvg.formatAmount()}",
                    "%.1f".format(weekendAvg / weekdayAvg)
                ),
                severity = InsightSeverity.INFO
            )
        }
        return null
    }

    fun detectLatteFactor(
        currentExpenses: List<TransactionEntity>,
        currencySymbol: String,
        lang: String
    ): FinancialInsightItem? {
        val microTxs = currentExpenses.filter { it.amount <= 35.0 }
        if (microTxs.size >= 6) {
            return FinancialInsightItem(
                id = "insight_latte_factor",
                title = AppStrings.INSIGHT_LATTE_FACTOR_TITLE.tr(lang),
                description = AppStrings.INSIGHT_LATTE_FACTOR_DESC.tr(lang).format(
                    microTxs.size, "${currencySymbol}35", "$currencySymbol${microTxs.sumOf { it.amount }.formatAmount()}"
                ),
                severity = InsightSeverity.INFO
            )
        }
        return null
    }

    fun detectNoSpendDays(
        currentExpenses: List<TransactionEntity>,
        currentOffset: Int,
        lang: String
    ): FinancialInsightItem? {
        if (currentExpenses.isEmpty()) return null
        val targetMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, currentOffset) }
        val effectiveMaxDay = if (currentOffset == 0) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else targetMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayCal = Calendar.getInstance()
        val spendDays = currentExpenses.mapTo(mutableSetOf()) {
            dayCal.timeInMillis = it.timestamp; dayCal.get(Calendar.DAY_OF_MONTH)
        }
        val noSpendCount = (1..effectiveMaxDay).count { it !in spendDays }
        if (noSpendCount >= 3) {
            return FinancialInsightItem(
                id = "insight_no_spend_days",
                title = AppStrings.INSIGHT_NO_SPEND_TITLE.tr(lang),
                description = AppStrings.INSIGHT_NO_SPEND_DESC.tr(lang).format(noSpendCount),
                severity = InsightSeverity.POSITIVE
            )
        }
        return null
    }
}
