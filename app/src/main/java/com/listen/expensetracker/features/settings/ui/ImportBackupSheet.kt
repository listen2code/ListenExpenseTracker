package com.listen.expensetracker.features.settings.ui

import com.listen.arch.i18n.tr

import com.listen.expensetracker.data.i18n.AppStrings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.StringsRes
import com.listen.expensetracker.data.model.AppDimens

/**
 * Bottom Sheet for pasting and importing JSON backup strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBackupSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    lang: String = "zh",
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var jsonText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.SpaceSection, vertical = AppDimens.SpaceSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
        ) {
            Text(
                text = AppStrings.import_json.tr(lang),
                fontWeight = FontWeight.Bold,
                fontSize = AppDimens.TextHeader
            )

            OutlinedTextField(
                value = jsonText,
                onValueChange = { jsonText = it },
                placeholder = { Text("请在此粘贴导出的备份 JSON 格式数据...", fontSize = AppDimens.TextBody) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(AppDimens.CornerButton)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(AppStrings.btn_cancel.tr(lang))
                }

                Button(
                    onClick = {
                        if (jsonText.isNotBlank()) {
                            onImport(jsonText.trim())
                            onDismiss()
                        }
                    },
                    enabled = jsonText.isNotBlank(),
                    shape = RoundedCornerShape(AppDimens.CornerButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(AppStrings.btn_done.tr(lang))
                }
            }

            Spacer(modifier = Modifier.height(AppDimens.SpaceLarge))
        }
    }
}
