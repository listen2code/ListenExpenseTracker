package com.listen.expensetracker.features.common.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import java.util.Calendar

/**
 * Modern Card-Style Year & Month Picker Dialog.
 * Features year navigation (< 2026 >), a 12-month visual grid, and quick "Jump to This Month".
 *
 * @param currentOffset Active month offset (0 = current month, -1 = last month)
 * @param onOffsetSelected Callback when a new month offset is picked
 * @param onDismiss Dismiss callback
 * @param lang Active language code
 */
@Composable
fun MonthPickerDialog(
    currentOffset: Int,
    onOffsetSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    val nowCalendar = remember { Calendar.getInstance() }
    val nowYear = nowCalendar.get(Calendar.YEAR)
    val nowMonth = nowCalendar.get(Calendar.MONTH) // 0-indexed (0..11)

    // Calculate the currently active target calendar
    val activeCalendar = remember(currentOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, currentOffset) }
    }
    val activeYear = activeCalendar.get(Calendar.YEAR)
    val activeMonth = activeCalendar.get(Calendar.MONTH)

    var viewingYear by remember { mutableIntStateOf(activeYear) }

    val monthNames = remember(lang) {
        when (lang.lowercase()) {
            "en" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            "ja" -> listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
            else -> listOf("1月", "2月", "3月", "4月", "5月", "6月", "7月", "8月", "9月", "10月", "11月", "12月")
        }
    }

    val yearFormat = when (lang.lowercase()) {
        "en" -> "$viewingYear"
        else -> "${viewingYear}年"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = AppStrings.select_month_dialog.tr(lang),
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextTitle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // "This Month" fast jump button
                TextButton(
                    onClick = {
                        onOffsetSelected(0)
                        onDismiss()
                    }
                ) {
                    Text(
                        text = if (lang == "en") "Current" else "本月",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year Switcher Header: [ < ] [ 2026年 ] [ > ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewingYear-- }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous Year",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = yearFormat,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(onClick = { viewingYear++ }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next Year",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 12-Month Grid (4 rows x 3 cols)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(monthNames.indices.toList()) { monthIndex ->
                        val isSelected = viewingYear == activeYear && monthIndex == activeMonth
                        val isCurrentActualMonth = viewingYear == nowYear && monthIndex == nowMonth

                        val containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else if (isCurrentActualMonth) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        }

                        val textColor = if (isSelected) {
                            Color.White
                        } else if (isCurrentActualMonth) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(containerColor)
                                .clickable {
                                    val newOffset = (viewingYear - nowYear) * 12 + (monthIndex - nowMonth)
                                    onOffsetSelected(newOffset)
                                    onDismiss()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monthNames[monthIndex],
                                fontSize = 14.sp,
                                fontWeight = if (isSelected || isCurrentActualMonth) FontWeight.Bold else FontWeight.Medium,
                                color = textColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btn_cancel.tr(lang))
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
