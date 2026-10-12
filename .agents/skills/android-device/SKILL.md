---
name: android-device
description: >-
  See and control the phone over adb while developing this app — take
  screenshots and view them, read the view hierarchy, tap/type/swipe, launch the
  app, tail logcat. Use when the user invokes /android-device or asks Claude to
  "look at my phone", "see the screen", "open the app on my phone", tap or drive
  the UI, check what a screen renders, or reproduce a TL;DR bug on device.
  Encodes this machine's adb quirks and the rule that the device's own settings
  are strictly off limits.
---

# /android-device — see & drive the TL;DR app on the phone

`adb` captures the screen, the Read tool renders PNGs, `adb shell input` sends
taps. No plugin, no MCP server.

## THIS IS THE USER'S DAILY DRIVER — HARD RULES

Not a test device. Not an emulator (there are none configured). It is the phone
they carry, with their messages, banking apps, and Wi-Fi on it.

**Only do what a human could do with their eyes and fingers: look at the screen,
tap, type, swipe, press Back/Home, open this app.** Nothing else.

**Never change any device or system setting — not even temporarily, not even to
test something, not even if you intend to revert it.** Specifically forbidden:

| Never run | Why |
|---|---|
| `settings put global\|system\|secure …` | system-wide, persists across reboots |
| `cmd uimode night yes\|no` | changes the user's theme device-wide |
| `svc wifi\|data disable`, airplane mode | also kills wireless debugging |
| any proxy / DNS / VPN change | breaks *every* app on the phone |
| `pm clear`, `pm disable`, `pm uninstall` | destroys real user data |
| `appops set`, `pm grant` for the overlay permission | the user grants it by hand |
| force-stopping or driving apps other than `com.example.tldr_ai` | not yours |
| Do Not Disturb, brightness, rotation lock, location | not yours |

Read-only inspection is fine (`settings get`, `dumpsys`, `getprop`, `logcat`).

### The incident that put this rule here

To reach the app's error state, an agent set a dead HTTP proxy
(`settings put global http_proxy 127.0.0.1:1`) and then "reverted" it with
`settings delete global http_proxy`, which reported success.

It was not reverted. ConnectivityService parses that key **once** and persists the
result into `global_http_proxy_host` / `global_http_proxy_port`, which the delete
left untouched. Those are device-wide — not per-network — so **all HTTP on the
phone was dead on Wi-Fi and on 5G, and survived a reboot.** The agent then
"verified" the revert with `ping`, which ignores HTTP proxies, so the check passed
while the phone was broken. The user lost their network until they reported it.

Two lessons, both general:

- A setting you can write is not a setting you can reliably restore. Don't write it.
- Verify with the same protocol you broke. `ping` proves nothing about HTTP.

### How to test the things you'd have reached for settings to test

| You want to test | Do this instead |
|---|---|
| Light theme | Temporarily pass `darkTheme = false` to `TldraiTheme` in `MainActivity`, build, look, revert. App-scoped, no device change. Or ask the user to flip it in Quick Settings. |
| Offline / fetch failure / error UI | Temporarily hardcode `SummaryUiState.Error("…")` in the screen, build, look, revert. Do **not** touch the network. |
| A URL that fails | You probably can't: on this network bogus hostnames resolve and blocked ports return a page, so `fetchUrl` succeeds with junk. Accept it and say the state is unverified. |
| Overlay ("Display over other apps") permission | Ask the user to grant it in Settings. |

If a state genuinely cannot be reached without touching the device, **say so and
report it as unverified.** That is a fine outcome. Breaking the phone is not.

## Leave no trace

The app writes to the user's real data — `HistoryRepository` (SharedPreferences,
shared with the overlay service). Every summary you trigger while testing lands in
their history.

Before you finish:

- Delete exactly the history entries you created (expand the row → **Delete**).
  Timestamps tell yours apart from theirs. **Never** `dev.sh clear`.
- Stop any `FloatingCardService` you started; check with
  `adb shell dumpsys activity services com.example.tldr_ai`.
