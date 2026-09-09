package com.listen.expensetracker.data.engine

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import java.util.Calendar
import kotlin.math.abs


/**
 * 智能财务洞察与深度环比诊断核心引擎 (FinancialInsightEngine)。
 * 负责当月 vs 上月环比分析、预算消耗速率预测、分类异动突增排查与全年收支总览计算。
 * 
 * 设计模式：使用了“检测策略模式”(Detection Strategy Pattern)。
 * 每一种洞察指标（如赤字、总额突增、分类主导等）都是一项独立无干扰的检测规则，保障了策略易于拔插及后续扩展。
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

        val currentMonthTxs = allTransactions.filter { it.timestamp in currentStart..currentEnd }
        val currentExpenses = currentMonthTxs.filter { it.type == TransactionType.EXPENSE }
        val currentIncomes = currentMonthTxs.filter { it.type == TransactionType.INCOME }
        val prevExpenses = allTransactions.filter { it.timestamp in prevStart..prevEnd && it.type == TransactionType.EXPENSE }

        val currentTotal = currentExpenses.sumOf { it.amount }
        val prevTotal = prevExpenses.sumOf { it.amount }
        val currentIncomeTotal = currentIncomes.sumOf { it.amount }

        // 1. 收支结余率与赤字分析 (Savings Rate & Surplus/Deficit)
        FinancialInsightDetectors.detectSavingsRate(currentIncomeTotal, currentTotal, currencySymbol, lang)?.let {
            insights.add(it)
        }

        // 2. 月环比总支出对比 (MoM Total Expense Analysis)
        // 通过对比 (当月支出 - 上月支出) / 上月支出 计算开销变化幅度，超 ±12% 时进行阈值洞察告警。
        if (prevTotal > 0 && currentTotal > 0) {
            val diff = (currentTotal - prevTotal) / prevTotal
            val pctStr = abs(diff * 100).formatPercentage()
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

        // 3. 预算消耗速率预测与节流表现 (Burn Rate & Frugal Progress)
        // 烧钱率预测算法：(当月已用总额 / 当前已过去的自然天数) * 当月总天数，由此动态预估月底花费是否会超预算。
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
                        description = AppStrings.INSIGHT_BURN_RATE_DESC.tr(lang).format("$currencySymbol${dailyAvg.formatAmount()}", exhaustedDay),
                        severity = InsightSeverity.WARNING,
                        isBudgetAction = true
                    )
                )
            } else if (currentDay >= 8 && estimatedTotal <= monthlyBudget * 0.70 && currentTotal < monthlyBudget) {
                insights.add(
                    FinancialInsightItem(
                        id = "insight_budget_frugal",
                        title = AppStrings.INSIGHT_BUDGET_FRUGAL_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_BUDGET_FRUGAL_DESC.tr(lang).format(
                            (currentDay * 100) / maxDays,
                            ((currentTotal / monthlyBudget) * 100).formatPercentage()
                        ),
                        severity = InsightSeverity.POSITIVE,
                        isBudgetAction = true
                    )
                )
            }
        }

        // 4. 单项分类过度倾斜检测 (Category Dominance, >= 45%)
        val currentCatMap = currentExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } }
        val prevCatMap = prevExpenses.groupBy { it.categoryId }.mapValues { it.value.sumOf { tx -> tx.amount } }
        if (currentTotal > 100.0 && currentExpenses.size >= 3) {
            val dominant = currentCatMap.maxByOrNull { it.value }
            if (dominant != null && dominant.value >= currentTotal * 0.45) {
                val catName = currentExpenses.firstOrNull { it.categoryId == dominant.key }?.categoryName ?: dominant.key
                insights.add(
                    FinancialInsightItem(
                        id = "insight_cat_dominant_${dominant.key}",
                        title = AppStrings.INSIGHT_CAT_DOMINANT_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_CAT_DOMINANT_DESC.tr(lang).format(catName, ((dominant.value / currentTotal) * 100).formatPercentage()),
                        severity = InsightSeverity.WARNING,
                        categoryId = dominant.key,
                        isCategoryAction = true
                    )
                )
            }
        }

        // 5. 突发分类异动排查 (Category Spike Drilldown)
        // 检测某个单项分类在本月的总开支是否出现了异动突增（如超过上月同类别的 1.8倍）。
        for ((catId, amt) in currentCatMap) {
            val prevAmt = prevCatMap[catId] ?: 0.0
            if (prevAmt > 50.0 && amt > prevAmt * 1.8) {
                val catName = currentExpenses.firstOrNull { it.categoryId == catId }?.categoryName ?: catId
                insights.add(
                    FinancialInsightItem(
                        id = "insight_cat_jump_$catId",
                        title = AppStrings.INSIGHT_CAT_JUMP_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_CAT_JUMP_DESC.tr(lang).format(catName, "%.1f".format(amt / prevAmt), "$currencySymbol${amt.formatAmount()}"),
                        severity = InsightSeverity.INFO,
                        categoryId = catId,
                        isCategoryAction = true
                    )
                )
                break
            }
        }

        // 6. 周末 vs 工作日消费偏好 (Weekend vs Weekday Shift)
        FinancialInsightDetectors.detectWeekendSpendingShift(currentExpenses, currencySymbol, lang)?.let {
            insights.add(it)
        }

        // 7. 高频小额支出累积 (Latte Factor)
        FinancialInsightDetectors.detectLatteFactor(currentExpenses, currencySymbol, lang)?.let {
            insights.add(it)
        }

        // 8. 单日开销最大峰值检测 (Peak Spending Day)
        // 挑出本月消费占比极高（>=35%）的单日峰值作为关键开支溯源依据。
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
                insights.add(
                    FinancialInsightItem(
                        id = "insight_peak_day",
                        title = AppStrings.INSIGHT_PEAK_DAY_TITLE.tr(lang),
                        description = AppStrings.INSIGHT_PEAK_DAY_DESC.tr(lang).format(peakDay, "$currencySymbol${peakDayAmount.formatAmount()}"),
                        severity = InsightSeverity.INFO,
                        targetDay = peakDay,
                        targetDateLabel = "${month}月${peakDay}日"
                    )
                )
            }
        }

        // 9. 零支出自律天数达成 (No-Spend Discipline Days)
        FinancialInsightDetectors.detectNoSpendDays(currentExpenses, currentOffset, lang)?.let {
            insights.add(it)
        }

        // 10. 兜底提示卡片（当月数据平稳或记录较少时）
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
        val targetYear = Calendar.getInstance().apply { add(Calendar.MONTH, currentOffset) }.get(Calendar.YEAR)
        return AnnualCalculationEngine.calculateAnnualSummaries(allTransactions, targetYear, lang)
    }
}
