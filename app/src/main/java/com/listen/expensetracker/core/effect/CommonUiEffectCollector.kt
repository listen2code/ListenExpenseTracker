package com.listen.expensetracker.core.effect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.listen.arch.mvi.BaseViewModel
import com.listen.arch.mvi.CommonUiEffect
import kotlinx.coroutines.flow.collectLatest
import androidx.core.net.toUri

/**
 * Universal Centralized Composable Hook to collect and handle CommonUiEffect across ViewModels.
 * Eliminates duplicate LaunchedEffect boilerplate for Toast, Snackbar, ShareText, Browser URL, and Navigation.
 *
 * @param viewModels List of ViewModels producing CommonUiEffect
 * @param snackbarHostState Active SnackbarHostState to show transient feedback
 * @param onNavigateBack Optional callback for back navigation
 * @param onNavigateTo Optional callback for screen routing
 */
@Composable
fun CollectCommonUiEffects(
    vararg viewModels: BaseViewModel<*, *, CommonUiEffect>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

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
                            duration = SnackbarDuration.Short
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            effect.onAction?.invoke()
                        }
                    }
                    is CommonUiEffect.ShareText -> {
                        shareSystemText(context, effect.content, effect.title)
                    }
                    is CommonUiEffect.NavigateTo -> {
                        onNavigateTo(effect.route)
                    }
                    is CommonUiEffect.NavigateBack -> {
                        onNavigateBack()
                    }
                    is CommonUiEffect.OpenUrl -> {
                        openBrowserUrl(context, effect.url)
                    }
                    is CommonUiEffect.HideKeyboard -> {
                        keyboardController?.hide()
                    }
                    else -> {
                        // 业务画面专属副作用（如 ScrollToMonth、ScrollToTop）已由各 Screen 独立消费，全局收集器直接忽略
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

/**
 * Helper function to open an external web URL via system browser.
 */
fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
