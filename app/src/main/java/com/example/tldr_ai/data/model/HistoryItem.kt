package com.example.tldr_ai.data.model

data class HistoryItem(
    val id: String,
    val url: String?,
    val title: String?,
    val clickbaitScore: Int,
    val summary: String,
    val timestamp: Long
)
