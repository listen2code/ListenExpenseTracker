package com.listen.expensetracker.data.engine

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.BudgetHealthStatus
import com.listen.expensetracker.data.model.Category
import com.listen.expensetracker.data.model.CategoryBudgetStatus
import com.listen.expensetracker.data.model.CategoryRepository

/**
 * 分类预算核算结果数据类 (CategoryBudgetCalculationResult)。
 */
data class CategoryBudgetCalculationResult(
    val totalBudget: Double,
    val totalSpent: Double,
    val remainingBudget: Double,
    val usageRatio: Float,
    val statusList: List<CategoryBudgetStatus>,
    val overBudgetCount: Int,
    val warningCount: Int,
    val normalCount: Int
)

/**
 * 分类预算核心核算引擎 (CategoryBudgetEngine)。
 * 负责聚合月度支出、对比分类预算并评估健康状况。
 */
object CategoryBudgetEngine {

    fun calculate(
        allTransactions: List<TransactionEntity>,
        currentOffset: Int,
        totalBudget: Double,
        categoryRatios: Map<String, Float>
    ): CategoryBudgetCalculationResult {
        val (startTs, endTs, _) = TransactionCalculationEngine.getMonthRangeAndTitle(currentOffset)
        val monthExpenses = allTransactions.filter {
            it.timestamp in startTs..endTs && it.type.equals(TransactionType.EXPENSE, ignoreCase = true)
        }

        val spentByCategory = monthExpenses.groupBy { it.categoryId }
            .mapValues { (_, txs) -> txs.sumOf { it.amount } }

        val expenseCategories = CategoryRepository.expenseCategories

        val statusList = expenseCategories.map { category ->
            val ratio = categoryRatios[category.id] ?: 0f
            val budgetAmount = totalBudget * ratio
            val spentAmount = spentByCategory[category.id] ?: 0.0
            val remaining = budgetAmount - spentAmount
            val usageRatio = if (budgetAmount > 0) (spentAmount / budgetAmount).toFloat() else if (spentAmount > 0) 2f else 0f
            val status = when {
                spentAmount >= budgetAmount && budgetAmount > 0 -> BudgetHealthStatus.OVERBUDGET
                budgetAmount == 0.0 && spentAmount > 0 -> BudgetHealthStatus.OVERBUDGET
                usageRatio >= 0.8f -> BudgetHealthStatus.WARNING
                else -> BudgetHealthStatus.NORMAL
            }
            CategoryBudgetStatus(
                category = category,
                budgetAmount = budgetAmount,
                spentAmount = spentAmount,
                ratio = ratio,
                usageRatio = usageRatio,
                remainingAmount = remaining,
                status = status
            )
        }.sortedWith(
            compareByDescending<CategoryBudgetStatus> { it.status == BudgetHealthStatus.OVERBUDGET }
                .thenByDescending { it.status == BudgetHealthStatus.WARNING }
                .thenByDescending { it.spentAmount }
        )

        val totalSpent = monthExpenses.sumOf { it.amount }
        val remainingBudget = totalBudget - totalSpent
        val totalUsageRatio = if (totalBudget > 0) (totalSpent / totalBudget).toFloat() else 0f

        return CategoryBudgetCalculationResult(
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            remainingBudget = remainingBudget,
            usageRatio = totalUsageRatio,
            statusList = statusList,
            overBudgetCount = statusList.count { it.status == BudgetHealthStatus.OVERBUDGET },
            warningCount = statusList.count { it.status == BudgetHealthStatus.WARNING },
            normalCount = statusList.count { it.status == BudgetHealthStatus.NORMAL }
        )
    }
}
