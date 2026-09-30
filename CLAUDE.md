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

## Design Language

The UI is monochrome on purpose: an "ink & paper" palette (near-black in dark, warm off-white in
light) with **no hue anywhere except the clickbait verdict**, so colour always means exactly one
thing. Dynamic colour is deliberately *not* used — the wallpaper-derived accent fought with the
verdict hues.

- **Palette:** `TldrPalette` via `LocalPalette` (see `ui/theme/Theme.kt`). Components read
  `palette.*` for exact tones; stock Material widgets get the mapped `colorScheme`.
- **Type:** sans for the interface, **serif for the summary itself** — the one thing meant to be
  read. `WordmarkStyle`, `OverlineStyle`, `ReadingStyle` live in `ui/theme/Type.kt`.
- **Verdict, not percentage:** the score is shown as a word (Informative / Some spin / Pure
  clickbait) plus a countable 12-segment meter. The number was never the point.
- **One input, no mode switch:** what you paste decides (`detectInputMode`). The detected mode is
  shown as a tappable chip in case the guess is wrong.
- **Layout:** composer pinned at the bottom within thumb reach; the answer rises out of it and is
  the only region that scrolls. No top app bar, no bottom nav.
- **The mark** (`drawable/logo_c_monogram.xml`): the launcher icon is the wordmark, not a metaphor —
  `TL;DR` on two lines in the same black weight and tight fit as `WordmarkStyle`, paper on ink.
  Deliberately no sparkle, star, nebula or violet: those signals say nothing about *this* app, and a
  violet would break the one-hue-means-one-thing rule. Both rows sit on one cap height and are flush
  to the same left and right edges, so the lockup reads as a single block rather than two ragged
  lines. Geometry lives in the 108 viewport inside the r=33 safe circle, so round, squircle and
  teardrop masks all crop cleanly.
- **The icon does not mute its semicolon**, though `wordmark()` does. The foreground doubles as the
  `<monochrome>` themed-icon layer, where the system discards the fill and paints the silhouette in
  one tint — a translucent element survives only as a low-contrast patch and can wash out entirely in
  a light Material You theme. At 48dp it also stopped reading as a semicolon and became a smudge.
  Emphasis in that layer has to come from shape, not alpha.
- **The `mipmap-*/ic_launcher*.webp` rasters are dead weight**, kept only so the repo holds no stale
  art: `aapt2 dump badging` shows every density resolving to `mipmap-anydpi-v21/ic_launcher.xml`, and
  minSdk is 30. They are encoded **lossless** (VP8L) — lossy webp puts a visible bevel of ringing
  around the glyph edges at these sizes. Regenerate them from the drawable if the mark changes.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3, fixed (non-dynamic) colour scheme
- **Min SDK:** 30 (Android 11)
- **Target SDK:** 36
- **AI:** All requests go through Nexos (`https://api.nexos.ai/v1`), which exposes both
  Claude and GPT models. The user picks one; the model list is fetched from the gateway catalog.
- **Networking:** OkHttp (SSE streaming), Gson
- **Persistence:** SharedPreferences (history + selected model)

## Project Structure

```
app/src/main/java/com/example/tldr_ai/
├── MainActivity.kt                # Two routes (Home / History), animated, no Scaffold bars
├── ShareReceiverActivity.kt       # Transparent activity for Android share-sheet intents
├── floatingcard/
│   └── FloatingCardService.kt     # Overlay foreground service (classic Views, not Compose)
├── data/
│   ├── api/
│   │   ├── ClaudeApiService.kt    # Gateway client: URL fetch + streaming summarize
│   │   ├── ModelCatalogService.kt # Fetches available models from the gateway
│   │   └── Gateway.kt             # Gateway base URL
│   ├── model/                     # SummaryResult, AiModel, HistoryItem, UI/stream state,
│   │                              #   detectInputMode / normalizeUrl
│   └── repository/                # Summary, History, ModelCatalog, Settings
└── ui/
    ├── components/                # ClickbaitMeter (Verdict), Composer, ModelPicker (sheet),
    │                              #   SummaryView, StateViews (Thinking/Legend/Error),
    │                              #   HistoryRow, Chips
    ├── screens/                   # MainScreen, HistoryScreen
    ├── viewmodel/MainViewModel.kt # UI state via StateFlow
    └── theme/                     # Color, Type, Theme (TldrPalette)
```

## Key Behaviours

- **Two entry points:** the full app, and a share-sheet flow — sharing a link from any app
  spawns a floating overlay card that summarizes in place, without opening the app.
- **Streaming:** responses stream over SSE. Live reasoning text is shown faintly while it
  arrives, but only for OpenAI models — the gateway redacts Anthropic thinking.
- **Model routing:** Both families use `/chat/completions` with Bearer authentication and
  the exact Nexos catalog ID in the `model` field. Catalog is fetched from `/models`.
- **History:** last 50 summaries in SharedPreferences, shared between the app and the overlay.
  Entries are collapsed to one scannable line and expand in place; single entries can be deleted.

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

### Phase 5 — UI redesign (Complete)
- [x] Fixed ink/paper palette, full type scale, serif reading face
- [x] Single auto-detecting input, bottom-anchored composer with one round action
- [x] Verdict meter replacing the percentage progress bar
- [x] Model bottom sheet replacing the dropdown
- [x] Scannable, expandable history with per-item delete
- [x] Overlay card restyled to match

### Next
- [ ] Better error handling: `fetchUrl` treats captive-portal/block pages as article content, so a
      dead link comes back as a confident "no content" *summary* instead of an error. `ErrorView`
      only shows on real transport failures.
- [ ] Retry logic beyond the manual "Try again" action
- [ ] Compose-based floating card (currently classic Views)

## Git Conventions

- Commit messages must be 100 characters or less
- Always commit after making code changes, without waiting for explicit prompting
- Code changes require unit test coverage for the logic they touch. A pre-commit hook
  (`.githooks/pre-commit`, wired up via `git config core.hooksPath .githooks` — run that once per
  clone) runs `./gradlew testDebugUnitTest` and blocks the commit if any test fails.

## Setup

1. Add your Nexos API key to `local.properties` (gitignored):
```properties
NEXOS_API_KEY=xxxxx
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
