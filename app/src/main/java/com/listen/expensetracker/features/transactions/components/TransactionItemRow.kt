package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.data.db.TransactionEntity
import com.listen.expensetracker.data.model.AccountRepository
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.CategoryRepository
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.parseHexColor

/**
 * Ultra-Compact Transaction Item Row Component.
 * Supports swipe-to-delete gesture, category icon badge, and privacy amount masking.
 *
 * @param transaction Transaction data entity
 * @param currencySymbol Active currency symbol
 * @param hideAmount True if privacy masking is active
 * @param onClick Callback when the transaction row is tapped
 * @param onDelete Callback when swiped to delete
 * @param modifier Composable modifier
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItemRow(
    transaction: TransactionEntity,
    currencySymbol: String,
    hideAmount: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissVal ->
            if (dismissVal == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier.clip(RoundedCornerShape(AppDimens.CornerCard)),
        backgroundContent = {
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ExpenseRed)
                        .padding(end = AppDimens.SpaceSection),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.White,
                        modifier = Modifier.size(AppDimens.IconSizeMedium)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                )
            }
        }
    ) {
        SurfaceCard(
            cornerRadius = AppDimens.CornerCard,
            contentPadding = AppDimens.SpaceMedium,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
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
                val accountDisplay = AccountRepository.getAccountName(transaction.accountType)

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

                    Column(modifier = Modifier.weight(1f, fill = false)) {
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
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }

                        if (transaction.note.isNotBlank()) {
                            Text(
                                text = transaction.note,
                                fontSize = AppDimens.TextMicro,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right Section: Signed Currency Amount
                val amountPrefix = if (transaction.type == "EXPENSE") "-" else "+"
                val amountColor = if (transaction.type == "EXPENSE") ExpenseRed else IncomeGreen

                Text(
                    text = if (hideAmount) "••••" else "$amountPrefix$currencySymbol${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = AppDimens.TextSubtitle,
                    color = amountColor,
                    maxLines = 1
                )
            }
        }
    }
}
