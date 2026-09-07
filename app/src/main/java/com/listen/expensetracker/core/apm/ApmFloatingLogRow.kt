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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
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
import androidx.compose.ui.text.style.TextOverflow
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
 * 优化文本排版：左右栏严谨约束防止时间截断，频道/级别轻量色标，自然字体排版杜绝中英混排跳行。
 */
@Composable
fun ApmFloatingLogRow(
    log: LogEntryUi,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }
    val timeStr = remember(log.timestamp) { sdf.format(Date(log.timestamp)) }

    val levelColor = when (log.levelName) {
        "DEBUG" -> Color(0xFF6B7280)
        "INFO" -> IncomeGreen
        "WARN" -> Color(0xFFF59E0B)
        "ERROR" -> ExpenseRed
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(8.dp)
    ) {
        Column {
            // 顶栏：左侧[频道+级别+Tag+TraceId]，右侧稳定对齐的时间戳
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ApmLogBadge(text = log.channelName, color = MaterialTheme.colorScheme.primary)
                    ApmLogBadge(text = log.levelName, color = levelColor)

                    Text(
                        text = log.tag,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (!log.traceId.isNullOrBlank()) {
                        Text(
                            text = "#${log.traceId}",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = timeStr,
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 日志正文：支持文本选择，使用默认规范字体杜绝 Monospace 在中英混排下的行高跳动
            SelectionContainer {
                Text(
                    text = log.message,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 堆栈信息：独立暗红警示底色容器包裹
            val stack = log.stackTrace
            if (!stack.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(ExpenseRed.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = stack,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            color = ExpenseRed,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 6,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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
                    tag = "PerformanceMonitorEngine",
                    message = "主线程渲染流畅，耗时 3.2ms (Frame 60fps)",
                    traceId = "tr-889"
                )
            )
        }
    }
}
