package com.listen.expensetracker.features.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.listen.uicomponent.components.CommonFilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import com.listen.uicomponent.components.CommonBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.expensetracker.core.i18n.tr
import com.listen.expensetracker.data.backup.TransactionBackupManager
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.formatAmount
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.SurfaceCard
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.listen.expensetracker.data.model.AppConstants

enum class ExportDateRange {
    ALL, THIS_MONTH, THIS_YEAR
}

/**
 * Bottom Sheet offering rich filtering options (Date Range, Transaction Type)
 * and dual actions (Save to File, One-click Share) for exporting Excel/CSV data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportOptionsSheet(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    onSaveToFile: (startTs: Long?, endTs: Long?, typeFilter: String, defaultFileName: String) -> Unit,
    onShare: (startTs: Long?, endTs: Long?, typeFilter: String) -> Unit,
    onDismiss: () -> Unit,
    lang: String = "zh",
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedRange by remember { mutableStateOf(ExportDateRange.ALL) }
    var selectedType by remember { mutableStateOf("ALL") }

    // 动态计算时间范围起止毫秒
    val (startTs, endTs) = remember(selectedRange) {
        val now = Calendar.getInstance()
        when (selectedRange) {
            ExportDateRange.ALL -> Pair(null, null)
            ExportDateRange.THIS_MONTH -> {
                val start = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (now.clone() as Calendar).apply {
                    add(Calendar.MONTH, 1); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    add(Calendar.MILLISECOND, -1)
                }.timeInMillis
                Pair(start, end)
            }
            ExportDateRange.THIS_YEAR -> {
                val start = (now.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val end = (now.clone() as Calendar).apply {
                    add(Calendar.YEAR, 1); set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    add(Calendar.MILLISECOND, -1)
                }.timeInMillis
                Pair(start, end)
            }
        }
    }

    // 即时过滤统计
    val filteredList = remember(transactions, startTs, endTs, selectedType) {
        TransactionBackupManager.filterTransactions(transactions, startTs, endTs, selectedType)
    }
    val totalSum = remember(filteredList) { filteredList.sumOf { it.amount } }

    val defaultFileName = remember(selectedRange, selectedType) {
        val dateSuffix = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        "ListenExpense_${selectedRange.name.lowercase()}_$dateSuffix.csv"
    }
    // [ListenUiComponent] 使用共通 CommonBottomSheet 统一抽屉圆角、避让与标题呈现 (Rule 25)
    CommonBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = AppStrings.EXPORT_EXCEL_TITLE.tr(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppDimens.SpaceSection),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceMedium)
        ) {

            // 1. 时间范围筛选
            Text(
                text = AppStrings.EXPORT_RANGE_LABEL.tr(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommonFilterChip(
                    selected = selectedRange == ExportDateRange.ALL,
                    onClick = { selectedRange = ExportDateRange.ALL },
                    label = AppStrings.EXPORT_RANGE_ALL.tr()
                )
                CommonFilterChip(
                    selected = selectedRange == ExportDateRange.THIS_MONTH,
                    onClick = { selectedRange = ExportDateRange.THIS_MONTH },
                    label = AppStrings.EXPORT_RANGE_MONTH.tr()
                )
                CommonFilterChip(
                    selected = selectedRange == ExportDateRange.THIS_YEAR,
                    onClick = { selectedRange = ExportDateRange.THIS_YEAR },
                    label = AppStrings.EXPORT_RANGE_YEAR.tr()
                )
            }

            // 2. 交易类型筛选
            Text(
                text = AppStrings.EXPORT_TYPE_LABEL.tr(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommonFilterChip(
                    selected = selectedType == "ALL",
                    onClick = { selectedType = "ALL" },
                    label = AppStrings.FILTER_TYPE_ALL.tr()
                )
                CommonFilterChip(
                    selected = selectedType == "EXPENSE",
                    onClick = { selectedType = "EXPENSE" },
                    label = AppStrings.TYPE_EXPENSE.tr()
                )
                CommonFilterChip(
                    selected = selectedType == "INCOME",
                    onClick = { selectedType = "INCOME" },
                    label = AppStrings.TYPE_INCOME.tr()
                )
            }

            // 3. 数据即时预估卡片
            SurfaceCard(
                cornerRadius = AppDimens.CornerCard,
                contentPadding = AppDimens.SpaceMedium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = String.format(
                            Locale.getDefault(),
                            AppStrings.EXPORT_PREVIEW_SUMMARY.tr(),
                            filteredList.size,
                            "$currencySymbol${totalSum.formatAmount()}"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 4. 双轨操作按键 (保存文件 + 一键分享)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
            ) {
                CommonButton(
                    text = AppStrings.EXPORT_ACTION_SAVE.tr(),
                    onClick = { onSaveToFile(startTs, endTs, selectedType, defaultFileName) },
                    style = CommonButtonStyle.Primary,
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = AppConstants.ContentDescription.SAVE, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.EXPORT_ACTION_SHARE.tr(),
                    onClick = { onShare(startTs, endTs, selectedType) },
                    style = CommonButtonStyle.Outlined,
                    icon = { Icon(Icons.Default.Share, contentDescription = AppConstants.ContentDescription.SHARE, modifier = Modifier.size(18.dp)) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
