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
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(8, maxDay)
        } else maxDay

        val generated = mutableListOf<TransactionEntity>()

        // 识别当月周末与工作日分布，确保跨周期行为特征稳定
        val checkCal = cal.clone() as Calendar
        val weekendDays = mutableListOf<Int>()
        val weekdayDays = mutableListOf<Int>()
        for (d in 1..currentDay) {
            checkCal.set(Calendar.DAY_OF_MONTH, d)
            val dow = checkCal.get(Calendar.DAY_OF_WEEK)
            if (dow == Calendar.SATURDAY || dow == Calendar.SUNDAY) weekendDays.add(d) else weekdayDays.add(d)
        }
        val wDay = weekendDays.firstOrNull() ?: 1
        val wkDay1 = weekdayDays.firstOrNull() ?: 2
        val wkDay2 = if (weekdayDays.size > 1) weekdayDays[1] else 3

        fun makeTx(
            catId: String, nameKey: String, colorHex: String, amt: Double,
            day: Int, h: Int, m: Int, note: String, isIncome: Boolean = false, acc: String = "BANK"
        ): TransactionEntity {
            val tCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, day.coerceIn(1, maxDay))
                set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m)
            }
            return TransactionEntity(
                id = UUID.randomUUID().toString(),
                type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                categoryId = catId, categoryName = nameKey.tr(lang),
                categoryIcon = catId, categoryColorHex = colorHex,
                amount = amt, timestamp = tCal.timeInMillis, note = note, accountType = acc
            )
        }

        // 1. 真实月度薪资发放 (16000元，结余率达 85% > 20%，触发 insight_savings_rate)
        generated.add(makeTx("c_salary", AppStrings.CAT_SALARY, "#10B981", 16000.0, wkDay1, 10, 0, if (lang == "zh") "月度薪资发放" else "Monthly Salary", isIncome = true))

        // 2. 周末峰值与大额异动 (触发 insight_peak_day, insight_weekend_shift, insight_category_dominant, insight_cat_jump)
        // 周末集中消费：数码购物 1350元 (占总支出 59% >= 45%) + 演唱会门票 480元 (较上月60元增长 8.7x > 1.8x)
        generated.add(makeTx("c_shopping", AppStrings.CAT_SHOPPING, "#EC4899", 1350.0, wDay, 14, 30, if (lang == "zh") "降噪无线耳机" else "Noise Canceling Earbuds", acc = "CREDIT"))
        generated.add(makeTx("c_entertainment", AppStrings.CAT_ENTERTAINMENT, "#8B5CF6", 480.0, wDay, 19, 45, if (lang == "zh") "演唱会门票" else "Concert Tickets"))

        // 3. 工作日日常支出与周期订阅
        generated.add(makeTx("c_shopping", AppStrings.CAT_SHOPPING, "#EC4899", 120.0, wkDay1, 15, 20, if (lang == "zh") "日常服饰配件" else "Apparel Accessories"))
        generated.add(makeTx("c_entertainment", AppStrings.CAT_ENTERTAINMENT, "#8B5CF6", 45.0, wkDay1, 9, 0, if (lang == "zh") "[周期] 流媒体月度订阅" else "[周期] Streaming Subscription"))
        generated.add(makeTx("c_food", AppStrings.CAT_FOOD, "#EF4444", 160.0, wkDay2, 19, 0, if (lang == "zh") "日式寿喜烧" else "Japanese Sukiyaki"))

        // 4. 注入 6 笔 <= 35 元的高频小额支出 (触发拿铁因子 insight_latte_factor)
        generated.add(makeTx("c_cafe", AppStrings.CAT_CAFE, "#84CC16", 22.0, wkDay1, 8, 30, if (lang == "zh") "星巴克拿铁" else "Starbucks Latte", acc = "CASH"))
        generated.add(makeTx("c_food", AppStrings.CAT_FOOD, "#EF4444", 28.0, wkDay1, 12, 15, if (lang == "zh") "便当午餐" else "Lunch Bento", acc = "CASH"))
        generated.add(makeTx("c_transport", AppStrings.CAT_TRANSPORT, "#3B82F6", 6.0, wkDay1, 18, 0, if (lang == "zh") "地铁通勤" else "Subway Commute", acc = "CASH"))
        generated.add(makeTx("c_cafe", AppStrings.CAT_CAFE, "#84CC16", 18.0, wkDay2, 14, 0, if (lang == "zh") "下午茶果茶" else "Fruit Tea", acc = "CASH"))
        generated.add(makeTx("c_transport", AppStrings.CAT_TRANSPORT, "#3B82F6", 12.0, wkDay2, 8, 45, if (lang == "zh") "公交出行" else "City Bus", acc = "CASH"))
        generated.add(makeTx("c_food", AppStrings.CAT_FOOD, "#EF4444", 15.0, wkDay2, 21, 30, if (lang == "zh") "便利店零食" else "Snack", acc = "CASH"))

        // 5. 跨月对比基准垫底生成：上月支出总计 1240 元，触发环比上涨 (+81.9% > 12%)
        val prevCal = Calendar.getInstance().apply { add(Calendar.MONTH, monthOffset - 1) }
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
