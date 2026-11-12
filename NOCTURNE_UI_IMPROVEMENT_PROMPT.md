# Nocturne - UI Improvement Prompt (Android + Desktop)

## Mission
Transform Nocturne into a sleek, iOS-style "Liquid Glass" music player inspired by Convx, while keeping it fast, clean, and functional. Improve both Android and Desktop UIs to match that premium aesthetic.

## Context
- **Repo**: `/home/never/Work/projects/nocturne`
- **Current Branch**: `android-ui-polish` (Android already partially polished)
- **Inspo**: https://github.com/cosmictaserdev-creator/Convx.git
- **Goal**: Study Convx patterns and apply them tastefully to Nocturne

## Required Skills
- `work-engineering-standard` - systematic approach
- `impeccable` or `emil-design-eng` - design polish
- `frontend-design` - UI aesthetics

## Phase 1: Study Convx First
Clone and study:
```bash
cd /tmp
git clone https://github.com/cosmictaserdev-creator/Convx.git
```

Focus on:
- `hig/` - glass primitives, blur modifiers
- `canvas/` - AGSL shaders, animated backgrounds
- `app/src/main/java/.../ui/player/` - fullscreen player UX
- `app/src/main/java/.../ui/theme/` - dynamic colors
- Look for: `haze`, `RenderEffect`, `RuntimeShader`, `AGSL`, `LookaheadLayout`, `spring`, `glass`

## Phase 2: Android Improvements

### Step 1: Add Dependencies
Update `androidApp/build.gradle.kts`:
```kotlin
implementation("dev.chrisbanes.haze:haze:1.6.10")
implementation("dev.chrisbanes.haze:haze-materials:1.6.10")
implementation("io.github.alexzhirkevich:compose-cupertino:0.1.0-alpha04") // or latest
```

### Step 2: Haze Blur System
- Create `HazeState()` at MainScreen level
- Apply `Modifier.hazeSource()` to background/content (library lists)
- Apply `Modifier.hazeChild(style = HazeMaterials.thin())` to NowPlayingStrip, cards, sheets
- Keep performance in mind

### Step 3: iOS-Style Controls
- Replace plain buttons with Cupertino components where appropriate
- Polish slider with smoother feel
- Better play/pause button (circular, elevated)
- Cleaner skip/shuffle/repeat

### Step 4: Liquid Background (Subtle)
- Add AGSL shader for animated gradient/blobs behind content
- Drive colors from album art using Palette API
- Keep it subtle, don't overdo
- Put behind everything but ensure readability

### Step 5: Motion Polish
- Use `spring()` animations over `tween()`
- Add subtle transitions
- Smooth card interactions

## Phase 3: Desktop Improvements

Look at `desktopApp/src/main/kotlin/com/never/localmusic/desktop/Main.kt`

Improvements:
- **Glassmorphism**: Add frosted glass effects to sidebars, player bar, cards
- **Better spacing/typography**: More refined, iOS-like
- **Smoother animations**: Use `animateContentSize`, `AnimatedVisibility` with springs
- **Player bar polish**: Make it more prominent, better controls
- **Cards**: Add subtle shadows/borders, rounded corners
- **Search bar**: Cleaner design
- **Queue/Playlists**: Better visual hierarchy

Use Compose Desktop's built-in effects. Can use `Modifier.blur()` on supported platforms or custom drawing.

## Phase 4: Testing & Polish

- [ ] Android builds: `./gradlew :androidApp:assembleDebug`
- [ ] Desktop builds: `./gradlew :desktopApp:compileKotlin`
- [ ] Both UIs look polished and consistent
- [ ] No performance issues
- [ ] All existing functionality works
- [ ] No crashes

## Rules
- **Study first, copy patterns not code** - Understand the layering
- **Subtle over flashy** - Convx is restrained and elegant
- **Performance matters** - Don't kill framerate
- **Keep it functional** - Don't break existing features
- **Tasteful blur** - Use sparingly where it makes sense
- **Consistent design** - Unify Android and Desktop aesthetics

## Execution Order
1. Clone Convx and study hig/ and ui/player/
2. Add dependencies to Android
3. Implement haze blur on Android
4. Polish Android controls
5. Improve Desktop UI
6. Test both
7. Commit changes

Start by cloning Convx and analyzing it, then implement improvements incrementally.
