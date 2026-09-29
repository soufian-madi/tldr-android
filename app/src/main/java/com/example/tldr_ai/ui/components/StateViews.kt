package com.example.tldr_ai.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.ui.theme.OverlineStyle
import com.example.tldr_ai.ui.theme.palette

/**
 * The waiting state. A pulsing label carries the wait; the model's own reasoning scrolls
 * underneath it when the gateway exposes it, faded at the top so it reads as background
 * chatter rather than the answer.
 */
@Composable
fun ThinkingView(
    label: String,
    reasoning: String,
    modifier: Modifier = Modifier
) {
    val pulse = rememberInfiniteTransition(label = "thinking_pulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "thinking_alpha"
    )

    val scroll = rememberScrollState()
    LaunchedEffect(reasoning) { scroll.animateScrollTo(scroll.maxValue) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(alpha)) {
            Spacer(
                Modifier
                    .size(6.dp)
                    .background(palette.onSurface, CircleShape)
            )
            Spacer(Modifier.width(10.dp))
            Text(text = label.uppercase(), style = OverlineStyle, color = palette.onSurface)
        }

        if (reasoning.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 190.dp)
                    .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                    .drawWithContent {
                        drawContent()
                        drawRect(
                            brush = Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.28f to Color.Black
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .verticalScroll(scroll)
            ) {
                Text(
                    text = reasoning,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted
                )
            }
        }
    }
}

/**
 * The idle state doubles as the explanation of the whole app, since the length rule is the
 * one thing a new user has no way to guess.
 */
@Composable
fun ConceptLegend(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "The more clickbait, the shorter the answer.",
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted
        )
        Spacer(Modifier.height(18.dp))
        Verdict.entries.forEachIndexed { index, verdict ->
            if (index > 0) HorizontalDivider(color = palette.outline)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    Modifier
                        .size(6.dp)
                        .background(verdictColor(verdict), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = verdict.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = verdict.consequence,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted
                )
            }
        }
    }
}

@Composable
fun ErrorView(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.outline, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = palette.surface
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "DIDN'T WORK",
                style = OverlineStyle,
                color = palette.bait
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = palette.onSurface
            )
            onRetry?.let {
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuietAction(Icons.Rounded.Refresh, "Try again", it)
                }
            }
        }
    }
}
