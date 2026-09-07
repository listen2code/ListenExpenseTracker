package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.expensetracker.features.transactions.viewmodel.TransactionSortOrder
import com.listen.uicomponent.components.ProgressSegment
import java.util.Calendar

/**
 * 年度流水数据筛选与预算聚合引擎 (AnnualTransactionEngine)。
 * 专门处理年度维度下的流水列表过滤、关键字匹配、分类筛选、排序及年度预算对比。
 */
object AnnualTransactionEngine {

    fun filterAndCalculateYearTransactions(
        allList: List<TransactionEntity>,
        yearOffset: Int,
        query: String,
        accountFilter: String,
        budget: Double,
        sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
        currencySymbol: String = "￥",
        lang: String = "zh",
        typeFilter: String = "ALL",
        selectedCategories: Set<String> = emptySet(),
        amountPreset: AmountFilterPreset = AmountFilterPreset.ALL,
        customMinAmount: Double? = null,
        customMaxAmount: Double? = null
    ): CalculationResult {
        val (startTs, endTs, yearTitle) = AnnualCalculationEngine.getYearRangeAndTitle(yearOffset, lang)
        val cleanQuery = query.trim().lowercase()
        val yearFilteredList = allList.filter { it.timestamp in startTs..endTs }
        val activeCategories = selectedCategories

        val matchedFiltered = yearFilteredList.filter { item ->
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.timestamp }
            val itemYear = itemCal.get(Calendar.YEAR)
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
                "$itemYear".contains(cleanQuery) ||
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

        val expenseShares = TransactionCalculationEngine.calculateCategoryShares(finalSorted.filter { it.type == TransactionType.EXPENSE }, totalExp)
        val expenseSegments = expenseShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val incomeShares = TransactionCalculationEngine.calculateCategoryShares(finalSorted.filter { it.type == TransactionType.INCOME }, totalInc)
        val incomeSegments = incomeShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

        val maxExpenseTx = finalSorted.filter { it.type == TransactionType.EXPENSE }.maxByOrNull { it.amount }
        val maxIncomeTx = finalSorted.filter { it.type == TransactionType.INCOME }.maxByOrNull { it.amount }

        val annualBudget = budget * 12
        val ratio = if (annualBudget > 0) (totalExp / annualBudget).toFloat() else 0f

        return CalculationResult(
            filteredTransactions = finalSorted,
            totalExpense = totalExp,
            totalIncome = totalInc,
            netBalance = totalInc - totalExp,
            monthlyBudget = annualBudget,
            remainingBudget = (annualBudget - totalExp).coerceAtLeast(0.0),
            budgetUsageRatio = ratio,
            isOverBudget = totalExp > annualBudget,
            categoryShares = expenseShares,
            progressSegments = expenseSegments,
            incomeCategoryShares = incomeShares,
            incomeProgressSegments = incomeSegments,
            dailyTrendBars = emptyList(),
            dailyTrendPoints = emptyList(),
            dailyAverageExpense = totalExp / 365.0,
            dailyAverageIncome = totalInc / 365.0,
            maxExpenseTransaction = maxExpenseTx,
            maxIncomeTransaction = maxIncomeTx,
            monthTitle = yearTitle
        )
    }
}
