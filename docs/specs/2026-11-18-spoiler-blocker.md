# Shush — Specification

Date: 2026-11-18
Status: Agreed

## Overview

**Shush** is a native Android app (Kotlin + Jetpack Compose) that hides spoiler content anywhere on the user's phone.

The user maintains a flat list of keywords — show names, character names, plot terms — that they don't want to see. After a guided onboarding, the user grants **Accessibility Service permission**, and from then on the app runs persistently in the background, surviving reboots and Android's background-process restrictions.

Whenever the visible screen contains text matching a blocked keyword (whole-word, case-insensitive), the app covers the **entire screen** with an opaque **"SPOILERS HIDDEN"** cover, in any app: Twitter, Reddit, YouTube, WhatsApp, Chrome, anywhere. The cover passes touches through, so the user simply scrolls until the spoiler content leaves the viewport — the cover then lifts automatically. A small **"Reveal · 5s"** pill rides on top of the cover for a deliberate 5-second peek.

**Detection** works by scanning the accessibility node tree of the active window when its content changes: the service finds text nodes containing a keyword. Scans run immediately on content change (throttled to 150 ms minimum interval; system event batching at 50 ms) so spoilers are covered as fast as possible. Text baked into images and video frames is out of scope for v1 (a future OCR detector can extend coverage through the same overlay system).

The core promise: once it's set up, the user never has to think about it. Spoilers are quietly covered, the phone stays usable, and battery impact stays minimal through throttled scanning.

## Scope

### In scope (v1)

- **Keyword management** — a single flat list: add, edit, delete keywords; persists locally on-device
- **Whole-word, case-insensitive matching** — "Red Wedding" matches "red wedding" but not "covered"
- **Guided onboarding** — one welcome screen explaining what the app does and why it needs accessibility access, a keyword entry step, then a button that jumps straight to the system's Accessibility settings to enable the service
- **Accessibility service** — listens for window content changes across all apps, walks the active window's node tree, finds keyword matches, computes their screen rectangles
- **Cover overlays** — opaque full-screen "SPOILERS HIDDEN" cover via `TYPE_ACCESSIBILITY_OVERLAY` (no extra permission needed); the cover is touch-transparent so scrolling works through it
- **Reveal pill** — small tappable "Reveal · 5s" pill on top of the cover; reveals everything for 5 seconds, then re-covers if spoilers remain
- **Persistence** — service auto-restarts when the system kills it; relaunches at boot; onboarding includes disabling battery optimization
- **Service status visibility** — the app's main screen clearly shows whether protection is currently active or the permission was revoked
- **Master on/off toggle** — "Spoiler blocking" switch on the main screen pauses/resumes blocking instantly without touching the accessibility permission; pausing also lifts any active cover immediately
- **Throttled scanning** — content-change events are scanned immediately (50 ms system batching, 150 ms minimum scan interval) so spoilers are covered with minimal latency

### Out of scope (v1)

- Text inside images, videos, or canvas-rendered apps (OCR — possible later, behind the same `KeywordMatch → screen rect` interface)
- Keyword groups with per-group toggles
- Per-app exemptions list
- iOS, or any cross-platform build
- Cloud sync / accounts — everything stays on-device
- Regex or advanced pattern matching

## Behavior

### First launch

1. Welcome screen: what the app does, why accessibility access is needed, and a privacy note that all analysis is on-device
2. Add-first-keywords step (skippable — but blocking stays inactive until at least one keyword exists)
3. "Enable protection" button → deep-links to system Accessibility settings
4. On return, the app detects the service is on and asks to disable battery optimization, then shows the main screen with an "Active" status

### Main screen (keyword list)

- Current status banner, three honest states: **Protected** (service on, blocking on), **Paused** (service on, blocking switched off), **Protection off — tap to fix** (permission revoked; button deep-links back to settings)
- **Spoiler blocking** master switch: pauses scanning entirely and lifts any active cover immediately; state persists across restarts
- List of keywords with delete (undoable via snackbar); text field + add button (IME Done submits; duplicates are rejected with a snackbar)
- Empty state explains the concept with an example ("Try: Red Wedding")

### Blocking, in real time

- On any screen content change, the service scans visible text immediately (50 ms event batching, 150 ms minimum scan interval)
- If any match is visible, the full-screen cover appears; it stays until a rescan finds no matches (e.g., the user scrolled the spoiler out of view)
- **Reveal pill:** tapping "Reveal · 5s" hides the cover for 5 seconds; it re-covers automatically if spoilers are still on screen

