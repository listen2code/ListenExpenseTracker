package com.listen.listenexpensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BakingViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> =
        MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> =
        _uiState.asStateFlow()

    private val generativeModel: GenerativeModel by lazy {
        Firebase.ai.generativeModel("gemini-2.5-flash")
    }

    fun sendPrompt(
        prompt: String
    ) {
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = generativeModel.generateContent(
                    prompt
                )
                response.text?.let { outputText ->
                    _uiState.value = UiState.Success(outputText)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.localizedMessage ?: "")
            }
        }
    }
}
