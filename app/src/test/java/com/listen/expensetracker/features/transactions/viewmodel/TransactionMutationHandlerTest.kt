package com.listen.expensetracker.features.transactions.viewmodel

import android.app.Application
import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.db.TransactionDao
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionMutationHandlerTest {

    private val application: Application = mock()
    private val dao: TransactionDao = mock()
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val effects = mutableListOf<CommonUiEffect>()

    private lateinit var handler: TransactionMutationHandler

    @Before
    fun setUp() {
        ExpenseStrings.init()
        effects.clear()
        handler = TransactionMutationHandler(
            application = application,
            dao = dao,
            scope = testScope,
            emitEffect = { effects.add(it) },
            onRestore = {}
        )
    }

    @Test
    fun seedDemoData_whenMonthAlreadyHasData_abortsAndShowsErrorToast() = testScope.runTest {
        whenever(dao.getTransactionCountInRange(any(), any())).thenReturn(5)

        handler.seedDemoData(monthOffset = 0, lang = "zh")
        advanceUntilIdle()

        verify(dao, never()).insertTransactions(any())
        assertEquals(1, effects.size)
        val effect = effects.first() as CommonUiEffect.ShowToast
        assertEquals(ExpenseStrings.get(AppStrings.SEED_MONTH_HAS_DATA_ERROR, "zh"), effect.message)
    }

    @Test
    fun seedDemoData_whenMonthIsEmpty_insertsTransactionsAndShowsSuccessToast() = testScope.runTest {
        whenever(dao.getTransactionCountInRange(any(), any())).thenReturn(0)

        handler.seedDemoData(monthOffset = 0, lang = "zh")
        advanceUntilIdle()

        verify(dao).insertTransactions(any())
        assertEquals(1, effects.size)
        val effect = effects.first() as CommonUiEffect.ShowToast
        assertTrue(effect.message.contains("已生成"))
    }
}
