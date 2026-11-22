# Nocturne UI Audit

**Date:** 2026-11-03
**Scope:** Current Android and desktop screenshots, selected Compose UI source, and the archived Convx-inspired screenshots. This is a visual/source review, not a live interaction or accessibility test.

## Overall score: 6.0/10

| Area | Score | Why |
|---|---:|---|
| Design | 6.5 | Clear dark music-player shell and useful hierarchy, weakened by inconsistent platform styling, repeated glass treatments, and fallback artwork. |
| Usability | 5.5 | The library is easy to scan, but the full-player capture is missing, the empty Favorites screen offers no direct action, and compact controls need a real target-size check. |
| Creativity | 6.0 | The floating player and ambient mesh are coherent, but Liquid Glass is a familiar category treatment rather than a distinct Nocturne idea. |
| Content | 5.0 | The desktop capture looks like a test library, and the Android captures show little or no real cover art. |

Weighted score: `0.4 × 6.5 + 0.3 × 5.5 + 0.2 × 6 + 0.1 × 5 = 6.0`.

## What I reviewed

- `screenshots/android_current_1.png` — Library, 1080 × 2436.
- `screenshots/android_full_player.png` — This is **not** the full player; it shows the empty Favorites tab.
- `screenshots/desktop_liquid_glass_new.png` — Desktop Home, 941 × 1030.
- `screenshots/archive/convx_new_ui.png`, `screenshots/archive/full_player_opened.png`, and `screenshots/archive/appearance_tab.png` — older designs for context.
- Android UI in `androidApp/src/main/kotlin/com/never/nocturne/MainActivity.kt` and `androidApp/src/main/kotlin/com/never/nocturne/ui/GlassStyle.kt`.
- Desktop UI in `desktopApp/src/main/kotlin/com/never/nocturne/desktop/Main.kt` and `desktopApp/src/main/kotlin/com/never/nocturne/desktop/Theme.kt`.

The current full-player dialog exists in source (`MainActivity.kt:864`), but I cannot judge its current rendering from the supplied screenshots. The archived full-player image is an older crimson layout, so it is not a substitute.

## Strengths

- **The library screen answers its main question quickly.** The app name, folder action, search, track count, song rows, and persistent playback controls have a clear order.
- **Android rows are legible and easy to scan.** Track and artist names are separated, the active track has a distinct state, and favorite actions are visible.
- **The desktop layout uses familiar music-player structure.** Navigation stays on the left, the library occupies the main area, and playback remains available at the bottom.
- **The styling has a clear point of view.** The dark surfaces, rounded floating controls, and artwork-reactive background form a recognizable Liquid Glass direction.
- The Android manifest declares the app name as **Nocturne** (`androidApp/src/main/AndroidManifest.xml:13-17`).

## Main issues and concrete fixes

### 1. The supplied full-player screenshot shows the wrong screen

`android_full_player.png` shows “No favorites yet — tap ♡ on any track,” not the Now Playing dialog. This leaves the most important playback screen unaudited. The archived full-player capture shows an earlier design and cannot verify the current version.

**Fix:** Replace or rename that capture and save a fresh screenshot with the current Now Playing dialog open. Include one screenshot with real cover art and one without it, so both rendering paths are visible. Keep the empty Favorites screen under a matching filename.

### 2. The Android launcher icon is not connected in the manifest

The application label is set to “Nocturne,” and `mipmap-*/ic_launcher.png` resources exist. But `androidApp/src/main/AndroidManifest.xml` has no `android:icon` or `android:roundIcon` attribute, so the supplied icon is not explicitly selected for the launcher.

**Fix:** Wire the intended launcher icon in the application manifest, then verify the installed launcher icon and themed/round appearance on a device. The in-app header mark does not verify the launcher icon.

### 3. Android and desktop do not read as one product

The Android capture uses a cyan accent on a navy-purple background. The desktop capture uses a pink/red accent over a much warmer olive-brown mesh. The visual language is related, but the accent and background feel like different themes. Android also hard-codes its component accent in `androidApp/src/main/kotlin/com/never/nocturne/ui/GlassStyle.kt:42-44`, while desktop resolves its accent from shared settings in `desktopApp/src/main/kotlin/com/never/nocturne/desktop/Theme.kt:56-75`.

**Fix:** Resolve both platforms from the same shared accent and surface tokens. Keep artwork-derived color as a quiet background tint, not a reason for the primary action and navigation to change identity. Compare both platforms with the same preset and the same test tracks.

