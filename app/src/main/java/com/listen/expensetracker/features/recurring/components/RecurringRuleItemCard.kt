package com.listen.expensetracker.features.recurring.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 周期规则卡片项 (RecurringRuleItemCard)。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecurringRuleItemCard(
    rule: RecurringRuleEntity,
    currencySymbol: String,
    lang: String,
    onToggleEnabled: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = CategoryRepository.getCategoryById(rule.categoryId)
    val catColor = parseHexColor(rule.categoryColorHex)
    val accountName = AccountRepository.getAccountDisplayName(rule.accountType, lang)

    val freqLabel = when (rule.frequency) {
        RecurringFrequency.DAILY -> AppStrings.RECURRING_FREQ_DAILY.tr(lang)
        RecurringFrequency.WEEKLY -> "${AppStrings.RECURRING_FREQ_WEEKLY.tr(lang)}${getWeekdayName(rule.dayOfPeriod, lang)}"
        RecurringFrequency.MONTHLY -> "${AppStrings.RECURRING_FREQ_MONTHLY.tr(lang)} ${rule.dayOfPeriod}日"
        RecurringFrequency.YEARLY -> "${AppStrings.RECURRING_FREQ_YEARLY.tr(lang)}"
    }

    val daysDiff = ((rule.nextExecutionDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val countdownText = if (!rule.isEnabled) {
        "已暂停"
    } else if (daysDiff <= 0) {
        AppStrings.RECURRING_DUE_TODAY.tr(lang)
    } else {
        AppStrings.RECURRING_DAYS_LEFT.tr(lang).format(daysDiff)
    }

    Surface(
        shape = RoundedCornerShape(AppDimens.CornerCard),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            // Tier 1: 头部行 (左侧图标 + 规则名称，右侧大字金额)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category?.icon ?: Icons.Default.Repeat,
                        contentDescription = rule.title,
                        tint = catColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                CommonText(
                    text = rule.title,
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val isExp = rule.type == TransactionType.EXPENSE
                CommonText(
                    text = "${if (isExp) "-" else "+"}$currencySymbol%.2f".format(rule.amount),
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.Bold,
                    color = if (isExp) ExpenseRed else IncomeGreen
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tier 2: 属性与操作行 (左侧频次、账户胶囊与倒计时，右侧 Switch)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                    ) {
                        CommonText(
                            text = freqLabel,
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        CommonText(
                            text = accountName,
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }

                    CommonText(
                        text = countdownText,
                        fontSize = AppDimens.TextMicro,
                        color = if (daysDiff <= 0 && rule.isEnabled) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    }
}

private fun getWeekdayName(day: Int, lang: String): String = when (day) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    else -> "日"
}
