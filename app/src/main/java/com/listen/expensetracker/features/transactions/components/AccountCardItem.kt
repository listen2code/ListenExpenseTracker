package com.listen.expensetracker.features.transactions.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonText
import com.listen.uicomponent.theme.ExpenseRed

/**
 * Polished single account card row with vibrant badge, clean typography, and action buttons.
 * Supports quick tap to edit and long press to delete custom accounts.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCardItem(
    acct: AccountTypeItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    lang: String = "zh"
) {
    val (icon, iconBg, iconTint) = getAccountVisuals(acct.key, acct.isSystem)

    Surface(
        shape = RoundedCornerShape(AppDimens.CornerCard),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDimens.CornerCard))
            .combinedClickable(
                onClick = { if (!acct.isSystem) onEdit() },
                onLongClick = { if (!acct.isSystem) onDelete() }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpaceMedium, vertical = AppDimens.SpaceSmall + 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    CommonText(
                        text = acct.getDisplayName(lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (acct.isSystem) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                else MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        CommonText(
                            text = if (acct.isSystem) {
                                AppStrings.ACCOUNT_BADGE_SYSTEM.tr(lang)
                            } else {
                                AppStrings.ACCOUNT_BADGE_CUSTOM.tr(lang)
                            },
                            fontSize = 9.sp,
                            color = if (acct.isSystem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (!acct.isSystem) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = ExpenseRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Returns distinct visual icon, background color, and tint for account types.
 */
@Composable
private fun getAccountVisuals(key: String, isSystem: Boolean): Triple<ImageVector, Color, Color> {
    return when (key) {
        "CASH" -> Triple(
            Icons.Default.Payments,
            Color(0xFF10B981).copy(alpha = 0.15f),
            Color(0xFF059669)
        )
        "BANK" -> Triple(
            Icons.Default.AccountBalance,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.primary
        )
        "CREDIT" -> Triple(
            Icons.Default.CreditCard,
            Color(0xFF8B5CF6).copy(alpha = 0.15f),
            Color(0xFF7C3AED)
        )
        else -> Triple(
            if (isSystem) Icons.Default.AccountBalanceWallet else Icons.Default.Savings,
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.tertiary
        )
    }
}
