package com.example.tldr_ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.ui.theme.TldraiTheme

/**
 * Visual indicator showing how clickbaity an article is.
 *
 * @param score Clickbait score from 0-100
 *              0-30: Low clickbait (green) - informative content
 *              31-60: Medium clickbait (yellow) - some sensationalism
 *              61-100: High clickbait (red) - pure clickbait
 */
@Composable
fun ClickbaitIndicator(
    score: Int,
    modifier: Modifier = Modifier
) {
    val normalizedScore = score.coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = normalizedScore / 100f,
        label = "clickbait_progress"
    )

    val indicatorColor by animateColorAsState(
        targetValue = when {
            normalizedScore <= 30 -> Color(0xFF4CAF50) // Green
            normalizedScore <= 60 -> Color(0xFFFF9800) // Orange
            else -> Color(0xFFF44336) // Red
        },
        label = "clickbait_color"
    )

    val label = when {
        normalizedScore <= 30 -> "Informative"
        normalizedScore <= 60 -> "Somewhat Clickbaity"
        else -> "Pure Clickbait"
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Clickbait Score",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$normalizedScore% - $label",
                style = MaterialTheme.typography.labelMedium,
                color = indicatorColor
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(8.dp)
                .clip(MaterialTheme.shapes.small),
            color = indicatorColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClickbaitIndicatorLowPreview() {
    TldraiTheme {
        ClickbaitIndicator(score = 20, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ClickbaitIndicatorMediumPreview() {
    TldraiTheme {
        ClickbaitIndicator(score = 50, modifier = Modifier.padding(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun ClickbaitIndicatorHighPreview() {
    TldraiTheme {
        ClickbaitIndicator(score = 85, modifier = Modifier.padding(16.dp))
    }
}
