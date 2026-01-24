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
- **AI:** Claude API (claude-sonnet-4-20250514)
- **Networking:** OkHttp

## Project Structure

```
app/src/main/java/com/example/tldr_ai/
├── MainActivity.kt
├── data/
│   ├── api/
│   │   └── ClaudeApiService.kt    # Claude API client
│   ├── model/
│   │   └── SummaryResult.kt       # Data models & UI state
│   └── repository/
│       └── SummaryRepository.kt   # Repository layer
└── ui/
    ├── components/
    │   ├── ClickbaitIndicator.kt  # Visual clickbait score
    │   ├── InputSection.kt        # URL/Text input with tabs
    │   └── SummaryCard.kt         # Result display card
    ├── screens/
    │   └── MainScreen.kt          # Main UI screen
    ├── viewmodel/
    │   └── MainViewModel.kt       # UI state management
    └── theme/
```

## Current Status

### Phase 1 (Complete)
- [x] UI skeleton with Jetpack Compose
- [x] Input modes: URL and paste text (tabbed)
- [x] Clickbait indicator (color-coded progress bar)
- [x] Summary card with copy/share actions

### Phase 2 (Complete)
- [x] ViewModel with StateFlow
- [x] Claude API integration
- [x] AI-powered clickbait detection and summarization

### Phase 3 (Future)
- [ ] URL fetching and article parsing
- [ ] History/saved summaries
- [ ] Better error handling and retry logic

## Git Conventions

- Commit messages must be 100 characters or less

## Setup

1. Add your Claude API key to `local.properties`:
```properties
CLAUDE_API_KEY=sk-ant-xxxxx
```

2. Build and run:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Environment

- API key is loaded from `local.properties` (gitignored) via BuildConfig
- Never commit API keys to the repository
