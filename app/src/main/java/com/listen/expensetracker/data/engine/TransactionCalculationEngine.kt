package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.charts.BarChartItem
import com.listen.uicomponent.charts.LineChartPoint
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.ProgressSegment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CalculationResult(
    val filteredTransactions: List<TransactionEntity>,
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double,
    val monthlyBudget: Double,
    val remainingBudget: Double,
    val budgetUsageRatio: Float,
    val isOverBudget: Boolean,
    val categoryShares: List<PieChartItem>,
    val progressSegments: List<ProgressSegment>,
    val incomeCategoryShares: List<PieChartItem>,
    val incomeProgressSegments: List<ProgressSegment>,
    val dailyTrendBars: List<BarChartItem>,
    val dailyTrendPoints: List<LineChartPoint>,
    val dailyAverageExpense: Double,
    val dailyAverageIncome: Double,
    val maxExpenseTransaction: TransactionEntity?,
    val maxIncomeTransaction: TransactionEntity?,
    val monthTitle: String
)

enum class AmountFilterPreset(val labelKey: String) {
    ALL("filter_amount_all"), SMALL_LT_50("filter_amount_small"),
    MEDIUM_50_500("filter_amount_medium"), LARGE_GT_500("filter_amount_large"), CUSTOM("filter_amount_custom")
}

object TransactionCalculationEngine {

    fun filterAndCalculate(
        allList: List<TransactionEntity>,
        currentOffset: Int,
        query: String,
        accountFilter: String,
        budget: Double,
        sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
        currencySymbol: String = "￥",
        lang: String = "zh",
        typeFilter: String = "ALL",
        selectedCategories: Set<String> = emptySet(),
        categoryFilter: String = "ALL",
        amountPreset: AmountFilterPreset = AmountFilterPreset.ALL,
        customMinAmount: Double? = null,
        customMaxAmount: Double? = null
    ): CalculationResult {
        val cleanQuery = query.trim().lowercase()
        val (startTs, endTs, title) = getMonthRangeAndTitle(currentOffset, lang)
        val monthFilteredList = allList.filter { it.timestamp in startTs..endTs }
        val activeCategories = if (selectedCategories.isNotEmpty()) selectedCategories else if (categoryFilter != "ALL") setOf(categoryFilter) else emptySet()

        val matchedFiltered = monthFilteredList.filter { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            val itemMonth = itemCal.get(Calendar.MONTH) + 1
            val itemDay = itemCal.get(Calendar.DAY_OF_MONTH)
            val dateLabelZh = "${itemMonth}月${itemDay}日"
            val matchesQuery = cleanQuery.isEmpty() ||
                    item.categoryName.lowercase().contains(cleanQuery) ||
                    item.note.lowercase().contains(cleanQuery) ||
                    item.accountType.lowercase().contains(cleanQuery) ||
                    "%.2f".format(item.amount).contains(cleanQuery) ||
                    item.amount.toLong().toString() == cleanQuery ||
                    dateLabelZh.contains(cleanQuery) ||
                    "%02d-%02d".format(itemMonth, itemDay).contains(cleanQuery) ||
                    "$itemMonth-$itemDay".contains(cleanQuery)
            val matchesAccount = accountFilter == "ALL" || item.accountType == accountFilter
            val matchesType = typeFilter == "ALL" || item.type.equals(typeFilter, ignoreCase = true)
            val matchesCategory = activeCategories.isEmpty() || activeCategories.contains("ALL") ||
                    activeCategories.any { catFilter ->
                        item.categoryName.equals(catFilter, ignoreCase = true) ||
                        item.categoryId.equals(catFilter, ignoreCase = true) ||
                        CategoryRepository.allCategories.any { cat ->
                            (cat.id.equals(catFilter, true) || cat.nameKey.equals(catFilter, true) || cat.customName.equals(catFilter, true)) &&
                            (item.categoryId.equals(cat.id, true) || item.categoryName.equals(cat.nameKey, true) || item.categoryName.equals(cat.customName, true))
                        }
                    }
            val matchesAmount = when (amountPreset) {
                AmountFilterPreset.ALL -> true
                AmountFilterPreset.SMALL_LT_50 -> item.amount < 50.0
                AmountFilterPreset.MEDIUM_50_500 -> item.amount in 50.0..500.0
                AmountFilterPreset.LARGE_GT_500 -> item.amount > 500.0
                AmountFilterPreset.CUSTOM -> {
                    (customMinAmount == null || item.amount >= customMinAmount) &&
                    (customMaxAmount == null || item.amount <= customMaxAmount)
                }
            }
            matchesQuery && matchesAccount && matchesType && matchesCategory && matchesAmount
        }

        val finalSorted = when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> matchedFiltered.sortedByDescending { it.timestamp }
            TransactionSortOrder.DATE_ASC -> matchedFiltered.sortedBy { it.timestamp }
            TransactionSortOrder.AMOUNT_DESC -> matchedFiltered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> matchedFiltered.sortedBy { it.amount }
        }

