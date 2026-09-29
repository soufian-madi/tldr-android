package com.example.tldr_ai.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.SummaryResult
import com.example.tldr_ai.ui.theme.ReadingStyle
import com.example.tldr_ai.ui.theme.TldraiTheme
import com.example.tldr_ai.ui.theme.palette

/**
 * The answer. Everything else on the screen is deliberately quiet so this can be the only
 * thing with weight: the headline that lied, the verdict, and the shortest honest version.
 */
@Composable
fun SummaryView(
    result: SummaryResult,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.outline, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = palette.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            result.originalTitle?.takeIf { it.isNotBlank() }?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(16.dp))
            }

            ClickbaitMeter(score = result.clickbaitScore)

            Spacer(Modifier.height(20.dp))

            SummaryBody(text = result.summary, style = ReadingStyle)

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = palette.outline)
            Spacer(Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                QuietAction(Icons.Rounded.ContentCopy, "Copy", onCopy)
                QuietAction(Icons.Rounded.IosShare, "Share", onShare)
            }
        }
    }
}

private val BULLET_PREFIX = Regex("^[-*•]\\s+")

/**
 * Renders the model's `- ` lines as real bullets. Informative articles come back as lists,
 * and a raw hyphen in the middle of serif text looks like a mistake.
 */
@Composable
fun SummaryBody(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier
) {
    val blocks = remember(text) { text.trim().lines().map(String::trim).filter(String::isNotEmpty) }

    if (blocks.none { BULLET_PREFIX.containsMatchIn(it) }) {
        Text(text = text.trim(), style = style, color = palette.onSurface, modifier = modifier)
        return
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(9.dp)) {
        blocks.forEach { line ->
            val match = BULLET_PREFIX.find(line)
            if (match == null) {
                Text(text = line, style = style, color = palette.onSurface)
            } else {
                Row {
                    Text(
                        text = "•",
                        style = style,
                        color = palette.muted,
                        modifier = Modifier.width(18.dp)
                    )
                    Text(
                        text = line.removeRange(match.range),
                        style = style,
                        color = palette.onSurface
                    )
                }
            }
        }
    }
}

/** Low-emphasis icon+label button, for actions that follow the content rather than lead it. */
@Composable
fun QuietAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: androidx.compose.ui.graphics.Color? = null
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = tint ?: palette.muted
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(7.dp))
            Text(text = label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Preview
@Composable
private fun SummaryPreview() {
    TldraiTheme {
        Column(Modifier.padding(20.dp)) {
            SummaryView(
                result = SummaryResult(
                    summary = "Elon Musk bought Twitter.",
                    clickbaitScore = 92,
                    originalTitle = "You Won't BELIEVE Who Just Bought Twitter for \$50 BILLION!!!"
                ),
                onCopy = {},
                onShare = {}
            )
        }
    }
}
