package com.listen.expensetracker

import com.listen.expensetracker.data.backup.TransactionBackupManager
import com.listen.expensetracker.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionBackupManagerTest {

    @Test
    fun testJsonExportAndImport() {
        val sampleList = listOf(
            TransactionEntity(
                id = "tx-100",
                type = "EXPENSE",
                categoryId = "c_food",
                categoryName = "餐饮",
                categoryIcon = "c_food",
                categoryColorHex = "#EF4444",
                amount = 128.5,
                note = "聚餐测试",
                accountType = "CASH",
                timestamp = 1723900000000L
            ),
            TransactionEntity(
                id = "tx-101",
                type = "INCOME",
                categoryId = "c_salary",
                categoryName = "工资",
                categoryIcon = "c_salary",
                categoryColorHex = "#10B981",
                amount = 20000.0,
                note = "月薪",
                accountType = "BANK",
                timestamp = 1723900000000L
            )
        )

        val json = TransactionBackupManager.exportToJson(sampleList)
        assertTrue(json.contains("tx-100"))
        assertTrue(json.contains("聚餐测试"))
        assertTrue(json.contains("20000.0"))

        val imported = TransactionBackupManager.importFromJson(json)
        assertEquals(2, imported.size)
        assertEquals("tx-100", imported[0].id)
        assertEquals("EXPENSE", imported[0].type)
        assertEquals(128.5, imported[0].amount, 0.001)
        assertEquals("聚餐测试", imported[0].note)

        assertEquals("tx-101", imported[1].id)
        assertEquals("INCOME", imported[1].type)
        assertEquals(20000.0, imported[1].amount, 0.001)
    }

    @Test
    fun testCsvExport() {
        val sampleList = listOf(
            TransactionEntity(
                id = "tx-200",
                type = "EXPENSE",
                categoryId = "c_transport",
                categoryName = "交通",
                categoryIcon = "c_transport",
                categoryColorHex = "#3B82F6",
                amount = 15.0,
                note = "打车",
                accountType = "CREDIT",
                timestamp = 1723900000000L
            )
        )

        val csv = TransactionBackupManager.exportToCsv(sampleList)
        assertTrue(csv.startsWith("ID,Type,Category,Amount,Account,Note,Date"))
        assertTrue(csv.contains("tx-200,EXPENSE,交通,15.0,CREDIT,打车"))
    }

    @Test
    fun testExcelCsvExportWithBomAndEscaping() {
        val sampleList = listOf(
            TransactionEntity(
                id = "tx-301",
                type = "EXPENSE",
                categoryId = "c_food",
                categoryName = "餐饮",
                categoryIcon = "c_food",
                categoryColorHex = "#EF4444",
                amount = 128.5,
                note = "聚餐, 带\"特殊\"字符\n换行",
                accountType = "WECHAT",
                timestamp = 1723900000000L
            ),
            TransactionEntity(
                id = "tx-302",
                type = "INCOME",
                categoryId = "c_salary",
                categoryName = "工资",
                categoryIcon = "c_salary",
                categoryColorHex = "#10B981",
                amount = 15000.0,
                note = "8月薪资",
                accountType = "BANK",
                timestamp = 1723901000000L
            )
        )

        // 1. 中文导出与 BOM 验证
        val bytesZh = TransactionBackupManager.exportToExcelCsv(sampleList, "zh")
        assertEquals(0xEF.toByte(), bytesZh[0])
        assertEquals(0xBB.toByte(), bytesZh[1])
        assertEquals(0xBF.toByte(), bytesZh[2])

        val contentZh = String(bytesZh, Charsets.UTF_8)
        assertTrue(contentZh.contains("交易单号,日期时间,类型,分类,金额,账户,备注"))
        assertTrue(contentZh.contains("支出"))
        assertTrue(contentZh.contains("128.50"))
        assertTrue(contentZh.contains("\"聚餐, 带\"\"特殊\"\"字符\n换行\""))

        // 2. 英文多语言表头验证
        val bytesEn = TransactionBackupManager.exportToExcelCsv(sampleList, "en")
        val contentEn = String(bytesEn, Charsets.UTF_8)
        assertTrue(contentEn.contains("Transaction ID,Date & Time,Type,Category,Amount,Account,Note"))
        assertTrue(contentEn.contains("Expense"))
        assertTrue(contentEn.contains("Income"))

        // 3. 筛选过滤验证
        val filteredIncome = TransactionBackupManager.filterTransactions(sampleList, typeFilter = "INCOME")
        assertEquals(1, filteredIncome.size)
        assertEquals("tx-302", filteredIncome[0].id)

        val filteredRange = TransactionBackupManager.filterTransactions(sampleList, startTs = 1723900500000L)
        assertEquals(1, filteredRange.size)
        assertEquals("tx-302", filteredRange[0].id)
    }
}
