package com.listen.expensetracker.data.engine

/**
 * 洞察严重等级枚举 (InsightSeverity)。
 * 决定轮播卡片的情感化着色方案、主题图标以及用户心理警示程度。
 */
enum class InsightSeverity {
    /** 信息提示 (科技蓝)：中立数据发现，如分类异动、消费习惯统计、单日波峰溯源 */
    INFO,
    /** 积极向好 (翡翠绿)：正面财务表现，如健康储蓄率、环比节流明显、零支出自律达标 */
    POSITIVE,
    /** 预警注意 (琥珀黄)：潜在财务风险，如月度支出环比攀升、分类严重倾斜、烧钱率超预算预警 */
    WARNING,
    /** 危险超支 (珊瑚红)：严重财务警报，如当月支出超过收入陷入收支赤字、总支出击穿月预算 */
    DANGER
}

/**
 * 智能财务洞察项领域实体 (FinancialInsightItem)。
 *
 * 作为计算引擎与 UI 呈现层之间的统一数据契约模型，
 * 封装了单项财务诊断结论的标题、描述、严重等级以及穿透式交互跳转的路由元数据。
 *
 * @property id 洞察项唯一标识码 (如 "insight_savings_rate", "insight_mom_increase")
 * @property title 卡片主标题 (已完成多语言本地化)
 * @property description 详尽诊断分析文案 (包含具体金额、百分比、倍数等格式化参数)
 * @property severity 情感化严重等级，决定卡片背景渐变底色与高亮图标
 * @property categoryId 关联的分类 ID (用于分类异动时的一键穿透筛选)
 * @property targetDay 关键目标日期 (1~31，用于单日开销峰值时的一键定位跳转)
 * @property targetDateLabel 目标日期的易读文本标签 (如 "9月11日")
 * @property diffPercentage 浮点型差异百分比 (用于环比增减幅或储蓄率展示)
 * @property isCategoryAction 是否支持点击后穿透跳转至对应分类筛选
 * @property isBudgetAction 是否支持点击后唤起预算调整与规划弹窗
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
 * 年度单月度汇总模型 (AnnualMonthSummary)。
 *
 * 封装自然年内某一特定月份的收支汇总聚合数据，
 * 用于渲染全年 12 个月的月度柱状对比图与年度走势图表。
 *
 * @property monthIndex 月份自然索引 (1 表示 1月，12 表示 12月)
 * @property monthLabel 国际化月份短标签 (如 "1月"、"Jan")
 * @property totalExpense 该月份总支出金额
 * @property totalIncome 该月份总收入金额
 * @property netBalance 该月份收支净结余 (totalIncome - totalExpense)
 */
data class AnnualMonthSummary(
    val monthIndex: Int,
    val monthLabel: String,
    val totalExpense: Double,
    val totalIncome: Double,
    val netBalance: Double
)

