package com.listen.expensetracker.data.model

/**
 * 预算健康状况枚举 (BudgetHealthStatus)。
 */
enum class BudgetHealthStatus {
    NORMAL,      // 正常 (支出 < 80% 预算)
    WARNING,     // 预警 (80% <= 支出 < 100% 预算)
    OVERBUDGET   // 超支 (支出 >= 100% 预算)
}

/**
 * 分类预算配置数据模型 (CategoryBudgetConfig)。
 * 记录总预算及各分类的占比分配。
 *
 * @param totalBudget 月度总预算金额
 * @param categoryRatios 各分类预算占比映射 (categoryId -> 比例, 如 "c_food" -> 0.35f)
 */
data class CategoryBudgetConfig(
    val totalBudget: Double = 5000.0,
    val categoryRatios: Map<String, Float> = defaultRatios
) {
    companion object {
        val defaultRatios: Map<String, Float> = mapOf(
            "c_food" to 0.30f,
            "c_housing" to 0.25f,
            "c_shopping" to 0.15f,
            "c_transport" to 0.10f,
            "c_entertainment" to 0.05f,
            "c_medical" to 0.05f,
            "c_other_exp" to 0.10f
        )
    }

    fun getRatio(categoryId: String): Float = categoryRatios[categoryId] ?: 0f

    fun getBudgetAmount(categoryId: String): Double = totalBudget * getRatio(categoryId)
}

/**
 * 分类预算实时执行状态 (CategoryBudgetStatus)。
 *
 * @param category 消费分类
 * @param budgetAmount 分配的预算金额
 * @param spentAmount 当月实际已发生支出
 * @param ratio 占总预算的比例
 * @param usageRatio 预算使用率 (spent / budget)
 * @param remainingAmount 剩余预算 (若超支则为负数)
 * @param status 预算健康度
 */
data class CategoryBudgetStatus(
    val category: Category,
    val budgetAmount: Double,
    val spentAmount: Double,
    val ratio: Float,
    val usageRatio: Float,
    val remainingAmount: Double,
    val status: BudgetHealthStatus
)
