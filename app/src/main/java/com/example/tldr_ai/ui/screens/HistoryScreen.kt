package com.example.tldr_ai.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.HistoryItem
import com.example.tldr_ai.ui.components.HistoryItemCard
import java.util.Calendar

private sealed interface HistoryListItem {
    data class Header(val label: String) : HistoryListItem
    data class Entry(val item: HistoryItem) : HistoryListItem
}

private fun groupItemsByDate(items: List<HistoryItem>): List<HistoryListItem> {
    val now = Calendar.getInstance()

    fun daysBetween(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val diffMs = today.timeInMillis - cal.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    val sections = linkedMapOf(
        "Today" to mutableListOf<HistoryItem>(),
        "Yesterday" to mutableListOf(),
        "Previous 7 days" to mutableListOf(),
        "Previous 30 days" to mutableListOf(),
        "Older" to mutableListOf()
    )

    for (item in items) {
        val days = daysBetween(item.timestamp)
        when {
            days == 0 -> sections["Today"]!!.add(item)
            days == 1 -> sections["Yesterday"]!!.add(item)
            days <= 7 -> sections["Previous 7 days"]!!.add(item)
            days <= 30 -> sections["Previous 30 days"]!!.add(item)
            else -> sections["Older"]!!.add(item)
        }
    }

    return sections.flatMap { (label, groupItems) ->
        if (groupItems.isEmpty()) emptyList()
        else listOf(HistoryListItem.Header(label)) + groupItems.map { HistoryListItem.Entry(it) }
    }
}

@Composable
fun HistoryScreen(
    items: List<HistoryItem>,
    topPadding: Dp = 0.dp,
    bottomPadding: Dp = 0.dp
) {
    if (items.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding, bottom = bottomPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No summaries yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val listItems = groupItemsByDate(items)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = topPadding + 8.dp,
                bottom = bottomPadding + 16.dp
            )
        ) {
            items(listItems, key = { item ->
                when (item) {
                    is HistoryListItem.Header -> "header_${item.label}"
                    is HistoryListItem.Entry -> item.item.id
                }
            }) { listItem ->
                when (listItem) {
                    is HistoryListItem.Header -> {
                        Text(
                            text = listItem.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp, bottom = 8.dp)
                        )
                    }
                    is HistoryListItem.Entry -> {
                        HistoryItemCard(
                            item = listItem.item,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
