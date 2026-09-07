package com.listen.expensetracker.features.statistics.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.CalculationResult
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.CommonEmpty
import com.listen.uicomponent.components.ProgressSegment
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ListenTheme

/**
 * 统计分类收支环形占比与分段比例条通用卡片 (StatisticsBreakdownCard)。
 */
@Composable
fun StatisticsBreakdownCard(
    shares: List<PieChartItem>,
    segments: List<ProgressSegment>,
    totalAmount: Double,
    isExpenseTab: Boolean,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean,
    modifier: Modifier = Modifier,
    key: Any? = null,
    onCategoryClick: ((categoryName: String) -> Unit)? = null
) {
    SurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = AppDimens.SpaceExtraSmall)
    ) {
        AnimatedContent(
            targetState = isExpenseTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(200))
            },
            label = "DonutChartTabTransition"
        ) { expenseTab ->
            var selectedDonutItem by remember(expenseTab, key) { mutableStateOf<PieChartItem?>(null) }

            if (shares.isEmpty() || totalAmount <= 0.0) {
                CommonEmpty(
                    message = if (expenseTab) AppStrings.EMPTY_MONTH_EXPENSE.tr(lang) else AppStrings.EMPTY_MONTH_INCOME.tr(lang),
                    modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DonutChart(
                        items = shares,
                        totalValue = totalAmount,
                        centerTitle = if (expenseTab) AppStrings.TOTAL_EXPENSE.tr(lang) else AppStrings.TOTAL_INCOME.tr(lang),
                        centerValueText = if (hideAmount) "••••" else "$currencySymbol${totalAmount.formatAmount()}",
                        currencySymbol = currencySymbol,
                        hideAmount = hideAmount,
                        selectedItem = selectedDonutItem,
                        onSelectionChange = { selectedDonutItem = it },
                        onTooltipClick = onCategoryClick?.let { callback -> { item -> callback(item.label) } },
                        modifier = Modifier.padding(vertical = AppDimens.SpaceSmall)
                    )
                    SegmentedProgressBar(
                        segments = segments,
                        highlightColorHex = selectedDonutItem?.colorHex,
                        onSegmentClick = { seg ->
                            val matchedItem = shares.find { it.colorHex.equals(seg.colorHex, ignoreCase = true) }
                            selectedDonutItem = if (selectedDonutItem == matchedItem) null else matchedItem
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = AppDimens.SpaceSmall)
                    )
                }
            }
        }
    }
}

/**
 * 月度维度便利重载方法。
 */
@Composable
fun StatisticsBreakdownCard(
    calc: CalculationResult,
    isExpenseTab: Boolean,
    monthOffset: Int,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean,
    modifier: Modifier = Modifier,
    onCategoryClick: ((categoryName: String) -> Unit)? = null
) {
    val shares = if (isExpenseTab) calc.categoryShares else calc.incomeCategoryShares
    val segments = if (isExpenseTab) calc.progressSegments else calc.incomeProgressSegments
    val amount = if (isExpenseTab) calc.totalExpense else calc.totalIncome
    StatisticsBreakdownCard(
        shares = shares,
        segments = segments,
        totalAmount = amount,
        isExpenseTab = isExpenseTab,
        currencySymbol = currencySymbol,
        lang = lang,
        hideAmount = hideAmount,
        modifier = modifier,
        key = monthOffset,
        onCategoryClick = onCategoryClick
    )
}

@Preview(showBackground = true)
@Composable
fun StatisticsBreakdownCardPreview() {
    ExpenseStrings.init()
    val sampleShares = listOf(
        PieChartItem("Food", "#EF4444", 1200.0, 0.4f),
        PieChartItem("Transport", "#3B82F6", 600.0, 0.2f),
        PieChartItem("Shopping", "#EC4899", 900.0, 0.3f),
        PieChartItem("Others", "#6B7280", 300.0, 0.1f)
    )
    val sampleSegments = sampleShares.map { ProgressSegment(colorHex = it.colorHex, percentage = it.percentage) }

    ListenTheme {
        StatisticsBreakdownCard(
            shares = sampleShares,
            segments = sampleSegments,
            totalAmount = 3000.0,
            isExpenseTab = true,
            currencySymbol = "$",
            lang = "en",
            hideAmount = false
        )
    }
}
