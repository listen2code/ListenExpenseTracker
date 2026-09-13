package com.listen.expensetracker.features.settings.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import com.listen.expensetracker.data.engine.TransactionCalculationEngine
import com.listen.expensetracker.features.settings.viewmodel.SettingsUiState

/**
 * 设置画面专用 UI 状态持有者 (SettingsStateHolder)。
 *
 * 【架构设计】：
 * 遵循 Google 官方推荐的 "Plain State Holder" 模式。
 * 核心职责：仅负责持有 UI 自身的交互状态（如滚动位置、标题计算）。
 * 副作用与系统级交互（如文件选择、ActivityResult 契约）在 [SettingsEffects] 中独立挂载，
 * 与本状态容器职责严格区分。
 */
class SettingsStateHolder(
    val listState: LazyListState,
    val currentMonthTitle: String
)

/**
 * 创建并记住 [SettingsStateHolder] 的 Composable 辅助函数。
 */
@Composable
fun rememberSettingsStateHolder(
    state: SettingsUiState,
    targetMonthOffset: Int = 0
): SettingsStateHolder {
    val lang = state.language

    // 1. 月份标题计算
    val (_, _, currentMonthTitle) = remember(targetMonthOffset, lang) {
        TransactionCalculationEngine.getMonthRangeAndTitle(targetMonthOffset, lang)
    }

    // 2. 列表滚动状态
    val listState = rememberSaveable(saver = LazyListState.Saver) {
        LazyListState()
    }

    return remember(listState, currentMonthTitle) {
        SettingsStateHolder(
            listState = listState,
            currentMonthTitle = currentMonthTitle
        )
    }
}
