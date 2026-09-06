package com.listen.expensetracker.features.statistics.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.Category
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.theme.ListenTheme
import com.listen.uicomponent.theme.parseHexColor

@Preview(showBackground = true)
@Composable
fun RankingCategoryItemPreview() {
    ExpenseStrings.init()
    val sampleShare = PieChartItem(
        label = "Dining",
        value = 1250.0,
        percentage = 0.45f,
        colorHex = "#EF4444"
    )
    ListenTheme {
        RankingCategoryItem(
            rank = 1,
            share = sampleShare,
            currencySymbol = "$",
            lang = "en",
            onClick = {}
        )
    }
}
