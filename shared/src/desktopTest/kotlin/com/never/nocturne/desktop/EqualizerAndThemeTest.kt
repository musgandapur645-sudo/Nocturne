package com.never.nocturne.desktop

import com.never.nocturne.core.EqPreset
import com.never.nocturne.core.EqualizerState
import com.never.nocturne.shared.ThemePresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EqualizerAndThemeTest {

    @Test
    fun equalizerDefaultsToFlatAndDisabled() {
        val state = EqualizerState()
        assertEquals(false, state.isEnabled)
        assertEquals(0, state.bassBoost)
        assertEquals(EqPreset.FLAT, state.preset)
        assertEquals(5, state.bands.size)
        assertEquals(listOf(60, 230, 910, 3600, 14000), state.bands.map { it.centerFreqHz })
        assertTrue(state.bands.all { it.gainDb == 0 })
    }

    @Test
    fun equalizerPresetsConfigureBandsProperly() {
        val rockBands = EqualizerState.defaultBands(EqPreset.ROCK)
        assertEquals(listOf(5, 3, -1, 3, 5), rockBands.map { it.gainDb })

        val vocalBands = EqualizerState.defaultBands(EqPreset.VOCAL)
        assertEquals(listOf(-2, 0, 4, 3, 1), vocalBands.map { it.gainDb })

        val bassBands = EqualizerState.defaultBands(EqPreset.BASS_BOOST)
        assertEquals(listOf(6, 4, 1, 0, 0), bassBands.map { it.gainDb })
    }

    @Test
    fun lightGlassThemePresetIsAvailable() {
        val light = ThemePresets.bySlug("light")
        assertNotNull(light)
        assertEquals("light", light.slug)
        assertEquals("Light Glass", light.displayName)
        assertEquals(0xFF2563EBL, light.accent)
        assertTrue(ThemePresets.all.any { it.slug == "light" })
    }
}
