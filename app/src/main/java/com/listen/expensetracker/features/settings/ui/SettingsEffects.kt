package com.listen.expensetracker.features.settings.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.listen.expensetracker.data.model.AppConstants
import com.listen.expensetracker.features.settings.viewmodel.SettingsEffect
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterIsInstance

import com.listen.expensetracker.core.state.NavTab
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * 设置画面专用副作用集中调度器 (SettingsEffects)。
 *
 * 【职责】：
 * 1. 注册并持有平台相关的系统回调 (ActivityResultLauncher)。
 * 2. 监听 ViewModel 发出的 Effect，并将其转化为物理的系统动作。
 * 3. 将系统返回的结果 (Uri) 包装成新的 Intent 发回 ViewModel。
 */
@Composable
fun SettingsEffects(
    viewModel: SettingsViewModel?,
    context: Context,
    listState: LazyListState,
    scrollToTopFlow: SharedFlow<NavTab>? = null
) {
    val currentListState by rememberUpdatedState(listState)

    // --- 1. 注册系统 ActivityResult 契约 (物理实现层) ---

    // JSON 导出
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(AppConstants.MimeTypes.JSON)
    ) { uri ->
        uri?.let { viewModel?.handleIntent(SettingsIntent.ExportJsonToFile(it)) }
    }

    // JSON 导入
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel?.handleIntent(SettingsIntent.ImportJsonFromFile(it)) }
    }

    // Excel 导出 (包含参数挂起逻辑)
    var pendingExcelConfig by remember { mutableStateOf<Triple<Long?, Long?, String>?>(null) }
    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(AppConstants.MimeTypes.CSV)
    ) { uri ->
        uri?.let {
            val (startTs, endTs, type) = pendingExcelConfig ?: Triple(null, null, AppConstants.Export.DEFAULT_TYPE_FILTER)
            viewModel?.handleIntent(SettingsIntent.ExportExcelToFile(it, startTs, endTs, type))
        }
    }

    // --- 2. 响应业务 Effect (逻辑监听层) ---

    LaunchedEffect(viewModel) {
        viewModel?.viewEffect?.filterIsInstance<SettingsEffect>()?.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.LaunchGoogleSignIn -> viewModel.launchGoogleAccountPicker(context)
                is SettingsEffect.ScrollToTop -> currentListState.animateScrollToItem(0)
                
                // 触发系统文件选择界面
                is SettingsEffect.TriggerJsonExport -> exportJsonLauncher.launch(effect.fileName)
                is SettingsEffect.TriggerJsonImport -> importJsonLauncher.launch(arrayOf(
                    AppConstants.MimeTypes.JSON, AppConstants.MimeTypes.ANY_TEXT, AppConstants.MimeTypes.ANY
                ))
                is SettingsEffect.TriggerExcelExport -> {
                    pendingExcelConfig = Triple(effect.startTs, effect.endTs, effect.typeFilter)
                    exportExcelLauncher.launch(effect.fileName)
                }
            }
        }
    }

    if (scrollToTopFlow != null) {
        LaunchedEffect(scrollToTopFlow) {
            scrollToTopFlow.collectLatest { tab ->
                if (tab == NavTab.SETTINGS) {
                    currentListState.animateScrollToItem(0)
                }
            }
        }
    }
}
