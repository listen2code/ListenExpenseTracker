package com.listen.expensetracker.core.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.listen.expensetracker.core.apm.ApmFloatingOverlay
import com.listen.expensetracker.core.state.ExpenseAppState

/**
 * Global App-Level Overlay Host Component.
 * Positioned on the highest Z-index layer above all Feature Screens and NavigationBars.
 * 作为根节点(Root Surface)级别的定位组件，置于所有的功能画面与导航栏(NavigationBar)之上。
 *
 * 它的渲染由 settingsViewModel 的配置状态直接驱动，而非依赖 ExpenseAppState.activeOverlay，
 * 确保悬浮窗是长生命周期的持久化元素，而非临时弹窗。
 */
@Composable
fun AppOverlayHost(
    appState: ExpenseAppState
) {
    val settingsState by appState.settingsViewModel.viewState.collectAsState()
    val lang = settingsState.language

    // 全局 APM 悬浮窗（由设置项持久化开关驱动，跨画面常驻于顶层）。
    // 被设计为支持拖拽的调试窗口，它独立于页面的 Navigation 栈，
    // 因此可以在任意的页面流转(Page transitions)中安全存活(Survive)。
    if (settingsState.apmFloatingWindowEnabled) {
        ApmFloatingOverlay(lang = lang)
    }
}
