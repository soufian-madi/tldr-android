package com.example.tldr_ai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.HistoryItem
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.data.model.StreamEvent
import com.example.tldr_ai.data.model.SummaryResult
import com.example.tldr_ai.data.model.SummaryUiState
import com.example.tldr_ai.data.repository.HistoryRepository
import com.example.tldr_ai.data.repository.SettingsRepository
import com.example.tldr_ai.data.repository.SummaryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SummaryRepository()
    private val historyRepository = HistoryRepository(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow<SummaryUiState>(SummaryUiState.Idle)
    val uiState: StateFlow<SummaryUiState> = _uiState.asStateFlow()

    private val _historyItems = MutableStateFlow(historyRepository.getItems())
    val historyItems: StateFlow<List<HistoryItem>> = _historyItems.asStateFlow()

    private val _selectedModel = MutableStateFlow(settingsRepository.getSelectedModel())
    val selectedModel: StateFlow<AiModel> = _selectedModel.asStateFlow()

    private var lastMode: InputMode = InputMode.URL
    private var lastInput: String = ""

    fun setModel(model: AiModel) {
        settingsRepository.setSelectedModel(model)
        _selectedModel.value = model
    }

    fun summarize(mode: InputMode, input: String) {
        if (input.isBlank()) return

        lastMode = mode
        lastInput = input
        val model = _selectedModel.value

        viewModelScope.launch {
            _uiState.value = SummaryUiState.Loading

            // Resolve the article text (fetch first for URLs so we can report fetch failures).
            val text = when (mode) {
                InputMode.TEXT -> input
                InputMode.URL -> {
                    val fetchResult = repository.fetch(input)
                    if (fetchResult.isFailure) {
                        _uiState.value = SummaryUiState.Error(
                            fetchResult.exceptionOrNull()?.message ?: "Failed to fetch article"
                        )
                        return@launch
                    }
                    fetchResult.getOrThrow()
                }
            }

            val reasoning = StringBuilder()
            repository.summarizeStream(text, model).collect { event ->
                when (event) {
                    is StreamEvent.Reasoning -> {
                        reasoning.append(event.delta)
                        _uiState.value = SummaryUiState.Reasoning(reasoning.toString())
                    }

                    is StreamEvent.Done -> {
                        saveToHistory(event.result)
                        _uiState.value = SummaryUiState.Success(event.result)
                    }

                    is StreamEvent.Failure -> {
                        _uiState.value = SummaryUiState.Error(event.message)
                    }
                }
            }
        }
    }

    private fun saveToHistory(result: SummaryResult) {
        if (result.summary == "Paywall") return
        val historyItem = HistoryItem(
            id = UUID.randomUUID().toString(),
            url = if (lastMode == InputMode.URL) lastInput else null,
            title = result.originalTitle,
            clickbaitScore = result.clickbaitScore,
            summary = result.summary,
            timestamp = System.currentTimeMillis()
        )
        historyRepository.addItem(historyItem)
        _historyItems.value = historyRepository.getItems()
    }

    fun resetState() {
        _uiState.value = SummaryUiState.Idle
    }

    fun refreshHistory() {
        _historyItems.value = historyRepository.getItems()
    }

    fun clearHistory() {
        historyRepository.clearAll()
        _historyItems.value = emptyList()
    }
}
