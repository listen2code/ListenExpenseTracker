package com.listen.expensetracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.data.backup.TransactionBackupManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBackupSheet(
    onDismiss: () -> Unit,
    onConfirmImport: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var jsonInput by remember { mutableStateOf("") }
    var parsedCount by remember { mutableStateOf<Int?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "从 JSON 备份导入账单",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请在下方粘贴导出的 JSON 备份文本：",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = jsonInput,
                onValueChange = {
                    jsonInput = it
                    if (it.isNotBlank()) {
                        try {
                            val list = TransactionBackupManager.importFromJson(it)
                            parsedCount = list.size
                            parseError = null
                        } catch (e: Exception) {
                            parsedCount = null
                            parseError = "格式有误: ${e.message}"
                        }
                    } else {
                        parsedCount = null
                        parseError = null
                    }
                },
                placeholder = { Text("在此粘贴 [ { \"type\": \"EXPENSE\", ... } ]", fontSize = 12.sp) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                maxLines = 10
            )

            Spacer(modifier = Modifier.height(8.dp))

            parsedCount?.let { count ->
                Text(
                    text = "✓ 已识别 $count 笔账单记录，可直接导入",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            parseError?.let { err ->
                Text(
                    text = "✗ $err",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消")
                }

                Button(
                    onClick = {
                        if (parsedCount != null && parsedCount!! > 0) {
                            onConfirmImport(jsonInput)
                            onDismiss()
                        }
                    },
                    enabled = parsedCount != null && parsedCount!! > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("确认导入")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
