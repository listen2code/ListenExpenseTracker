package com.listen.expensetracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 记账核心领域实体 (TransactionEntity) - 对应 SQLite 数据表 "transactions"。
 *
 * 
 * 1. [Entity] 与 [PrimaryKey]：Room 数据库核心注解，标记此数据类为关系数据库中的一张表。
 * 2. 字段设计保持最小化不可变状态（val），使用 UUID 生成分布式唯一主键，配合毫秒级时间戳支撑时间轴排序。
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String, // 交易类型：取值来源于 TransactionType.EXPENSE 或 TransactionType.INCOME
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColorHex: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val accountType: String = "CASH"
)

/**
 * 交易类型常量收口对象 (TransactionType)。
 *
 * 统一集中收口支出、收入、全部过滤类型的常量值。既能在 Kotlin 编译期避免拼写错误，
 * 又能直接作为 Room 数据库中的列值，无须编写额外的 TypeConverter，保持轻量高效。
 */
object TransactionType {
    const val EXPENSE = "EXPENSE" // 支出
    const val INCOME = "INCOME"   // 收入
    const val ALL = "ALL"         // 全部（筛选过滤用）
}
