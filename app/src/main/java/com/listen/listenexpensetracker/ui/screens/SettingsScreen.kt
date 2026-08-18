package com.listen.listenexpensetracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.AppLanguage
import com.listen.arch.i18n.StringsRes
import com.listen.arch.sync.SyncStatus
import com.listen.listenexpensetracker.ui.state.TransactionsIntent
import com.listen.listenexpensetracker.ui.state.TransactionsUiState
import com.listen.uicomponent.components.SurfaceCard
import com.listen.uicomponent.theme.AccentColor
import com.listen.uicomponent.theme.ExpenseRed
import com.listen.uicomponent.theme.IncomeGreen
import com.listen.uicomponent.theme.ThemeMode
import com.listen.uicomponent.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: TransactionsUiState,
    onIntent: (TransactionsIntent) -> Unit,
    onOpenApmInspector: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenImportSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClearDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(state.monthlyBudget.toString()) }

    val sym = state.currencySymbol
    val lang = state.language

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = StringsRes.get("settings_title", lang),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Theme Mode Selection
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("settings_appearance", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ThemeMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = state.themeMode == mode,
                                onClick = { onIntent(TransactionsIntent.ChangeThemeMode(mode)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ThemeMode.entries.size)
                            ) {
                                val text = when (mode) {
                                    ThemeMode.LIGHT -> StringsRes.get("theme_light", lang)
                                    ThemeMode.DARK -> StringsRes.get("theme_dark", lang)
                                    ThemeMode.SYSTEM -> StringsRes.get("theme_system", lang)
                                }
                                Text(text, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Accent Color Palette
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("settings_accent", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val isSelected = state.accentColor == accent
                            val color = parseHexColor(accent.colorHex)

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onIntent(TransactionsIntent.ChangeAccentColor(accent)) }
                            )
                        }
                    }
                }
            }

            // Currency & Language
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("settings_currency_lang", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Currency row
                    val currencies = listOf("￥" to "CNY", "$" to "USD", "€" to "EUR", "£" to "GBP", "円" to "JPY")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        currencies.forEachIndexed { index, (symbol, label) ->
                            SegmentedButton(
                                selected = state.currencySymbol == symbol,
                                onClick = { onIntent(TransactionsIntent.ChangeCurrencySymbol(symbol)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = currencies.size)
                            ) {
                                Text("$symbol $label", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.entries.forEachIndexed { index, l ->
                            SegmentedButton(
                                selected = state.language == l.code,
                                onClick = { onIntent(TransactionsIntent.ChangeLanguage(l.code)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppLanguage.entries.size)
                            ) {
                                Text(l.displayName, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Monthly Budget
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("settings_budget", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            budgetInput = state.monthlyBudget.toString()
                            showBudgetDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${StringsRes.get("monthly_budget", lang)}: $sym${String.format("%.0f", state.monthlyBudget)}", fontSize = 12.sp)
                    }
                }
            }

            // Cloud Sync Section
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = StringsRes.get("settings_cloud", lang),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        val statusText = when (state.syncState.status) {
                            SyncStatus.IDLE -> "就绪"
                            SyncStatus.SYNCING -> "同步中..."
                            SyncStatus.SUCCESS -> "同步成功"
                            SyncStatus.ERROR -> "同步失败"
                        }
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            color = if (state.syncState.status == SyncStatus.SUCCESS) IncomeGreen else MaterialTheme.colorScheme.primary
                        )
                    }

                    if (state.syncState.lastSyncTimestamp > 0) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        Text(
                            text = "上次同步：${sdf.format(Date(state.syncState.lastSyncTimestamp))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onIntent(TransactionsIntent.TriggerCloudBackup) },
                            enabled = state.syncState.status != SyncStatus.SYNCING,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("备份至云端", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onIntent(TransactionsIntent.TriggerCloudRestore) },
                            enabled = state.syncState.status != SyncStatus.SYNCING,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("从云端恢复", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Local Data Backup & Restore Section
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("settings_local_backup", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExportJson,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(StringsRes.get("export_json", lang), fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = onExportCsv,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(StringsRes.get("export_csv", lang), fontSize = 11.sp)
                        }

                        Button(
                            onClick = onOpenImportSheet,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(StringsRes.get("import_json", lang), fontSize = 11.sp)
                        }
                    }
                }
            }

            // APM & Data Operations Section
            SurfaceCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(
                        text = StringsRes.get("settings_apm", lang),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenApmInspector,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text(StringsRes.get("apm_inspector", lang), fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { showAboutDialog = true },
                            modifier = Modifier.weight(0.8f)
                        ) {
                            Text(StringsRes.get("about_app", lang), fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onIntent(TransactionsIntent.SeedDemoData) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(StringsRes.get("seed_demo", lang), fontSize = 11.sp)
                        }

                        Button(
                            onClick = { showClearDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(StringsRes.get("clear_all", lang), fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(StringsRes.get("budget_dialog_title", lang)) },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("预算金额 ($sym)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amount = budgetInput.toDoubleOrNull() ?: 5000.0
                        if (amount > 0) {
                            onIntent(TransactionsIntent.UpdateMonthlyBudget(amount))
                            showBudgetDialog = false
                        }
                    }
                ) {
                    Text(StringsRes.get("btn_save", lang), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text(StringsRes.get("btn_cancel", lang))
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(StringsRes.get("about_app", lang)) },
            text = {
                Column {
                    Text("版本: v1.0.0 (Build 2026.08)", fontWeight = FontWeight.SemiBold)
                    Text("架构: MVI + Clean Architecture", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("核心 SDK: ListenArch & ListenUiComponent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("特性: 本地优先 (Local-First)、APM 链路追踪、多币种符号切换、收支双维度统计、云端备份与全量 JSON/CSV 数据导出导入。", fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(StringsRes.get("btn_done", lang))
                }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(StringsRes.get("confirm_clear_title", lang)) },
            text = { Text(StringsRes.get("confirm_clear_desc", lang)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onIntent(TransactionsIntent.ClearAllData)
                        showClearDialog = false
                    }
                ) {
                    Text(StringsRes.get("btn_delete", lang), color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(StringsRes.get("btn_cancel", lang))
                }
            }
        )
    }
}
