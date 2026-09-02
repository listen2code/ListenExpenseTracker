package com.listen.expensetracker.features.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.CategoryBudgetEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.common.components.PAGER_BASE_INDEX
import com.listen.expensetracker.features.common.components.PAGER_PAGE_COUNT
import kotlinx.coroutines.launch

/**
 * 分类预算中心主看板内容 (CategoryBudgetCenterContent)。
 * 支持 HorizontalPager 水平左右滑动手势切换月份，并与顶部胶囊月份联动。
 */
@Composable
fun CategoryBudgetCenterContent(
    allTransactions: List<TransactionEntity>,
    monthlyBudget: Double,
    categoryRatios: Map<String, Float>,
    currencySymbol: String,
    lang: String,
    hideAmount: Boolean,
    modifier: Modifier = Modifier,
    initialMonthOffset: Int = 0,
    onMonthOffsetChange: (Int) -> Unit = {}
) {
    val pagerState = rememberPagerState(
        initialPage = PAGER_BASE_INDEX + initialMonthOffset,
        pageCount = { PAGER_PAGE_COUNT }
    )
    val coroutineScope = rememberCoroutineScope()
    val currentOffset = pagerState.currentPage - PAGER_BASE_INDEX

    LaunchedEffect(currentOffset) {
        onMonthOffsetChange(currentOffset)
    }

    val (_, _, monthTitle) = remember(currentOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(currentOffset, lang)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. 居中紧凑胶囊型月份切换器
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage > 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(15.dp))
                    }
                    Text(text = monthTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage < PAGER_PAGE_COUNT - 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // 2. 水平滑动分页器：左右滑动无缝切换月份
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val pageOffset = page - PAGER_BASE_INDEX
            val pageResult = remember(allTransactions, pageOffset, monthlyBudget, categoryRatios) {
                CategoryBudgetEngine.calculate(
                    allTransactions = allTransactions,
                    currentOffset = pageOffset,
                    totalBudget = monthlyBudget,
                    categoryRatios = categoryRatios
                )
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 紧凑总览卡片 (内嵌收支进度与健康徽章)
                BudgetOverviewHeaderCard(result = pageResult, currencySymbol = currencySymbol, lang = lang, hideAmount = hideAmount)

                // 分类预算执行列表 (自适应填充剩余空间，防止高度跳动)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(items = pageResult.statusList, key = { it.category.id }) { status ->
                        CategoryBudgetItemCard(
                            status = status,
                            currencySymbol = currencySymbol,
                            lang = lang,
                            hideAmount = hideAmount
                        )
                    }
                }
            }
        }
    }
}
