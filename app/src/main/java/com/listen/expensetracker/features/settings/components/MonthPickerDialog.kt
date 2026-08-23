package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Month Picker Dialog allowing user to quickly jump to any month in the last 24 months.
 */
@Composable
fun MonthPickerDialog(
    currentOffset: Int,
    onOffsetSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    val months = (0 downTo -23).map { offset ->
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, offset) }
        val sdf = SimpleDateFormat("yyyy年MM月", Locale.getDefault())
        val title = if (offset == 0) "本月 (${sdf.format(cal.time)})" else sdf.format(cal.time)
        offset to title
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(StringsRes.get("select_month_dialog", lang), fontWeight = FontWeight.Bold, fontSize = AppDimens.TextHeader) },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(months) { (offset, title) ->
                    val isSelected = currentOffset == offset
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.CornerButton))
                            .clickable {
                                onOffsetSelected(offset)
                                onDismiss()
                            }
                            .padding(horizontal = AppDimens.SpaceLarge, vertical = AppDimens.SpaceMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = title,
                            fontSize = AppDimens.TextSubtitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(StringsRes.get("btn_cancel", lang))
            }
        }
    )
}
