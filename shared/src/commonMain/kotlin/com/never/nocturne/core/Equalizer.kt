package com.never.nocturne.core

import kotlinx.serialization.Serializable

@Serializable
enum class EqPreset(val displayName: String, val gainsDb: List<Int>) {
    FLAT("Flat", listOf(0, 0, 0, 0, 0)),
    BASS_BOOST("Bass Boost", listOf(6, 4, 1, 0, 0)),
    ROCK("Rock", listOf(5, 3, -1, 3, 5)),
    POP("Pop", listOf(-1, 2, 5, 2, -1)),
    VOCAL("Vocal", listOf(-2, 0, 4, 3, 1));

    companion object {
        fun fromName(name: String?): EqPreset =
            entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: FLAT
    }
}

@Serializable
data class EqualizerBand(
    val index: Int,
    val centerFreqHz: Int,
    val gainDb: Int,
    val minGainDb: Int = -12,
    val maxGainDb: Int = 12,
)

@Serializable
data class EqualizerState(
    val isEnabled: Boolean = false,
    val bassBoost: Int = 0, // 0 to 100%
    val preset: EqPreset = EqPreset.FLAT,
    val bands: List<EqualizerBand> = defaultBands(EqPreset.FLAT),
) {
    companion object {
        val DEFAULT_FREQS = listOf(60, 230, 910, 3600, 14000)

        fun defaultBands(preset: EqPreset): List<EqualizerBand> {
            return DEFAULT_FREQS.mapIndexed { i, freq ->
                EqualizerBand(
                    index = i,
                    centerFreqHz = freq,
                    gainDb = preset.gainsDb.getOrElse(i) { 0 },
                )
            }
        }
    }
}
