package com.example.tldr_ai.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.HistoryItem
import com.example.tldr_ai.ui.theme.ReadingCompactStyle
import com.example.tldr_ai.ui.theme.palette
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * A history entry, collapsed to one scannable line by default. The old list showed every
 * summary in full, which made 50 saved items unreadable — the verdict and the headline are
 * enough to find the one you meant.
 */
@Composable
fun HistoryRow(
    item: HistoryItem,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title?.takeIf { it.isNotBlank() }
                        ?: item.url?.let(::hostOf)
                        ?: item.summary.lineSequence().first(),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(9.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    VerdictTag(score = item.clickbaitScore)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = formatTimestamp(item.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.muted
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = palette.muted,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(chevronRotation)
            )
        }

        if (expanded) {
            Spacer(Modifier.height(16.dp))
            SummaryBody(text = item.summary, style = ReadingCompactStyle)

            item.url?.let { url ->
                Spacer(Modifier.height(14.dp))
                Text(
                    text = url,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                QuietAction(Icons.Rounded.ContentCopy, "Copy", onCopy)
                QuietAction(Icons.Rounded.IosShare, "Share", onShare)
                QuietAction(Icons.Rounded.DeleteOutline, "Delete", onDelete, tint = palette.bait)
            }
        }
    }
}

private fun hostOf(url: String): String = url
    .removePrefix("https://")
    .removePrefix("http://")
    .removePrefix("www.")
    .substringBefore('/')

private fun formatTimestamp(timestamp: Long): String {
    val today = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = today.get(Calendar.YEAR) == then.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)
    val pattern = if (sameDay) "HH:mm" else "d MMM"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}
