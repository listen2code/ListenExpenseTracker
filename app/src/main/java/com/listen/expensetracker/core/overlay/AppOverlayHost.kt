package com.listen.expensetracker.core.overlay

import androidx.compose.runtime.Composable
import com.listen.expensetracker.core.apm.ApmInspectorHost
import com.listen.expensetracker.core.state.AppOverlay
import com.listen.expensetracker.core.state.ExpenseAppState

/**
 * Global App-Level Overlay Host Component.
 * Positioned on the highest Z-index layer above all Feature Screens and NavigationBars.
 * Dispatches overlays based on AppState.activeOverlay (zero imperative boolean flags or raw if conditions in Activity).
 *
 * Prepared for future global floating bubbles, overlays, and system-wide dialogs.
 */
@Composable
fun AppOverlayHost(
    appState: ExpenseAppState
) {
    when (appState.activeOverlay) {
        is AppOverlay.ApmInspector -> {
            ApmInspectorHost(
                visible = true,
                onDismiss = { appState.dismissOverlay() }
            )
        }
        null -> Unit
    }
}
