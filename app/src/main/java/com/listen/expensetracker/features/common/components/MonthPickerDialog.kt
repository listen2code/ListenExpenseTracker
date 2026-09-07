package com.listen.expensetracker.features.common.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonDialog
import com.listen.uicomponent.components.CommonText
import java.util.Calendar

private enum class MonthPickerViewMode { MONTHS, YEARS }

/**
 * Modern Card-Style Year & Month Picker Dialog (Scheme A: Header Drill-Down).
 * Supports seamless drill-down from month grid to year grid without redundant buttons.
 */
@Composable
fun MonthPickerDialog(
    currentMonthOffset: Int,
    onOffsetSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    currentYearOffset: Int = 0,
    isYearMode: Boolean = false,
    onYearSelected: ((Int) -> Unit)? = null,
    lang: String = "zh"
) {
    val nowCalendar = remember { Calendar.getInstance() }
    val nowYear = nowCalendar.get(Calendar.YEAR)
    val nowMonth = nowCalendar.get(Calendar.MONTH)

    val activeMonthCal = remember(currentMonthOffset) {
        Calendar.getInstance().apply { add(Calendar.MONTH, currentMonthOffset) }
    }
    val activeMonthYear = activeMonthCal.get(Calendar.YEAR)
    val activeMonth = activeMonthCal.get(Calendar.MONTH)

    val activeYearCal = remember(currentYearOffset) {
        Calendar.getInstance().apply { add(Calendar.YEAR, currentYearOffset) }
    }
    val activeYear = activeYearCal.get(Calendar.YEAR)

    var viewMode by remember {
        mutableStateOf(if (isYearMode && onYearSelected != null) MonthPickerViewMode.YEARS else MonthPickerViewMode.MONTHS)
    }
    var viewingYear by remember { mutableIntStateOf(if (isYearMode) activeYear else activeMonthYear) }
    var yearPageBase by remember { mutableIntStateOf((viewingYear / 12) * 12) }

    val monthNames = remember(lang) {
        if (lang.lowercase() == "en") listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        else (1..12).map { "${it}月" }
    }
    val yearFormat = if (lang.lowercase() == "en") "$viewingYear" else "${viewingYear}年"

    CommonDialog(
        onDismissRequest = onDismiss,
        title = if (viewMode == MonthPickerViewMode.YEARS) AppStrings.SELECT_YEAR_TITLE.tr(lang) else AppStrings.SELECT_MONTH_DIALOG.tr(lang),
        confirmButton = {
            TextButton(
                onClick = {
                    if (viewMode == MonthPickerViewMode.YEARS && onYearSelected != null) onYearSelected(0) else onOffsetSelected(0)
                    onDismiss()
                }
            ) {
                CommonText(
                    text = (if (viewMode == MonthPickerViewMode.YEARS) AppStrings.JUMP_TO_THIS_YEAR else AppStrings.JUMP_TO_THIS_MONTH).tr(lang),
                    fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = { CommonButton(text = AppStrings.BTN_CANCEL.tr(lang), onClick = onDismiss, style = CommonButtonStyle.Text) },
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (viewMode == MonthPickerViewMode.MONTHS) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewingYear--; yearPageBase = (viewingYear / 12) * 12 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            if (onYearSelected != null) { yearPageBase = (viewingYear / 12) * 12; viewMode = MonthPickerViewMode.YEARS }
                        }.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        CommonText(text = yearFormat, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        if (onYearSelected != null) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Year", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    IconButton(onClick = { viewingYear++; yearPageBase = (viewingYear / 12) * 12 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    items(monthNames.indices.toList()) { mIndex ->
                        PickerGridItem(
                            label = monthNames[mIndex],
                            isSelected = !isYearMode && viewingYear == activeMonthYear && mIndex == activeMonth,
                            isCurrent = viewingYear == nowYear && mIndex == nowMonth,
                            onClick = { onOffsetSelected((viewingYear - nowYear) * 12 + (mIndex - nowMonth)); onDismiss() }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { yearPageBase -= 12 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Dec", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                    CommonText(text = "$yearPageBase - ${yearPageBase + 11}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    IconButton(onClick = { yearPageBase += 12 }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Dec", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 4.dp), horizontalArrangement = Arrangement.Start) {
                    TextButton(onClick = { viewMode = MonthPickerViewMode.MONTHS }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        CommonText(text = AppStrings.BACK_TO_MONTHS.tr(lang), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()
                ) {
                    val yearsList = (0..11).map { yearPageBase + it }
                    items(yearsList) { yr ->
                        val yrLabel = if (lang.lowercase() == "en") "$yr" else "${yr}年"
                        PickerGridItem(
                            label = yrLabel,
                            isSelected = isYearMode && yr == activeYear,
                            isCurrent = yr == nowYear,
                            onClick = { if (onYearSelected != null) { onYearSelected(yr - nowYear); onDismiss() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerGridItem(
    label: String,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val containerColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isCurrent -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
    }
    val textColor = when {
        isSelected -> Color.White
        isCurrent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(10.dp)).background(containerColor).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        CommonText(text = label, fontSize = 14.sp, fontWeight = if (isSelected || isCurrent) FontWeight.Bold else FontWeight.Medium, color = textColor, textAlign = TextAlign.Center)
    }
}
