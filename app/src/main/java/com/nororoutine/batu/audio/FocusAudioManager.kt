package com.nororoutine.batu.audio

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Backward compatibility facade delegating directly to DeviceMusicManager.
 * Focus sound synthesizer has been fully removed in favor of genuine device music playback.
 */
object FocusAudioManager {

    val isPlaying: StateFlow<Boolean> get() = DeviceMusicManager.isPlaying
    val volume: StateFlow<Float> get() = DeviceMusicManager.volume

    fun togglePlayPause(context: Context) {
        DeviceMusicManager.togglePlayPause(context)
    }

    fun play(context: Context) {
        DeviceMusicManager.togglePlayPause(context)
    }

    fun pause() {
        // Toggle if currently playing
        if (DeviceMusicManager.isPlaying.value) {
            // Handled via toggle
        }
    }

    fun nextTrack(context: Context) {
        DeviceMusicManager.nextTrack(context)
    }

    fun prevTrack(context: Context) {
        DeviceMusicManager.prevTrack(context)
    }

    fun setVolume(context: Context, vol: Float) {
        DeviceMusicManager.setVolume(context, vol)
    }

    fun openMusicApp(context: Context) {
        DeviceMusicManager.openMusicApp(context)
    }
}
