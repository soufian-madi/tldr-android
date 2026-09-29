package com.example.tldr_ai.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.HistoryItem
import com.example.tldr_ai.ui.components.HistoryRow
import com.example.tldr_ai.ui.theme.OverlineStyle
import com.example.tldr_ai.ui.theme.palette
import java.util.Calendar

private sealed interface HistoryListItem {
    data class Header(val label: String) : HistoryListItem
    data class Entry(val item: HistoryItem) : HistoryListItem
}

private fun groupItemsByDate(items: List<HistoryItem>): List<HistoryListItem> {
    fun daysBetween(timestamp: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        listOf(today, cal).forEach {
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MILLISECOND, 0)
        }
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
    onBack: () -> Unit,
    onDelete: (HistoryItem) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        ClearHistoryDialog(
            count = items.size,
            onConfirm = {
                onClearAll()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = palette.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = "History",
                style = MaterialTheme.typography.titleLarge,
                color = palette.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.weight(1f))
            if (items.isNotEmpty()) {
                IconButton(onClick = { showClearDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Clear history",
                        tint = palette.muted,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (items.isEmpty()) {
            EmptyHistory()
            return@Column
        }

        val listItems = groupItemsByDate(items)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp)
        ) {
            items(listItems, key = { entry ->
                when (entry) {
                    is HistoryListItem.Header -> "header_${entry.label}"
                    is HistoryListItem.Entry -> entry.item.id
                }
            }) { entry ->
                when (entry) {
                    is HistoryListItem.Header -> Text(
                        text = entry.label.uppercase(),
                        style = OverlineStyle,
                        color = palette.muted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 4.dp)
                    )

                    is HistoryListItem.Entry -> {
                        val item = entry.item
                        HorizontalDivider(color = palette.outline)
                        HistoryRow(
                            item = item,
                            expanded = expandedId == item.id,
                            onToggle = {
                                expandedId = if (expandedId == item.id) null else item.id
                            },
                            onCopy = { copy(context, item.summary) },
                            onShare = { share(context, item.summary) },
                            onDelete = { onDelete(item) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHistory() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 60.dp)
        ) {
            Text(
                text = "Nothing here yet",
                style = MaterialTheme.typography.titleMedium,
                color = palette.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Summaries you get — in the app or from the share sheet — are kept here.",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ClearHistoryDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.surfaceHigh,
        titleContentColor = palette.onSurface,
        textContentColor = palette.muted,
        title = { Text("Clear history?") },
        text = {
            Text(
                if (count == 1) "The one saved summary will be deleted."
                else "All $count saved summaries will be deleted."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Clear", color = palette.bait)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = palette.muted)
            }
        }
    )
}

private fun copy(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("TL;DR Summary", text))
    Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
}

private fun share(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share TL;DR"))
}
