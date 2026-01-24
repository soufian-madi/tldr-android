package com.example.tldr_ai.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.SummaryUiState
import com.example.tldr_ai.data.repository.SummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: SummaryRepository = SummaryRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    fun summarize(mode: InputMode, input: String) {
        if (input.isBlank()) return

        viewModelScope.launch {
            _uiState.value = SummaryUiState.Loading

            val result = when (mode) {
                InputMode.TEXT -> repository.summarizeFromText(input)
                InputMode.URL -> repository.summarizeFromUrl(input)
            }

            _uiState.value = result.fold(
                onSuccess = { SummaryUiState.Success(it) },
                onFailure = { SummaryUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun resetState() {
        _uiState.value = SummaryUiState.Idle
    }
}
