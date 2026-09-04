package com.listen.expensetracker.features.recurring.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.ExecutionType
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.RecurringTransactionEngine
import com.listen.expensetracker.data.model.CategoryRepository

const val MAX_RECURRING_AMOUNT = 999_999.99

/**
 * 周期性规则编辑表单状态持有者 (RecurringEditState)。
 */
class RecurringEditState(
    val initialRule: RecurringRuleEntity?,
    val lang: String
) {
    var title by mutableStateOf(initialRule?.title ?: "")
    var type by mutableStateOf(initialRule?.type ?: TransactionType.EXPENSE)
    var amountStr by mutableStateOf(initialRule?.amount?.let { "%.2f".format(it) } ?: "")
    var note by mutableStateOf(initialRule?.note ?: "")

    private val initialCats = if (type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
    var selectedCategoryId by mutableStateOf(initialRule?.categoryId ?: initialCats.first().id)
    var selectedAccount by mutableStateOf(initialRule?.accountType ?: "CASH")
    var frequency by mutableStateOf(initialRule?.frequency ?: RecurringFrequency.MONTHLY)
    var dayOfPeriod by mutableIntStateOf(initialRule?.dayOfPeriod ?: 1)
    var executionType by mutableStateOf(initialRule?.executionType ?: ExecutionType.AUTO_INSERT)

    val rawAmount: Double
        get() = amountStr.toDoubleOrNull() ?: 0.0

    val isValid: Boolean
        get() = title.isNotBlank() && rawAmount > 0 && rawAmount <= MAX_RECURRING_AMOUNT

    fun buildEntity(): RecurringRuleEntity {
        val amt = rawAmount.coerceIn(0.01, MAX_RECURRING_AMOUNT)
        val now = System.currentTimeMillis()
        val nextDate = initialRule?.nextExecutionDate ?: RecurringTransactionEngine.calculateNextExecutionDate(frequency, dayOfPeriod, now - 86400000L)
        val activeCats = if (type == TransactionType.EXPENSE) CategoryRepository.expenseCategories else CategoryRepository.incomeCategories
        val cat = activeCats.find { it.id == selectedCategoryId } ?: activeCats.first()
        return (initialRule ?: RecurringRuleEntity(
            title = title.trim(), categoryId = cat.id, categoryName = cat.getDisplayName(lang),
            categoryIcon = cat.id, categoryColorHex = cat.colorHex, amount = amt
        )).copy(
            title = title.trim(), type = type, categoryId = cat.id,
            categoryName = cat.getDisplayName(lang), categoryIcon = cat.id,
            categoryColorHex = cat.colorHex, amount = amt, accountType = selectedAccount,
            note = note.trim(), frequency = frequency, dayOfPeriod = dayOfPeriod,
            executionType = executionType, nextExecutionDate = nextDate
        )
    }
}
