package com.listen.expensetracker.features.transactions.components

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountTypeItem
import com.listen.expensetracker.data.model.AppDimens
import com.listen.expensetracker.data.model.Category
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reusable horizontal category picker row for transaction edit and add sheets.
 */
@Composable
fun TransactionCategoryPicker(
    categories: List<Category>,
    selectedCategory: Category,
    onCategorySelected: (Category) -> Unit,
    lang: String,
    modifier: Modifier = Modifier,
    onManageCategories: (() -> Unit)? = null
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium),
        contentPadding = PaddingValues(vertical = AppDimens.SpaceSmall),
        modifier = modifier.fillMaxWidth()
    ) {
        items(categories, key = { it.id }) { cat ->
            val isSelected = selectedCategory.id == cat.id
            val catColor = parseHexColor(cat.colorHex)
            val catName = cat.getDisplayName(lang)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.CornerCard))
                    .clickable { onCategorySelected(cat) }
                    .padding(AppDimens.SpaceSmall)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) catColor else catColor.copy(alpha = 0.16f))
                        .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = catName,
                        tint = if (isSelected) Color.White else catColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = catName,
                    fontSize = AppDimens.TextMicro,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (onManageCategories != null) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(AppDimens.CornerCard))
                        .clickable { onManageCategories() }
                        .padding(AppDimens.SpaceSmall)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Manage Categories",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = AppStrings.SETTINGS_CATEGORY_MANAGE.tr(lang),
                        fontSize = AppDimens.TextMicro,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Reusable horizontal account selector chips row for transaction edit and add sheets.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionAccountPicker(
    accounts: List<AccountTypeItem>,
    selectedAccount: String,
    onAccountSelected: (String) -> Unit,
    onAccountLongClick: (AccountTypeItem) -> Unit,
    lang: String,
    modifier: Modifier = Modifier
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall),
        modifier = modifier.fillMaxWidth().padding(vertical = AppDimens.SpaceSmall)
    ) {
        items(accounts, key = { it.key }) { acct ->
            val isSelected = selectedAccount == acct.key
            val acctName = acct.getDisplayName(lang)
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            val labelColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            val borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

            Surface(
                shape = RoundedCornerShape(AppDimens.CornerButton),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(AppDimens.CornerButton))
                    .combinedClickable(
                        onClick = { onAccountSelected(acct.key) },
                        onLongClick = if (!acct.isSystem) ({ onAccountLongClick(acct) }) else null
                    )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = acctName,
                        fontSize = AppDimens.TextSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = labelColor
                    )
                }
            }
        }
    }
}

/**
 * Reusable compact Date Picker button for transaction forms.
 */
@Composable
fun TransactionDatePickerButton(
    selectedTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    OutlinedButton(
        onClick = {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
            DatePickerDialog(
                context,
                { _, y, m, d ->
                    val newCal = Calendar.getInstance().apply { set(y, m, d) }
                    onDateSelected(newCal.timeInMillis)
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        },
        shape = RoundedCornerShape(AppDimens.CornerButton),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.height(38.dp)
    ) {
        Icon(Icons.Default.CalendarToday, contentDescription = "Date", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(sdf.format(Date(selectedTimestamp)), fontSize = AppDimens.TextSmall)
    }
}
