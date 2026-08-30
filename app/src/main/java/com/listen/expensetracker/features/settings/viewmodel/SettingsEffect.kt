package com.listen.expensetracker.features.settings.viewmodel

/**
 * Feature-specific one-shot side effects dedicated exclusively to the Settings screen.
 * Keeps business-specific auth and configuration side effects decoupled from universal architecture effects.
 */
sealed interface SettingsEffect {
    data object LaunchGoogleSignIn : SettingsEffect
}
