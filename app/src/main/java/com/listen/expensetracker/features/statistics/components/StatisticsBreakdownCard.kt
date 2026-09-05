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
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.engine.CalculationResult
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.charts.DonutChart
import com.listen.uicomponent.charts.PieChartItem
import com.listen.uicomponent.components.CommonEmpty
import com.listen.uicomponent.components.SegmentedProgressBar
import com.listen.uicomponent.components.SurfaceCard

/**
 * 统计分类收支环形占比与分段比例条卡片 (StatisticsBreakdownCard)。
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
            val shares = if (expenseTab) calc.categoryShares else calc.incomeCategoryShares
            val segments = if (expenseTab) calc.progressSegments else calc.incomeProgressSegments
            val amount = if (expenseTab) calc.totalExpense else calc.totalIncome

            var selectedDonutItem by remember(expenseTab, monthOffset) { mutableStateOf<PieChartItem?>(null) }

            if (shares.isEmpty() || amount <= 0.0) {
                CommonEmpty(
                    message = if (expenseTab) AppStrings.EMPTY_MONTH_EXPENSE.tr(lang) else AppStrings.EMPTY_MONTH_INCOME.tr(lang),
                    modifier = Modifier.padding(vertical = AppDimens.SpaceSection)
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DonutChart(
                        items = shares,
                        totalValue = amount,
                        centerTitle = if (expenseTab) AppStrings.TOTAL_EXPENSE.tr(lang) else AppStrings.TOTAL_INCOME.tr(lang),
                        centerValueText = if (hideAmount) "••••" else "$currencySymbol${amount.formatAmount()}",
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
