package com.example.tldr_ai.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.SummaryResult
import com.example.tldr_ai.ui.theme.TldraiTheme

/**
 * Card displaying the summary result with clickbait indicator and action buttons.
 */
@Composable
fun SummaryCard(
    result: SummaryResult,
    onCopyClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Original title if available
            result.originalTitle?.let { title ->
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            }

            // Clickbait indicator
            ClickbaitIndicator(
                score = result.clickbaitScore,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Summary text
            Text(
                text = result.summary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // TODO: Replace with proper copy icon resource
                IconButton(onClick = onCopyClick) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_save),
                        contentDescription = "Copy to clipboard",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onShareClick) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardLowClickbaitPreview() {
    TldraiTheme {
        SummaryCard(
            result = SummaryResult(
                summary = "Android 14 introduces several improvements:\n" +
                        "- Better battery optimization\n" +
                        "- Enhanced privacy controls\n" +
                        "- Improved accessibility features\n" +
                        "- New customization options for the lock screen",
                clickbaitScore = 15,
                originalTitle = "Android 14 Released with Performance Improvements"
            ),
            onCopyClick = {},
            onShareClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryCardHighClickbaitPreview() {
    TldraiTheme {
        SummaryCard(
            result = SummaryResult(
                summary = "Elon Musk bought Twitter.",
                clickbaitScore = 92,
                originalTitle = "You Won't BELIEVE Who Just Bought Twitter for \$50 BILLION!!!"
            ),
            onCopyClick = {},
            onShareClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
