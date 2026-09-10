package com.listen.expensetracker.core.effect

import android.content.Context
import android.content.Intent
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
 * @param viewModels List of ViewModels producing CommonUiEffect (使用 vararg 允许一次性传入所有 ViewModels，单点注册)
 * @param snackbarHostState Active SnackbarHostState to show transient feedback
 * @param onNavigateBack Optional callback for back navigation
 * @param onNavigateTo Optional callback for screen routing
 */
@Composable
fun CollectCommonUiEffects(
    vararg viewModels: BaseViewModel<*, *>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    viewModels.forEach { vm ->
        // 以 vm 作为 key，为每一个 ViewModel 启动一个独立互不干扰的协程收集器
        LaunchedEffect(vm) {
            // 使用 collectLatest：如果新 Effect 在旧 Effect 完成前到达，会取消旧的收集协程。
            // 这对于 Snackbar 这类会挂起(suspend)直到消失的 UI 元素尤为重要，防止队列阻塞。
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
                        // 为什么留空：业务画面专属的具体副作用（如 ScrollToMonth、ScrollToTop 等）
                        // 不属于基础通用 Effect，它们交由各 Screen 在内部独立消费，全局收集器在此直接忽略。
                    }
                }
            }
        }
    }
}

/**
 * Helper function to launch Android native Chooser intent for text sharing.
 * 使用 Intent.createChooser 拉起 Android 系统原生的分享面板(Share Sheet)。
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
 * 采用静默的 try-catch：即使用户设备上没有安装浏览器，也不会发生崩溃(Crash)。
 */
fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
