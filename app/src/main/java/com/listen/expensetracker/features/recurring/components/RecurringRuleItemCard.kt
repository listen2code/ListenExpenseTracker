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
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.db.RecurringFrequency
import com.listen.expensetracker.data.db.RecurringRuleEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.engine.formatAmount
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
        RecurringFrequency.MONTHLY -> "${AppStrings.RECURRING_FREQ_MONTHLY.tr(lang)} ${AppStrings.RECURRING_DAY_SUFFIX.tr(lang).format(rule.dayOfPeriod)}"
        RecurringFrequency.YEARLY -> AppStrings.RECURRING_FREQ_YEARLY.tr(lang)
    }

    val daysDiff = ((rule.nextExecutionDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val countdownText = if (!rule.isEnabled) {
        AppStrings.RECURRING_PAUSED.tr(lang)
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
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Tier 1: 规则名称 (全宽独占整行，同行无其他任何项目，最多展示 2 行)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(catColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = category?.icon ?: Icons.Default.Repeat,
                        contentDescription = rule.title,
                        tint = catColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                androidx.compose.material3.Text(
                    text = rule.title,
                    fontSize = AppDimens.TextSubtitle,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 底部内容区：左侧第2行徽标与第3行金额靠左顶格，右侧 Switch 居中对齐
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // 第 2 行: 属性徽标行 ([每月 15日] [微信支付] 倒计时)
                    Row(
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

                    // 第 3 行: 完整金额 (左对齐顶格，自适应缩小，不缩略)
                    val isExp = rule.type == TransactionType.EXPENSE
                    CommonText(
                        text = "${if (isExp) "-" else "+"}$currencySymbol${rule.amount.formatAmount()}",
                        fontSize = AppDimens.TextTitle,
                        minFontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExp) ExpenseRed else IncomeGreen,
                        maxLines = 1,
                        autoResize = true
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Switch 与 2、3 行整体垂直居中
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
    1 -> AppStrings.WEEKDAY_MON.tr(lang)
    2 -> AppStrings.WEEKDAY_TUE.tr(lang)
    3 -> AppStrings.WEEKDAY_WED.tr(lang)
    4 -> AppStrings.WEEKDAY_THU.tr(lang)
    5 -> AppStrings.WEEKDAY_FRI.tr(lang)
    6 -> AppStrings.WEEKDAY_SAT.tr(lang)
    else -> AppStrings.WEEKDAY_SUN.tr(lang)
}
