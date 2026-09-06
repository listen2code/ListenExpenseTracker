package com.listen.expensetracker.data.engine

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import java.util.Calendar
import java.util.UUID
import kotlin.random.Random

data class DemoTemplate(
    val categoryId: String,
    val categoryNameKey: String,
    val notes: List<String>,
    val minAmount: Int,
    val maxAmount: Int,
    val colorHex: String
)

/**
 * Rehearsal / Demo Data Generator Engine.
 * Generates realistic randomized transactions strictly bounded by the given monthOffset.
 */
object DemoDataEngine {

    private val expenseTemplates = listOf(
        DemoTemplate("c_food", AppStrings.CAT_FOOD, listOf("Lunch Bento", "McDonald's Meal", "Japanese Sukiyaki", "Hot Pot Feast", "Steak Dinner", "Craft Beer", "Brunch & Pancakes", "Subway Sandwich"), 18, 360, "#EF4444"),
        DemoTemplate("c_transport", AppStrings.CAT_TRANSPORT, listOf("Subway Commute", "Taxi Ride", "City Bus", "Uber Ride", "Gas Station Refuel", "Parking Fee"), 4, 220, "#3B82F6"),
        DemoTemplate("c_cafe", AppStrings.CAT_CAFE, listOf("Starbucks Latte", "Iced Americano", "Matcha Latte", "Caramel Macchiato", "Fruit Tea", "Cold Brew"), 12, 48, "#84CC16"),
        DemoTemplate("c_shopping", AppStrings.CAT_SHOPPING, listOf("Uniqlo Apparel", "Groceries & Snacks", "Digital Accessories", "Supermarket Run", "Noise Canceling Earbuds", "Skincare Products"), 39, 699, "#EC4899"),
        DemoTemplate("c_entertainment", AppStrings.CAT_ENTERTAINMENT, listOf("Movie Tickets", "Steam Game", "Concert Tickets", "Museum Admission", "Board Game Night"), 45, 380, "#8B5CF6"),
        DemoTemplate("c_fitness", AppStrings.CAT_FITNESS, listOf("Badminton Court", "Gym Day Pass", "Whey Protein Powder", "Running Shoes", "Swimming Session"), 30, 450, "#F59E0B"),
        DemoTemplate("c_pets", AppStrings.CAT_PETS, listOf("Cat Food Cans", "Pet Deworming", "Cat Litter Refill", "Pet Grooming & Spa"), 35, 300, "#14B8A6"),
        DemoTemplate("c_medical", AppStrings.CAT_MEDICAL, listOf("Cold & Flu Medicine", "Dental Cleaning", "Vitamin Supplements", "First Aid Supplies"), 20, 280, "#06B6D4")
    )

    private val incomeTemplates = listOf(
        DemoTemplate("c_salary", AppStrings.CAT_SALARY, listOf("Monthly Payroll", "Performance Bonus", "Consulting Fee"), 12000, 26000, "#10B981"),
        DemoTemplate("c_investment", AppStrings.CAT_INVESTMENT, listOf("Mutual Fund Dividend", "Stock Dividends", "Interest Payout"), 300, 3500, "#6366F1"),
        DemoTemplate("c_gift", AppStrings.CAT_GIFT, listOf("Birthday Gift Cash", "Holiday Bonus", "Lucky Draw Reward"), 200, 1000, "#F43F5E")
    )

