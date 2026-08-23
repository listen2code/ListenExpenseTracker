package com.listen.expensetracker.core.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.listen.expensetracker.core.apm.ApmInspectorHost
import com.listen.expensetracker.core.state.AppOverlay
import com.listen.expensetracker.core.state.ExpenseAppState

/**
 * Global App-Level Overlay Host Component.
 * Positioned on the highest Z-index layer above all Feature Screens and NavigationBars.
 * Dispatches overlays based on AppState.activeOverlay.
 */
@Composable
fun AppOverlayHost(
    appState: ExpenseAppState
) {
    val settingsState by appState.settingsViewModel.viewState.collectAsState()
    val lang = settingsState.language

    when (appState.activeOverlay) {
        is AppOverlay.ApmInspector -> {
            ApmInspectorHost(
                visible = true,
                onDismiss = { appState.dismissOverlay() },
                lang = lang
            )
        }
        null -> Unit
    }
}
