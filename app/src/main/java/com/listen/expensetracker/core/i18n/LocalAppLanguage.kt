package com.listen.expensetracker.core.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.listen.arch.i18n.StringsRes

/**
 * 全局语言状态单例，供非 Compose 环境（小组件、后台同步、Notification、Worker 等）及 Composable 读取与兜底。
 * 内部基于 Compose [mutableStateOf] 实现，在任何 @Composable 作用域内被读取时，
 * 语言更新将自动触发相应 Composable 的局部重组刷新。
 */
object AppLanguage {
    var current: String by mutableStateOf("zh")
        internal set

    fun update(lang: String) {
        current = lang
    }
}

/**
 * 全局 Compose 隐式语言环境提供者。
 * 默认回退至 [AppLanguage.current]，在根节点由 CompositionLocalProvider 动态注入最新设置。
 */
val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.current }

/**
 * 全局统一无参翻译扩展函数。
 * 无论是 Composable 还是非 Composable 环境，均可直接调用，无缝感知全局语言。
 *
 * 示例：
 * ```kotlin
 * Text(text = AppStrings.TOTAL_EXPENSE.tr())
 * ```
 */
fun String.tr(): String = StringsRes.get(this, AppLanguage.current)
