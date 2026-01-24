package com.example.tldr_ai.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.tldr_ai.data.model.InputMode
import com.example.tldr_ai.ui.theme.TldraiTheme

/**
 * Input section with tabs to switch between URL and Text input modes.
 */
@Composable
fun InputSection(
    inputMode: InputMode,
    onInputModeChange: (InputMode) -> Unit,
    urlValue: String,
    onUrlChange: (String) -> Unit,
    textValue: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = if (inputMode == InputMode.URL) 0 else 1
        ) {
            Tab(
                selected = inputMode == InputMode.URL,
                onClick = { onInputModeChange(InputMode.URL) },
                text = { Text("URL") },
                enabled = enabled
            )
            Tab(
                selected = inputMode == InputMode.TEXT,
                onClick = { onInputModeChange(InputMode.TEXT) },
                text = { Text("Paste Text") },
                enabled = enabled
            )
        }

        AnimatedContent(
            targetState = inputMode,
            label = "input_mode_content"
        ) { mode ->
            when (mode) {
                InputMode.URL -> {
                    OutlinedTextField(
                        value = urlValue,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        label = { Text("Article URL") },
                        placeholder = { Text("https://example.com/article") },
                        singleLine = true,
                        enabled = enabled,
                        shape = MaterialTheme.shapes.medium
                    )
                }
                InputMode.TEXT -> {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(200.dp),
                        label = { Text("Article Text") },
                        placeholder = { Text("Paste the article content here...") },
                        enabled = enabled,
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun InputSectionUrlPreview() {
    TldraiTheme {
        InputSection(
            inputMode = InputMode.URL,
            onInputModeChange = {},
            urlValue = "",
            onUrlChange = {},
            textValue = "",
            onTextChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InputSectionTextPreview() {
    TldraiTheme {
        InputSection(
            inputMode = InputMode.TEXT,
            onInputModeChange = {},
            urlValue = "",
            onUrlChange = {},
            textValue = "Some article content here...",
            onTextChange = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
