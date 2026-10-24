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

## Status

The desktop local player (scan, playback, playlists, favorites, history, search) is implemented and visually verified. Android compiles; on-device behavior and the customization phase (theme presets, accent color, glass intensity, settings import/export) are in progress.

See [PROJECT_STATUS.md](PROJECT_STATUS.md) for the detailed phase gates and verification history, and [docs/](docs/) for the Phase 0 playback-engine comparison.

## Privacy

Nocturne makes no network requests for local playback. It scans only the folders you choose, stores your library index and settings under your platform's data directory, and keeps no telemetry or cloud profile.
