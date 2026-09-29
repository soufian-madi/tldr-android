# TL;DR — Android article summarizer

A Kotlin / Jetpack Compose app that turns articles into concise summaries. The
more clickbaity the headline, the shorter the answer: informative reporting gets
a short explanation; a sensational headline gets straight to the withheld fact.

## Features

- Paste an article URL or text, with automatic input detection.
- Stream summaries from OpenAI or Anthropic models through a compatible gateway.
- Choose a model from the gateway's live catalog.
- See a three-level clickbait verdict and a 12-segment meter.
- Share a link from another app into a floating summary card.
- Browse and delete locally stored summary history (up to 50 entries).
- Monochrome Compose UI with serif summaries and light/dark themes.

## Architecture

`ui/` contains Compose screens and a StateFlow-backed ViewModel. `data/` separates
HTTP clients, response models and repositories. OkHttp handles SSE streaming and
Gson handles JSON. The share-sheet entry point starts a foreground overlay service;
the overlay and main app share a SharedPreferences-backed history repository.

## Build and test

Requires Android Studio or a compatible JDK (17+) and Android SDK 36.
Minimum device version: Android 11 (API 30).

1. Copy `local.properties.example` to `local.properties` and set the SDK path,
   gateway URL and your own gateway key.
2. Run:

   ```sh
   ./gradlew testDebugUnitTest assembleDebug
   ```

3. Install the debug APK from `app/build/outputs/apk/debug/`, or run
   `./gradlew installDebug` with a development device connected.
4. Grant **Display over other apps** manually to use the floating summary card.

An unconfigured checkout builds with an empty key and a reserved example URL;
live summarization requires your own compatible gateway.

### Gateway contract

This is a gateway client, not a drop-in client for the public OpenAI or Anthropic
API. The base URL must support:

| Route | Purpose |
| --- | --- |
| `GET /meta/models` | Catalog with `models`, including `id`, `provider`, optional `aliases`, `lifecycle`, `replacementModelId` |
| `POST /v1/responses` | OpenAI streaming responses |
| `POST /chat/completions` | Anthropic chat-completion streaming |

Requests authenticate with `x-api-key`; generation requests select a model with
`x-model`. See `ClaudeApiService.kt` and `ModelCatalogService.kt` for request and
response details. A gateway server is not included in this repository.

### Credentials and data

The build reads `GATEWAY_BASE_URL` and `CLAUDE_API_KEY` from ignored
`local.properties`. These values are compiled into the APK: **do not distribute
an APK built with a private key**. Public app distribution needs a separate
authentication/backend design. Publish source, not locally configured binaries.

Article text is sent to the configured AI gateway. History stays in the app's
local preferences. Build outputs, local configuration and device captures are
excluded from Git.

## Tests and limitations

The unit suite covers HTML preprocessing, response parsing, input detection,
model catalog handling and summary repository/model behavior. Optional local
commit checks: `git config core.hooksPath .githooks`.

Some blocked/captive-portal pages can be treated as article text; retries are
manual. The overlay uses classic Android Views while the main app uses Compose.
