package com.example.tldr_ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tldr_ai.data.model.HistoryItem
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.SummaryUiState
import com.example.tldr_ai.data.repository.HistoryRepository
import com.example.tldr_ai.data.repository.SummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SummaryRepository()
    private val historyRepository = HistoryRepository(application)

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private val _historyItems = MutableStateFlow(historyRepository.getItems())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()

    private var lastMode: InputMode = InputMode.URL
    private var lastInput: String = ""

    fun summarize(mode: InputMode, input: String) {
        if (input.isBlank()) return

        lastMode = mode
        lastInput = input

        viewModelScope.launch {
            _uiState.value = SummaryUiState.Loading

            val result = when (mode) {
                InputMode.TEXT -> repository.summarizeFromText(input)
                InputMode.URL -> repository.summarizeFromUrl(input)
            }

            _uiState.value = result.fold(
                onSuccess = { summaryResult ->
                    if (summaryResult.summary != "Paywall") {
                        val historyItem = HistoryItem(
                            id = UUID.randomUUID().toString(),
                            url = if (lastMode == InputMode.URL) lastInput else null,
                            title = summaryResult.originalTitle,
                            clickbaitScore = summaryResult.clickbaitScore,
                            summary = summaryResult.summary,
                            timestamp = System.currentTimeMillis()
                        )
                        historyRepository.addItem(historyItem)
                        _historyItems.value = historyRepository.getItems()
                    }
                    SummaryUiState.Success(summaryResult)
                },
                onFailure = { SummaryUiState.Error(it.message ?: "Unknown error") }
            )
        }
    }

    fun resetState() {
        _uiState.value = SummaryUiState.Idle
    }

    fun clearHistory() {
        historyRepository.clearAll()
        _historyItems.value = emptyList()
    }
}
