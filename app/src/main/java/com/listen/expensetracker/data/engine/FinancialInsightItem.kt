package com.listen.expensetracker.data.engine

/**
 * 洞察严重等级枚举，对应卡片的情感化着色与优先级。
 */
enum class InsightSeverity {
    INFO,       // 信息提示 (主题蓝)
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
