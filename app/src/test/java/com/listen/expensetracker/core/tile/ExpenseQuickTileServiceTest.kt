package com.listen.expensetracker.core.tile

import android.content.Context
import android.content.Intent
import com.listen.expensetracker.data.model.AppConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class ExpenseQuickTileServiceTest {

    @Test
    fun constants_andIntentConfig_areConfiguredProperly() {
        assertEquals("lexpense://quick_add", ExpenseQuickTileService.QUICK_ADD_URI)
        assertEquals(Intent.ACTION_VIEW, ExpenseQuickTileService.TILE_ACTION)
        assertEquals(AppConstants.DeepLink.SCHEME, "lexpense")
        assertEquals(AppConstants.DeepLink.HOST_QUICK_ADD, "quick_add")

        // 验证 Activity Flag
        val hasNewTask = (ExpenseQuickTileService.TILE_FLAGS and Intent.FLAG_ACTIVITY_NEW_TASK) != 0
        val hasClearTop = (ExpenseQuickTileService.TILE_FLAGS and Intent.FLAG_ACTIVITY_CLEAR_TOP) != 0
        assertTrue("Intent must contain FLAG_ACTIVITY_NEW_TASK for Service launch", hasNewTask)
        assertTrue("Intent must contain FLAG_ACTIVITY_CLEAR_TOP", hasClearTop)

        // 验证方法安全生成非空 Intent 对象
        val mockContext: Context = mock()
        val intent = ExpenseQuickTileService.createQuickAddIntent(mockContext)
        assertNotNull("Generated intent should not be null", intent)
    }
}
