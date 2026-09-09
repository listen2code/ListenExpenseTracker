package com.listen.expensetracker.data.backup

import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
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

    /**
     * 导出标准 CSV 格式账单数据（原因：统一采用标准化英文字段与类型，消除多语言分支，保障跨系统、跨设备及数据分析工具的解析兼容性）。
     *
     * @param transactions 待导出的账单实体列表
     * @param lang 保留默认参数以保持调用兼容性（已废弃多语言分支，统一使用标准字段）
     */
    fun exportToCsv(transactions: List<TransactionEntity>, @Suppress("UNUSED_PARAMETER") lang: String? = null): String {
        val sb = StringBuilder()
        sb.append("ID,Type,Category,Amount,Account,Note,Date\n")
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        transactions.forEach { tx ->
            val timeStr = sdf.format(Date(tx.timestamp))
            val cleanNote = tx.note.replace(",", " ")
            sb.append("${tx.id},${tx.type},${tx.categoryName},${tx.amount},${tx.accountType},$cleanNote,$timeStr\n")
        }
        return sb.toString()
    }

    /**
     * 导出带有 UTF-8 BOM 头的 Excel 友好 CSV 字节流。
     * 包含 0xEF, 0xBB, 0xBF 字节序标记，确保 Windows/Mac/Android 下 Microsoft Excel、WPS、Numbers 打开 0 乱码。
     * 严格遵循 RFC 4180 CSV 转义规范。
     */
    fun exportToExcelCsv(
        transactions: List<TransactionEntity>,
        lang: String = "zh"
    ): ByteArray {
        val bos = ByteArrayOutputStream()
        // 写入 UTF-8 BOM (Byte Order Mark)，使 Excel/WPS 默认使用 UTF-8 编码加载中文与日文
        bos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        val writer = OutputStreamWriter(bos, Charsets.UTF_8)

        val header = when (lang.lowercase()) {
            "en" -> "Transaction ID,Date & Time,Type,Category,Amount,Account,Note"
            "ja" -> "取引ID,日時,種類,カテゴリ,金額,口座,メモ"
            else -> "交易单号,日期时间,类型,分类,金额,账户,备注"
        }
        writer.write(header + "\r\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        for (tx in transactions) {
            val dateStr = sdf.format(Date(tx.timestamp))
            val typeStr = when {
                tx.type == TransactionType.INCOME -> if (lang == "en") "Income" else if (lang == "ja") "収入" else "收入"
                else -> if (lang == "en") "Expense" else if (lang == "ja") "支出" else "支出"
            }
            val cleanCategory = escapeCsvField(tx.categoryName)
            val amountStr = String.format(Locale.US, "%.2f", tx.amount)
            val cleanAccount = escapeCsvField(tx.accountType)
            val cleanNote = escapeCsvField(tx.note)

            writer.write("${tx.id},$dateStr,$typeStr,$cleanCategory,$amountStr,$cleanAccount,$cleanNote\r\n")
        }
        writer.flush()
        return bos.toByteArray()
    }

    /**
     * 根据时间戳区间与收支类型过滤账单列表。
     */
    fun filterTransactions(
        transactions: List<TransactionEntity>,
        startTs: Long? = null,
        endTs: Long? = null,
        typeFilter: String = "ALL"
    ): List<TransactionEntity> {
        return transactions.filter { tx ->
            val matchTime = (startTs == null || tx.timestamp >= startTs) &&
                    (endTs == null || tx.timestamp <= endTs)
            val matchType = typeFilter == "ALL" || tx.type.equals(typeFilter, ignoreCase = true)
            matchTime && matchType
        }
    }

    /**
     * RFC 4180 CSV 单元格转义：对包含逗号、换行符或双引号的内容使用双引号包裹，内部双引号替换为两个双引号。
     */
    fun escapeCsvField(value: String): String {
        if (!value.contains(",") && !value.contains("\"") && !value.contains("\n") && !value.contains("\r")) {
            return value
        }
        return "\"" + value.replace("\"", "\"\"") + "\""
    }
}
