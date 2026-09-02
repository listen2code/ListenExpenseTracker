package com.listen.expensetracker.features.budget.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.CategoryBudgetCalculationResult
import com.listen.expensetracker.data.engine.CategoryBudgetEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine

/**
 * 分类预算中心主看板内容 (CategoryBudgetCenterContent)。
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
    initialMonthOffset: Int = 0
) {
    var monthOffset by remember(initialMonthOffset) { mutableIntStateOf(initialMonthOffset) }
    val (_, _, monthTitle) = remember(monthOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(monthOffset, lang)
    }

    val result: CategoryBudgetCalculationResult = remember(allTransactions, monthOffset, monthlyBudget, categoryRatios) {
        CategoryBudgetEngine.calculate(
            allTransactions = allTransactions,
            currentOffset = monthOffset,
            totalBudget = monthlyBudget,
            categoryRatios = categoryRatios
        )
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
                    IconButton(onClick = { monthOffset-- }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", modifier = Modifier.size(15.dp))
                    }
                    Text(text = monthTitle, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 6.dp))
                    IconButton(onClick = { monthOffset++ }, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", modifier = Modifier.size(15.dp))
                    }
                }
            }
        }

        // 2. 紧凑总览卡片 (内嵌收支进度与健康徽章)
        BudgetOverviewHeaderCard(result = result, currencySymbol = currencySymbol, lang = lang, hideAmount = hideAmount)

        // 3. 分类预算执行列表 (自适应填充剩余空间，防止高度跳动)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items = result.statusList, key = { it.category.id }) { status ->
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