- Leave the app on a neutral screen, input field cleared.

If a Back press drops you out of the app onto the launcher or lock screen,
relaunch immediately. Don't screenshot, read, or describe what's there — it's
their home screen, not test data.

## Setup on this machine

| Thing | Value |
|---|---|
| `adb` binary | `~/Android/Sdk/platform-tools/adb` — **not on `$PATH`**, always use the full path |
| Device | Pixel 9 Pro (`caiman`), over **wireless debugging** (`_adb-tls-connect._tcp`) |
| Emulators / AVDs | **None configured.** `~/.android/avd/` is empty — there is no virtual device |
| Screen | 1280 × 2856 |
| App | `com.example.tldr_ai` / `.MainActivity` |
| Helper script | `.agents/skills/android-device/scripts/dev.sh` |

The connection is wireless, so the device can drop off after a reboot or network
change. `dev.sh devices` returning nothing means the user must re-enable
**Developer options → Wireless debugging** and pair again — Claude cannot restore
it remotely. USB also works and is more reliable.

## Work cheap: grep the hierarchy, don't screenshot

Reading a screenshot costs ~1.5–2k tokens and a slow round trip. A hierarchy grep
costs ~50 tokens and answers "what state is it in" more precisely.

```bash
S=.agents/skills/android-device/scripts/dev.sh
$S ui | grep -E 'text="(INFORMATIVE|SOME SPIN|PURE CLICKBAIT|DIDN.T WORK)"'
```

**Screenshot only when layout, spacing, or colour is the actual question**, or when
the user wants to see it. Budget a handful per session, not one per step.

Two more habits that save whole turns:

- **Chain a whole interaction into one bash call** with `&&` and `sleep`; each
  separate call is a round trip.
  ```bash
  $S tapon "Clear" && sleep 1 && $S type "https://example.com/x" && sleep 1 && $S ui | grep -E 'text="(Link|Text)"'
  ```
- **Filter build output** to one line: `$S install 2>&1 | grep -E "^e:|BUILD"`.
- A summarize takes ~5–12 s. `sleep 12` once beats polling three times.
- `$S shot` overwrites `/tmp/screen.png` — pass distinct names when comparing
  before/after.

## Quirk 1: `exec-out screencap` returns a black image

The idiomatic one-liner **silently fails on this device** — a valid PNG of pure
black (~20 KB) while the screen is genuinely on and unlocked:

```bash
adb exec-out screencap -p > screen.png   # ✗ black frame, no error
```

`dev.sh shot` already does the working thing (capture to a file on the device, then
pull). **A black screenshot is this bug, not a sleeping phone** — confirm with
`dev.sh state`: `mScreenState=ON` and `mAwake=true` means the display is fine.

## Quirk 2: never compute taps from a screenshot

Screenshots are 1280 × 2856 but are downscaled for viewing (~896 × 2000), so
coordinates read off a rendered image are wrong by ~1.43×. Don't do that
arithmetic. `dev.sh ui` prints real device `bounds`; `dev.sh tapon "<text>"` finds
a label and taps its centre.

## Quirk 3: Compose exposes no resource-ids

Every node in this app comes back with `resource-id=""`, so match on **`text="…"`**
or **`content-desc="…"`** (exact match). The visible label is usually a
non-clickable `TextView` inside a clickable parent — tapping its centre works
anyway because the click propagates to the clickable ancestor.

## Quirk 4: the text field has no label to tap

`BasicTextField` shows up as **no `text=` node at all** when empty — not even the
placeholder, and no `EditText` in the dump. Don't guess coordinates. Anchor off the
round button, which always has a `content-desc`, and tap left of it at the same y:

```bash
$S ui | grep -E 'content-desc="(Paste from clipboard|Get the TL;DR)"'
# → bounds="[1093,2621][1150,2678]"   then tap the field beside it:
$S tap 400 2620
```

Once the field has content, its text *is* in the dump and `tapon` works on it.

## Current UI map (post-redesign)

Saves an exploration round trip. Re-check with `$S ui` if the UI has moved on.

