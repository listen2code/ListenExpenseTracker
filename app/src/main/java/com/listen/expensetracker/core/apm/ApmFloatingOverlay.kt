package com.listen.expensetracker.core.apm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.apm.ApmLogger
import com.listen.arch.i18n.tr
import com.listen.expensetracker.core.effect.shareSystemText
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.uicomponent.apm.LogEntryUi
import com.listen.uicomponent.theme.ExpenseRed
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * 全局可拖拽 APM 调试悬浮窗 (ApmFloatingOverlay)。
 *
 * 核心交互特征：
 * 1. 【收起状态】：屏幕边缘圆形浮动气泡，支持全屏安全区域内任意拖拽；
 * 2. 【防误触识别】：拖拽结束时若位移 < 12px 判定为轻触，平滑唤出 APM 悬浮大面板；
 * 3. 【展开状态】：保持原 APM 面板布局与全部调试能力，点击半透明蒙层或收起按钮平滑返回气泡；
 * 4. 【参数顺序与国际化】：modifier 位于首个可选参数位置，文案通过 AppStrings 字典统一解析。
 */
@Composable
fun ApmFloatingOverlay(
    modifier: Modifier = Modifier,
    lang: String = "zh"
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val rawApmLogs by ApmLogger.logsFlow.collectAsState()

    val logs = remember(rawApmLogs) {
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

    val errorCount = remember(logs) { logs.count { it.levelName == "ERROR" } }

    var isExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(-1f) }
    var offsetY by remember { mutableFloatStateOf(-1f) }
    var isDragging by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val bubbleSizePx = with(density) { 54.dp.toPx() }
        val marginPx = with(density) { 16.dp.toPx() }

        // 初次挂载自动吸附于屏幕右侧偏下方位置
        if (offsetX < 0f && screenWidthPx > 0f) {
            offsetX = (screenWidthPx - bubbleSizePx - marginPx).coerceAtLeast(0f)
            offsetY = (screenHeightPx * 0.65f).coerceIn(marginPx, screenHeightPx - bubbleSizePx - marginPx)
        }

        // 动态计算气泡中心锚点作为大面板展开/收拢的 TransformOrigin
        val pivotX = if (screenWidthPx > 0f) ((offsetX + bubbleSizePx / 2f) / screenWidthPx).coerceIn(0f, 1f) else 0.85f
        val pivotY = if (screenHeightPx > 0f) ((offsetY + bubbleSizePx / 2f) / screenHeightPx).coerceIn(0f, 1f) else 0.65f
        val transformOrigin = remember(pivotX, pivotY) { TransformOrigin(pivotX, pivotY) }

        val bubbleScale by animateFloatAsState(
            targetValue = if (isDragging) 1.12f else 1.0f,
            label = "bubbleDragScale"
        )

        // 1. 收起状态：圆形可拖动悬浮球（平滑缩放淡入淡出，杜绝生硬闪烁）
        AnimatedVisibility(
            visible = !isExpanded,
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .scale(bubbleScale),
            enter = fadeIn(animationSpec = tween(180)) + scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(animationSpec = tween(120)) + scaleOut(
                targetScale = 0.5f,
                animationSpec = tween(120, easing = FastOutSlowInEasing)
            )
        ) {
            Box(
                modifier = Modifier.pointerInput(screenWidthPx, screenHeightPx) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val touchSlop = viewConfiguration.touchSlop
                        var isDragStarted = false
                        var totalDragDistance = 0f

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break

                            if (change.changedToUp()) {
                                change.consume()
                                if (!isDragStarted) {
                                    isExpanded = true
                                }
                                break
                            }

                            if (!change.pressed) break

                            val drag = change.positionChange()
                            totalDragDistance += hypot(drag.x, drag.y)
                            if (!isDragStarted && totalDragDistance > touchSlop) {
                                isDragStarted = true
                                isDragging = true
                            }

                            if (isDragStarted) {
                                change.consume()
                                val maxX = (screenWidthPx - bubbleSizePx).coerceAtLeast(0f)
                                val maxY = (screenHeightPx - bubbleSizePx).coerceAtLeast(0f)
                                offsetX = (offsetX + drag.x).coerceIn(0f, maxX)
                                offsetY = (offsetY + drag.y).coerceIn(0f, maxY)
                            }
                        }
                        isDragging = false
                    }
                }
            ) {
                ApmBubbleContent(errorCount = errorCount)
            }
        }

        // 2. 展开状态：居中全功能 APM 调试控制台面板（以悬浮球坐标为锚点绽放展开/收拢）
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(220)),
            exit = fadeOut(animationSpec = tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable { isExpanded = false },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.animateEnterExit(
                        enter = scaleIn(
                            initialScale = 0.15f,
                            transformOrigin = transformOrigin,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(180)),
                        exit = scaleOut(
                            targetScale = 0.15f,
                            transformOrigin = transformOrigin,
                            animationSpec = tween(160, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(140))
                    )
                ) {
                    ApmFloatingInspectorCard(
                        logs = logs,
                        onClearLogs = { ApmLogger.clear() },
                        onExportLogs = {
                            val shareTitle = AppStrings.APM_SHARE_TITLE.tr(lang)
                            val logText = logs.joinToString("\n") {
                                "[${it.channelName}][${it.levelName}] ${it.tag}: ${it.message}"
                            }
                            shareSystemText(context, logText, shareTitle)
                        },
                        onCollapse = { isExpanded = false },
                        lang = lang
                    )
                }
            }
        }
    }
}
