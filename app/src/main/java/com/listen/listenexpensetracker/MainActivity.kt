package com.listen.listenexpensetracker

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.listen.arch.apm.CrashHandler
import com.listen.arch.i18n.StringsRes
import com.listen.listenexpensetracker.ui.screens.ImportBackupSheet
import com.listen.listenexpensetracker.ui.screens.SettingsScreen
import com.listen.listenexpensetracker.ui.screens.StatisticsScreen
import com.listen.listenexpensetracker.ui.screens.TransactionsScreen
import com.listen.listenexpensetracker.ui.state.TransactionsEffect
import com.listen.listenexpensetracker.ui.state.TransactionsIntent
import com.listen.listenexpensetracker.ui.viewmodel.TransactionsViewModel
import com.listen.uicomponent.apm.LogInspectorSheet
import com.listen.uicomponent.theme.ListenTheme
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: TransactionsViewModel by viewModels {
        TransactionsViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.init(this)

        setContent {
            val state by viewModel.viewState.collectAsState()
            val apmLogs by viewModel.apmLogsUiFlow.collectAsState()
            var showApmSheet by remember { mutableStateOf(false) }
            var showImportSheet by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                viewModel.viewEffect.collectLatest { effect ->
                    when (effect) {
                        is TransactionsEffect.ShowToast -> {
                            Toast.makeText(this@MainActivity, effect.message, Toast.LENGTH_SHORT).show()
                        }
                        is TransactionsEffect.TransactionAddedSuccess -> {
                            // Haptic feedback
                        }
                    }
                }
            }

            ListenTheme(
                themeMode = state.themeMode,
                accentColor = state.accentColor
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ListenExpenseTrackerApp(
                        viewModel = viewModel,
                        onOpenApm = { showApmSheet = true },
                        onExportJson = {
                            val json = viewModel.exportBackupJson()
                            shareText(json, "分享 JSON 账单备份")
                        },
                        onExportCsv = {
                            val csv = viewModel.exportBackupCsv()
                            shareText(csv, "分享 CSV 账单表格")
                        },
                        onOpenImportSheet = { showImportSheet = true }
                    )

                    if (showImportSheet) {
                        ImportBackupSheet(
                            onDismiss = { showImportSheet = false },
                            onConfirmImport = { json ->
                                viewModel.handleIntent(TransactionsIntent.ImportBackupData(json))
                            }
                        )
                    }

                    if (showApmSheet) {
                        LogInspectorSheet(
                            logs = apmLogs,
                            onClearLogs = { viewModel.clearApmLogs() },
                            onExportLogs = {
                                val logText = viewModel.exportApmLogs()
                                shareText(logText, "分享 APM 日志")
                            },
                            onDismiss = { showApmSheet = false }
                        )
                    }
                }
            }
        }
    }

    private fun shareText(content: String, chooserTitle: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, content)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, chooserTitle)
        startActivity(shareIntent)
    }
}

@Composable
fun ListenExpenseTrackerApp(
    viewModel: TransactionsViewModel,
    onOpenApm: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onOpenImportSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.viewState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val lang = state.language

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Transactions") },
                    label = { Text(StringsRes.get("nav_transactions", lang)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = "Statistics") },
                    label = { Text(StringsRes.get("nav_statistics", lang)) }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(StringsRes.get("nav_settings", lang)) }
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        val screenModifier = Modifier.padding(innerPadding)
        when (selectedTab) {
            0 -> TransactionsScreen(state = state, onIntent = viewModel::handleIntent, modifier = screenModifier)
            1 -> StatisticsScreen(state = state, onIntent = viewModel::handleIntent, modifier = screenModifier)
            2 -> SettingsScreen(
                state = state,
                onIntent = viewModel::handleIntent,
                onOpenApmInspector = onOpenApm,
                onExportJson = onExportJson,
                onExportCsv = onExportCsv,
                onOpenImportSheet = onOpenImportSheet,
                modifier = screenModifier
            )
        }
    }
}
