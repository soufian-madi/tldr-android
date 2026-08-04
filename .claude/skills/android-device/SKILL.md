---
name: android-device
description: >-
  See and control the phone over adb while developing this app — take
  screenshots and view them, read the view hierarchy, tap/type/swipe, launch the
  app, tail logcat. Use when the user invokes /android-device or asks Claude to
  "look at my phone", "see the screen", "open the app on my phone", tap or drive
  the UI, check what a screen renders, or reproduce a TL;DR bug on device.
  Encodes this machine's specific adb quirks.
---

# /android-device — see & drive the TL;DR app on the phone

Claude can view and control the phone with **no plugin and no MCP server**:
`adb` captures the screen, the Read tool renders PNGs, and `adb shell input`
sends taps. The only things worth knowing are the three quirks below.

## Setup on this machine

| Thing | Value |
|---|---|
| `adb` binary | `~/Android/Sdk/platform-tools/adb` — **not on `$PATH`**, always use the full path |
| Device | Pixel 9 Pro (`caiman`), over **wireless debugging** (`_adb-tls-connect._tcp`) |
| Emulators / AVDs | **None configured.** `~/.android/avd/` is empty — there is no virtual device |
| Screen | 1280 × 2856 |
| App | `com.example.tldr_ai` / `.MainActivity` |
| Helper script | `.claude/skills/android-device/scripts/dev.sh` |

The connection is wireless, so the device can drop off after a reboot or network
change. `dev.sh devices` returning nothing means the user must re-enable
**Developer options → Wireless debugging** and pair again — Claude cannot
restore it remotely. USB also works and is more reliable.

## Quirk 1: `exec-out screencap` returns a black image

The idiomatic one-liner **silently fails on this device** — a valid PNG of pure
black (~20 KB) while the screen is genuinely on and unlocked:

```bash
adb exec-out screencap -p > screen.png   # ✗ black frame, no error
```

Always capture to a file on the device and pull it (~2.4 MB of real pixels):

```bash
ADB=~/Android/Sdk/platform-tools/adb
$ADB shell screencap -p /sdcard/s.png && $ADB pull /sdcard/s.png /tmp/screen.png && $ADB shell rm /sdcard/s.png
```

**A black screenshot is this bug, not a sleeping phone.** Confirm with
`dev.sh state`: if it reports `mScreenState=ON` and `mAwake=true`, the display is
fine and the capture method is the problem.

## Quirk 2: don't tap coordinates read off a screenshot

Screenshots are 1280 × 2856 but get downscaled for viewing (~896 × 2000), so
positions read off a rendered image are wrong by ~1.43×. Don't do that
arithmetic. Dump the view hierarchy and use its real `bounds`:

```bash
$ADB shell uiautomator dump /sdcard/ui.xml
```

`dev.sh ui` prints every `text=` with its `bounds`; `dev.sh tapon "<text>"` finds
a label and taps its centre.

## Quirk 3: Compose exposes no resource-ids

Every node in this app comes back with `resource-id=""`, so match on
**`text="..."`** or **`content-desc="..."`**. The visible label is usually a
non-clickable `TextView` nested inside a clickable parent `View` — tapping the
label's centre works anyway, because the click propagates to the clickable
ancestor. That is what `tapon` relies on.

## Verify with the hierarchy, not just a screenshot

After an action, re-dump and grep for the expected text — cheaper and less
ambiguous than reading another image, and it catches "the tap landed but the
state didn't change":

```bash
$ADB shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
$ADB shell cat /sdcard/ui.xml | grep -oE 'text="(Claude|GPT)[^"]*"' | sort -u
```

Screenshot when the user wants to *see* it, or when layout/colour is the
question. Grep the hierarchy when the question is *what state is it in*.

## Helper script

```bash
S=.claude/skills/android-device/scripts/dev.sh

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
$S stop [pkg]            # force-stop
$S log [pkg] [lines]     # logcat, filtered to the package's PID
$S install               # ./gradlew installDebug, then relaunch
$S clear [pkg]           # clear app data (destructive — confirm first)
```

## Typical loop for this project

```bash
S=.claude/skills/android-device/scripts/dev.sh
$S install                          # build + install + relaunch
$S shot                             # then Read /tmp/screen.png
$S tapon "Paste Text"               # switch input mode
$S type "https://example.com/article"
$S tapon "Get TL;DR"
$S log                              # check for API / parse errors
$S shot /tmp/after.png              # then Read it
```

Selecting a model from the picker, which is a two-step dropdown:

```bash
$S tapon "GPT-5.6 Luna"   # if the menu is already open
# otherwise open it first — the trigger row is the currently-selected model name:
$S tapon "Claude Fable 5 (direct)" && sleep 1 && $S ui 'GPT|Claude'
```

## Notes

- `type` converts spaces to `%s`, which `adb shell input text` requires. Other
  special characters are not escaped — for awkward strings, set the clipboard and
  long-press-paste instead.
- A disabled button reports `enabled="false"` in the dump, so a tap that "does
  nothing" is often correct behaviour (e.g. **Get TL;DR** while the input is
  empty) rather than a missed hit.
- Wireless debugging plus an awake screen drains the battery fast; `$S state`
  prints the level, and a cable is worth suggesting for long sessions.
- `clear` wipes app data including saved history. Confirm before running it.
