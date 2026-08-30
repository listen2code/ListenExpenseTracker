package com.listen.expensetracker.core.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.LifecycleEvent

/**
 * 通用泛型 MVI 路由组件 (CommonRoute)。
 * 自动将任意 [BaseViewModel] 实例与纯无状态 Screen Composable 绑定，
 * 并作为全工程统一的生命周期适配器：自动监听系统生命周期与跨 Tab 挂载/卸载事件，
 * 并将其作为纯正的 MVI Intent 派发至 ViewModel 状态机。
 *
 * @param S UiState 类型
 * @param I UiIntent 类型
 * @param VM BaseViewModel 类型
 * @param viewModel ViewModel 实例
 * @param content 纯无状态 Screen 槽位，接收 (state, onIntent)
 */
@Composable
inline fun <S : Any, I : Any, reified VM : BaseViewModel<S, I>> CommonRoute(
    viewModel: VM = viewModel(),
    crossinline content: @Composable (state: S, onIntent: (I) -> Unit) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    // 全工程唯一定点收口：捕获系统生命周期与页面挂载/卸载并派发为 MVI Intent
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_APPEAR)
                Lifecycle.Event.ON_PAUSE -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DISAPPEAR)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_APPEAR)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DISAPPEAR)
        }
    }

    val state by viewModel.viewState.collectAsState()
    content(state, viewModel::handleIntent)
}
