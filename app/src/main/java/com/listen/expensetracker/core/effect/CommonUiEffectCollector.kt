package com.listen.expensetracker.core.effect

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import kotlinx.coroutines.flow.collectLatest

/**
 * Universal Centralized Composable Hook to collect and handle CommonUiEffect across ViewModels.
 * Eliminates duplicate LaunchedEffect boilerplate for Toast, Snackbar, ShareText, and APM sheet.
 *
 * @param viewModels List of ViewModels producing CommonUiEffect
 * @param snackbarHostState Active SnackbarHostState to show transient feedback
 * @param onOpenApm Callback to display the APM log inspector bottom sheet
 */
@Composable
fun CollectCommonUiEffects(
    vararg viewModels: BaseViewModel<*, *, CommonUiEffect>,
    snackbarHostState: SnackbarHostState,
    onOpenApm: () -> Unit = {},
    onLaunchGoogleSignIn: () -> Unit = {}
) {
    val context = LocalContext.current

    viewModels.forEach { vm ->
        LaunchedEffect(vm) {
            vm.viewEffect.collectLatest { effect ->
                when (effect) {
                    is CommonUiEffect.ShowToast -> {
                        Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                    }
                    is CommonUiEffect.ShowSnackbar -> {
                        val res = snackbarHostState.showSnackbar(
                            message = effect.message,
                            actionLabel = effect.actionLabel,
                            duration = androidx.compose.material3.SnackbarDuration.Short
                        )
                        if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                            effect.onAction?.invoke()
                        }
                    }
                    is CommonUiEffect.ShareText -> {
                        shareSystemText(context, effect.content, effect.title)
                    }
                    is CommonUiEffect.OpenApmInspector -> {
                        onOpenApm()
                    }
                    is CommonUiEffect.LaunchGoogleSignIn -> {
                        onLaunchGoogleSignIn()
                    }
                    is CommonUiEffect.NavigateTo -> {
                        // Navigation handled by routing if needed
                    }
                }
            }
        }
    }
}

/**
 * Helper function to launch Android native Chooser intent for text sharing.
 */
fun shareSystemText(context: Context, content: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, content)
        type = "text/plain"
    }
    val chooser = Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
