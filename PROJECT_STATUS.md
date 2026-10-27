# Local Music Player — Project Status

Last updated: 2026-10-26 (fat + thin jar packaging verified; Space shortcut and UI spacing refinements committed)

## Current phase

**Phase 2 — Convx liquid-glass restyle (implemented and verified visually against Convx's source).**
Phase 1 runtime gate below is still open.

## Phase 2 — Convx liquid-glass pass (2026-10-24)

- Ported Convx's `MeshGradientBackground` algorithm from its source (`github.com/cosmictaserdev-creator/Convx`): four artwork-derived colour blobs drifting on sinusoidal anchors under a heavy blur, laid over a hue-matched near-black base — replacing the earlier static low-contrast mesh. The whole backdrop re-tunes when the playing track changes.
- `glassPanel` now carries real lift: soft directional shadow plus a specular top-edge highlight that fades down the panel.
- Sidebar and main content are two separate floating rounded glass islands (16 dp radius) with ambient mesh flowing around them; every list/row/shelf surface is translucent so the artwork glow refracts through.
- Dock rebuilt to the Convx `AppleMiniPlayer` capsule: 64 dp glass pill, 40 dp artwork, 15 sp semibold title with tight tracking, 11 sp muted artist (both marquee), heart toggle, vector transport, and the Convx signature 3 dp playback strip along the dock's bottom edge with drag-to-seek.
- Startup flags added to `Main.kt`: `desktopApp <musicDir>` boots the library straight from a folder (no JFileChooser), and `--play` also starts the first track once scanning finishes.
- Sidebar header now derives the folder name from the loaded library (`TrackView.id` is the file path) instead of picker-local state, so it no longer shows "No library loaded" after a bootstrap or restarted session.
- Verified on screen with a synthetic 8-track library in `/tmp/sv-test-library` (tagged MP3s with embedded covers, generated via ffmpeg): loaded library, playing-row accent highlight, Queue live count (7), artwork-driven magenta mesh, and the dock capsule + progress strip all render as intended.
- Compile: `./gradlew :desktopApp:compileKotlin --console=plain` — **PASS**.
- Not verified: hover-press transitions (cursor interaction still pending in the scripted harness) and the seek-drag gesture in the running app.

## Phase 2 — sidebar shell and vector icons (2026-10-24)

## Phase 2 — sidebar shell and vector icons (2026-10-24)

- Desktop shell is now a left sidebar (220 dp) plus a content column, replacing the seven nav pills that were cramped in an 860 dp window. Sidebar holds the SoundVault logo tile, `BROWSE` (Home, Artists, Albums, Queue) and `LIBRARY` (Playlists, Favorites, History) sections with live counts on Queue and Favorites, the chosen folder name, and an `Open Folder` button pinned to the bottom.
- Every emoji glyph in `Main.kt` was replaced with a `PlayerIcons` vector from `ui/Icons.kt`: sidebar and folder button, transport controls, favourite hearts, track-row add-to-playlist, and the artwork fallback note.
- Track lists moved from bordered cards to a flat table style: rows are transparent until they are the playing track, with a 32 dp index column, artwork, title/artist, duration, and the favourite toggle. Favourites rows gained artwork.
- The floating dock keeps the existing transport and seek slider, now with vector icons, a favourite toggle for the current track, a `BorderSubtle` hairline, and a slimmer seek track with a white thumb.
- Window default is 1100×800; content reserves 108 dp bottom padding for the dock.
- `Theme.kt`, `DesktopLocalPlayer`, playback, folder picking, and persistence are unchanged.
- Compile: `./gradlew :desktopApp:compileKotlin --console=plain` — **PASS**, BUILD SUCCESSFUL. Only the known `loadImageBitmap` deprecation warning.
- Not verified at runtime: no GUI pass, so sidebar spacing, icon weights, the dock's fit inside 108 dp, and the flat row hover/playing states have not been seen on screen.

## Phase 2 — Dark Glass theme and discovery shelves (2026-10-24)

- Added `desktopApp/src/main/kotlin/com/never/localmusic/desktop/Theme.kt` with the Dark Glass tokens (`GlassColors`), the `glassPanel` modifier, and the `DarkGlassTheme` wrapper around Material 3's dark colour scheme.
- Replaced the desktop shell in `Main.kt`: header with search and folder picker, navigation pills, Home / Playlists / Favorites / History views, a "Jump Back In" history shelf above the track list, and a floating now-playing dock with shuffle, previous, play/pause, next, repeat, and a seek slider.
- Existing behaviour kept intact: audio playback, folder picking through `DesktopMain.chooseFolder` with `JFileChooser` on the EDT, and all `UserDataStore`-backed state (playlists, favorites, history, history pause) still go through `DesktopLocalPlayer`.
- Adapted the supplied sketch to the real snapshot API: `nowPlaying` (not `currentTrack`), `favorites` (not `favoriteIds`), no `folder` field (the header now shows the folder chosen in this session), and `HorizontalDivider` instead of the deprecated `Divider`.
- Kept two existing playlist actions the sketch dropped, so playlists stay usable: play a playlist and delete a playlist from the Playlists view, and add a track to a playlist ("+" on each track row when playlists exist).
- The header search now filters the Home track list through `snap.filteredLibrary`; the sketch's list ignored the query.
- Compile: `./gradlew :desktopApp:compileKotlin --console=plain` — **PASS**, 7 actionable tasks (3 executed, 4 up to date), BUILD SUCCESSFUL in 11s.
- Not verified at runtime: no interactive smoke of the new layout, scrolling shelves, dock controls, or the restyled dialogs. Layout and visual polish need a GUI pass.

## Phase 2 — album cover art (2026-10-24)

- `DesktopLocalPlayer.readTrack` now reads the first embedded artwork from the tag (jaudiotagger), writes the bytes to `<storageDir>/art_cache/<hash>.jpg`, and puts the cached path in `TrackView.artPath`. The hash is `(absolutePath + lastModified)` so a retagged file gets a fresh entry; failures fall back to no art and never fail the scan.
- New `TrackArtwork` composable in `Main.kt` decodes the cached file with `loadImageBitmap` and draws it with `ContentScale.Crop`; missing, unreadable, or invalid images fall back to the music-note tile. Decoding is memoised per path with `remember(artPath)`.
- Artwork now appears on the "Jump Back In" cards (126 dp), the floating now-playing dock (44 dp), and as a 36 dp thumbnail on each library row.
- Compile: `./gradlew :desktopApp:compileKotlin --console=plain` — **PASS**, 7 actionable tasks (5 executed, 2 up to date), BUILD SUCCESSFUL in 13s. One warning: `loadImageBitmap` is deprecated in favour of Compose resources, which cannot load arbitrary runtime files; the call is intentional.
- Not verified at runtime: no track with embedded art has been scanned or seen on screen yet, and the fallback path was not exercised interactively.

## Phase 2 — artist, album and queue browsing (2026-10-24)

- Navigation now has seven pills: Home, Artists, Albums, Queue, Playlists, Favorites, History.
- `ArtistsView` groups the library by tag artist (`Unknown Artist` fallback), sorted case-insensitively, and expands an artist in place to list their tracks. Each row uses the first track's cached art as a round thumbnail.
- `AlbumsView` groups by tag album (`Unknown Album` fallback) with the same expand-in-place pattern, showing the most common track artist or "Various Artists" for compilations.
- `QueueView` shows the current track under "Now Playing" and the remaining queue from `snap.queue`, with a Clear Queue action wired to `player.clearQueue()`.
- No playback, folder picking, persistence, `Theme.kt`, or `DesktopLocalPlayer` code was changed; these views only read the existing snapshot.
- Compile: `./gradlew :desktopApp:compileKotlin --console=plain` — **PASS**, 7 actionable tasks (3 executed, 4 up to date), BUILD SUCCESSFUL in 8s. The only warning is the known `loadImageBitmap` deprecation.
- Not verified at runtime: expand/collapse, queue ordering, and the seven-pill row width at 860 dp were not exercised interactively.

## Desktop runtime follow-up (eighth pass)

- Runtime inspection found that opening the blocking `JFileChooser` directly from the Compose click handler could crash the desktop UI.
- The chooser now runs from `Dispatchers.IO` and uses `SwingUtilities.invokeAndWait` to show the dialog on the Swing EDT. A selected folder is passed back to `Dispatchers.Main` before the player starts scanning.
- Clicking the folder button opened `JFileChooser` without crashing. Selecting `/home/never/Music/lmp-spike/` scanned three tracks with metadata.
- Playback testing exposed that compressed MP3/OGG streams were passed directly to `Clip.open`, which failed. The desktop player now converts those streams to signed 16-bit PCM and closes the clip on open failure.
- After rebuilding, the UI played the MP3 and OGG tracks; the transport paused and resumed the MP3. History displayed both entries and retained them after a full app restart. The rebuilt app remains open on the History tab.
- FLAC remains unsupported by the packaged Java Sound readers; a direct open returned `UnsupportedAudioFileException`. The scanner still lists that file, and failed playback is not shown as a user-facing error.
- `adb devices` showed no attached device. The configured Android AVD had failed to connect during the prior boot attempt.

## Desktop audio format follow-up (ninth pass)

- The desktop scanner now accepts only `mp3`, `ogg`, `wav`, `aiff`, `aif`, and `au`. It no longer lists `flac`, `m4a`, `wma`, or `aac`, which the packaged app cannot decode.
- The distributable includes MP3SPI/JLayer and VorbisSPI/JOrbis. A decoder probe used those exact packaged app JARs on the classpath: MP3, OGG, WAV, AIFF, AIF, and AU all decoded to PCM; FLAC, M4A, WMA, and AAC returned `UnsupportedAudioFileException`.
- Probe details: a temporary Java program called `AudioSystem.getAudioInputStream` and converted the streams to signed 16-bit PCM with Temurin 21.0.12 and the distributable's `lib/app/*` classpath. It used generated WAV, AIF, AU, MP3, FLAC, M4A, WMA, and AAC samples plus `/home/never/Music/lmp-spike/spike.mp3`, `spike.ogg`, and `spike.flac`.
- The desktop UI now shows a playback error for decoder/open failures and for files that have disappeared or become unreadable since scanning. MP3/OGG PCM conversion and stream/clip cleanup remain in place.
- Build after the error-path fix: `./gradlew :shared:compileKotlinDesktop :desktopApp:compileKotlin :desktopApp:createDistributable --console=plain` — **PASS**, 14 tasks, 5 executed, 9 up to date.
- Interactive smoke on the rebuilt app was **not verified**. Orca reported `runtime_unavailable` for GUI capabilities and app listing, including after `orca-ide open --json`; therefore the current filtered library, error banner, and playback were not visually checked. The prior eighth-pass MP3/OGG playback observation predates the format filter.
- Launch in the default sandbox failed with `AWTError: Can't connect to X11 window server using ':0'`. With escalated display access, `timeout --signal=TERM --kill-after=2s 6s desktopApp/build/compose/binaries/main/app/local-music-player/bin/local-music-player` remained alive for six seconds and then exited 124 from the timeout with no error output. This confirms startup only, not the GUI smoke.

## Desktop post-filter interactive smoke (2026-10-23)

- Attempted to open the built distributable at `desktopApp/build/compose/binaries/main/app/local-music-player/bin/local-music-player`. The process stayed alive for at least 10 seconds with no terminal output, then I stopped the process; this confirms process startup only.
- Orca computer-use capabilities were available, but `list-apps` returned an empty app list both before and after launch. Orca reported accessibility and screenshot permissions as unsupported. The alternate connected desktop surface also returned `apps: []` and `browsers: []`; its `listWindows()` method was unavailable.
- Blocked: I could not observe the app window, select `/home/never/Music/lmp-spike/`, confirm MP3/OGG appear and FLAC is absent, test MP3/OGG play and pause/resume, or trigger the missing/unreadable-file error. No UI behavior is claimed for this pass.
- No code defect was reproduced, and no code was changed. The Phase 1 gate remains open; desktop post-filter runtime behavior and Android behavior remain unverified.

---

## Summary of all Phase 1 fixes (cumulative)

### DesktopLocalPlayer (`shared/src/desktopMain/kotlin/…/desktop/DesktopLocalPlayer.kt`)

| # | Issue | Fix | Verified |
| --- | --- | --- | --- |
| 1 | `advanceQueue` re-seeded library on every empty queue | Replaced with `_playOrder` list + `_cursor` index | Compile |
| 2 | Position polling absent | 500 ms coroutine reads `clip.microsecondPosition` | Compile |
| 3 | Scan depth-only, no time/item limit, no error surface | `deadline` + item counter; early return sets `scanError` | Compile |
| 4 | Metadata: filenames only | `AudioFileIO.read()` (jaudiotagger); filename fallback on error | Compile |
| 5 | Playlists / favorites / history absent | JSON persistence via `UserDataCodec`; loaded at startup | Compile |
| 6 | Search absent | `search(query)` filters library case-insensitively | Compile |
| 7 | `_library`/`_playOrder` mutated on `Dispatchers.Default` during scan while UI reads them | `scanDirectory` accumulates to local list; `indexFolder` commits inside `withContext(Dispatchers.Main)` | Compile |
| 8 | Stale scan could overwrite a newer folder selection | `scanGeneration` counter: incremented in `updateSelectedFolder`, checked before commit; stale scans discard | Compile |
| 9 | `isPaused` and `currentClip` plain `var`, read/written from multiple threads | `@Volatile` on both fields | Compile |
| 10 | `togglePlayPause` set `isPaused=true` after `clip.stop()` | `isPaused = true` now set before `clip.stop()` | Compile |
| 11 | STOP listener had no clip identity check | `if (clip !== currentClip) return@addLineListener` at top of STOP handler | Compile |
| 12 | `stopAndRelease()` nulled `currentClip` after `stop()` — STOP event could fire while identity check still passed | `currentClip = null` set before `clip.stop()` in `stopAndRelease()` | Compile |
| 13 | `Dispatchers.Main` in `shared/desktopMain` had no explicit provider on the module's own classpath | `kotlinx-coroutines-swing:1.10.2` added as explicit `implementation` dep in `shared/desktopMain` | Gradle dependency report |
| 14 | STOP listener called `handleTrackEnd()` directly on the JavaSound EventDispatcher thread; `handleTrackEnd()` reads and mutates `_playOrder`, `_cursor`, `_history`, `positionPollJob` — all EDT-owned state | STOP listener pre-filters with `@Volatile`/clip-local checks only, then `scope.launch(Dispatchers.Main)`. On the EDT, re-checks `clip === currentClip && !isPaused` before calling `handleTrackEnd()` | Compile |
| 15 | Compressed MP3/OGG streams were passed to `Clip.open()` without decoding to PCM, so playback failed | Convert streams to signed 16-bit PCM before opening the clip; close the clip if setup fails | Desktop runtime smoke: MP3 and OGG |
| 16 | Scanner advertised formats without packaged decoders | Limit desktop scan to MP3, OGG, WAV, AIFF/AIF, and AU; omit FLAC, M4A, WMA, AAC | Decoder probe using the packaged app JARs |
| 17 | Playback failures, missing files, and unreadable files were silent | Add `playbackError` to the snapshot and show a clear desktop message | Build passed; interactive error display not verified |

### AndroidLocalPlayer (`androidApp/src/main/kotlin/…/android/AndroidLocalPlayer.kt`)

| # | Issue | Fix | Verified |
| --- | --- | --- | --- |
| 1 | `playNext`/`playPrevious` did not advance state | `_playOrder` + `_cursor` approach | Compile |
| 2 | `clearQueue` restarted the current track | Drops entries after `_cursor` without touching the controller | Compile |
| 3 | `skipForward` clamped to 0 when `durationMs` is 0 | Returns current position unchanged if `durationMs <= 0` | Compile |
| 4 | Position polling absent | 500 ms coroutine reads `controller.currentPosition` | Compile |
| 5 | Track-end advance absent | `onPlaybackStateChanged(STATE_ENDED)` calls `handleTrackEnd()` | Compile |
| 6 | Playlists / favorites / history / search absent | JSON persistence; stored in app-private files directory | Compile |
| 7 | `addToLibrary` posted redundant coroutine onto `Dispatchers.Main` | Removed wrapper; runs synchronously on Main | Confirmed by independent review |
| 8 | MediaController callback ran on wrong thread at startup | Executor set to `runOnUiThread` in `MainActivity.onStart` | Confirmed correct |

### MainActivity (`androidApp/src/main/kotlin/…/MainActivity.kt`)

| # | Issue | Fix | Verified |
| --- | --- | --- | --- |
| 1 | `MediaMetadataRetriever.release()` only on success path | Wrapped in `try { … } finally { mmr.release() }` | Confirmed by independent review |

### Build config (`gradle/libs.versions.toml`, `shared/build.gradle.kts`)

| # | Issue | Fix | Verified |
| --- | --- | --- | --- |
| 1 | `kotlinx-coroutines-swing` absent from `shared/desktopMain` own classpath | `coroutinesSwing = "1.10.2"`, `coroutines-swing` library entry, `implementation(libs.coroutines.swing)` in `desktopMain` | `desktopMainRuntimeClasspath` report; re-confirmed this session |

---

## Checks run (this session — eighth pass)

| Check | Command | Result |
| --- | --- | --- |
| `desktopMainRuntimeClasspath` includes swing provider | `./gradlew :shared:dependencies --configuration desktopMainRuntimeClasspath` | **PASS** — `kotlinx-coroutines-swing:1.10.2` present as direct entry |
| Shared + Android + Desktop compile + APK | `./gradlew :shared:compileKotlinDesktop :androidApp:compileDebugKotlin :desktopApp:compileKotlin :androidApp:assembleDebug --console=plain` | **PASS** (52 tasks; 2 executed, 50 up to date, BUILD SUCCESSFUL in 10s) |
| Desktop compile + distribution | `./gradlew :shared:compileKotlinDesktop :desktopApp:compileKotlin :desktopApp:createDistributable --console=plain` | **PASS** (14 tasks; 6 executed, 8 up to date; BUILD SUCCESSFUL in 22s) |
| Desktop runtime smoke | Choose `/home/never/Music/lmp-spike/`, scan, play MP3/OGG, pause/resume, restart | **PASS** — three tracks scanned; MP3/OGG showed playing and PipeWire exposed an audio stream; MP3 pause/resume worked; History survived restart. Audio was not independently listened to. |
| Desktop FLAC playback | Try bundled `spike.flac` through packaged Java Sound readers | **BLOCKED** — no FLAC reader; direct open returned `UnsupportedAudioFileException`; UI has no playback error message |
| Android emulator / device | `~/Android/Sdk/platform-tools/adb devices` | **BLOCKED** — no device connected; AVD boot did not establish adb connection |

---

## Thread-ownership for `DesktopLocalPlayer` (verified by code review, this session)

### EDT-owned mutable state
`_library`, `_playOrder`, `_cursor`, `_playlists`, `_favorites`, `_history`, `positionPollJob`.

Every access to these is on the Swing EDT:

| Call site | Thread | Note |
| --- | --- | --- |
| `loadUserData()` | Construction thread → Swing EDT | `DesktopLocalPlayer` is created in a Compose `remember` block |
| `updateSelectedFolder` | Swing EDT | Compose button callback |
| `indexFolder` commit (`withContext(Main)`) | Swing EDT | Dispatched from Default; generation guard re-checked |
| `addToLibrary` | Swing EDT | Compose callback |
| `playSelected`, `playNext`, `playPrevious` | Swing EDT | Compose callbacks |
| `toggleShuffle`, `clearQueue`, `search` | Swing EDT | Compose callbacks |
| `playPlaylist`, `createPlaylist`, `deletePlaylist`, etc. | Swing EDT | Compose callbacks |
| `startClipFor` | Swing EDT | Called from transport methods above |
| `handleTrackEnd` | Swing EDT | Dispatched via `scope.launch(Dispatchers.Main)` from STOP listener |
| `stopPositionPoll` (inside `handleTrackEnd`, `stopAndRelease`) | Swing EDT | `positionPollJob` written only on EDT |
| `recordHistoryPlay` | Swing EDT | Called from `startClipFor` on EDT |
| `saveUserData` snapshot | Swing EDT | Snapshots before IO dispatch; IO coroutine only sees immutable data |
| `dispose` | Swing EDT | Window close handler |

### `@Volatile` cross-thread fields
| Field | Written by | Read by |
| --- | --- | --- |
| `currentClip` | Swing EDT | JavaSound EventDispatcher (STOP/CLOSE checks); position poll coroutine (`currentClip ?: break`) |
| `isPaused` | Swing EDT | JavaSound EventDispatcher (STOP pre-filter) |
| `scanGeneration` | Swing EDT | `indexFolder` coroutine (Default then Main) |

### Other cross-thread accesses
| Access | Thread | Safety |
| --- | --- | --- |
| `_flow.value` (read in STOP listener) | JavaSound EventDispatcher | `StateFlow` is thread-safe |
| `_flow.update` (in position poll) | Default dispatcher | `MutableStateFlow.update` uses a lock-free CAS loop — thread-safe |
| `clip.microsecondPosition`, `clip.isRunning` | Position poll (Default); STOP listener (EventDispatcher) | Reads on the captured `Clip` instance; JavaSound Clip API is thread-safe for these reads |

### STOP event path (step by step)
1. JavaSound EventDispatcher fires STOP on the captured `clip`.
2. `if (clip !== currentClip)` — `@Volatile` read; fails if clip was replaced or `stopAndRelease()` ran → **early return**.
3. `_flow.value.durationMs` — thread-safe StateFlow read.
4. `clip.microsecondPosition` — clip-local read.
5. `if (isPaused || dur <= 0 || pos < dur - 200L)` — `@Volatile` read; returns if paused, duration unknown, or position not near end → **early return**.
6. `scope.launch(Dispatchers.Main)` — posts to Swing EDT.
7. **On the EDT:** `if (clip !== currentClip || isPaused) return@launch` — re-checks in case state changed during dispatch.
8. `handleTrackEnd()` — reads/writes `_playOrder`, `_cursor`, `_history` safely on EDT.

---

## Remaining — unverified runtime behavior

1. **Android folder scan + metadata.** `MediaMetadataRetriever` on `Dispatchers.IO`; not tested with real codec containers.
2. **Desktop GUI after the ninth-pass format filter.** Packaged decoder probing passed for the enabled formats, but this pass could not verify the rebuilt library view, playback, or error banner because Orca GUI access was unavailable. The eighth-pass MP3/OGG GUI smoke predates the filter.
3. **Android position poll.** `MediaController.currentPosition` accuracy not tested on a real device.
4. **Desktop `Clip.microsecondPosition` seek.** Correct per spec; not re-run with real audio files.
5. **Scan bounds.** Logic verified by code review; not tested with >5 000 file folders.
6. **Playlists / favorites / history persistence round-trip.** JSON persistence implemented; saving and restoring through a real user flow not verified.
7. **Android background playback lifecycle.** `PlaybackService` unchanged from Phase 0 (confirmed working then); new `handleTrackEnd` queue path not tested across background transitions.
8. **Windows target.** No Windows host available.
9. **`.deb` packaging.** Blocked on `dpkg-deb`/`fakeroot` not present on Arch.

---

## Phase 1 gate status

Gate: "a user can choose a folder, find and play a local track offline, create a playlist, leave and return to playback, and confirm history controls."

| Gate item | Status |
| --- | --- |
| Choose a folder | Desktop chooser and scan verified with three tracks in the eighth pass; the ninth-pass format filter is not visually verified; Android runtime not verified |
| Find and play a local track offline | Desktop MP3 and OGG playback verified in the eighth pass; the rebuilt app was not smoke-tested after the ninth-pass changes; Android runtime not verified |
| Create a playlist | Implemented with JSON persistence — user flow not runtime-verified |
| Leave and return to playback (background) | Android service unchanged from Phase 0 (verified then); new queue/cursor path not re-verified |
| History controls (pause/clear) | History display and persistence verified; history pause/clear buttons not tested |
| Persistence across restarts | Desktop history survived restart; playlist/favorites and Android round-trips not verified |

The gate remains open until an interactive smoke pass verifies folder selection, playback, persistence across restart, and Android background playback.

---

## Phase 3 — jar packaging and desktop distribution (2026-10-26)

- Added two Gradle jar tasks to `desktopApp/build.gradle.kts`:
  - `runnableJar`: fat, self-contained jar (40 MB) that bundles all runtime dependencies; runs with `java -jar desktopApp/build/libs/desktopApp-runnable.jar`.
  - `thinJar`: classes-only jar (252 KB) with a `Class-Path` manifest attribute pointing at `lib/*.jar`; runs with an explicit classpath or with `java -jar` when `lib/` is alongside the jar.
- Added `copyDependencies` task to populate a `lib/` directory from `runtimeClasspath`; 58 dependency jars are copied.
- Verified both jars launch the app and render the full Nocturne UI from the fat jar screenshot.
- Added `lib/` and `META-INF/` to `.gitignore` so regenerated distribution artifacts are not committed.
- Committed as `ca79949 feat(desktop): add keyboard shortcuts and fine-tune UI spacing`.

## Desktop UI refinements (2026-10-26)

- Added a global Space = play/pause shortcut on the window; the shortcut yields while the search field owns keyboard focus, mirroring Spotify/Convx.
- Added a quiet keyboard focus ring on sidebar nav items (1.5 dp accent border, instant, never animated).
- Tightened the Home library list spacing so rows read as one list rather than separated cards.
- Removed the fixed height on the search `OutlinedTextField` so the placeholder no longer clips.
- Swapped ASCII `...` and straight quotes for typographic equivalents (`…`, `‘`, `’`, `“`, `”`) in user-facing strings.
- Deleted the unused experimental `SpikeScreen.kt`.
- Verified on screen from the fat jar: app launches, shows the "No library loaded" state, and renders all navigation sections and the bottom player bar.

## Thin-jar distributable (2026-10-27)

- Rewrote `assembleDistributive` as a plain `Copy` into `build/dist` (the old Sync/into wiring produced an empty dist, so `packageDistributive` stayed `NO-SOURCE`).
- `packageDistributive` now builds `desktopApp/nocturne-thin-dist.zip`, which unzips to `nocturne/desktopApp-thin.jar` + `nocturne/lib/` (58 dependency jars).
- Fixed the `Class-Path` manifest write (back to `doFirst`; `doLast` runs after the jar is already written) and made `runnableJar`/`thinJar` depend on `compileKotlin` via the source set output instead of a fragile `inputs.dir` check.
- Verified: zip unpacks and `java -jar nocturne/desktopApp-thin.jar` launches cleanly (launch confirmed via timeout kill, no errors in the log). Requires Java 21.

## Desktop playback smoke (2026-10-27)

Driven both visually (app launched in Hyprland, screenshot-verified) and through a JShell harness on `DesktopLocalPlayer`:

- **Scan filtering**: a folder with MP3, OGG, WAV, FLAC, and a junk-header `.mp3` lists exactly 4 tracks — the `.flac` is skipped (no packaged decoder), `scanError` null.
- **Playback**: MP3 (including the junk-header one), OGG, and WAV all play with the position polling (pos ≈2 s at the 2.5 s probe); live audio confirmed via an active `PipeWire ALSA [java]` sink-input while the app ran. Duration is reported for WAV (10 s); MP3 and OGG report `dur=0` (unknown via the SPI).
- **Error path**: a text file named `.wav` (an accepted extension) fails cleanly — `isPlaying=false`, position 0, and `playbackError: "Cannot play 'fake-audio.wav': unsupported format, damaged file, or unavailable audio output"`.

## Next step

1. Re-run the Android smoke pass when an emulator or device is responsive: folder selection, playback, persistence across restart, and background playback.
4. Test playlist creation and the History pause/clear controls in an interactive flow.

## Desktop runtime recheck (2026-10-23)

- Rebuilt with `./gradlew :shared:compileKotlinDesktop :desktopApp:compileKotlin :desktopApp:createDistributable --console=plain` — **PASS**, 14 tasks (2 executed, 12 up to date).
- Launched the rebuilt distributable with a 12-second timeout — process remained alive until the timeout sent TERM; no error output. This checks startup only.
- `/home/never/Music/lmp-spike/` contains `spike.mp3`, `spike.ogg`, and `spike.flac`.
- Interactive smoke remains **unverified**: `cua.getState()` returned no apps or browsers, so this session could not inspect the library, select the folder, play tracks, or view the error message.
- Android smoke remains **unavailable**: `adb` and `emulator` are not installed in this environment.
- No code defect was observed. The Phase 1 runtime gate remains open.

## Desktop post-filter interactive smoke (2026-10-23, dispatched pass)

- Re-launched the built distributable `/home/never/Work/projects/local-music-player/desktopApp/build/compose/binaries/main/app/local-music-player/bin/local-music-player`; process stayed alive for 8 seconds with no output, then terminated by timeout (exit 124). Startup only.
- GUI inspection was not possible: Kilo's `orca-ide` command failed to start the runtime ("Unable to start the Orca CLI through Electron node mode"), and the session's Orca `computer list-apps --json` returned an empty app list. No app window, library list, chooser, playback state, or error banner was observed.
- `/home/never/Music/lmp-spike/` contains `spike.mp3`, `spike.ogg`, `spike.flac` (3 files).
- Result: blocked. Could not verify MP3/OGG appear and FLAC absent, could not play/pause/resume MP3 and OGG, and could not trigger a missing/unreadable-file error. No UI behavior is claimed.
- No code defect was reproduced; no code changed. Phase 1 runtime gate remains open for desktop post-filter behavior and all Android behavior.