        val totalExp = finalSorted.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val totalInc = finalSorted.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

        val expenseShares = calculateCategoryShares(finalSorted.filter { it.type == TransactionType.EXPENSE }, totalExp)
        val expenseSegments = expenseShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val incomeShares = calculateCategoryShares(finalSorted.filter { it.type == TransactionType.INCOME }, totalInc)
        val incomeSegments = incomeShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val maxExpenseTx = finalSorted.filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amount }
        val maxIncomeTx = finalSorted.filter { it.type == TransactionType.INCOME }.maxByOrNull { it.amount }

        val daysInMonth = getDaysInMonth(currentOffset)
        val dailyAvgExp = if (daysInMonth > 0) totalExp / daysInMonth else 0.0
        val dailyAvgInc = if (daysInMonth > 0) totalInc / daysInMonth else 0.0

        val trendBars = calculateRecentDaysTrend(finalSorted.filter { it.type == TransactionType.EXPENSE })
        val trendPoints = calculateMonthDailyTrend(finalSorted.filter { it.type == TransactionType.EXPENSE }, currentOffset)
        val ratio = if (budget > 0) (totalExp / budget).toFloat() else 0f

        return CalculationResult(
            filteredTransactions = finalSorted,
            totalExpense = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
            monthlyBudget = budget,
            remainingBudget = (budget - totalExp).coerceAtLeast(0.0),
            budgetUsageRatio = ratio,
            isOverBudget = totalExp > budget,
            categoryShares = expenseShares,
            progressSegments = expenseSegments,
            incomeCategoryShares = incomeShares,
            incomeProgressSegments = incomeSegments,
            dailyTrendBars = trendBars,
            dailyTrendPoints = trendPoints,
            dailyAverageExpense = dailyAvgExp,
            dailyAverageIncome = dailyAvgInc,
            maxExpenseTransaction = maxExpenseTx,
            maxIncomeTransaction = maxIncomeTx,
            monthTitle = title
        )
    }

    fun calculateCategoryShares(items: List<TransactionEntity>, total: Double): List<PieChartItem> {
        if (total <= 0) return emptyList()
        return items
            .groupBy { it.categoryName to it.categoryColorHex }
            .map { (key, txs) ->
                val amount = txs.sumOf { it.amount }
                val percentage = (amount / total).toFloat()
                PieChartItem(
                    label = key.first,
                    colorHex = key.second,
                    value = amount,
                    percentage = percentage
                )
            }
            .sortedByDescending { it.value }
    }

    fun calculateRecentDaysTrend(expenses: List<TransactionEntity>): List<BarChartItem> {
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())
        val dayGroups = expenses.groupBy {
            sdf.format(Date(it.timestamp))
        }

        val result = mutableListOf<BarChartItem>()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, -i) }
            val dayKey = sdf.format(c.time)
            val sum = dayGroups[dayKey]?.sumOf { it.amount } ?: 0.0
            result.add(
                BarChartItem(
                    label = dayKey,
                    value = sum,
                    colorHex = "#3B82F6"
                )
            )
        }
        return result
    }

    fun calculateMonthDailyTrend(
        expenses: List<TransactionEntity>,
        offset: Int
    ): List<LineChartPoint> {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dayGroups = expenses.groupBy {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.DAY_OF_MONTH)
        }
        val month = cal.get(Calendar.MONTH) + 1
        val result = mutableListOf<LineChartPoint>()
        for (day in 1..maxDaysInMonth) {
            val sum = dayGroups[day]?.sumOf { it.amount } ?: 0.0
            result.add(LineChartPoint(label = "$day", value = sum, subLabel = "${month}月${day}日"))
        }
        return result
    }

    fun getMonthRangeAndTitle(offset: Int, lang: String = "zh"): Triple<Long, Long, String> {
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, offset)
            set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val startTs = cal.timeInMillis
        cal.add(Calendar.MONTH, 1); cal.add(Calendar.MILLISECOND, -1)
        val endTs = cal.timeInMillis

        val sdf = when (lang.lowercase()) {
            "en" -> SimpleDateFormat("MMM yyyy", Locale.ENGLISH)
            "ja" -> SimpleDateFormat("yyyy年MM月", Locale.JAPANESE)
            else -> SimpleDateFormat("yyyy年MM月", Locale.CHINESE)
        }
        val currentPrefix = when (lang.lowercase()) {
            "en" -> "This Month"
            "ja" -> "今月"
            else -> "本月"
        }
        val formatted = sdf.format(Date(startTs))
        val title = if (offset == 0) "$currentPrefix ($formatted)" else formatted

        return Triple(startTs, endTs, title)
    }

    fun getDaysInMonth(offset: Int): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
