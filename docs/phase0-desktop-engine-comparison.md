# Phase 0 — Desktop audio engine comparison

Evidence gathered on this machine: Omarchy Linux, PipeWire (PulseAudio server), ffmpeg available.
Sample files generated locally with ffmpeg (30 s MP3 with real ID3 tags, 10 s OGG, 10 s FLAC); nothing committed from other projects.

## What was measured

Autotest sequence (`--play=<file> --autotest`): open → autoplay 3 s → seek to 20 s → pause → resume.

| Aspect | A. javax.sound + mp3spi/vorbisspi (pure JVM) | B. JavaFX Media | C. vlcj (libvlc) | D. Rust (Symphonia+CPAL) via JNI | E. GStreamer (gst1-java-core) |
| --- | --- | --- | --- | --- | --- |
| Local proof on this machine | ✅ MP3 play/seek/pause/resume, OGG, tags, system audio via PipeWire | not tested | not tested | ❌ no Rust toolchain here | not tested (not installed) |
| Streaming formats baseline: MP3/OGG/WAV | MP3✅ OGG✅ WAV✅ | MP3✅ WAV✅ (no OGG) | all | most (MP3/FLAC/OGG) | all |
| M4A/AAC | ❌ | ✅ | ✅ | partial | ✅ |
| FLAC | ❌ (needs extra SPI or re-encode) | ❌ | ✅ | ✅ | ✅ |
| Gapless between files | no (per-clip) | no | yes-ish | manual | yes |
| Packaging weight | tiny (few MB jars) | JavaFX runtime per OS | libvlc binaries per OS (~80–100 MB) or user-installed VLC | Rust toolchain + JNI glue, runtime small | GStreamer runtime (~150 MB) |
| Java/Sw/Kotlin integration effort | trivial (done in spike) | moderate (JavaFX+Compose co-exist) | moderate | high (FFI, two languages) | moderate-high |
| Windows support | same API on Windows JVM (unverified here) | official | proven widely | doable | doable |
| Media/session controls | app-level (Compose) | app-level | app-level | manual | app-level |
| Maintenance health | mp3spi/vorbisspi old but stable; jaudiotagger active (LGPL) | strong (JetBrains/OpenJDK) | strong (Caprica, libvlc 3) | strong but adds 2nd language | strong |

## Desktop engine decision for Phase 1 start

**Option A (pure JVM)** — it is the smallest complete change that already plays MP3/OGG/WAV with real tags on this machine, and it keeps packaging near-zero. This matches the plan's "do not choose Rust just because it sounds faster."

**Documented triggers to revisit:**

1. If Phase 1 shows real users need **FLAC** on desktop (common for local libraries) → add a FLAC SPI, or move to **vlcj**, which covers every format including M4A/FLAC and gapless in one move.
2. If **gapless** between albums is required → vlcj or GStreamer.
3. If Windows javax.sound shows latency/CPU problems → re-measure; JavaFX Media is the fallback for MP3/AAC-class files.

Android is not affected: Media3 is the Android engine per plan, already proven on the emulator.

## Licenses of Phase 0 dependencies (from their POMs / upstream pages)

- Android Media3 (ExoPlayer, Session) — Apache-2.0 ✅
- androidx.activity-compose — Apache-2.0 ✅
- Kotlin, Compose Multiplatform, JDK (Temurin) — Apache-2.0 / GPLv2+CE ✅
- jaudiotagger 3.0.1 — LGPL ✅
- mp3spi / vorbisspi (soundlibs repack of JavaZoom) — LGPL ✅
- Compose Desktop packaging uses the user's JVM via jpackage. The project stays Apache-2.0 compatible.
- No GPL-only code (e.g. Convx) was copied or linked; GPL-3.0 reference projects were used for product-pattern study only.
