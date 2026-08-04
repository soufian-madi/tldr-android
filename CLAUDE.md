# TL;DR Android App

## Project Overview

A simple Android app that uses AI to summarize articles from the web. Users can input text or a URL, and the AI provides a summary of the main points.

**Target:** Android 11+ (API 30+)

## Core Concept: Clickbait-Proportional Brevity

The key differentiator: **the more clickbaity the article, the shorter the response**.

### Summary Length Rules

| Article Type | Example Title | Response Format |
|--------------|---------------|-----------------|
| Informative | "Android 14 released with performance improvements" | Short paragraph or bullet points |
| Moderately clickbaity | "These 5 features in Android 14 will change how you use your phone" | 2-3 sentences |
| Highly clickbaity | "You won't BELIEVE who just bought Twitter for $50B!" | Single sentence: "Elon Musk bought Twitter" |

### Clickbait Detection Signals
- Excessive capitalization
- Sensationalist language ("shocking", "unbelievable", "you won't believe")
- Withholding key information in the title
- Question-based headlines that could be answered simply
- Numbered lists promising secrets/hacks/tricks

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3 + Dynamic Colors
- **Min SDK:** 30 (Android 11)
- **Target SDK:** 36
- **AI:** All requests go through the AI gateway (`ao-ai-api-management`), which exposes both
  Claude and GPT models. The user picks one; the model list is fetched from the gateway catalog.
- **Networking:** OkHttp (SSE streaming), Gson
- **Persistence:** SharedPreferences (history + selected model)

## Project Structure

```
app/src/main/java/com/example/tldr_ai/
├── MainActivity.kt                # Scaffold + bottom nav (Summarize / History)
├── ShareReceiverActivity.kt       # Transparent activity for Android share-sheet intents
├── floatingcard/
│   └── FloatingCardService.kt     # Overlay foreground service (classic Views, not Compose)
├── data/
│   ├── api/
│   │   ├── ClaudeApiService.kt    # Gateway client: URL fetch + streaming summarize
│   │   ├── ModelCatalogService.kt # Fetches available models from the gateway
│   │   └── Gateway.kt             # Gateway base URL
│   ├── model/                     # SummaryResult, AiModel, HistoryItem, UI/stream state
│   └── repository/                # Summary, History, ModelCatalog, Settings
└── ui/
    ├── components/                # ClickbaitIndicator, InputSection, SummaryCard, HistoryItemCard
    ├── screens/                   # MainScreen, HistoryScreen
    ├── viewmodel/MainViewModel.kt # UI state via StateFlow
    └── theme/
```

## Key Behaviours

- **Two entry points:** the full app, and a share-sheet flow — sharing a link from any app
  spawns a floating overlay card that summarizes in place, without opening the app.
- **Streaming:** responses stream over SSE. Live reasoning text is shown faintly while it
  arrives, but only for OpenAI models — the gateway redacts Anthropic thinking.
- **Model routing:** OpenAI models hit `/v1/responses`, Anthropic models hit
  `/chat/completions`, selected via the `x-model` header with the catalog's *short* alias.
- **History:** last 50 summaries in SharedPreferences, shared between the app and the overlay.

## Current Status

### Phase 1 (Complete)
- [x] UI skeleton with Jetpack Compose
- [x] Input modes: URL and paste text (tabbed)
- [x] Clickbait indicator (color-coded progress bar)
- [x] Summary card with copy/share actions

### Phase 2 (Complete)
- [x] ViewModel with StateFlow
- [x] Gateway API integration
- [x] AI-powered clickbait detection and summarization

### Phase 3 (Complete)
- [x] URL fetching with HTML preprocessing
- [x] History/saved summaries (with clear-all confirmation)
- [x] Multi-model picker (Claude + GPT, loaded from the gateway catalog)
- [x] Streaming responses with live reasoning display
- [x] Regex fallback parser for malformed JSON responses

### Phase 4 (Complete)
- [x] Share-sheet integration (`ShareReceiverActivity`)
- [x] Floating overlay card that summarizes shared links without opening the app

### Next
- [ ] Better error handling and retry logic
- [ ] Compose-based floating card (currently classic Views)

## Git Conventions

- Commit messages must be 100 characters or less
- Always commit after making code changes, without waiting for explicit prompting

## Setup

1. Add your gateway API key to `local.properties` (the property name is historical — it is the
   gateway's `x-api-key`, not an Anthropic key):
```properties
CLAUDE_API_KEY=xxxxx
```

2. Build and run:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Environment

- API key is loaded from `local.properties` (gitignored) via BuildConfig
- Never commit API keys to the repository
- The floating card needs the "Display over other apps" permission granted manually on device
- The `/android-device` skill drives the phone over adb (screenshots, taps, logcat)
