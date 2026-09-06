package com.listen.expensetracker.core.apm

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.theme.ListenTheme

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
fun ApmFloatingInspectorCardPreview() {
    ExpenseStrings.init()
    val sampleLogs = listOf(
        LogEntryUi(
            id = "1",
            timestamp = System.currentTimeMillis() - 10000,
            levelName = "INFO",
            channelName = "APP",
            tag = "MainActivity",
            message = "Application resumed in 142ms",
            traceId = "tr-001"
        ),
        LogEntryUi(
            id = "2",
            timestamp = System.currentTimeMillis() - 5000,
            levelName = "WARN",
            channelName = "SYNC",
            tag = "GoogleDriveSync",
            message = "Sync delay: WiFi disconnected",
            traceId = "tr-002"
        ),
        LogEntryUi(
            id = "3",
            timestamp = System.currentTimeMillis() - 1000,
            levelName = "ERROR",
            channelName = "DB",
            tag = "TransactionDao",
            message = "Query timed out after 5000ms",
            traceId = "tr-003",
            stackTrace = "java.util.concurrent.TimeoutException\n\tat com.listen.expensetracker.data.db.TransactionDao_Impl.getMonthly(TransactionDao_Impl.java:412)"
        )
    )

    ListenTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            ApmFloatingInspectorCard(
                logs = sampleLogs,
                onClearLogs = {},
                onExportLogs = {},
                onCollapse = {},
                lang = "zh"
            )
        }
    }
}