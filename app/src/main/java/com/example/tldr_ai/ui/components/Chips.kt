package com.example.tldr_ai.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.ui.theme.palette

/**
 * A low-emphasis pill. Used for the things that are settings rather than actions — input mode
 * and model choice — so they stay legible without competing with the primary button.
 */
@Composable
fun QuietChip(
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val contentColor = if (enabled) palette.muted else palette.muted.copy(alpha = 0.4f)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = palette.surface,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Icon(it, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium
            )
            trailingIcon?.let {
                Spacer(Modifier.width(4.dp))
                Icon(it, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}
