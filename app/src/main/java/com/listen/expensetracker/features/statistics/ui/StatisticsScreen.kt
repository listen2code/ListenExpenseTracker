package com.listen.expensetracker.features.statistics.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import kotlinx.coroutines.flow.Flow
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.expensetracker.features.statistics.components.StatisticsContentList
import com.listen.expensetracker.features.statistics.components.StatisticsDialogHost
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsIntent
import com.listen.expensetracker.features.statistics.viewmodel.StatisticsUiState
import com.listen.uicomponent.components.BaseScreenScaffold
import com.listen.uicomponent.components.CommonSegmentedControl
import kotlinx.coroutines.launch

private const val PAGER_BASE_INDEX = 600
private const val PAGER_PAGE_COUNT = 1200

/**
 * Pure Stateless Statistics Screen.
 * The Expense/Income Segmented Toggle stays pinned at top, while the Donut Chart,
 * Trend Chart, Metrics, and Category Rankings glide smoothly in a horizontal PageView underneath with real-time month calculation.
 */
@Composable
fun StatisticsScreen(
    state: StatisticsUiState,
    onIntent: (StatisticsIntent) -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopFlow: Flow<Unit>? = null
) {
    val lang = state.language
    val coroutineScope = rememberCoroutineScope()
    val isExpenseTab = state.statisticsTab == "EXPENSE"

    val pagerState = rememberPagerState(
        initialPage = PAGER_BASE_INDEX + state.selectedMonthOffset,
        pageCount = { PAGER_PAGE_COUNT }
    )

    // Dynamically compute the month header title in real-time as the user swipes
    val activeOffset = pagerState.currentPage - PAGER_BASE_INDEX
    val (_, _, currentMonthTitle) = remember(activeOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(activeOffset, lang)
    }

    // Synchronize external month changes (MonthPickerDialog, etc.) with smooth page scroll
    LaunchedEffect(state.selectedMonthOffset) {
        val targetPage = PAGER_BASE_INDEX + state.selectedMonthOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Synchronize settled page changes with ViewModel state
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            val offset = page - PAGER_BASE_INDEX
            if (offset != state.selectedMonthOffset) {
                onIntent(StatisticsIntent.SetMonthOffset(offset))
            }
        }
    }

    BaseScreenScaffold(
        titleSlot = {
            MonthNavigationCapsule(
                monthTitle = currentMonthTitle,
                onPreviousMonth = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                    }
                },
                onNextMonth = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                },
                onTitleClick = { onIntent(StatisticsIntent.OpenMonthPicker) }
            )
        },
        actions = {
            IconButton(onClick = { onIntent(StatisticsIntent.ToggleHideAmount(!state.hideAmount)) }) {
                Icon(
                    imageVector = if (state.hideAmount) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Amount",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Pinned Top Expense vs Income Segmented Toggle (Stationary)
            val tabs = listOf(AppStrings.tab_expense_analysis.tr(lang), AppStrings.tab_income_analysis.tr(lang))
            CommonSegmentedControl(
                items = tabs,
                selectedIndex = if (isExpenseTab) 0 else 1,
                onIndexChange = { index ->
                    onIntent(StatisticsIntent.ChangeStatisticsTab(if (index == 0) "EXPENSE" else "INCOME"))
                },
                modifier = Modifier
                    .padding(horizontal = AppDimens.SpaceLarge)
                    .padding(bottom = AppDimens.SpaceExtraSmall)
            )

            // 2. Horizontal PageView Slider with month-specific analytics calculation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset = page - PAGER_BASE_INDEX
                StatisticsContentList(
                    state = state,
                    monthOffset = pageOffset,
                    onIntent = onIntent,
                    scrollToTopFlow = if (page == pagerState.currentPage) scrollToTopFlow else null
                )
            }
        }
    }

    // Feature-Level Dialog Host
    StatisticsDialogHost(state = state, onIntent = onIntent)
}
