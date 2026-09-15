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
import com.listen.expensetracker.data.model.AppConstants

/**
 * 集中化 UI 副作用处理器 (Common UI Effect Handler)。
 * 对称于 AppSideEffectHandler，负责跨 ViewModel 统一收集并执行通用的 UI 反馈任务（如 Toast、Snackbar、分享、导航等）。
 *
 * @param viewModels 产生通用副作用的 ViewModel 列表
 * @param snackbarHostState 用于显示 Snackbar 的全局状态宿主
 * @param onNavigateBack 自定义返回导航逻辑
 * @param onNavigateTo 自定义路由跳转逻辑
 */
@Composable
fun CommonUiEffectHandler(
    vararg viewModels: BaseViewModel<*, *>,
    snackbarHostState: SnackbarHostState,
    onNavigateBack: () -> Unit = {},
    onNavigateTo: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    viewModels.forEach { vm ->
        // 为每个 ViewModel 开启独立的订阅协程，互不阻塞
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
                        // 业务画面专属的副作用交由各 Screen 独立消费，全局处理器在此忽略
                    }
                }
            }
        }
    }
}

/**
 * 辅助函数：拉起系统分享面板
 */
fun shareSystemText(context: Context, content: String, title: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, content)
        type = AppConstants.MimeTypes.PLAIN_TEXT
    }
    val chooser = Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

/**
 * 辅助函数：通过外部浏览器打开 URL
 */
fun openBrowserUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
