#!/usr/bin/env bash
# See and control the phone over adb. See ../SKILL.md for the quirks this encodes.
set -uo pipefail

ADB="${ADB:-$HOME/Android/Sdk/platform-tools/adb}"
PKG_DEFAULT="com.example.tldr_ai"
UI_REMOTE=/sdcard/_dev_ui.xml
SHOT_REMOTE=/sdcard/_dev_shot.png

[[ -x "$ADB" ]] || { echo "adb not found at $ADB (set ADB=/path/to/adb)" >&2; exit 1; }

die() { echo "$*" >&2; exit 1; }

# Fresh hierarchy dump -> stdout. uiautomator writes progress to stdout, so discard it.
ui_xml() {
  "$ADB" shell uiautomator dump "$UI_REMOTE" >/dev/null 2>&1 \
    || die "uiautomator dump failed — is the screen on and a device connected?"
  "$ADB" shell cat "$UI_REMOTE"
}

# Centre coordinates of the first node whose text= or content-desc= matches $1 exactly.
find_center() {
  local needle="$1" line nums
  line=$(ui_xml | tr '<' '\n' \
    | grep -F -e "text=\"$needle\"" -e "content-desc=\"$needle\"" \
    | grep -oE 'bounds="\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]"' | head -1)
  [[ -n "$line" ]] || return 1
  nums=$(echo "$line" | grep -oE '[0-9]+' | tr '\n' ' ')
  # shellcheck disable=SC2086
  set -- $nums
  echo "$(( ($1 + $3) / 2 )) $(( ($2 + $4) / 2 ))"
}

pid_of() { "$ADB" shell pidof "$1" 2>/dev/null | tr -d '\r'; }

cmd=${1:-help}; shift || true

case "$cmd" in
  devices)
    "$ADB" devices -l
    ;;

  state)
    "$ADB" shell dumpsys display | grep -iE 'mScreenState' | head -1
    "$ADB" shell dumpsys window  | grep -iE 'mAwake|mDreamingLockscreen' | head -1
    echo "battery: $("$ADB" shell dumpsys battery | grep -E '^\s+level' | grep -oE '[0-9]+')%"
    ;;

  fg)
    "$ADB" shell dumpsys window | grep -E 'mCurrentFocus' | head -1
    ;;

  shot)
    out=${1:-/tmp/screen.png}
    # NOTE: `exec-out screencap` yields an all-black PNG on this device. Capture + pull.
    "$ADB" shell screencap -p "$SHOT_REMOTE" || die "screencap failed"
    "$ADB" pull "$SHOT_REMOTE" "$out" >/dev/null || die "pull failed"
    "$ADB" shell rm -f "$SHOT_REMOTE"
    if [[ $(stat -c%s "$out") -lt 40000 ]]; then
      echo "warning: $out is suspiciously small — may be a black frame; check '$0 state'" >&2
    fi
    echo "$out"
    ;;

  ui)
    xml=$(ui_xml)
    "$ADB" shell rm -f "$UI_REMOTE"
    out=$(echo "$xml" | tr '<' '\n' | grep -oE '(text|content-desc)="[^"]+"[^>]*bounds="[^"]*"' \
          | sed -E 's/ (class|package|checkable|checked|password|drawing-order|index|hint|long-clickable|scrollable|focusable|focused|selected)="[^"]*"//g')
    if [[ $# -gt 0 ]]; then
      echo "$out" | grep -E "$1" || echo "(no node matching /$1/ — is the right screen in the foreground? '$0 fg')" >&2
    else
      echo "$out"
    fi
    ;;

  tap)
    [[ $# -eq 2 ]] || die "usage: $0 tap <x> <y>"
    "$ADB" shell input tap "$1" "$2"
    ;;

  tapon)
    [[ $# -ge 1 ]] || die "usage: $0 tapon \"<visible text>\""
    xy=$(find_center "$1") || die "no node with text/content-desc \"$1\" — try '$0 ui' to see what's on screen"
    "$ADB" shell rm -f "$UI_REMOTE"
    # shellcheck disable=SC2086
    "$ADB" shell input tap $xy && echo "tapped \"$1\" at $xy"
    ;;

  type)
    [[ $# -ge 1 ]] || die "usage: $0 type \"text\""
    # `input text` cannot take literal spaces.
    "$ADB" shell input text "${*// /%s}"
    ;;

  key)
    [[ $# -ge 1 ]] || die "usage: $0 key <KEYCODE>"
    k=$1; [[ $k == KEYCODE_* ]] || k="KEYCODE_$k"
    "$ADB" shell input keyevent "$k"
    ;;

  swipe)
    [[ $# -ge 4 ]] || die "usage: $0 swipe <x1> <y1> <x2> <y2> [ms]"
    "$ADB" shell input swipe "$1" "$2" "$3" "$4" "${5:-300}"
    ;;

  launch)
    pkg=${1:-$PKG_DEFAULT}
    act=$("$ADB" shell cmd package resolve-activity --brief -c android.intent.category.LAUNCHER "$pkg" | tail -1 | tr -d '\r')
    [[ $act == */* ]] || die "could not resolve a launcher activity for $pkg (installed?)"
    "$ADB" shell am start -n "$act" >/dev/null || die "am start failed"
    sleep 2
    "$ADB" shell dumpsys window | grep -E 'mCurrentFocus' | head -1
    ;;

  stop)
    "$ADB" shell am force-stop "${1:-$PKG_DEFAULT}" && echo "stopped ${1:-$PKG_DEFAULT}"
    ;;

  log)
    pkg=${1:-$PKG_DEFAULT}; lines=${2:-100}
    pid=$(pid_of "$pkg")
    if [[ -n "$pid" ]]; then
      "$ADB" logcat -d --pid="$pid" -t "$lines"
    else
      echo "($pkg not running — showing unfiltered tail)" >&2
      "$ADB" logcat -d -t "$lines"
    fi
    ;;

  install)
    [[ -x ./gradlew ]] || die "no ./gradlew here — run from the project root"
    ./gradlew installDebug || die "gradle install failed"
    exec "$0" launch
    ;;

  clear)
    pkg=${1:-$PKG_DEFAULT}
    "$ADB" shell pm clear "$pkg"
    ;;

  help|--help|-h)
    sed -n '/^  devices)/,/^  help)/p' "$0" | grep -oE '^  [a-z|-]+\)' | tr -d ' )'
    echo "(see ../SKILL.md for details)"
    ;;

  *)
    die "unknown command: $cmd — try '$0 help'"
    ;;
esac
