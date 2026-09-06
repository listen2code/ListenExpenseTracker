package com.listen.expensetracker.core.apm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.ListenTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 单条 APM 日志卡片行组件 (ApmFloatingLogRow)。
 */
@Composable
fun ApmFloatingLogRow(
    log: LogEntryUi,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }

    val levelColor = when (log.levelName) {
        "DEBUG" -> Color.Gray
        "INFO" -> IncomeGreen
        "WARN" -> Color(0xFFF59E0B)
        "ERROR" -> ExpenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[${log.channelName}]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "[${log.levelName}]",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor
                    )
                    Text(
                        text = log.tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (!log.traceId.isNullOrBlank()) {
                        Text(
                            text = "[${log.traceId}]",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Text(
                    text = timeStr,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = log.message,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )

            val stack = log.stackTrace
            if (!stack.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stack,
                    fontSize = 9.sp,
                    color = ExpenseRed,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 6
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ApmFloatingLogRowPreview() {
    ExpenseStrings.init()
    ListenTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            ApmFloatingLogRow(
                log = LogEntryUi(
                    id = "1",
                    timestamp = System.currentTimeMillis(),
                    levelName = "INFO",
                    channelName = "APP",
                    tag = "PerformanceMonitor",
                    message = "Frame render took 3.2ms",
                    traceId = "tr-889"
                )
            )
        }
    }
}
