package com.listen.expensetracker.data.backup

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Backup and Export/Import Engine for Transactions (JSON and CSV formats).
 */
object TransactionBackupManager {

    fun exportToJson(transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        transactions.forEachIndexed { index, tx ->
            sb.append("  {\n")
            sb.append("    \"id\": \"${escapeJson(tx.id)}\",\n")
            sb.append("    \"type\": \"${escapeJson(tx.type)}\",\n")
            sb.append("    \"categoryId\": \"${escapeJson(tx.categoryId)}\",\n")
            sb.append("    \"categoryName\": \"${escapeJson(tx.categoryName)}\",\n")
            sb.append("    \"categoryIcon\": \"${escapeJson(tx.categoryIcon)}\",\n")
            sb.append("    \"categoryColorHex\": \"${escapeJson(tx.categoryColorHex)}\",\n")
            sb.append("    \"amount\": ${tx.amount},\n")
            sb.append("    \"note\": \"${escapeJson(tx.note)}\",\n")
            sb.append("    \"accountType\": \"${escapeJson(tx.accountType)}\",\n")
            sb.append("    \"timestamp\": ${tx.timestamp}\n")
            sb.append("  }")
            if (index < transactions.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("]")
        return sb.toString()
    }

    fun importFromJson(jsonStr: String): List<TransactionEntity> {
        val list = mutableListOf<TransactionEntity>()
        val trimmed = jsonStr.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()

        val objectRegex = Regex("""\{([^}]+)\}""")
        val matches = objectRegex.findAll(trimmed)

        for (match in matches) {
            val content = match.groupValues[1]
            val map = parseJsonObjectContent(content)
            if (map.isNotEmpty()) {
                val tx = TransactionEntity(
                    id = map["id"] ?: UUID.randomUUID().toString(),
                    type = map["type"] ?: TransactionType.EXPENSE,
                    categoryId = map["categoryId"] ?: "c_other_exp",
                    categoryName = map["categoryName"] ?: "其他",
                    categoryIcon = map["categoryIcon"] ?: "c_other_exp",
                    categoryColorHex = map["categoryColorHex"] ?: "#6B7280",
                    amount = map["amount"]?.toDoubleOrNull() ?: 0.0,
                    note = map["note"] ?: "",
                    accountType = map["accountType"] ?: "CASH",
                    timestamp = map["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()
                )
                list.add(tx)
            }
        }
        return list
    }

    private fun parseJsonObjectContent(content: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val pairRegex = Regex(""""([^"]+)"\s*:\s*("(?:\\.|[^"\\])*"|[\d.-]+|true|false|null)""")
        for (match in pairRegex.findAll(content)) {
            val key = match.groupValues[1]
            var rawVal = match.groupValues[2].trim()
            if (rawVal.startsWith("\"") && rawVal.endsWith("\"")) {
                rawVal = unescapeJson(rawVal.substring(1, rawVal.length - 1))
            }
            map[key] = rawVal
        }
        return map
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(str: String): String {
        return str
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    fun exportToCsv(transactions: List<TransactionEntity>, lang: String = "zh"): String {
        val sb = StringBuilder()
        val header = when (lang.lowercase()) {
            "en" -> "ID,Type,Category,Amount,Account,Note,Date\n"
            "ja" -> "ID,種類,カテゴリー,金額,口座,メモ,日時\n"
            else -> "ID,类型,分类,金额,账户,备注,时间\n"
        }
        sb.append(header)
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        transactions.forEach { tx ->
            val isExpense = tx.type == TransactionType.EXPENSE
            val typeStr = when (lang.lowercase()) {
                "en" -> if (isExpense) "Expense" else "Income"
                "ja" -> if (isExpense) "支出" else "収入"
                else -> if (isExpense) "支出" else "收入"
            }
            val timeStr = sdf.format(Date(tx.timestamp))
            val cleanNote = tx.note.replace(",", " ")
            sb.append("${tx.id},$typeStr,${tx.categoryName},${tx.amount},${tx.accountType},$cleanNote,$timeStr\n")
        }
        return sb.toString()
    }
}
