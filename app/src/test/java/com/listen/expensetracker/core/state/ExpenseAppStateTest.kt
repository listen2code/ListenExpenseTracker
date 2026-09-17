package com.listen.expensetracker.core.state

import androidx.compose.material3.SnackbarHostState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * 验证精简后的 ExpenseAppState UI 导航与状态持有逻辑。
 */
class ExpenseAppStateTest {

    private val transactionsViewModel: TransactionsViewModel = mock()
    private val snackbarHostState: SnackbarHostState = mock()
    private val txState = MutableStateFlow(TransactionsUiState())

    private lateinit var appState: ExpenseAppState

    @Before
    fun setUp() {
        whenever(transactionsViewModel.viewState).thenReturn(txState)
        appState = ExpenseAppState(
            transactionsViewModel = transactionsViewModel,
            snackbarHostState = snackbarHostState
        )
    }

    @Test
    fun initialTab_isTransactions() {
        assertEquals(NavTab.TRANSACTIONS, appState.currentTab)
    }

    @Test
    fun switchTab_changesCurrentTab() {
        appState.switchTab(NavTab.STATISTICS)
        assertEquals(NavTab.STATISTICS, appState.currentTab)

        appState.switchTab(NavTab.SETTINGS)
        assertEquals(NavTab.SETTINGS, appState.currentTab)
    }

    @Test
    fun navigateToTransactionsMonth_clearsExistingFilters_andSwitchesToTransactionsTab() {
        appState.switchTab(NavTab.STATISTICS)
        appState.navigateToTransactionsMonth(monthOffset = -6)

        verify(transactionsViewModel).handleIntent(TransactionsIntent.ResetAllFilters)
        verify(transactionsViewModel).handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
        verify(transactionsViewModel).handleIntent(TransactionsIntent.SelectMonth(-6))
        assertEquals(NavTab.TRANSACTIONS, appState.currentTab)
    }

    @Test
    fun navigateToTransactionsCategory_filtersCategory_andSwitchesTab() {
        appState.switchTab(NavTab.STATISTICS)
        appState.navigateToTransactionsCategory(categoryName = "Food", monthOffset = -1)

        verify(transactionsViewModel).handleIntent(TransactionsIntent.FilterByCategory("Food", -1))
        assertEquals(NavTab.TRANSACTIONS, appState.currentTab)
    }

    @Test
    fun navigateToTransactionsDate_filtersDate_andSwitchesTab() {
        appState.switchTab(NavTab.STATISTICS)
        appState.navigateToTransactionsDate(monthOffset = 0, day = 15, dateLabel = "2026-09-15")

        verify(transactionsViewModel).handleIntent(TransactionsIntent.FilterByDate(0, 15, "2026-09-15"))
        assertEquals(NavTab.TRANSACTIONS, appState.currentTab)
    }

    @Test
    fun triggerScrollToTop_transactions_triggersViewModelScroll() {
        appState.triggerScrollToTop(NavTab.TRANSACTIONS)
        verify(transactionsViewModel).handleIntent(TransactionsIntent.ScrollToTop)
    }

    @Test
    fun triggerScrollToTop_emitsEvent() = runTest {
        var received: NavTab? = null
        val job = launch {
            appState.scrollToTopEvents.collect {
                received = it
            }
        }
        testScheduler.runCurrent()
        appState.triggerScrollToTop(NavTab.STATISTICS)
        testScheduler.runCurrent()
        assertEquals(NavTab.STATISTICS, received)
        job.cancel()
    }
}
