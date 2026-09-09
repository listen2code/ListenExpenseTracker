package com.listen.expensetracker.core.state

import androidx.compose.material3.SnackbarHostState
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsPeriod
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsViewModel
import com.listen.expensetracker.features.transactions.viewmodel.TransactionPeriod
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExpenseAppStateTest {

    private val transactionsViewModel: TransactionsViewModel = mock()
    private val statisticsViewModel: StatisticsViewModel = mock()
    private val settingsViewModel: SettingsViewModel = mock()
    private val snackbarHostState: SnackbarHostState = mock()

    private val txState = MutableStateFlow(TransactionsUiState())
    private val statsState = MutableStateFlow(StatisticsUiState())

    private lateinit var appState: ExpenseAppState

    @Before
    fun setUp() {
        whenever(transactionsViewModel.viewState).thenReturn(txState)
        whenever(statisticsViewModel.viewState).thenReturn(statsState)
        appState = ExpenseAppState(
            transactionsViewModel = transactionsViewModel,
            statisticsViewModel = statisticsViewModel,
            settingsViewModel = settingsViewModel,
            snackbarHostState = snackbarHostState
        )
    }

    @Test
    fun drillDownFromStatisticsYear_maintainsYearViewWhenImmediatelySwitchingBack() {
        statsState.value = statsState.value.copy(period = StatisticsPeriod.YEAR, selectedYearOffset = 0)

        appState.navigateToTransactionsMonth(monthOffset = -3)
        txState.value = txState.value.copy(period = TransactionPeriod.MONTH, selectedMonthOffset = -3)

        verify(transactionsViewModel).handleIntent(TransactionsIntent.ResetAllFilters)
        verify(transactionsViewModel).handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
        verify(transactionsViewModel).handleIntent(TransactionsIntent.SelectMonth(-3))
        verify(statisticsViewModel, never()).handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))

        // Immediately switch back to Statistics tab
        appState.switchTab(NavTab.STATISTICS)

        verify(statisticsViewModel, never()).handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        verify(statisticsViewModel, never()).handleIntent(StatisticsIntent.SetMonthOffset(-3))
        assertEquals(NavTab.STATISTICS, appState.currentTab)
    }

    @Test
    fun drillDownFromStatisticsYear_switchesMonthInTransactions_thenSwitchingBackStillMaintainsYearView() {
        statsState.value = statsState.value.copy(period = StatisticsPeriod.YEAR, selectedYearOffset = 0)

        appState.navigateToTransactionsMonth(monthOffset = -3)
        txState.value = txState.value.copy(period = TransactionPeriod.MONTH, selectedMonthOffset = -3)

        // User changes months in Transactions screen
        txState.value = txState.value.copy(selectedMonthOffset = -5)

        // Then switches back to Statistics tab
        appState.switchTab(NavTab.STATISTICS)

        verify(statisticsViewModel, never()).handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        verify(statisticsViewModel, never()).handleIntent(StatisticsIntent.SetMonthOffset(-5))
        assertEquals(NavTab.STATISTICS, appState.currentTab)
    }

    @Test
    fun normalSwitchTab_fromTransactionsYearToStatistics_syncsPeriodAndYearOffset() {
        statsState.value = statsState.value.copy(period = StatisticsPeriod.MONTH, selectedMonthOffset = 0)
        txState.value = txState.value.copy(period = TransactionPeriod.YEAR, selectedYearOffset = -1)

        appState.switchTab(NavTab.STATISTICS)

        verify(statisticsViewModel).handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.YEAR))
        verify(statisticsViewModel).handleIntent(StatisticsIntent.SetYearOffset(-1))
        assertEquals(NavTab.STATISTICS, appState.currentTab)
    }

    @Test
    fun normalSwitchTab_fromTransactionsMonthToStatistics_syncsPeriodAndMonthOffset() {
        statsState.value = statsState.value.copy(period = StatisticsPeriod.YEAR, selectedYearOffset = 0)
        txState.value = txState.value.copy(period = TransactionPeriod.MONTH, selectedMonthOffset = 2)

        appState.switchTab(NavTab.STATISTICS)

        verify(statisticsViewModel).handleIntent(StatisticsIntent.ChangePeriod(StatisticsPeriod.MONTH))
        verify(statisticsViewModel).handleIntent(StatisticsIntent.SetMonthOffset(2))
        assertEquals(NavTab.STATISTICS, appState.currentTab)
    }

    @Test
    fun normalSwitchTab_fromStatisticsYearToTransactions_syncsPeriodAndYearOffset() {
        // Start on Statistics screen
        appState.switchTab(NavTab.STATISTICS)
        statsState.value = statsState.value.copy(period = StatisticsPeriod.YEAR, selectedYearOffset = -2)
        txState.value = txState.value.copy(period = TransactionPeriod.MONTH, selectedMonthOffset = 0)

        appState.switchTab(NavTab.TRANSACTIONS)

        verify(transactionsViewModel).handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.YEAR))
        verify(transactionsViewModel).handleIntent(TransactionsIntent.SetYearOffset(-2))
        assertEquals(NavTab.TRANSACTIONS, appState.currentTab)
    }

    @Test
    fun navigateToTransactionsMonth_clearsExistingFilters_andSwitchesToTransactionsTab() {
        appState.navigateToTransactionsMonth(monthOffset = -6)

        // 验证按顺序触发了 ResetAllFilters 清除既有筛选条件、切换为月周期、以及选择目标月份
        verify(transactionsViewModel).handleIntent(TransactionsIntent.ResetAllFilters)
        verify(transactionsViewModel).handleIntent(TransactionsIntent.ChangePeriod(TransactionPeriod.MONTH))
        verify(transactionsViewModel).handleIntent(TransactionsIntent.SelectMonth(-6))
        assertEquals(NavTab.TRANSACTIONS, appState.currentTab)
    }
}
