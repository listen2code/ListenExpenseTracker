package com.listen.expensetracker.core.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.listen.expensetracker.core.apm.ApmFloatingOverlay
import com.listen.expensetracker.core.state.ExpenseAppState

/**
 * Global App-Level Overlay Host Component.
 * Positioned on the highest Z-index layer above all Feature Screens and NavigationBars.
 * Dispatches overlays based on global floating preferences and AppState.
 */
@Composable
fun AppOverlayHost(
    appState: ExpenseAppState
) {
    val settingsState by appState.settingsViewModel.viewState.collectAsState()
    val lang = settingsState.language

    // 全局 APM 可拖动悬浮窗（由设置项持久化开关驱动，跨画面常驻于顶层）
    if (settingsState.apmFloatingWindowEnabled) {
        ApmFloatingOverlay(lang = lang)
    }
}
