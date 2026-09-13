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
 * @param content 使用 crossinline 修饰，防止 lambda 中的非局部返回(non-local returns)，确保组件生命周期的稳定性
 * reified VM: 使得 viewModel() 的默认参数能在编译期正确解析并推断出具体的 ViewModel 类型
 */
@Composable
inline fun <S : Any, I : Any, reified VM : BaseViewModel<S, I>> CommonRoute(
    viewModel: VM = viewModel(),
    crossinline content: @Composable (state: S, onIntent: (I) -> Unit) -> Unit
) {
    // 获取当前 Composable 作用域内的生命周期持有者 (LifecycleOwner)。
    // 在 Compose 中，LocalLifecycleOwner 是一个 CompositionLocal，它提供了当前组件所在的 Activity 或 Fragment 的生命周期。
    // 我们需要它来监听系统的生命周期事件（如 ON_RESUME, ON_PAUSE），以便将这些事件同步给 ViewModel。
    val lifecycleOwner = LocalLifecycleOwner.current

    // 全工程唯一定点收口：捕获系统生命周期与页面挂载/卸载并派发为 MVI Intent
    // 这里使用了“双重派发 (Dual Dispatch)”策略：
    // 1. 响应系统级的 Lifecycle.Event (如应用后台/前台切换、屏幕旋转)
    // 2. 响应 Compose 的生命周期 (页面 Composition 挂载 / onDispose 卸载，例如 Tab 切换)
    // 此设计确保了无论是系统级切换还是 Compose 内部切换，ViewModel 都能收到正确的 ON_APPEAR / ON_DISAPPEAR 事件。
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_CREATE)
                Lifecycle.Event.ON_START -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_START)
                Lifecycle.Event.ON_RESUME -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_RESUME)
                Lifecycle.Event.ON_PAUSE -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_PAUSE)
                Lifecycle.Event.ON_STOP -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_STOP)
                Lifecycle.Event.ON_DESTROY -> viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_DESTROY)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        // 覆盖 Compose 组件首次挂载生命周期
        viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_RESUME)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // 覆盖 Compose 组件卸载生命周期
            viewModel.dispatchLifecycleEvent(LifecycleEvent.ON_PAUSE)
        }
    }

    val state by viewModel.viewState.collectAsState()
    content(state, viewModel::handleIntent)
}