**Home** — `text="TL;DR"` (wordmark), `content-desc="History"` (top right).
Idle legend: `Informative` / `Some spin` / `Pure clickbait`.
Chips above the composer: `Link` or `Text` (only once input is non-empty; tap to
override the auto-detected mode), the model name (e.g. `GPT-5.6 Luna`, opens the
sheet), and `Clear`.
The round button's `content-desc` *is* the state: **`Paste from clipboard`** (empty)
→ **`Get the TL;DR`** (ready) → none (working, spinner).
Result card: overline `INFORMATIVE` / `SOME SPIN` / `PURE CLICKBAIT`, plus `Copy`
and `Share`. Errors show `DIDN'T WORK` and `Try again`.

**History** — `content-desc="Back"`, `text="History"`,
`content-desc="Clear history"`. Rows carry title + verdict + time and
`content-desc="Expand"` / `"Collapse"`; expanded rows add `Copy`, `Share`, `Delete`.

**Model sheet** — `ANTHROPIC` / `OPENAI` headers with model names under them; tap a
name to select and dismiss.

## Helper script

```bash
S=.agents/skills/android-device/scripts/dev.sh

$S devices               # list connected devices
$S state                 # display on/off, awake, lockscreen, battery
$S fg                    # current foreground activity
$S shot [out.png]        # screenshot → /tmp/screen.png (default); then Read it
$S ui [pattern]          # every text= with its bounds, optionally filtered
$S tap <x> <y>           # tap raw device coordinates
$S tapon "<text>"        # find a label, tap its centre  ← prefer this
$S type "some text"      # type into the focused field (spaces handled)
$S key BACK|HOME|ENTER   # keyevent (KEYCODE_ prefix optional)
$S swipe <x1> <y1> <x2> <y2> [ms]
$S launch [pkg]          # defaults to com.example.tldr_ai
$S stop [pkg]            # force-stop (this app only)
$S log [pkg] [lines]     # logcat, filtered to the package's PID
$S install               # ./gradlew installDebug, then relaunch
$S clear [pkg]           # ✗ wipes the user's real history — needs explicit permission
```

## Typical loop

```bash
S=.agents/skills/android-device/scripts/dev.sh
$S install 2>&1 | grep -E "^e:|BUILD"
$S ui | grep -E 'content-desc="(Paste from clipboard|Get the TL;DR)"'   # find the composer
$S tap 400 2620 && sleep 1 && $S type "https://example.com/article"
$S tapon "Get the TL;DR" && sleep 12
$S ui | grep -oE 'text="(INFORMATIVE|SOME SPIN|PURE CLICKBAIT)"'        # cheap verify
$S shot /tmp/after.png                                                  # only if you must see it
$S log                                                                  # on unexpected results
```

Iterating over list rows (e.g. deleting your test entries) works well as a loop —
the top row's bounds are stable, so re-tap the same spot after each removal:

```bash
for i in 1 2 3; do
  $S tap 400 635 >/dev/null; sleep 1      # expand top row
  $S tapon "Delete" >/dev/null; sleep 1
done
```

## Gotchas

- **`type` breaks on apostrophes and quotes** — `WON'T` fails with
  `/system/bin/sh: no closing quote`. Spaces are handled (converted to `%s`);
  nothing else is escaped. Reword the test string, or set the clipboard and use the
  **Paste** button.
- **A config change restarts the activity and resets Compose state** — route
  returns to Home; only `rememberSaveable` values survive. Expect to re-navigate.
- **Back on the Home route exits the app.** One Back too many and you're on the
  launcher; relaunch rather than pressing Back again.
- A disabled button reports `enabled="false"`, so a tap that "does nothing" is often
  correct behaviour rather than a missed hit.
- `uiautomator dump` needs the screen on and unlocked; if `$S ui` returns lock-screen
  nodes, the phone locked itself — stop and ask rather than poking around it.
- Wireless debugging plus an awake screen drains the battery fast; `$S state` prints
  the level, and a cable is worth suggesting for long sessions.
