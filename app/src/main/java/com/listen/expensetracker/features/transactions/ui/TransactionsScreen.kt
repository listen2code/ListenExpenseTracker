package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.expensetracker.features.transactions.components.TransactionsContentList
import com.listen.expensetracker.features.transactions.components.TransactionsDialogHost
import com.listen.expensetracker.features.transactions.components.TransactionsHeaderFilters
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.uicomponent.components.BaseScreenScaffold
import kotlinx.coroutines.launch

private const val PAGER_BASE_INDEX = 600
private const val PAGER_PAGE_COUNT = 1200

/**
 * Pure Stateless Transactions Screen.
 * Search bar and Account Filter chips stay stationary at top, while the Balance Overview
 * and Grouped Transaction Items glide smoothly in a horizontal PageView underneath with real-time month calculation.
 */
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    scrollToTopTrigger: Long = 0L
) {
    val lang = state.language
    var showSortMenu by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                onIntent(TransactionsIntent.SetMonthOffset(offset))
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
                onTitleClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.MonthPicker)) }
            )
        },
        actions = {
            IconButton(onClick = { onIntent(TransactionsIntent.ToggleHideBalance(!state.hideBalance)) }) {
                Icon(
                    imageVector = if (state.hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle Balance",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(TransactionsIntent.OpenDialog(TransactionsDialog.AddTransaction)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.btn_add_transaction.tr(lang))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Pinned Top Search Bar & Account Filters (Stationary)
            TransactionsHeaderFilters(
                state = state,
                onIntent = onIntent,
                showSortMenu = showSortMenu,
                onShowSortMenuChange = { showSortMenu = it },
                modifier = Modifier
                    .padding(horizontal = AppDimens.SpaceLarge)
                    .padding(bottom = AppDimens.SpaceSmall)
            )

            // 2. Horizontal PageView Slider with month-specific page calculation
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset = page - PAGER_BASE_INDEX
                TransactionsContentList(
                    state = state,
                    monthOffset = pageOffset,
                    onIntent = onIntent,
                    scrollToTopTrigger = if (page == pagerState.currentPage) scrollToTopTrigger else 0L
                )
            }
        }
    }

    // Feature-Level Dialog Host
    TransactionsDialogHost(state = state, onIntent = onIntent)
}
