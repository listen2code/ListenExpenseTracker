package com.listen.expensetracker.features.settings.ui

import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.settings.viewmodel.SettingsIntent
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState
import com.listen.expensetracker.features.settings.viewmodel.SettingsViewModel

/**
 * 设置画面专用 UI 状态持有者 (SettingsStateHolder)。
 *
 * Google 官方 UI State Holder 设计模式说明：
 * 1. 【职责分离】：
 *    - [SettingsUiState]：存放业务与用户偏好数据（语言、货币符号、主题模式、登录状态等），由 ViewModel 管理；
 *    - [SettingsStateHolder]：承载系统级契约回调（文件导入/导出 ActivityResultLauncher）、滚动位置与副作用监听，生命周期与 Compose 树绑定。
 * 2. 【收口系统 ActivityResult 契约】：避免在 Screen 内部书写大量样板代码，将系统文档选择器闭包封装在 StateHolder 中。
 */
class SettingsStateHolder(
    val listState: LazyListState,
    val currentMonthTitle: String,
    val exportJsonLauncher: ManagedActivityResultLauncher<String, Uri?>,
    val importJsonLauncher: ManagedActivityResultLauncher<Array<String>, Uri?>,
    val exportExcelLauncher: ManagedActivityResultLauncher<String, Uri?>,
    val onPrepareExportExcel: (startTs: Long?, endTs: Long?, typeFilter: String, fileName: String) -> Unit
)

/**
 * 创建并记住 [SettingsStateHolder] 的 Composable 辅助函数。
 *
 * 
 * - 使用 [LocalContext.current] 获取 Compose 环境上下文，传递给状态容器；
 * - 使用 [rememberSaveable] 保存列表滚动位置，防止旋转屏幕或折叠屏折叠后滚动丢失；
 * - 声明 [rememberLauncherForActivityResult] 统一管理系统文件选择器并转发 MVI Intent。
 */
@Composable
fun rememberSettingsStateHolder(
    state: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    targetMonthOffset: Int = 0,
    viewModel: SettingsViewModel? = null
): SettingsStateHolder {
    val context = LocalContext.current
    val lang = state.language

    // 1. 根据当前目标月份计算展示标题
    val (_, _, currentMonthTitle) = remember(targetMonthOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(targetMonthOffset, lang)
    }

    // 2. 列表滚动状态
    // 使用 rememberSaveable 持久化滚动位置，防止发生 Configuration Change（如折叠屏折叠或旋转屏幕）时产生位置丢失。
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    // 3. 挂载副作用监听
    SettingsEffects(
        viewModel = viewModel,
        context = context,
        listState = listState
    )

    // 4. JSON 导出系统文档创建器
    // 所有的 ActivityResultLauncher 必须被注册并在 Compose 树生命周期内存活，否则系统回调将无法正确路由。
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let { onIntent(SettingsIntent.ExportJsonToFile(it)) }
    }

    // 5. JSON 导入系统文档读取器
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onIntent(SettingsIntent.ImportJsonFromFile(it)) }
    }

    // 6. Excel (CSV) 导出系统文档创建器
    // 设计了“两阶段”挂起导出模式 (Pending Export Pattern)：
    // 先将过滤配置参数 (startTs, endTs, type) 缓存到状态中，然后再调用系统的文件保存框，
    // 最后在系统的回调中结合该挂起配置真正发送导出 Intent。
    var pendingExportExcelConfig by remember { mutableStateOf<Triple<Long?, Long?, String>?>(null) }
    val exportExcelLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            val (startTs, endTs, type) = pendingExportExcelConfig ?: Triple(null, null, "ALL")
            onIntent(SettingsIntent.ExportExcelToFile(it, startTs, endTs, type))
        }
    }

    val onPrepareExportExcel: (startTs: Long?, endTs: Long?, typeFilter: String, fileName: String) -> Unit = remember {
        { startTs, endTs, typeFilter, fileName ->
            pendingExportExcelConfig = Triple(startTs, endTs, typeFilter)
            exportExcelLauncher.launch(fileName)
        }
    }

    return remember(listState, currentMonthTitle, exportJsonLauncher, importJsonLauncher, exportExcelLauncher, onPrepareExportExcel) {
        SettingsStateHolder(
            listState = listState,
            currentMonthTitle = currentMonthTitle,
            exportJsonLauncher = exportJsonLauncher,
            importJsonLauncher = importJsonLauncher,
            exportExcelLauncher = exportExcelLauncher,
            onPrepareExportExcel = onPrepareExportExcel
        )
    }
}
