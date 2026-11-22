# Nocturne UI Audit Prompt

## Objective
Audit the Nocturne music player UI (Android + Desktop) for design quality, polish, and usability. Be brutally honest - point out flaws, suggest fixes.

## Project
`/home/never/Work/projects/nocturne`

## Context
- Name: Nocturne
- Platforms: Android (Jetpack Compose) + Desktop (Compose Desktop)
- Design: iOS-style Liquid Glass inspired by Convx
- Recent changes: Floating nav, mini-player, full player sheet, glass effects, toned mesh background

## Screenshots to Review
- `screenshots/android_current_1.png` - Android main screen
- `screenshots/android_full_player.png` - Android full player
- `screenshots/desktop_liquid_glass_new.png` - Desktop app
- `screenshots/archive/` - Previous iterations for comparison

## Audit Checklist

### Visual Design
- [ ] Overall aesthetic - Does it look premium/professional?
- [ ] Consistency - Android and Desktop feel like same app?
- [ ] Color palette - Cohesive, readable?
- [ ] Typography - Hierarchy clear?
- [ ] Spacing/padding - Balanced?
- [ ] Glass effects - Tasteful or overdone?
- [ ] Icons - Crisp and appropriate?

### Android Specific
- [ ] App name/icon in launcher correct (Nocturne)?
- [ ] Bottom nav - Clean, not cramped?
- [ ] Mini-player - Good positioning/size?
- [ ] Full player sheet - Feels polished?
- [ ] Track cards - Readable, tappable?
- [ ] Search bar - Clean?
- [ ] Folder button - Properly rendered?

### Desktop Specific
- [ ] Layout balance - Good use of space?
- [ ] Sidebar - Clean navigation?
- [ ] Player bar - Prominent and functional?
- [ ] Track list - Easy to scan?
- [ ] Mesh background - Too strong or toned well?
- [ ] Accent colors - Used as accents or overwhelming?

### UX/Usability
- [ ] Information hierarchy clear?
- [ ] Touch targets adequate (Android)?
- [ ] Readable in all states?
- [ ] No visual bugs (text overflow, clipping)?
- [ ] Feels snappy/intuitive?

### Issues to Find
- Text rendering problems
- Color clashes
- Spacing issues
- Misaligned elements
- Overuse of effects
- Anything that looks "AI slop"

## Deliverables
Create `UI_AUDIT_REPORT.md` with:
1. Overall score (1-10)
2. Strengths
3. Weaknesses with specifics
4. Concrete fixes for each issue
5. Comparison to reference (Convx)
6. Final verdict: Ready to ship or needs work?

Be brutally honest. Point out specific issues with file/line suggestions if possible.
