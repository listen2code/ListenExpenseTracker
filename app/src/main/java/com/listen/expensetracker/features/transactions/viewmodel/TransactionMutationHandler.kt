package com.listen.expensetracker.features.transactions.viewmodel

import android.app.Application
import com.listen.arch.apm.ApmLogChannel
import com.listen.arch.apm.TraceManager
import com.listen.arch.i18n.tr
import com.listen.arch.mvi.CommonUiEffect
import com.listen.expensetracker.data.db.TransactionDao
import com.listen.expensetracker.data.db.TransactionEntity
import com.listen.expensetracker.data.engine.DemoDataEngine
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.data.i18n.AppStrings
import com.listen.expensetracker.data.model.AccountRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 账单数据增删改与演示数据生成代理 (TransactionMutationHandler)。
 * 将持久化写操作与 ViewModel 状态流转解耦，遵循 Rule 3 单一职责与行数约束。
 */
class TransactionMutationHandler(
    private val application: Application,
    private val dao: TransactionDao,
    private val scope: CoroutineScope,
    private val emitEffect: (CommonUiEffect) -> Unit,
    private val onRestore: (TransactionEntity) -> Unit
) {
    fun addTransaction(intent: TransactionsIntent.AddTransaction, traceId: String) = scope.launch {
        val entity = TransactionEntity(
            type = intent.type, categoryId = intent.categoryId, categoryName = intent.categoryName,
            categoryIcon = intent.categoryIcon, categoryColorHex = intent.categoryColorHex,
            amount = intent.amount, note = intent.note, accountType = intent.accountType, timestamp = intent.timestamp
        )
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "InsertTransaction", traceId = traceId) {
            dao.insertTransaction(entity)
        }
    }

    fun updateTransaction(transaction: TransactionEntity, traceId: String) = scope.launch {
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "UpdateTransaction", traceId = traceId) {
            dao.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(id: String, traceId: String, lang: String) = scope.launch {
        val entity = dao.getTransactionById(id) ?: return@launch
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "DeleteTransaction", traceId = traceId) {
            dao.deleteTransaction(entity)
        }
        emitEffect(CommonUiEffect.ShowSnackbar(
            message = AppStrings.UNDO_DELETE_TOAST.tr(lang),
            actionLabel = AppStrings.UNDO_ACTION_LABEL.tr(lang),
            onAction = { onRestore(entity) }
        ))
    }

    fun restoreDeletedTransaction(tx: TransactionEntity, traceId: String, lang: String) = scope.launch {
        TraceManager.trace(channel = ApmLogChannel.DB, tag = "RoomDB", operationName = "RestoreTransaction", traceId = traceId) {
            dao.insertTransaction(tx)
        }
        emitEffect(CommonUiEffect.ShowToast(AppStrings.UNDO_SUCCESS_TOAST.tr(lang)))
    }

    fun seedDemoData(monthOffset: Int, lang: String) = scope.launch {
        val (startTs, endTs, title) = TransactionCalculationEngine.getMonthRangeAndTitle(monthOffset, lang)
        val count = dao.getTransactionCountInRange(startTs, endTs)
        if (count > 0) {
            emitEffect(CommonUiEffect.ShowToast(AppStrings.SEED_MONTH_HAS_DATA_ERROR.tr(lang)))
            return@launch
        }
        val accounts = AccountRepository.getAllAccounts().map { it.key }.ifEmpty { listOf("CASH", "BANK", "CREDIT") }
        val generated = DemoDataEngine.generate(monthOffset, lang, accounts)
        dao.insertTransactions(generated)
        emitEffect(CommonUiEffect.ShowToast(AppStrings.SEED_MONTH_SUCCESS_TOAST.tr(lang).format(title, generated.size)))
    }
}
