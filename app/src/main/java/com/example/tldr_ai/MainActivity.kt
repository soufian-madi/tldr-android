package com.example.tldr_ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.tldr_ai.ui.screens.HistoryScreen
import com.example.tldr_ai.ui.screens.MainScreen
import com.example.tldr_ai.ui.theme.TldraiTheme
import com.example.tldr_ai.ui.theme.palette
import com.example.tldr_ai.ui.viewmodel.MainViewModel

private enum class Route { Home, History }

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        setContent {
            TldraiTheme {
                val uiState by viewModel.uiState.collectAsState()
                val historyItems by viewModel.historyItems.collectAsState()
                val selectedModel by viewModel.selectedModel.collectAsState()
                val models by viewModel.models.collectAsState()

                var route by rememberSaveable { mutableStateOf(Route.Home) }

                BackHandler(enabled = route != Route.Home) { route = Route.Home }

                Surface(modifier = Modifier.fillMaxSize(), color = palette.background) {
                    AnimatedContent(
                        targetState = route,
                        transitionSpec = {
                            val forward = targetState == Route.History
                            val duration = tween<Float>(260)
                            if (forward) {
                                (slideInHorizontally(tween(260)) { it } + fadeIn(duration))
                                    .togetherWith(
                                        slideOutHorizontally(tween(260)) { -it / 8 } +
                                                fadeOut(duration)
                                    )
                            } else {
                                (slideInHorizontally(tween(260)) { -it / 8 } + fadeIn(duration))
                                    .togetherWith(
                                        slideOutHorizontally(tween(260)) { it } + fadeOut(duration)
                                    )
                            }
                        },
                        label = "route"
                    ) { current ->
                        // safeDrawing rather than a Scaffold: there are no system bars of our own
                        // to reserve space for any more, and it keeps the keyboard from covering
                        // the composer.
                        val insets = Modifier
                            .fillMaxSize()
                            .windowInsetsPadding(WindowInsets.safeDrawing)

                        when (current) {
                            Route.Home -> Box(insets) {
                                MainScreen(
                                    uiState = uiState,
                                    onSummarize = { mode, input ->
                                        viewModel.summarize(mode, input)
                                    },
                                    selectedModel = selectedModel,
                                    models = models,
                                    onModelSelected = { viewModel.setModel(it) },
                                    onOpenHistory = { route = Route.History }
                                )
                            }

                            Route.History -> Box(insets) {
                                HistoryScreen(
                                    items = historyItems,
                                    onBack = { route = Route.Home },
                                    onDelete = { viewModel.deleteHistoryItem(it) },
                                    onClearAll = { viewModel.clearHistory() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshHistory()
    }
}
