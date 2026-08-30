package com.listen.expensetracker.features.settings.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.listen.expensetracker.features.settings.viewmodel.SettingsEffect
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance

/**
 * 设置画面专用副作用集中调度器 (SettingsEffects)。
 *
 * 
 * 1. [LaunchedEffect(viewModel)]：监听 ViewModel 发射的独有业务副作用。由于 Google 登录与一键置顶属于单次即逝事件（One-off Events），
 *    使用 [collectLatest] 保证前一个未完成事件在接收到新事件时自动取消，防止快速重复点击触发多次系统账户选择弹窗。
 */
@Composable
fun SettingsEffects(
    viewModel: SettingsViewModel?,
    context: Context,
    listState: LazyListState
) {
    LaunchedEffect(viewModel) {
        viewModel?.viewEffect?.filterIsInstance<SettingsEffect>()?.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.LaunchGoogleSignIn -> viewModel.launchGoogleAccountPicker(context)
                is SettingsEffect.ScrollToTop -> listState.animateScrollToItem(0)
            }
        }
    }
}
