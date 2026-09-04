package com.listen.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 周期性规则执行频次枚举 (RecurringFrequency)。
 */
enum class RecurringFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

/**
 * 周期性规则履约模式 (ExecutionType)。
 */
enum class ExecutionType {
    AUTO_INSERT,
    NOTIFY_CONFIRM
}

/**
 * 周期性收支与订阅规则实体 (RecurringRuleEntity)。
 */
@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String = TransactionType.EXPENSE,
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val amount: Double,
    val accountType: String = "CASH",
    val note: String = "",
    val frequency: RecurringFrequency = RecurringFrequency.MONTHLY,
    val dayOfPeriod: Int = 1,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val lastExecutionDate: Long? = null,
    val nextExecutionDate: Long = System.currentTimeMillis(),
    val executionType: ExecutionType = ExecutionType.AUTO_INSERT,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