    fun generate(
        monthOffset: Int,
        lang: String = "zh",
        accounts: List<String> = listOf("CASH", "BANK", "CREDIT")
    ): List<TransactionEntity> {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset) }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = if (monthOffset == 0) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDay)
        } else maxDay

        val generated = mutableListOf<TransactionEntity>()

        // 1. 生成 1~2 笔真实收入
        val incAmt = Random.nextInt(12000, 22000).toDouble()
        val incCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, Random.nextInt(1, currentDay.coerceAtLeast(2)))
            set(Calendar.HOUR_OF_DAY, 10); set(Calendar.MINUTE, 0)
        }
        generated.add(
            TransactionEntity(
                id = UUID.randomUUID().toString(), type = TransactionType.INCOME,
                categoryId = "c_salary", categoryName = AppStrings.CAT_SALARY.tr(lang),
                categoryIcon = "c_salary", categoryColorHex = "#10B981",
                amount = incAmt, timestamp = incCal.timeInMillis,
                note = if (lang == "zh") "月度薪资发放" else "Monthly Salary", accountType = "BANK"
            )
        )

        // 2. 注入针对 4 种告警状态的关键特征支出数据
        // 特征A：单日开销最大峰值 (Peak Day: >= 35% 总支出, 如 1280 元数码配件)
        val peakDay = if (currentDay >= 3) 2 else 1
        val peakCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, peakDay); set(Calendar.HOUR_OF_DAY, 14); set(Calendar.MINUTE, 30)
        }
        generated.add(
            TransactionEntity(
                id = UUID.randomUUID().toString(), type = TransactionType.EXPENSE,
                categoryId = "c_shopping", categoryName = AppStrings.CAT_SHOPPING.tr(lang),
                categoryIcon = "c_shopping", categoryColorHex = "#EC4899",
                amount = 1280.0, timestamp = peakCal.timeInMillis,
                note = if (lang == "zh") "降噪无线耳机" else "Noise Canceling Earbuds", accountType = "CREDIT"
            )
        )

        // 特征B：突发分类异动 (Category Spike: 娱乐消费达 480 元，结合上月仅 60 元触发 >1.8x)
        val spikeDay = if (currentDay >= 4) 3 else 1
        val spikeCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, spikeDay); set(Calendar.HOUR_OF_DAY, 19); set(Calendar.MINUTE, 45)
        }
        generated.add(
            TransactionEntity(
                id = UUID.randomUUID().toString(), type = TransactionType.EXPENSE,
                categoryId = "c_entertainment", categoryName = AppStrings.CAT_ENTERTAINMENT.tr(lang),
                categoryIcon = "c_entertainment", categoryColorHex = "#8B5CF6",
                amount = 480.0, timestamp = spikeCal.timeInMillis,
                note = if (lang == "zh") "演唱会门票" else "Concert Tickets", accountType = "BANK"
            )
        )

        // 3. 生成多笔日常随机分散支出 (共 14~18 笔，保证总额约 3000~3600 元，日均偏高触发预算预警)
        val dailyCount = Random.nextInt(12, 16)
        for (i in 0 until dailyCount) {
            val exp = expenseTemplates.random()
            val amt = Random.nextInt(exp.minAmount, (exp.maxAmount / 2).coerceAtLeast(exp.minAmount + 5)).toDouble()
            val expDay = Random.nextInt(1, (currentDay + 1).coerceAtMost(maxDay + 1))
            val expCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, expDay.coerceIn(1, maxDay))
                set(Calendar.HOUR_OF_DAY, Random.nextInt(7, 23))
                set(Calendar.MINUTE, Random.nextInt(0, 59))
            }
            generated.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(), type = TransactionType.EXPENSE,
                    categoryId = exp.categoryId, categoryName = exp.categoryNameKey.tr(lang),
                    categoryIcon = exp.categoryId, categoryColorHex = exp.colorHex,
                    amount = amt, timestamp = expCal.timeInMillis,
                    note = exp.notes.random(), accountType = accounts.random()
                )
            )
        }

        // 4. 生成周期订阅支出项
        val subDay = if (currentDay >= 5) 5 else 1
        val subCal = (cal.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, subDay); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
        }
        val subNote = if (lang == "zh") "[周期] 流媒体月度订阅" else if (lang == "ja") "[周期] サブスクリプション" else "[周期] Streaming Subscription"
        generated.add(
            TransactionEntity(
                id = UUID.randomUUID().toString(), type = TransactionType.EXPENSE,
                categoryId = "c_entertainment", categoryName = AppStrings.CAT_ENTERTAINMENT.tr(lang),
                categoryIcon = "c_entertainment", categoryColorHex = "#8B5CF6",
                amount = 45.0, timestamp = subCal.timeInMillis, note = subNote, accountType = "BANK"
            )
        )

        // 5. 跨月对比基准垫底生成：若为当月或特定月份，顺带注入上个月对比基准数据（支出总计约 1300 元）
        // 从而直接触发 MoM 环比上涨 (+150% > 12%) 与娱乐分类突增 (> 1.8x)
        val prevCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset - 1)
        }
        val prevMaxDay = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val prevBaselineList = listOf(
            Triple("c_food", 450.0, if (lang == "zh") "上月日常餐饮" else "Past Dining"),
            Triple("c_transport", 280.0, if (lang == "zh") "上月交通出行" else "Past Commute"),
            Triple("c_shopping", 450.0, if (lang == "zh") "上月日常百货" else "Past Groceries"),
            Triple("c_entertainment", 60.0, if (lang == "zh") "上月电影票" else "Past Movie")
        )
        prevBaselineList.forEachIndexed { idx, (catId, baseAmt, note) ->
            val bCal = (prevCal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, (idx * 5 + 3).coerceIn(1, prevMaxDay))
                set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
            }
            val tpl = expenseTemplates.find { it.categoryId == catId }
            generated.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(), type = TransactionType.EXPENSE,
                    categoryId = catId, categoryName = tpl?.categoryNameKey?.tr(lang) ?: catId,
                    categoryIcon = catId, categoryColorHex = tpl?.colorHex ?: "#3B82F6",
                    amount = baseAmt, timestamp = bCal.timeInMillis, note = note, accountType = "BANK"
                )
            )
        }

        return generated
    }

    fun generateDefaultRecurringRules(lang: String = "zh"): List<RecurringRuleEntity> {
        val now = System.currentTimeMillis()
        fun getNextExec(day: Int): Long = Calendar.getInstance().apply {
            add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, day); set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
        }.timeInMillis

        return listOf(
            RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                title = if (lang == "zh") "住房租金" else if (lang == "ja") "家賃" else "Apartment Rent",
                type = TransactionType.EXPENSE, categoryId = "c_shopping",
                categoryName = AppStrings.CAT_SHOPPING.tr(lang), categoryIcon = "c_shopping",
                categoryColorHex = "#EC4899", amount = 2600.0, accountType = "BANK",
                note = if (lang == "zh") "每月1日房租" else "Monthly Rent", frequency = RecurringFrequency.MONTHLY,
                dayOfPeriod = 1, startDate = now, nextExecutionDate = getNextExec(1), executionType = ExecutionType.AUTO_INSERT
            ),
            RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                title = if (lang == "zh") "Netflix 会员" else if (lang == "ja") "Netflix 会員" else "Netflix",
                type = TransactionType.EXPENSE, categoryId = "c_entertainment",
                categoryName = AppStrings.CAT_ENTERTAINMENT.tr(lang), categoryIcon = "c_entertainment",
                categoryColorHex = "#8B5CF6", amount = 45.0, accountType = "BANK",
                note = if (lang == "zh") "高级家庭套餐" else "Premium", frequency = RecurringFrequency.MONTHLY,
                dayOfPeriod = 5, startDate = now, nextExecutionDate = getNextExec(5), executionType = ExecutionType.AUTO_INSERT
            ),
            RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                title = if (lang == "zh") "每月薪资" else if (lang == "ja") "毎月の給与" else "Monthly Salary",
                type = TransactionType.INCOME, categoryId = "c_salary",
                categoryName = AppStrings.CAT_SALARY.tr(lang), categoryIcon = "c_salary",
                categoryColorHex = "#10B981", amount = 18000.0, accountType = "BANK",
                note = if (lang == "zh") "固定工资发放" else "Base Salary", frequency = RecurringFrequency.MONTHLY,
                dayOfPeriod = 10, startDate = now, nextExecutionDate = getNextExec(10), executionType = ExecutionType.NOTIFY_CONFIRM
            ),
            RecurringRuleEntity(
                id = UUID.randomUUID().toString(),
                title = if (lang == "zh") "iCloud 云存储" else if (lang == "ja") "iCloud ストレージ" else "iCloud Storage",
                type = TransactionType.EXPENSE, categoryId = "c_entertainment",
                categoryName = AppStrings.CAT_ENTERTAINMENT.tr(lang), categoryIcon = "c_entertainment",
                categoryColorHex = "#8B5CF6", amount = 21.0, accountType = "CREDIT",
                note = if (lang == "zh") "200GB 空间" else "200GB Plan", frequency = RecurringFrequency.MONTHLY,
                dayOfPeriod = 15, startDate = now, nextExecutionDate = getNextExec(15), executionType = ExecutionType.AUTO_INSERT
            )
        )
    }
}
