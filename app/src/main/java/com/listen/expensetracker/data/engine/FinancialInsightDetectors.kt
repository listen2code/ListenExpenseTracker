package com.listen.expensetracker.data.engine

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.i18n.AppStrings
import java.util.Calendar
import kotlin.math.abs

/**
 * 财务行为习惯与生活方式洞察规则检测器 (FinancialInsightDetectors)。
 *
 * 采用轻量级无状态的独立规则函数设计，专注以下四项核心维度：
 * 1. 储蓄率健康度与收支赤字检测 (detectSavingsRate)
 * 2. 周末报复性消费偏好检测 (detectWeekendSpendingShift)
 * 3. 微额高频「拿铁因子」累积检测 (detectLatteFactor)
 * 4. 零支出自律生活天数统计 (detectNoSpendDays)
 */
internal object FinancialInsightDetectors {

    /**
     * 1. 储蓄率健康度与赤字熔断检测
     *
     * 【精算算法】：
     * - 净结余 = 总收入 - 总支出
     * - 储蓄率 = 净结余 / 总收入
     * - 当储蓄率 >= 20% 时：判定为积极健康的财务状态 (POSITIVE)
     * - 当净结余 < 0 时：判定为入不敷出的超支赤字状态 (DANGER)
     * - 若介于 0% ~ 20% 之间：属于普通持平状态，不产生干扰性打扰卡片 (返回 null)
     *
     * @param currentIncomeTotal 当月总收入
     * @param currentTotal 当月总支出
     * @param currencySymbol 货币符号 (如 "￥")
     * @param lang 多语言代号
     */
    fun detectSavingsRate(
        currentIncomeTotal: Double,
        currentTotal: Double,
        currencySymbol: String,
        lang: String
    ): FinancialInsightItem? {
        // 无收入或无支出记录时，不具备统计学评估基准
        if (currentIncomeTotal <= 0 || currentTotal <= 0) return null

        val net = currentIncomeTotal - currentTotal
        val savingsRate = net / currentIncomeTotal

        return if (savingsRate >= 0.20) {
            // 达成 20% 稳健储蓄基准线
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
            // 支出超过收入，亮起最高级别赤字警报
            FinancialInsightItem(
                id = "insight_deficit",
                title = AppStrings.INSIGHT_DEFICIT_TITLE.tr(lang),
                description = AppStrings.INSIGHT_DEFICIT_DESC.tr(lang).format("$currencySymbol${abs(net).formatAmount()}"),
                severity = InsightSeverity.DANGER
            )
        } else null
    }

    /**
     * 2. 周末消费偏好分析 (Weekend vs Weekday Shift)
     *
     * 【行为金融学洞察】：
     * 分析用户是否存在工作日节约、周末发生“补偿性/报复性消费”的生活行为模式。
     *
     * 【判定门槛】：
     * - 当月支出流水笔数 >= 4（样本过少不具可信度）
     * - 周末日均消费 >= 工作日日均消费 * 1.6 倍
     * - 周末消费总额 > 100 元（过滤微额基数扰动，避免周末仅花 10 元工作日仅花 5 元的伪异常）
     */
    fun detectWeekendSpendingShift(
        currentExpenses: List<TransactionEntity>,
        currencySymbol: String,
        lang: String
    ): FinancialInsightItem? {
        if (currentExpenses.size < 4) return null

        val calCheck = Calendar.getInstance()
        var weekendSum = 0.0
        var weekdaySum = 0.0
        val weekendDays = mutableSetOf<Int>()
        val weekdayDays = mutableSetOf<Int>()

        // 遍历所有账单，按自然星期拆分至周末组与工作日组
        for (tx in currentExpenses) {
            calCheck.timeInMillis = tx.timestamp
            val dow = calCheck.get(Calendar.DAY_OF_WEEK)
            val dom = calCheck.get(Calendar.DAY_OF_MONTH)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) {
                weekendSum += tx.amount
                weekendDays.add(dom) // 记录发生消费的周末实际天数
            } else {
                weekdaySum += tx.amount
                weekdayDays.add(dom) // 记录发生消费的工作日实际天数
            }
        }

        // 两组必须均存在消费天数基底方可进行倍率对比
        if (weekendDays.isEmpty() || weekdayDays.isEmpty()) return null

        val weekendAvg = weekendSum / weekendDays.size
        val weekdayAvg = weekdaySum / weekdayDays.size

        // 触发条件：周末日均超工作日 1.6 倍，且周末总额突破 100 元
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

    /**
     * 3. 高频小额支出「拿铁因子」累积分析 (Latte Factor)
     *
     * 【经济学概念】：
     * “拿铁因子”指日常生活中看似不起眼的微小开销（如每日咖啡、奶茶、共享单车），
     * 虽单笔无痛但高频累积后往往占据可观的财务开支比例。
     *
     * 【判定门槛】：
     * - 单笔金额 <= 35.0 元（定义为微额日常消费）
     * - 当月微额消费笔数 >= 6 笔（体现高频习惯）
     */
    fun detectLatteFactor(
        currentExpenses: List<TransactionEntity>,
        currencySymbol: String,
        lang: String
    ): FinancialInsightItem? {
        val microTxs = currentExpenses.filter { it.amount <= 35.0 }
        if (microTxs.size >= 6) {
            val totalMicroAmount = microTxs.sumOf { it.amount }
            return FinancialInsightItem(
                id = "insight_latte_factor",
                title = AppStrings.INSIGHT_LATTE_FACTOR_TITLE.tr(lang),
                description = AppStrings.INSIGHT_LATTE_FACTOR_DESC.tr(lang).format(
                    microTxs.size,
                    "${currencySymbol}35",
                    "$currencySymbol${totalMicroAmount.formatAmount()}"
                ),
                severity = InsightSeverity.INFO
            )
        }
        return null
    }

    /**
     * 4. 零支出自律天数统计 (No-Spend Discipline Days)
     *
     * 【正向激励算法】：
     * 统计当前月内完全没有发生任何支出的健康自律天数。
     *
     * 【防未来时间误算边界】：
     * - 若当前为本月 (currentOffset == 0)：统计上限仅截至今天已过的自然天数 (DAY_OF_MONTH)；
     * - 若为历史历史月 (currentOffset < 0)：统计上限为该月全月自然总天数 (28~31天)；
     * - 当自律天数 >= 3 天时：授予积极自律卡片 (POSITIVE)。
     */
    fun detectNoSpendDays(
        currentExpenses: List<TransactionEntity>,
        currentOffset: Int,
        lang: String
    ): FinancialInsightItem? {
        if (currentExpenses.isEmpty()) return null

        val targetMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, currentOffset) }
        // 关键边界：本月不可将未来的未到天数计入“零支出天数”
        val effectiveMaxDay = if (currentOffset == 0) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        } else {
            targetMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        }

        // 提取产生过支出的所有日期集合 (1..31)
        val dayCal = Calendar.getInstance()
        val spendDays = currentExpenses.mapTo(mutableSetOf()) {
            dayCal.timeInMillis = it.timestamp
            dayCal.get(Calendar.DAY_OF_MONTH)
        }

        // 集合差集求出未发生支出的天数
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

