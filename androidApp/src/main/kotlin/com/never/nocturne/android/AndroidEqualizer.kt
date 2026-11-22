package com.never.nocturne.android

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import com.never.nocturne.core.EqualizerState

/**
 * Manages the Android AudioFX Equalizer and BassBoost instances.
 * Attaches to the global audio session (0) or a specific audio session ID.
 */
class AndroidEqualizer {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var attachedSessionId: Int = -1

    fun attach(audioSessionId: Int = 0) {
        if (equalizer != null && attachedSessionId == audioSessionId) return
        release()
        attachedSessionId = audioSessionId
        runCatching {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = true
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = true
            }
        }
    }

    fun applyState(state: EqualizerState) {
        if (equalizer == null) {
            attach(0)
        }
        val eq = equalizer ?: return
        runCatching {
            eq.enabled = state.isEnabled
            if (state.isEnabled) {
                val numBands = eq.numberOfBands.toInt()
                val minLevel = eq.bandLevelRange.getOrNull(0)?.toInt() ?: -1500
                val maxLevel = eq.bandLevelRange.getOrNull(1)?.toInt() ?: 1500

                state.bands.forEachIndexed { i, band ->
                    if (i < numBands) {
                        val level = (band.gainDb * 100).coerceIn(minLevel, maxLevel).toShort()
                        eq.setBandLevel(i.toShort(), level)
                    }
                }
            }
            bassBoost?.let { bb ->
                bb.enabled = state.isEnabled
                if (state.isEnabled && bb.strengthSupported) {
                    val strength = (state.bassBoost * 10).coerceIn(0, 1000).toShort()
                    bb.setStrength(strength)
                }
            }
        }
    }

    fun release() {
        runCatching {
            equalizer?.release()
            bassBoost?.release()
        }
        equalizer = null
        bassBoost = null
        attachedSessionId = -1
    }
}