### 4. The screenshots do not yet show representative music content

The desktop capture is rooted at `lmp-spike` and shows “Spike One,” “Spike Two,” and “Test Artist”; the recent shelf repeats those two tracks. The Android capture includes a filename-like timestamp and generic-sounding track/artist names. These may be test files, but they read as fixture content. Most visible cover tiles also fall back to a note icon.

**Fix:** Before using these as release or store screenshots, capture a representative local library with real metadata and cover art. If the empty-art fallback is common, give it a deliberate, calm treatment that does not make every song look like the same album. Consider showing distinct recent tracks in “Jump Back In”; repeated listens can still be reflected in history.

### 5. Empty Favorites is visually stranded and has no direct next step

`android_full_player.png` has one centered sentence surrounded by a large empty area, while playback and navigation occupy the bottom. The message explains what Favorites is, but does not help the user get there from that state.

**Fix:** Keep the empty state compact and add a clear action that returns to Library (or another direct route to find a track). Retain enough open space to distinguish an empty state, but make the next step visible without relying on the user to infer the tab bar.

### 6. Desktop glass and mesh compete with the content

The desktop screenshot’s large warm color wash shows through both floating panels. The panels and page blend into one another, while the small sidebar labels and muted metadata recede further. The mesh is a good supporting effect, but it currently takes more attention than the two-track library.

**Fix:** Increase panel opacity and lower the mesh’s visible saturation/brightness at the content layer. Keep text at or above the app-screen minimum of 11sp; the “BROWSE” and “LIBRARY” labels are 10sp in `desktopApp/src/main/kotlin/com/never/nocturne/desktop/Main.kt:306-334`. Recheck the screen with light, vivid, and missing album art.

### 7. Android mini-player text and controls are cramped

In `android_current_1.png`, the artist is truncated in the mini-player, and several controls compete for width beside the track title. The implementation sets the mini-player icon buttons to 34dp and the play button to 38dp (`MainActivity.kt:792-825`). Compose may enforce a larger interaction target around these controls, so the screenshot alone cannot confirm the actual touch bounds.

**Fix:** Check the rendered semantics/touch bounds on a 360dp-wide device. Preserve at least a 44dp target for each action; if the row is too crowded, remove a secondary control from the mini-player rather than shrinking the tap area or hiding the track identity. The full player can hold the remaining controls.

### 8. Some navigation labels are vague

The current Android bottom bar shortens Playlists, Favorites, and Appearance to “Lists,” “Favs,” and “More” (`MainActivity.kt:640-645`). This avoids the three-line “Appearance” wrap visible in the older archive, but “More” does not tell users that it opens appearance settings.

**Fix:** Use a short but meaningful label such as “Settings,” or make the destination clear through an accessible label and a visible screen title. Keep the one-line fit improvement from the current version.

## Convx comparison

Nocturne has adopted the main Convx cues: a blurred, artwork-reactive field, floating navigation/player surfaces, rounded controls, and an expanded playback view. The current Android library is cleaner than the archived `convx_new_ui.png`: the old screenshot wraps “Appearance” across several lines, while the current bar uses a compact one-line label.

The current result still leans on the same broad Liquid Glass vocabulary many music players use. The next step is not to add more blur or glow. Make Nocturne feel specific through consistent shared color tokens, real cover art, accurate track metadata, and a distinct empty-library/favorites experience. The archived screenshots document earlier iterations; they are not proof that the current player dialog or appearance screen is correct.

## Screen questions and evidence gaps

- **Android Library:** “What can I play?” — mostly answered; search and the track list are immediately visible.
- **Android Favorites capture:** “What have I saved?” — answered as empty, but the next action is weak.
- **Desktop Home:** “What should I play or open?” — navigation and track list are visible, but the test-looking content makes it feel like a development build.
- **Current full player:** Not evaluated visually; the provided file named `android_full_player.png` is an empty Favorites screen.
- **Launcher identity:** The label is correct in the manifest; the icon is present as a resource but is not declared there. No launcher screenshot was provided.
- **Interaction/accessibility:** Not verified from static images. In particular, touch target bounds, focus/keyboard navigation, screen-reader labels, playback transitions, and dynamic text contrast need an on-device check.

## Final verdict

**Needs work before a public release.** The main library and desktop shell are a solid internal preview, but the current evidence does not establish that the full player is polished, the launcher uses the intended icon, or the two platforms share one visual identity. Fix the screenshot coverage and icon wiring first, then align the palettes and validate the compact controls and empty states on-device.
