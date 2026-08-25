package com.listen.expensetracker.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.listen.arch.i18n.tr
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AppDimens
import com.listen.uicomponent.components.CommonBottomSheet
import com.listen.uicomponent.components.CommonButton
import com.listen.uicomponent.components.CommonButtonStyle
import com.listen.uicomponent.components.CommonEditText

/**
 * Bottom Sheet for pasting and importing JSON backup strings using standardized ListenUiComponent elements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBackupSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
    lang: String = "zh"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var jsonText by remember { mutableStateOf("") }

    CommonBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        title = AppStrings.import_json.tr(lang),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard)
        ) {
            CommonEditText(
                value = jsonText,
                onValueChange = { jsonText = it },
                placeholder = "请在此粘贴导出的备份 JSON 格式数据...",
                singleLine = false,
                maxLines = 8,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppDimens.SpaceStandard),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CommonButton(
                    text = AppStrings.btn_cancel.tr(lang),
                    onClick = onDismiss,
                    style = CommonButtonStyle.Outlined,
                    modifier = Modifier.weight(1f)
                )

                CommonButton(
                    text = AppStrings.btn_done.tr(lang),
                    onClick = {
                        if (jsonText.isNotBlank()) {
                            onImport(jsonText.trim())
                            onDismiss()
                        }
                    },
                    enabled = jsonText.isNotBlank(),
                    style = CommonButtonStyle.Primary,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(AppDimens.SpaceMedium))
        }
    }
}
