package com.listen.expensetracker.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.listen.arch.i18n.AppLanguage
import com.listen.arch.i18n.StringsRes
import com.listen.arch.sync.SyncStatus
import com.listen.expensetracker.auth.GoogleAuthManager
import com.listen.expensetracker.ui.components.CategoryManageDialog
import com.listen.expensetracker.ui.components.GoogleAccountChooserDialog
import com.listen.expensetracker.ui.state.TransactionsIntent
import com.listen.expensetracker.ui.state.TransactionsUiState
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
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showClearDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showGoogleChooserDialog by remember { mutableStateOf(false) }
    var showCategoryManageDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var budgetInput by remember { mutableStateOf(state.monthlyBudget.toString()) }

    // Google Sign-In Activity Result Launcher
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val authResult = GoogleAuthManager.parseSignInResult(result.data)
            authResult.onSuccess { acc ->
                onIntent(
                    TransactionsIntent.LinkGoogleAccount(
                        email = acc.email ?: "google.user@gmail.com",
                        displayName = acc.displayName,
                        avatarUrl = acc.photoUrl?.toString()
                    )
                )
            }.onFailure {
                showGoogleChooserDialog = true
            }
        } else {
            showGoogleChooserDialog = true
        }
    }

    val sym = state.currencySymbol
    val lang = state.language
    val isGoogleLoggedIn = state.googleAccountEmail != null

    val currencies = listOf(
        Triple("￥", "CNY", "人民币 (CNY)"),
        Triple("$", "USD", "美元 (USD)"),
        Triple("€", "EUR", "欧元 (EUR)"),
        Triple("£", "GBP", "英镑 (GBP)"),
        Triple("¥", "JPY", "日元 (JPY)")
    )

    fun getCurrencyLabel(symbol: String): String {
        return currencies.find { it.first == symbol }?.third ?: "人民币 (CNY)"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ==========================================
        // GROUP 1: ☁️ Google 账户连携与云端服务
        // ==========================================
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "☁️ " + StringsRes.get("settings_cloud", lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    val statusText = when (state.syncState.status) {
                        SyncStatus.IDLE -> StringsRes.get("cloud_status_idle", lang)
                        SyncStatus.SYNCING -> StringsRes.get("cloud_status_syncing", lang)
                        SyncStatus.SUCCESS -> StringsRes.get("cloud_status_success", lang)
                        SyncStatus.ERROR -> StringsRes.get("cloud_status_error", lang)
                    }
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (state.syncState.status == SyncStatus.SUCCESS) IncomeGreen else MaterialTheme.colorScheme.primary
                    )
                }

                // Google Account Profile Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isGoogleLoggedIn) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                        .clickable {
                            if (!isGoogleLoggedIn) {
                                try {
                                    val signInIntent = GoogleAuthManager.getClient(context).signInIntent
                                    googleSignInLauncher.launch(signInIntent)
                                } catch (_: Exception) {
                                    showGoogleChooserDialog = true
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isGoogleLoggedIn) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (state.googleDisplayName?.firstOrNull() ?: state.googleAccountEmail?.firstOrNull() ?: 'G').uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Google Account",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = if (isGoogleLoggedIn) (state.googleDisplayName ?: "Google 账户已连携") else "未连携 Google 账户",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = state.googleAccountEmail ?: "点击调起 Google SDK 登录以开启云端同步",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isGoogleLoggedIn) {
                            IconButton(
                                onClick = { showLogoutConfirmDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "Unlink",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Link Account",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                if (state.syncState.lastSyncTimestamp > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = "Synced", tint = IncomeGreen, modifier = Modifier.size(12.dp))
                        Text(
                            text = "${StringsRes.get("cloud_last_sync", lang)}${sdf.format(Date(state.syncState.lastSyncTimestamp))}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onIntent(TransactionsIntent.TriggerCloudBackup) },
                        enabled = isGoogleLoggedIn && state.syncState.status != SyncStatus.SYNCING,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        if (state.syncState.status == SyncStatus.SYNCING) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Backup", modifier = Modifier.size(15.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(StringsRes.get("cloud_backup_btn", lang), fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { onIntent(TransactionsIntent.TriggerCloudRestore) },
                        enabled = isGoogleLoggedIn && state.syncState.status != SyncStatus.SYNCING,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Restore", modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(StringsRes.get("cloud_restore_btn", lang), fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }

        // ==========================================
        // GROUP 2: 🎨 个性化与偏好设置
        // ==========================================
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "🎨 个性化与偏好",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                // 1. Theme Mode
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(StringsRes.get("settings_appearance", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text(text, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 2. Accent Color Palette
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(StringsRes.get("settings_accent", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = when (lang) {
                                "en" -> state.accentColor.nameEn
                                "ja" -> state.accentColor.nameJa
                                else -> state.accentColor.nameZh
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val isSelected = state.accentColor == accent
                            val color = parseHexColor(accent.colorHex)

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onIntent(TransactionsIntent.ChangeAccentColor(accent)) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Language & Currency Clickable Tiles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Currency Tile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showCurrencyDialog = true }
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(StringsRes.get("settings_currency", lang), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("$sym  ${getCurrencyLabel(sym)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                        }
                    }

                    // Category Tags Management Tile
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { showCategoryManageDialog = true }
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("分类标签", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("管理收支标签", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                // Multi-Language Toggle
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(StringsRes.get("settings_language", lang), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        AppLanguage.entries.forEachIndexed { index, l ->
                            SegmentedButton(
                                selected = state.language == l.code,
                                onClick = { onIntent(TransactionsIntent.ChangeLanguage(l.code)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = AppLanguage.entries.size)
                            ) {
                                Text(l.displayName, fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // GROUP 3: 💰 预算控制
        // ==========================================
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "💰 " + StringsRes.get("settings_budget", lang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "当前月度预算: $sym${String.format("%.0f", state.monthlyBudget)} · 剩余 $sym${String.format("%.0f", state.remainingBudget)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        budgetInput = state.monthlyBudget.toString()
                        showBudgetDialog = true
                    }
                ) {
                    Text("修改预算", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // ==========================================
        // GROUP 4: 💾 数据备份与恢复
        // ==========================================
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "💾 本地数据管理与备份",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onExportJson() }
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = "JSON", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("导出 JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("结构化备份", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onExportCsv() }
                            .padding(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = "CSV", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("导出 CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("Excel表格", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .clickable { onOpenImportSheet() }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.FileUpload, contentDescription = "Import", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Column {
                                Text("从本地导入 JSON 备份", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text("支持无缝还原历史记账与账户", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Go", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // ==========================================
        // GROUP 5: 🛠️ 运维与演示工具 (Responsive 2x2 Clean Grid, Zero Wrap)
        // ==========================================
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "🛠️ 系统工具与测试",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenApmInspector,
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text(StringsRes.get("apm_inspector", lang), fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text(StringsRes.get("about_app", lang), fontSize = 11.sp, maxLines = 1)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onIntent(TransactionsIntent.SeedDemoData) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text("🌱 填充数据", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                    ) {
                        Text(StringsRes.get("clear_all", lang), fontSize = 11.sp, color = Color.White, maxLines = 1)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Google Account Chooser Dialog (Fallback & Account Selector)
    if (showGoogleChooserDialog) {
        GoogleAccountChooserDialog(
            currentEmail = state.googleAccountEmail,
            onAccountSelected = { email ->
                onIntent(TransactionsIntent.LinkGoogleAccount(email = email))
            },
            onDismiss = { showGoogleChooserDialog = false },
            lang = lang
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmDialog = false },
            title = { Text("确认退出 Google 账户", fontWeight = FontWeight.Bold) },
            text = { Text("退出登录后，本地已存账单将继续保留，但将暂停云端实时备份与跨端同步功能。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        GoogleAuthManager.signOut(context)
                        onIntent(TransactionsIntent.UnlinkGoogleAccount)
                        showLogoutConfirmDialog = false
                    }
                ) {
                    Text("退出登录", color = ExpenseRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmDialog = false }) {
                    Text(StringsRes.get("btn_cancel", lang))
                }
            }
        )
    }

    // Currency Selection Dialog
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Text(StringsRes.get("currency_dialog_title", lang), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    currencies.forEach { (symbol, _, label) ->
                        val isSelected = state.currencySymbol == symbol
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .clickable {
                                    onIntent(TransactionsIntent.ChangeCurrencySymbol(symbol))
                                    showCurrencyDialog = false
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = symbol,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "💡 提示：切换记账币种将更新全站展示符号，账单历史数值将保持不变以保障账目原始精准度。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text(StringsRes.get("btn_done", lang))
                }
            }
        )
    }

    // Category Management Dialog
    if (showCategoryManageDialog) {
        CategoryManageDialog(
            onCategoryChanged = {},
            onDismiss = { showCategoryManageDialog = false },
            lang = lang
        )
    }

    if (showBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text(StringsRes.get("budget_dialog_title", lang)) },
            text = {
                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = { budgetInput = it },
                    label = { Text("${StringsRes.get("monthly_budget", lang)} ($sym)") },
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
                    Text("lExpense", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    Text("版本: v0.0.1 (Build 2026.08)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("架构: MVI + Clean Architecture", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("核心 SDK: ListenArch & ListenUiComponent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("特性: 本地优先 (Local-First)、APM 链路追踪、Google 账户多端加密云备份与恢复、多币种符号切换、收支双维度统计、全量 JSON/CSV 数据导出导入。", fontSize = 12.sp)
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
