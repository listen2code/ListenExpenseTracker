package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor

/**
 * Ultra-Compact Transaction Item Row Component.
 * Supports single-tap to edit, long-press to delete, category icon badge, and privacy amount masking.
 *
 * @param transaction Transaction data entity
 * @param currencySymbol Active currency symbol
 * @param hideAmount True if privacy masking is active
 * @param onClick Callback when the transaction row is tapped
 * @param onLongClick Callback when the transaction row is long pressed
 * @param lang Active language code
 * @param modifier Composable modifier
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionItemRow(
    modifier: Modifier = Modifier,
    transaction: TransactionEntity,
    currencySymbol: String,
    hideAmount: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    lang: String = "zh"
) {
    SurfaceCard(
        cornerRadius = AppDimens.CornerCard,
        contentPadding = AppDimens.SpaceExtraSmall,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpaceExtraSmall, vertical = AppDimens.SpaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val cat = CategoryRepository.getCategoryById(transaction.categoryId)
            val color = parseHexColor(transaction.categoryColorHex)
            val accountDisplay = AccountRepository.getAccountDisplayName(transaction.accountType, lang)

            // Left Section: Category Icon, Name, Account Badge, Note
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                modifier = Modifier.weight(1f, fill = true)
            ) {
                // Category Icon Bubble
                Box(
                    modifier = Modifier
                        .size(AppDimens.IconSizeLarge)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = transaction.categoryName,
                        tint = color,
                        modifier = Modifier.size(AppDimens.IconSizeSmall)
                    )
                }

                val isRecurring = transaction.note.startsWith("[周期]")
                val displayNote = if (isRecurring) transaction.note.removePrefix("[周期]").trim() else transaction.note

                Column(modifier = Modifier.weight(1f, fill = true)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)
                    ) {
                        Text(
                            text = transaction.categoryName,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = AppDimens.TextSubtitle,
                            maxLines = 1
                        )
                        Text(
                            text = "· $accountDisplay",
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                        if (isRecurring) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Recurring",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(9.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "周期",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (displayNote.isNotBlank()) {
                        Text(
                            text = displayNote,
                            fontSize = AppDimens.TextMicro,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // Right Section: Signed Currency Amount
            val isExpense = transaction.type == TransactionType.EXPENSE
            val amountPrefix = if (isExpense) "-" else "+"
            val amountColor = if (isExpense) ExpenseRed else IncomeGreen

            CommonText(
                text = if (hideAmount) "••••" else "$amountPrefix$currencySymbol${"%.2f".format(transaction.amount)}",
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TextSubtitle,
                minFontSize = 10.sp,
                color = amountColor,
                maxLines = 1,
                autoResize = true
            )
        }
    }
}
