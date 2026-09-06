package com.listen.expensetracker.features.transactions.components

import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.ListenTheme

@Preview(showBackground = true)
@Composable
fun BalanceOverviewCardPreview() {
    ExpenseStrings.init()
    ListenTheme {
        BalanceOverviewCard(
            currencySymbol = "$",
            netBalance = 1500.0,
            totalExpense = 2000.0,
            totalIncome = 3500.0,
            budgetUsageRatio = 0.65f,
            isOverBudget = false,
            hideBalance = false,
            lang = "en",
            monthlyBudget = 3000.0,
            remainingBudget = 1000.0
        )
    }
}
