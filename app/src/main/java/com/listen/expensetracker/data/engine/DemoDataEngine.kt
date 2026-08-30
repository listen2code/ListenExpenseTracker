package com.listen.expensetracker.data.engine

import com.listen.arch.i18n.tr
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
        val cal = Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
        }
        val maxDayInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val currentDay = if (monthOffset == 0) {
            Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceIn(1, maxDayInMonth)
        } else {
            maxDayInMonth
        }

        val count = Random.nextInt(14, 20)
        val generated = mutableListOf<TransactionEntity>()

        // 1. Generate 1-2 Income transactions
        val incomeCount = Random.nextInt(1, 3)
        for (i in 0 until incomeCount) {
            val incomeItem = incomeTemplates.random()
            val incAmt = Random.nextInt(incomeItem.minAmount, incomeItem.maxAmount).toDouble()
            val incDay = Random.nextInt(1, currentDay.coerceAtLeast(2))
            val incCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, incDay)
                set(Calendar.HOUR_OF_DAY, Random.nextInt(9, 18))
                set(Calendar.MINUTE, Random.nextInt(0, 59))
            }
            generated.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.INCOME,
                    categoryId = incomeItem.categoryId,
                    categoryName = incomeItem.categoryNameKey.tr(lang),
                    categoryIcon = incomeItem.categoryId,
                    categoryColorHex = incomeItem.colorHex,
                    amount = incAmt,
                    timestamp = incCal.timeInMillis,
                    note = incomeItem.notes.random(),
                    accountType = "BANK"
                )
            )
        }

        // 2. Generate varied Expense transactions
        val expenseCount = count - incomeCount
        for (i in 0 until expenseCount) {
            val exp = expenseTemplates.random()
            val amt = Random.nextInt(exp.minAmount, exp.maxAmount).toDouble()
            val expDay = Random.nextInt(1, (currentDay + 1).coerceAtLeast(2).coerceAtMost(maxDayInMonth + 1))
            val expCal = (cal.clone() as Calendar).apply {
                set(Calendar.DAY_OF_MONTH, expDay.coerceIn(1, maxDayInMonth))
                set(Calendar.HOUR_OF_DAY, Random.nextInt(7, 23))
                set(Calendar.MINUTE, Random.nextInt(0, 59))
            }
            generated.add(
                TransactionEntity(
                    id = UUID.randomUUID().toString(),
                    type = TransactionType.EXPENSE,
                    categoryId = exp.categoryId,
                    categoryName = exp.categoryNameKey.tr(lang),
                    categoryIcon = exp.categoryId,
                    categoryColorHex = exp.colorHex,
                    amount = amt,
                    timestamp = expCal.timeInMillis,
                    note = exp.notes.random(),
                    accountType = accounts.random()
                )
            )
        }

        return generated
    }
}
