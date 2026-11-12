package com.never.localmusic.shared

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Customization primitives shared by desktop and Android.
 *
 * Colours are carried as `0xAARRGGBB` Longs so this module stays free of any
 * Compose dependency. Compose turns them into `Color` with `Color(value)`.
 *
 * See docs/customization-spec.md for the design and the JSON schema.
 */

// ---------------------------------------------------------------------------
// Presets
// ---------------------------------------------------------------------------

data class ThemePreset(
    val slug: String,
    val displayName: String,
    /** `0xAARRGGBB` */
    val accent: Long,
)

object ThemePresets {

    val all: List<ThemePreset> = listOf(
        ThemePreset("crimson", "Crimson", 0xFFE11D48),
        ThemePreset("ocean", "Ocean", 0xFF0EA5E9),
        ThemePreset("forest", "Forest", 0xFF10B981),
        ThemePreset("amber", "Amber", 0xFFF59E0B),
        ThemePreset("violet", "Violet", 0xFF8B5CF6),
    )

    /** The shipped look; also the fallback for unknown slugs. */
    val default: ThemePreset = all[0]

    fun bySlug(slug: String?): ThemePreset =
        all.firstOrNull { it.slug == slug?.trim()?.lowercase() } ?: default

    fun slugOrNull(slug: String?): String? =
        all.firstOrNull { it.slug == slug?.trim()?.lowercase() }?.slug
}

// ---------------------------------------------------------------------------
// Accent tokens
// ---------------------------------------------------------------------------

data class AccentTokens(
    /** Primary fill: play button, active row, focus ring. */
    val accent: Long,
    /** Hover/pressed fill: accent lightened toward white. */
    val accentHover: Long,
    /** Soft halo behind the accent: accent at 25% alpha. */
    val accentGlow: Long,
    /** Text/icons drawn on top of the accent. */
    val onAccent: Long = 0xFFFFFFFFL,
)

/**
 * Resolve the four accent tokens for a preset plus an optional custom accent.
 * An invalid override is ignored rather than failing the theme.
 */
fun accentTokens(presetSlug: String?, accentOverride: String?): AccentTokens {
    val base = parseHex(accentOverride) ?: ThemePresets.bySlug(presetSlug).accent
    return AccentTokens(
        accent = base,
        accentHover = lighten(base, 0.12f),
        accentGlow = withAlpha(base, 0x40),
    )
}

// ---------------------------------------------------------------------------
// Glass intensity
// ---------------------------------------------------------------------------

data class GlassTokens(
    val intensity: Int,
    /** Multiplier for panel fill alpha: 1.0 at the default 60. */
    val fillFactor: Float,
    /** Multiplier for highlight/border alpha: 1.0 at the default 60. */
    val edgeFactor: Float,
    /** Human-readable band for the slider label. */
    val tier: String,
) {
    /** Flat at 0, the shipped look at 60, most transparent at 100. */
    val isDefault: Boolean get() = intensity == GLASS_DEFAULT
}

const val GLASS_MIN = 0
const val GLASS_MAX = 100
const val GLASS_DEFAULT = 60

fun glassTokens(intensity: Int): GlassTokens {
    val value = intensity.coerceIn(GLASS_MIN, GLASS_MAX)
    val t = value / 100f
    val tier = when {
        value <= 24 -> "Solid"
        value <= 49 -> "Frosted"
        value <= 74 -> "Crystal"
        else -> "Vapor"
    }
    // Anchored so 60 is exactly 1.0: the default look never changes.
    val fill = 1.75f - 1.25f * t
    return GlassTokens(
        intensity = value,
        fillFactor = fill,
        edgeFactor = 2f - fill,
        tier = tier,
    )
}

// ---------------------------------------------------------------------------
// Settings model + JSON
// ---------------------------------------------------------------------------

@Serializable
data class AppSettings(
    val schemaVersion: Int = 1,
    /** Preset slug; unknown values fall back to [ThemePresets.default]. */
    val preset: String = ThemePresets.default.slug,
    /** `#RRGGBB` override, or null to use the preset accent. */
    val accent: String? = null,
    /** 0–100, see [glassTokens]. */
    val glassIntensity: Int = GLASS_DEFAULT,
) {
    /** Clamp and canonicalise: always safe to persist or hand to the UI. */
    fun normalized(): AppSettings = copy(
        preset = ThemePresets.bySlug(preset).slug,
        accent = parseHex(accent)?.let(::formatHex),
        glassIntensity = glassIntensity.coerceIn(GLASS_MIN, GLASS_MAX),
    )

    fun accentTokens(): AccentTokens = com.never.localmusic.shared.accentTokens(preset, accent)

    fun glassTokens(): GlassTokens = com.never.localmusic.shared.glassTokens(glassIntensity)
}

object SettingsCodec {

    private val json = Json {
        ignoreUnknownKeys = true // forward-compatible with newer builds
        encodeDefaults = true
        prettyPrint = true
    }

    fun encode(settings: AppSettings): String = json.encodeToString(settings.normalized())

    /**
     * Decode settings text.
     *
     * Returns null when the text is not a JSON object or a field has the wrong
     * type, so the caller can show a message and leave the current state alone.
     * Missing fields fall back to their defaults.
     */
    fun decode(text: String): AppSettings? =
        runCatching { json.decodeFromString<AppSettings>(text.trim()) }
            .getOrNull()
            ?.normalized()

    /** Human-readable reason for [decode] returning null. */
    fun decodeError(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "The file is empty."
        if (!trimmed.startsWith("{")) return "The file is not a JSON object."
        return "The file has a field with the wrong type."
    }
}

// ---------------------------------------------------------------------------
// Colour helpers (ARGB Long)
// ---------------------------------------------------------------------------

private val HEX_PATTERN = Regex("^#[0-9a-fA-F]{6}$")

/** Parse `#RRGGBB` into opaque `0xAARRGGBB`, or null when not that exact form. */
fun parseHex(value: String?): Long? {
    val hex = value?.trim() ?: return null
    if (!HEX_PATTERN.matches(hex)) return null
    return 0xFF000000L or hex.substring(1).toLong(16)
}

/** `0xAARRGGBB` to `#RRGGBB` (alpha dropped). */
fun formatHex(argb: Long): String =
    "#" + (argb and 0xFFFFFFL).toString(16).padStart(6, '0').uppercase()

private fun withAlpha(argb: Long, alpha: Int): Long =
    (alpha.toLong() shl 24) or (argb and 0xFFFFFFL)

private fun lighten(argb: Long, amount: Float): Long {
    val r = ((argb shr 16) and 0xFF).toFloat()
    val g = ((argb shr 8) and 0xFF).toFloat()
    val b = (argb and 0xFF).toFloat()
    fun mix(c: Float) = (c + (255f - c) * amount).toInt().coerceIn(0, 255).toLong()
    return (0xFF000000L) or (mix(r) shl 16) or (mix(g) shl 8) or mix(b)
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
