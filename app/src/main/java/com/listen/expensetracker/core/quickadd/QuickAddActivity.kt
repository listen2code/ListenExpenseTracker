package com.listen.expensetracker.core.quickadd

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.listen.expensetracker.R
import com.listen.expensetracker.core.security.BiometricSecurityManager
import com.listen.expensetracker.core.security.SecurityPreferences
import com.listen.expensetracker.core.shortcut.ExpenseShortcutManager
import com.listen.expensetracker.data.db.AppDatabase
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.db.TransactionType
import com.listen.expensetracker.data.i18n.ExpenseStrings
import com.listen.expensetracker.data.pref.ExpenseDataStoreManager
import com.listen.expensetracker.data.pref.ExpensePreferences
import com.listen.expensetracker.features.transactions.components.TransactionSheet
import com.listen.expensetracker.widget.ListenExpenseAppWidgetProvider
import com.listen.uicomponent.theme.ListenTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 闪电记账半透明浮动 Activity (QuickAddActivity)。
 * 核心特性：
 * 1. 透明无边框背景：在任意第三方应用或桌面触发时不切换全屏宿主，直接在当前界面顶层拉起半屏记账弹窗；
 * 2. 极速保存并退出：记完即走，保存后直接 finish() 返回用户刚才正在使用的 App；
 * 3. 隐私兼顾：若开启生物识别锁则先校验指纹，未开启则直开记账键盘。
 */
class QuickAddActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        disableWindowAnimation()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ExpenseStrings.init()

        val initialData = ListenExpenseAppWidgetProvider.parseQuickAddIntent(intent)
        val initialCategory = initialData?.first
        val initialType = initialData?.second ?: TransactionType.EXPENSE

        val isBiometricEnabled = SecurityPreferences.isBiometricEnabled(this) &&
            BiometricSecurityManager.isBiometricOrCredentialAvailable(this)

        setContent {
            val prefManager = remember { ExpenseDataStoreManager.getInstance(applicationContext) }
            val preferences by prefManager.preferencesFlow.collectAsState(initial = ExpensePreferences())

            var isUnlocked by remember { mutableStateOf(!isBiometricEnabled) }

            if (!isUnlocked) {
                remember(Unit) {
                    BiometricSecurityManager.promptUnlock(
                        activity = this@QuickAddActivity,
                        title = getString(R.string.qs_tile_quick_add_label),
                        subtitle = getString(R.string.app_name),
                        onSuccess = { isUnlocked = true },
                        onError = { _, _ -> finish() }
                    )
                }
            } else {
                ListenTheme(
                    themeMode = preferences.themeMode,
                    accentColor = preferences.accentColor,
                    pureBlackDark = preferences.isPureBlackDark
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        TransactionSheet(
                            currencySymbol = preferences.currencySymbol,
                            initialCategoryId = initialCategory,
                            initialType = initialType,
                            onDismiss = { finish() },
                            onSave = { entity -> saveTransactionAndClose(entity) },
                            onSaveAndContinue = { entity -> saveTransactionContinuously(entity) },
                            lang = preferences.language
                        )
                    }
                }
            }
        }
    }

    private fun saveTransactionAndClose(entity: TransactionEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            db.transactionDao().insertTransaction(entity)
            ExpenseShortcutManager.updateDynamicShortcuts(applicationContext)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@QuickAddActivity, getString(R.string.quick_add_saved_toast), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveTransactionContinuously(entity: TransactionEntity) {
        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getInstance(applicationContext)
            db.transactionDao().insertTransaction(entity)
            ExpenseShortcutManager.updateDynamicShortcuts(applicationContext)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@QuickAddActivity, getString(R.string.quick_add_saved_toast), Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun finish() {
        super.finish()
        disableWindowAnimation()
    }

    private fun disableWindowAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        fun createIntent(context: Context, categoryId: String? = null, type: String = TransactionType.EXPENSE): Intent {
            return Intent(context, QuickAddActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(ListenExpenseAppWidgetProvider.EXTRA_QUICK_ADD_CATEGORY, categoryId)
                putExtra(ListenExpenseAppWidgetProvider.EXTRA_QUICK_ADD_TYPE, type)
            }
        }
    }
}
