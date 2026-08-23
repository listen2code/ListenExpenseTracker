package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.charts.BarChartItem
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
    val dailyAverageExpense: Double,
    val dailyAverageIncome: Double,
    val maxExpenseTransaction: TransactionEntity?,
    val maxIncomeTransaction: TransactionEntity?,
    val monthTitle: String
)

object TransactionCalculationEngine {

    fun filterAndCalculate(
        allList: List<TransactionEntity>,
        currentOffset: Int,
        query: String,
        accountFilter: String,
        budget: Double,
        sortOrder: TransactionSortOrder,
        currencySymbol: String = "￥"
    ): CalculationResult {
        val cleanQuery = query.trim().lowercase()
        val (startTs, endTs, title) = getMonthRangeAndTitle(currentOffset)

        val monthFilteredList = if (currentOffset == 0 && cleanQuery.isBlank() && accountFilter == "ALL") {
            allList
        } else {
            allList.filter { it.timestamp in startTs..endTs }
        }

        val matchedFiltered = monthFilteredList.filter { item ->
            val matchesQuery = cleanQuery.isEmpty() ||
                    item.categoryName.lowercase().contains(cleanQuery) ||
                    item.note.lowercase().contains(cleanQuery)
            val matchesAccount = accountFilter == "ALL" || item.accountType == accountFilter
            matchesQuery && matchesAccount
        }

        val finalSorted = when (sortOrder) {
            TransactionSortOrder.DATE_DESC -> matchedFiltered.sortedByDescending { it.timestamp }
            TransactionSortOrder.DATE_ASC -> matchedFiltered.sortedBy { it.timestamp }
            TransactionSortOrder.AMOUNT_DESC -> matchedFiltered.sortedByDescending { it.amount }
            TransactionSortOrder.AMOUNT_ASC -> matchedFiltered.sortedBy { it.amount }
        }

        val totalExp = finalSorted.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalInc = finalSorted.filter { it.type == "INCOME" }.sumOf { it.amount }

        val expenseShares = calculateCategoryShares(finalSorted.filter { it.type == "EXPENSE" }, totalExp)
        val expenseSegments = expenseShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val incomeShares = calculateCategoryShares(finalSorted.filter { it.type == "INCOME" }, totalInc)
        val incomeSegments = incomeShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val maxExpenseTx = finalSorted.filter { it.type == "EXPENSE" }.maxByOrNull { it.amount }
        val maxIncomeTx = finalSorted.filter { it.type == "INCOME" }.maxByOrNull { it.amount }

        val daysInMonth = getDaysInMonth(currentOffset)
        val dailyAvgExp = if (daysInMonth > 0) totalExp / daysInMonth else 0.0
        val dailyAvgInc = if (daysInMonth > 0) totalInc / daysInMonth else 0.0

        val trendBars = calculateRecentDaysTrend(finalSorted.filter { it.type == "EXPENSE" })
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

    fun getMonthRangeAndTitle(offset: Int): Triple<Long, Long, String> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTs = cal.timeInMillis

        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        val endTs = cal.timeInMillis

        val sdf = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        val title = if (offset == 0) "本月 (${sdf.format(Date(startTs))})" else sdf.format(Date(startTs))

        return Triple(startTs, endTs, title)
    }

    fun getDaysInMonth(offset: Int): Int {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, offset)
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
}
