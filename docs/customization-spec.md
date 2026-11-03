# Nocturne customization — design spec

Status: implemented (phase 4). Applies to desktop and Android.

The goal is a small, boring customization surface: pick a look, pick an accent,
set how frosted the glass is, and move that setup between machines with one
file. Nothing here changes audio behaviour.

## 1. Theme presets

A preset is a named bundle of exactly two colours: the accent and its glow.
Every other surface colour stays the same in this phase — the app is dark-only.

| Slug | Name | Accent | Glow (accent at 25% alpha) |
| --- | --- | --- | --- |
| `crimson` | Crimson | `#E11D48` | `#40E11D48` |
| `ocean` | Ocean | `#0EA5E9` | `#400EA5E9` |
| `forest` | Forest | `#10B981` | `#4010B981` |
| `amber` | Amber | `#F59E0B` | `#40F59E0B` |
| `violet` | Violet | `#8B5CF6` | `#408B5CF6` |

`crimson` is the default and matches the shipped look, so an upgrade with no
settings file looks exactly like before.

Unknown preset slugs (for example a preset added by a newer build) fall back
to `crimson` instead of failing the load.

## 2. Accent colour tokens

Each preset resolves to four tokens. The same four are derived for a custom
accent, so the UI never has to special-case "preset vs custom".

| Token | Meaning | Rule |
| --- | --- | --- |
| `accent` | Primary fill: play button, active row, slider, focus ring | preset value or the user's `#RRGGBB` |
| `accentHover` | Hover/pressed fill | accent lightened 12% toward white |
| `accentGlow` | Soft halo behind the accent | accent at 25% alpha |
| `onAccent` | Text/icons drawn on the accent | always `#FFFFFF` |

Custom accents are accepted as `#RRGGBB` (case-insensitive). `#RGB` shorthand,
names, and alpha suffixes are rejected so the stored value is always one
canonical form.

## 3. Glass intensity (0–100)

One integer, `glassIntensity`, controls how transparent the floating panels
are. It produces two multipliers that scale the alpha of surfaces the UI
already draws:

```
t          = intensity / 100
fillFactor = 1.75 - 1.25 * t   (panel fill; 1.75 at 0, 1.00 at 60, 0.50 at 100)
edgeFactor = 2.00 - fillFactor (highlight + hairline; 0.25 at 0, 1.00 at 60, 1.50 at 100)
```

Anchored at 1.0 for the default of 60, so an untouched install renders exactly
as it did before this phase. Factors are clamped into `0..1` after multiplying
the source alpha.

The slider shows a tier name for orientation; the tiers are labels only and do
not snap the value:

| Range | Tier | Feel |
| --- | --- | --- |
| 0–24 | Solid | Panels read as opaque cards |
| 25–49 | Frosted | Light shows through the edges |
| 50–74 | Crystal (default 60) | Current shipped look |
| 75–100 | Vapor | Artwork glow dominates the panels |

Interpretation per platform:

- **Desktop** — the two factors scale `glassPanel`'s fill, top highlight and
  border. At 0 the app looks like flat dark cards; at 100 the mesh backdrop is
  strongest.
- **Android** — the same factors are applied to the surface and card colours
  drawn over the accent-tinted backdrop. Android has no blur, so intensity
  changes translucency only.

Out-of-range values are clamped to 0–100. Missing or non-numeric values fall
back to 60.

## 4. Settings file

One file per install, `settings.json`, stored beside `user_data.json`
(`~/.local/share/local-music-player/settings.json` on Linux, the app-private
files directory on Android).

```json
{
  "schemaVersion": 1,
  "preset": "crimson",
  "accent": null,
  "glassIntensity": 60
}
```

### Field rules

| Field | Type | Required | Default | Notes |
| --- | --- | --- | --- | --- |
| `schemaVersion` | integer | no | `1` | Readers accept any value; a future version bumps this when a field changes meaning |
| `preset` | string | no | `"crimson"` | Unknown slugs fall back to `crimson` |
| `accent` | string or null | no | `null` | `null` means "use the preset accent". When set it must match `^#[0-9a-fA-F]{6}$` |
| `glassIntensity` | integer | no | `60` | Clamped to 0–100 |

### JSON Schema (draft 2020-12)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://nocturne.local/settings.schema.json",
  "title": "Nocturne settings",
  "type": "object",
  "additionalProperties": true,
  "properties": {
    "schemaVersion": { "type": "integer", "minimum": 1 },
    "preset": {
      "type": "string",
      "enum": ["crimson", "ocean", "forest", "amber", "violet"]
    },
    "accent": {
      "type": ["string", "null"],
      "pattern": "^#[0-9a-fA-F]{6}$"
    },
    "glassIntensity": { "type": "integer", "minimum": 0, "maximum": 100 }
  }
}
```

`additionalProperties: true` is deliberate: a newer build may add fields, and
an older build must still import the file rather than reject it.

### Import / export behaviour

- Export writes the schema above, pretty-printed, and reports the saved path.
- Import accepts a JSON object, ignores unknown keys, clamps out-of-range
  numbers, and rejects (with a message) text that is not a JSON object or that
  has a wrongly typed field.
- A rejected import changes nothing.
- On both platforms the file is written after every change, so the running app
  and the file never disagree.

## 5. Where it lives in code

| Concern | Location |
| --- | --- |
| Presets, tokens, clamping, JSON codec | `shared/src/commonMain/kotlin/com/never/localmusic/shared/Appearance.kt` |
| Snapshot exposure + persistence | `LocalPlayerSnapshot.settings`, `LocalPlayer.updateSettings/importSettingsJson/exportSettingsJson` |
| Desktop palette + glass mapping | `desktopApp/.../Theme.kt` (`GlassPalette`, `LocalGlassPalette`) |
| Desktop appearance screen | `desktopApp/.../Main.kt` (`AppearanceView`, nav item 8) |
| Desktop file dialogs | `DesktopMain.chooseSettingsFileToOpen/Save` |
| Android appearance tab | `androidApp/.../MainActivity.kt` (`AppearanceTab`) |
