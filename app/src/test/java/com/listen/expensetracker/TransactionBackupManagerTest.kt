package com.listen.expensetracker

import com.listen.expensetracker.data.backup.TransactionBackupManager
import com.listen.expensetracker.data.db.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
                accountType = "WECHAT",
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
                accountType = "ALIPAY",
                timestamp = 1723900000000L
            )
        )

        val csv = TransactionBackupManager.exportToCsv(sampleList)
        assertTrue(csv.startsWith("ID,类型,分类,金额,账户,备注,时间"))
        assertTrue(csv.contains("tx-200,支出,交通,15.0,ALIPAY,打车"))
    }
}
