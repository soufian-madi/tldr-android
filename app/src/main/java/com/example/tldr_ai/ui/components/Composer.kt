package com.example.tldr_ai.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.ui.theme.palette

/**
 * The one input, and it sits at the bottom of the screen within thumb reach.
 *
 * There is no URL/text mode switch: what you paste decides, which removes the only decision
 * the user had to make before getting an answer. The single round button covers the whole
 * lifecycle — paste, send, working — so nothing else has to be on screen.
 */
@Composable
fun Composer(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, palette.outline, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = palette.surface
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp, end = 8.dp)) {
                if (value.isEmpty()) {
                    Text(
                        text = "Paste a link, or the article itself",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.muted
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !isLoading,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.onSurface),
                    cursorBrush = SolidColor(palette.onSurface),
                    minLines = 2,
                    // A pasted article would otherwise swallow the screen; the field scrolls
                    // internally past this.
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = {
                        keyboard?.hide()
                        onSubmit()
                    }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            val action = when {
                isLoading -> ComposerAction.WORKING
                value.isBlank() -> ComposerAction.PASTE
                else -> ComposerAction.SEND
            }

            AnimatedContent(
                targetState = action,
                transitionSpec = {
                    (fadeIn(tween(180)) + scaleIn(tween(180), initialScale = 0.8f))
                        .togetherWith(fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.8f))
                },
                label = "composer_action"
            ) { current ->
                when (current) {
                    ComposerAction.PASTE -> RoundAction(
                        icon = Icons.Rounded.ContentPaste,
                        label = "Paste from clipboard",
                        container = palette.surfaceHigh,
                        content = palette.muted
                    ) { clipboard.getText()?.text?.let(onValueChange) }

                    ComposerAction.SEND -> RoundAction(
                        icon = Icons.Rounded.ArrowUpward,
                        label = "Get the TL;DR",
                        container = if (canSubmit) palette.onSurface else palette.surfaceHigh,
                        content = if (canSubmit) palette.background else palette.muted,
                        onClick = {
                            keyboard?.hide()
                            onSubmit()
                        }
                    )

                    ComposerAction.WORKING -> Box(
                        modifier = Modifier.size(46.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = palette.muted,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }
        }
    }
}

private enum class ComposerAction { PASTE, SEND, WORKING }

@Composable
private fun RoundAction(
    icon: ImageVector,
    label: String,
    container: Color,
    content: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(46.dp),
        shape = CircleShape,
        color = container,
        contentColor = content
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(19.dp))
        }
    }
}

/** Clears the field. Kept out of the composer itself so the round button stays unambiguous. */
@Composable
fun ClearAction(onClear: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClear,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        contentColor = palette.muted
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(text = "Clear", style = MaterialTheme.typography.labelMedium)
        }
    }
}
