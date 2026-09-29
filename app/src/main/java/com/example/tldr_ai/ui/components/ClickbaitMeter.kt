package com.example.tldr_ai.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.ui.theme.OverlineStyle
import com.example.tldr_ai.ui.theme.TldraiTheme
import com.example.tldr_ai.ui.theme.palette

/**
 * How much the headline is working on you. This is the app's one piece of colour, so it is
 * shown as a plain verdict plus a countable meter rather than a percentage: the number was
 * never the point, the resulting brevity is.
 */
enum class Verdict(val label: String, val consequence: String) {
    CALM("Informative", "full summary"),
    SPIN("Some spin", "a few sentences"),
    BAIT("Pure clickbait", "one line")
}

fun verdictOf(score: Int): Verdict = when {
    score.coerceIn(0, 100) <= 30 -> Verdict.CALM
    score.coerceIn(0, 100) <= 60 -> Verdict.SPIN
    else -> Verdict.BAIT
}

@Composable
fun verdictColor(verdict: Verdict): Color = when (verdict) {
    Verdict.CALM -> palette.calm
    Verdict.SPIN -> palette.spin
    Verdict.BAIT -> palette.bait
}

private const val SEGMENTS = 12

/** Verdict word on the left, a 12-segment meter on the right. */
@Composable
fun ClickbaitMeter(
    score: Int,
    modifier: Modifier = Modifier
) {
    val normalized = score.coerceIn(0, 100)
    val verdict = verdictOf(normalized)
    val color by animateColorAsState(verdictColor(verdict), label = "verdict_color")

    // At least one segment always lights up, so the meter never reads as "no data".
    val filled by animateIntAsState(
        targetValue = ((normalized / 100f) * SEGMENTS).toInt().coerceIn(1, SEGMENTS),
        animationSpec = tween(450),
        label = "verdict_fill"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = verdict.label.uppercase(),
            style = OverlineStyle,
            color = color
        )
        Segments(filled = filled, color = color)
    }
}

@Composable
private fun Segments(filled: Int, color: Color) {
    // Slightly brighter than the hairline outline, or the empty segments vanish on the card.
    val track = palette.muted.copy(alpha = 0.28f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(SEGMENTS) { index ->
            if (index > 0) Spacer(Modifier.width(3.dp))
            Spacer(
                Modifier
                    .size(width = 5.dp, height = if (index < filled) 10.dp else 6.dp)
                    .background(
                        color = if (index < filled) color else track,
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/** Verdict as a single dot plus word — for dense lists. */
@Composable
fun VerdictTag(
    score: Int,
    modifier: Modifier = Modifier
) {
    val verdict = verdictOf(score)
    val color = verdictColor(verdict)
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .size(6.dp)
                .background(color, CircleShape)
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = verdict.label,
            style = OverlineStyle,
            color = color
        )
    }
}

@Preview
@Composable
private fun MeterPreview() {
    TldraiTheme {
        Column(
            modifier = Modifier
                .background(palette.background)
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ClickbaitMeter(score = 12)
            ClickbaitMeter(score = 48)
            ClickbaitMeter(score = 92)
            VerdictTag(score = 92)
        }
    }
}
