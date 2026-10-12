package com.example.tldr_ai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.AiModel
import com.example.tldr_ai.data.model.ModelFamily
import com.example.tldr_ai.ui.theme.OverlineStyle
import com.example.tldr_ai.ui.theme.palette

/**
 * A sheet, not a dropdown: the gateway catalog can run to a dozen entries, which an anchored
 * menu shows badly on a phone — and a sheet gives room to group them by provider.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSheet(
    models: List<AiModel>,
    selected: AiModel,
    onSelect: (AiModel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.surface,
        contentColor = palette.onSurface,
        // Material's default scrim is derived from onSurface, which in this theme is nearly
        // white — it washed the screen out instead of dimming it.
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))
                Spacer(
                    Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .background(palette.outline, RoundedCornerShape(2.dp))
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    ) {
        LazyColumn(modifier = Modifier.navigationBarsPadding()) {
            val grouped = models.groupBy { it.family }
            listOf(ModelFamily.ANTHROPIC, ModelFamily.OPENAI).forEach { family ->
                val entries = grouped[family].orEmpty()
                if (entries.isEmpty()) return@forEach

                item(key = "${family.name}-header") {
                    Text(
                        text = if (family == ModelFamily.ANTHROPIC) "ANTHROPIC" else "OPENAI",
                        style = OverlineStyle,
                        color = palette.muted,
                        modifier = Modifier.padding(start = 24.dp, top = 18.dp, bottom = 6.dp)
                    )
                }
                items(entries, key = { it.id }) { model ->
                    ModelRow(
                        model = model,
                        isSelected = model.id == selected.id,
                        onClick = { onSelect(model) }
                    )
                }
            }

            item(key = "thinking-note") {
                Text(
                    text = "Thinking is only visible while GPT models work; the gateway hides it for Claude.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.muted,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp)
                )
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: AiModel,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = model.displayName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) palette.onSurface else palette.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.weight(1f)
        )
        if (isSelected) {
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = palette.onSurface,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}
