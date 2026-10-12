---
name: switch-providers
description: >-
  Move this app's AI gateway/provider (Azure gateway → Nexos → Eden AI, etc.).
  Use when asked to switch, replace, or add an AI provider, change the API
  base URL or key name, or restrict which models the picker offers.
---

# Switching AI providers

The provider is isolated to a few files. `git show` the last provider-switch
commit first: it is the best template, and the diff is small.

## Touch points (in this order)

| File | What to change |
|---|---|
| `data/api/Gateway.kt` | `GATEWAY` base URL |
| `app/build.gradle.kts` | `local.properties` key name + `buildConfigField` |
| `data/api/ClaudeApiService.kt` | `BuildConfig.<KEY>`, auth headers, request body, and the "no key" error text |
| `data/api/ModelCatalogService.kt` | catalog URL, auth, JSON shape, and the **model filter** (`parseCatalog`) |
| `data/model/AiModel.kt` | `FALLBACK` list (ids in the new provider's format), `ModelFamily` |
| `ui/components/ModelPicker.kt` | family section headers, if families change |
| `data/repository/ModelCatalogRepository.kt` | rename `PREFS_NAME` so the old provider's cached catalog is not reused |
| `CLAUDE.md` | the AI / Model routing / Setup sections |
| `app/src/test/.../ModelCatalogServiceTest.kt` | rewrite fixtures for the new catalog shape |

## Efficient workflow

1. **Read the provider docs first** (`webfetch`); find the chat-completions URL,
   auth header, model-id format and whether the model list is public.
2. **Curl the real catalog** before writing the filter, instead of guessing
   field names: `curl -s <url>/models | python3 -c ...`. Pick fields such as
   `owned_by`, `model_name`, `alias_of`. Skip `-latest` style aliases so models
   are not duplicated.
3. **Make all edits in one pass**, then `./gradlew testDebugUnitTest --offline -q`.
4. **Rename the key in `local.properties`** (gitignored; never print its value).
   A stale key name is the usual cause of "API key not configured": the build
   silently bakes in an empty string. Check with
   `grep -n KEY local.properties | sed 's/=.*/=<redacted>/'`.
5. Install: `.agents/skills/android-device/scripts/dev.sh install 2>&1 | grep -E "^e:|BUILD"`.
   Do not trigger summaries on the phone unless asked; they land in the user's history.
6. Stale saved selection: ids differ between providers, so a model saved in
   `SettingsRepository` fails after a switch until the user re-picks.

## Gotchas

- Model ids are sent verbatim in the request body. Use the provider's exact
  format (Eden AI: `provider/model`).
- `extractDelta` already parses OpenAI-style chat chunks; a provider that streams
  that format needs no parser change.
- Some gateways block the default OkHttp User-Agent; add one only if you get 403s.
