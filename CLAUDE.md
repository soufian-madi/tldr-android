# TL;DR Android App

## Project Overview

A simple Android app that uses AI to summarize articles from the web. Users can input text or a URL, and the AI provides a summary of the main points.

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

- **Language**: Kotlin
- **UI**: Jetpack Compose (preferred) or XML layouts
- **AI Integration**: TBD (likely OpenAI API or similar)
- **Networking**: Retrofit or Ktor for HTTP requests

## Features (Planned)

1. Text input for pasting article content
2. URL input for fetching and parsing articles
3. AI-powered summarization with clickbait detection
4. Copy summary to clipboard
5. Share functionality

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
