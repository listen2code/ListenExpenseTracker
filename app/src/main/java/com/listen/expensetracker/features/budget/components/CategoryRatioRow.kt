package com.listen.expensetracker.features.budget.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.model.Category
import kotlin.math.roundToInt

/**
 * 单个分类预算比例调整行 (CategoryRatioRow)。
 */
@Composable
fun CategoryRatioRow(
    category: Category,
    ratio: Float,
    totalBudget: Double,
    currencySymbol: String,
    lang: String,
    onRatioChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val percent = (ratio * 100).roundToInt()
    val amount = totalBudget * ratio
    val catColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = category.icon, contentDescription = null, tint = catColor, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(text = category.getDisplayName(lang), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "$currencySymbol${"%.0f".format(amount)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { onRatioChange((percent - 5).coerceAtLeast(0) / 100f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Remove, contentDescription = "-", modifier = Modifier.size(16.dp))
            }
            Text(
                text = "$percent%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            IconButton(
                onClick = { onRatioChange((percent + 5).coerceAtMost(100) / 100f) },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "+", modifier = Modifier.size(16.dp))
            }
        }
    }
}
