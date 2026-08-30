package com.listen.expensetracker.core.route

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.listen.arch.mvi.BaseViewModel

/**
 * Universal Generic MVI Route Component.
 * Automatically binds any BaseViewModel instance to its stateless Screen Composable,
 * collecting state as Compose State and providing direct intent dispatching with zero repetitive boilerplate.
 *
 * @param S UiState type
 * @param I UiIntent type
 * @param VM BaseViewModel type
 * @param viewModel ViewModel instance
 * @param content Stateless Screen Composable slot receiving (state, onIntent)
 */
@Composable
inline fun <S : Any, I : Any, reified VM : BaseViewModel<S, I>> CommonRoute(
    viewModel: VM = viewModel(),
    crossinline content: @Composable (state: S, onIntent: (I) -> Unit) -> Unit
) {
    val state by viewModel.viewState.collectAsState()
    content(state, viewModel::handleIntent)
}
