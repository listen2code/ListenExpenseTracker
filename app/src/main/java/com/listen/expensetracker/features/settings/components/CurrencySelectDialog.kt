package com.listen.expensetracker.features.settings.components

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens

/**
 * Currency Selection Dialog allowing users to choose base currency symbol.
 */
@Composable
fun CurrencySelectDialog(
    currentSymbol: String,
    onSymbolSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    lang: String = "zh"
) {
    val currencyOptions = listOf(
        "￥" to "人民币 / 日元 (CNY / JPY - ￥)",
        "$" to "美元 / 国际标准 (USD - $)",
        "€" to "欧元 (EUR - €)",
        "£" to "英镑 (GBP - £)",
        "₩" to "韩元 (KRW - ₩)",
        "HK$" to "港币 (HKD - HK$)",
        "NT$" to "新台币 (TWD - NT$)"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = AppStrings.currency_dialog_title.tr(lang),
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TextHeader
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceSmall)) {
                currencyOptions.forEach { (sym, desc) ->
                    val isSelected = currentSymbol == sym
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDimens.CornerButton))
                            .clickable {
                                onSymbolSelected(sym)
                                onDismiss()
                            }
                            .padding(horizontal = AppDimens.SpaceLarge, vertical = AppDimens.SpaceMedium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = desc,
                            fontSize = AppDimens.TextSubtitle,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.btn_cancel.tr(lang))
            }
        }
    )
}
