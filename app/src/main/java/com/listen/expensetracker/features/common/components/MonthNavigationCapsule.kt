package com.listen.expensetracker.features.common.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Month Navigation Capsule component for ListenExpenseTracker.
 * Displays previous/next buttons and a clickable center month title in a pill container.
 * Supports horizontal swipe gestures (swipe left for next month, swipe right for previous month).
 *
 * @param monthTitle Display text for the active month (e.g., "本月 (2026年08月)")
 * @param onPreviousMonth Callback triggered when tapping previous button or swiping right
 * @param onNextMonth Callback triggered when tapping next button or swiping left
 * @param onTitleClick Callback triggered when tapping the center title (e.g., open MonthPickerDialog)
 * @param modifier Composable modifier
 */
@Composable
fun MonthNavigationCapsule(
    monthTitle: String,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTitleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                    },
                    onDragEnd = {
                        val thresholdPx = 50f
                        if (dragAccumulator > thresholdPx) {
                            onPreviousMonth()
                        } else if (dragAccumulator < -thresholdPx) {
                            onNextMonth()
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = {
                        dragAccumulator = 0f
                    }
                )
            }
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onPreviousMonth,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Previous Month",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = monthTitle,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onTitleClick() }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            IconButton(
                onClick = onNextMonth,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next Month",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
