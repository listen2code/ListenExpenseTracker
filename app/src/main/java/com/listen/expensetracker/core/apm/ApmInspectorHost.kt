package com.listen.expensetracker.core.apm

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.listen.arch.apm.ApmLogger
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.core.effect.shareSystemText
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.apm.LogInspectorSheet

/**
 * Self-contained APM Inspector Host Component.
 * Automatically subscribes to ApmLogger logsFlow, maps entries to UI models, handles log clearing and export,
 * and displays the LogInspectorSheet.
 *
 * @param visible True if the APM inspector should be displayed
 * @param onDismiss Callback when the inspector is dismissed
 * @param lang ISO Language code
 */
@Composable
fun ApmInspectorHost(
    visible: Boolean,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    if (!visible) return

    val context = LocalContext.current
    val rawApmLogs by ApmLogger.logsFlow.collectAsState()

    val apmLogs = remember(rawApmLogs) {
        rawApmLogs.map { entry ->
            LogEntryUi(
                id = entry.id,
                timestamp = entry.timestamp,
                levelName = entry.level.name,
                channelName = entry.channel.name,
                tag = entry.tag,
                message = entry.message,
                traceId = entry.traceId ?: "",
                stackTrace = entry.stackTrace
            )
        }
    }

    val shareTitle = when (lang) {
        "en" -> "Share APM Logs"
        "ja" -> "APM ログを共有"
        else -> "分享 APM 日志"
    }

    LogInspectorSheet(
        logs = apmLogs,
        onClearLogs = { ApmLogger.clear() },
        onExportLogs = {
            val logText = apmLogs.joinToString("\n") {
                "[${it.channelName}][${it.levelName}] ${it.tag}: ${it.message}"
            }
            shareSystemText(context, logText, shareTitle)
        },
        onDismiss = onDismiss,
        lang = lang
    )
}