### Edge cases

- **Keyword edited/deleted while covered:** stickers refresh on next scan; stale stickers clear when the window changes
- **Password fields, banking apps:** the service never logs, transmits, or stores screen text — matching is in-memory only
- **Service killed by system:** auto-restarts; if Android fully revokes it, the status banner flips to Inactive on next app open
- **Reboot:** service restores automatically; blocking resumes without opening the app

## Visual design — Material 3 Expressive

### Design intent

The user arrives spoiler-anxious; the app must feel calm and quietly vigilant. Two things deserve attention: the **status** ("am I protected right now?") and the **sticker** (the product's entire promise). Everything else — the keyword list, settings — stays calm. Expressive restraint: a few strong expressive moments, not uniform loudness.

### Theme

Dynamic color (Material You) from the user's wallpaper, with a custom fallback palette. All screens use semantic roles only:

- Screen background: `surface`
- Primary text: `onSurface`; secondary: `onSurfaceVariant`
- `primary` reserved for the single main action per screen
- Status banner: `primaryContainer` / `onPrimaryContainer`
- "Protection off" state: `errorContainer` / `onErrorContainer` (paired with the word "Off", never color alone)
- Keyword list items: `surface` / `surfaceContainer`

### Main screen

A status banner up top built on `primaryContainer` — a contained region (not card-in-card) with **emphasized display typography**: "Protected" large in the expressive type style, keyword count a step below in `onSurfaceVariant`. Below, the keyword list as calm expressive list items, delete via icon button. Add keywords through a filled button in `primary` — the screen's one loud action. Empty state explains the concept with a real example, no illustration.

### The cover — signature element

The app's one expressive object: an opaque dark charcoal (#141318) full-screen cover with "SPOILERS HIDDEN" in the app's display voice, a calm instruction line beneath, and a translucent white "Reveal · 5s" pill at the top. The cover never shows content through it (it performs safety work, not decoration), and its pill honors the same 48 dp minimum touch target as the rest of the app.

### Accessibility

- The sticker must meet full contrast (it performs safety work, not decoration)
- All touch targets ≥ 48 dp
- Text scales with the system font
- State is never color-only (the banner pairs color with the word "Protected"/"Off")

## Success criteria

1. **Blocking works end-to-end:** with keyword "Red Wedding" enabled, a tweet reading "the red wedding shocked everyone" is covered by the full-screen cover within ~1 s of appearing on screen
2. **Coverage is complete:** no spoiler text is readable while matches are visible — the whole screen is covered, not just the matching line
3. **Scroll case:** with the cover up, scrolling moves the spoiler content out of view and the cover lifts automatically once no matches remain visible
4. **Reveal pill:** tapping "Reveal · 5s" uncovers the screen for 5 s; it re-covers if spoilers are still visible
5. **Keyword lifecycle:** deleting a keyword removes its stickers on the next screen change; editing one updates matching
6. **Persistence:** after a reboot, without opening the app, matched text is blocked (service auto-starts)
7. **Status honesty:** the main screen banner accurately reflects service state in both directions (enabled/disabled/revoked)
8. **Battery:** no scanning when the screen is off; event debounce prevents scan storms — idle drain effectively zero, active-use drain imperceptible
9. **Privacy:** the manifest requests **no `INTERNET` permission** — data can never leave the device; matching happens in memory only
10. **Onboarding:** a new user goes from install to protected state in under a minute

## Architecture decision

**Chosen approach: accessibility node scanning + accessibility overlays** (structured for a future OCR detector).

The `AccessibilityService` listens for window-content-change events. When text on screen changes, it walks the active window's accessibility node tree, finds text nodes containing blocked keywords, and gets each node's on-screen bounds (`boundsInScreen`). It then draws an opaque sticker over exactly those bounds using a `TYPE_ACCESSIBILITY_OVERLAY` window — which the accessibility service can add **without any extra permission**.

Alternatives considered and rejected for v1:
- **Screenshot + OCR** (MediaProjection + ML Kit): catches text inside images, but needs two extra scary permissions, drains battery, is laggy and privacy-sensitive. Future extension, not foundation.
- The internal pipeline is defined as `KeywordMatch → screen rect` so an OCR detector can slot in later without rework.
