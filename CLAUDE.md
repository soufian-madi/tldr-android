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
- **AI Integration:** TBD (Phase 2)
- **Networking:** TBD (Phase 2)

## Project Structure

```
app/src/main/java/com/example/tldr_ai/
├── MainActivity.kt              # Entry point
├── data/
│   ├── model/
│   │   └── SummaryResult.kt     # Data models & UI state
│   └── repository/
│       └── SummaryRepository.kt # AI integration (TODO)
└── ui/
    ├── components/
    │   ├── ClickbaitIndicator.kt  # Visual clickbait score
    │   ├── InputSection.kt        # URL/Text input with tabs
    │   └── SummaryCard.kt         # Result display card
    ├── screens/
    │   └── MainScreen.kt          # Main UI screen
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## Current Status

### Phase 1 (Complete)
- [x] UI skeleton with Jetpack Compose
- [x] Input modes: URL and paste text (tabbed)
- [x] Clickbait indicator (color-coded progress bar)
- [x] Summary card with copy/share actions
- [x] Mock responses for UI testing

### Phase 2 (Next)
- [ ] Add ViewModel with StateFlow
- [ ] Implement AI service integration
- [ ] URL fetching and article parsing
- [ ] Clickbait detection algorithm
- [ ] Error handling

## Git Conventions

- Commit messages must be 100 characters or less

## Build & Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

## Environment

API keys should be stored in `.env` file (gitignored):
```
AI_API_KEY=your_key_here
```
