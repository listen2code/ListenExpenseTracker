package com.listen.expensetracker.features.transactions.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.features.common.components.MonthNavigationCapsule
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.transactions.components.TransactionsContentList
import com.listen.expensetracker.features.transactions.components.TransactionsDialogHost
import com.listen.expensetracker.features.transactions.components.TransactionsHeaderFilters
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsDialog
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsIntent
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsUiState
import com.listen.expensetracker.features.transactions.viewmodel.TransactionsViewModel
import com.listen.uicomponent.components.BaseScreenScaffold

/**
 * 纯无状态流水主画面 (TransactionsScreen)。
 *
 * 【教学重点 - Google 官方 UI State Holder 架构规范】：
 * 本 Screen 严格遵循“单向数据流 (UDF)”与“状态提升 (State Hoisting)”模式：
 * 1. 业务只读数据由 [state] ([TransactionsUiState]) 纯数据类驱动；
 * 2. 交互状态与动画控制器（PagerState、LazyListState、副作用监听）统一由 [rememberTransactionsStateHolder] 承接；
 * 3. Screen 函数内部无任何悬挂逻辑或散落状态，开门见山直接声明 UI 布局树。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TransactionsViewModel? = null
) {
    // 🌟 一行收拢所有 Pager、ListState 与副作用协同逻辑
    val holder = rememberTransactionsStateHolder(state, onIntent, viewModel)
    val lang = state.language

    BaseScreenScaffold(
        titleSlot = {
            MonthNavigationCapsule(
                monthTitle = holder.currentMonthTitle,
                onPreviousMonth = {
                    onIntent(TransactionsIntent.SelectMonth(holder.currentMonthOffset - 1))
                },
                onNextMonth = {
                    onIntent(TransactionsIntent.SelectMonth(holder.currentMonthOffset + 1))
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
                Icon(Icons.Default.Add, contentDescription = AppStrings.BTN_ADD_TRANSACTION.tr(lang))
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // 1. 顶部常驻搜索栏与账户过滤芯片 (Stationary)
            TransactionsHeaderFilters(
                state = state,
                onIntent = onIntent,
                modifier = Modifier
                    .padding(horizontal = AppDimens.SpaceLarge)
                    .padding(bottom = AppDimens.SpaceSmall)
            )

            // 2. 水平双向无限滑动分页器 (HorizontalPager)
            HorizontalPager(
                state = holder.pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                val pageOffset = page - PAGER_BASE_INDEX
                TransactionsContentList(
                    state = state,
                    monthOffset = pageOffset,
                    onIntent = onIntent,
                    listState = if (page == holder.pagerState.currentPage) holder.listState else rememberLazyListState()
                )
            }
        }
    }

    // 弹窗宿主分发器
    TransactionsDialogHost(state = state, onIntent = onIntent)
}
