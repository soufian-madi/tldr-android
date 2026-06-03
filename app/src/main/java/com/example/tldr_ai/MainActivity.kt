package com.example.tldr_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tldr_ai.ui.screens.HistoryScreen
import com.example.tldr_ai.ui.screens.MainScreen
import com.example.tldr_ai.ui.theme.TldraiTheme
import com.example.tldr_ai.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            TldraiTheme {
                val viewModel: MainViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsState()
                val historyItems by viewModel.historyItems.collectAsState()

                var selectedTab by remember { mutableIntStateOf(0) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = if (selectedTab == 0) "TL;DR" else "History",
                                    style = MaterialTheme.typography.headlineMedium
                                )
                            },
                            actions = {
                                if (selectedTab == 1 && historyItems.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearHistory() }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Clear history"
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = {
                                    Icon(Icons.Default.Home, contentDescription = "Summarize")
                                },
                                label = { Text("Summarize") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = {
                                    Icon(Icons.Default.History, contentDescription = "History")
                                },
                                label = { Text("History") }
                            )
                        }
                    }
                ) { paddingValues ->
                    val topPadding = paddingValues.calculateTopPadding()
                    val bottomPadding = paddingValues.calculateBottomPadding()

                    when (selectedTab) {
                        0 -> MainScreen(
                            uiState = uiState,
                            onSummarize = { mode, input -> viewModel.summarize(mode, input) },
                            topPadding = topPadding,
                            bottomPadding = bottomPadding
                        )
                        1 -> HistoryScreen(
                            items = historyItems,
                            topPadding = topPadding,
                            bottomPadding = bottomPadding + 8.dp
                        )
                    }
                }
            }
        }
    }
}
