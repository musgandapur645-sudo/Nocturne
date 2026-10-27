# Nocturne

A free, local-first music player for Android, Linux, and Windows. Your files, on your device — no account, no subscription, no ads, no server. Built with Kotlin and Compose Multiplatform.

## Highlights

- **Local library** — pick a folder, and Nocturne indexes it in place. Nothing is moved or uploaded. MP3, OGG, WAV, AIFF/AIF, AU.
- **Artwork-driven look** — a dark glass interface whose ambient mesh background re-tints itself to the colors of whatever is playing.
- **Everything local** — playlists, favorites, listening history, and search all work fully offline.
- **Playback** — queue, shuffle, repeat, seek, favorites, and a draggable progress strip on the player dock.
- **Browsing** — by artist, album, queue, playlists, favorites, and history.

## Build

Requires JDK 21+.

```bash
# Desktop (Linux/Windows)
./gradlew :desktopApp:run

# Boot straight into a folder (skips the file dialog), optionally starting playback
./gradlew :desktopApp:run --args="/path/to/music --play"

# Android debug APK
./gradlew :androidApp:assembleDebug
```

## Distribution jars

Two runnable jars are produced from the desktop module:

```bash
# Build both jars
./gradlew :desktopApp:runnableJar :desktopApp:thinJar
```

| Jar | Size | Contents | Run |
| --- | --- | --- | --- |
| `desktopApp-runnable.jar` | ~40 MB | Classes + all runtime dependencies (self-contained) | `java -jar desktopApp/build/libs/desktopApp-runnable.jar` |
| `desktopApp-thin.jar` | ~250 KB | Classes only; needs the dependency jars on the classpath | See below |

### Running the thin jar

The thin jar is useful when you want to redistribute only the application code and keep the dependency jars separate (for example, to avoid re-packaging unchanged dependencies on every build).

```bash
# Run with an explicit classpath
java -cp "lib/*:desktopApp/build/libs/desktopApp-thin.jar" com.never.localmusic.desktop.MainKt

# Or package lib/ alongside the jar and use -jar
java -jar desktopApp/build/libs/desktopApp-thin.jar
```

The `Class-Path` manifest attribute in `desktopApp-thin.jar` is pre-populated from the current `runtimeClasspath`, so `java -jar` works when the dependency jars are in a `lib/` directory next to the jar.

A ready-to-run zip can be built in one step:

```bash
./gradlew :desktopApp:packageDistributive

desktopApp/nocturne-thin-dist.zip
# unzips to a nocturne/ folder: nocturne/desktopApp-thin.jar + nocturne/lib/*.jar

cd nocturne && java -jar desktopApp-thin.jar
```

## Status

The desktop local player (scan, playback, playlists, favorites, history, search) is implemented and visually verified. Android compiles; on-device behavior and the customization phase (theme presets, accent color, glass intensity, settings import/export) are in progress.

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for the detailed phase gates and verification history, and [docs/](docs/) for the Phase 0 playback-engine comparison.

## Privacy

Nocturne makes no network requests for local playback. It scans only the folders you choose, stores your library index and settings under your platform's data directory, and keeps no telemetry or cloud profile.
