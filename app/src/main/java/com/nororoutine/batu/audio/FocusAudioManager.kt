package com.nororoutine.batu.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

data class FocusTrack(
    val id: String,
    val title: String,
    val subtitle: String,
    val genre: String,
    val baseFreq: Double,
    val isSynth: Boolean = true
)

enum class MusicSourceMode {
    FOCUS_SOUNDS,   // Yerleşik Derin Odak & Lo-Fi Ambiyans
    SYSTEM_MEDIA    // Cihazda Çalan Müzik (Spotify / YouTube Music)
}

object FocusAudioManager {

    val focusTracks = listOf(
        FocusTrack("lofi_focus", "Lo-Fi Deep Focus", "432Hz Sakinleştirici Akor", "Lo-Fi", 432.0),
        FocusTrack("rain_ambient", "Gece Yağmuru & Rüzgar", "Doğal Beyaz Gürültü", "Doğa", 216.0),
        FocusTrack("cafe_study", "Kütüphane & Kahve", "Ritim & Çalışma Modu", "Ambiyans", 320.0),
        FocusTrack("forest_zen", "Zen Ormanı & Kuşlar", "Frekans Meditasyonu", "Zen", 528.0)
    )

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentMode = MutableStateFlow(MusicSourceMode.FOCUS_SOUNDS)
    val currentMode: StateFlow<MusicSourceMode> = _currentMode.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _volume = MutableStateFlow(0.75f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private var audioJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    fun getCurrentTrack(): FocusTrack {
        return focusTracks[_currentTrackIndex.value.coerceIn(focusTracks.indices)]
    }

    fun togglePlayPause(context: Context) {
        if (_isPlaying.value) {
            pause()
            if (_currentMode.value == MusicSourceMode.SYSTEM_MEDIA) {
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PAUSE)
            }
        } else {
            play(context)
            if (_currentMode.value == MusicSourceMode.SYSTEM_MEDIA) {
                dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
            }
        }
    }

    fun play(context: Context) {
        _isPlaying.value = true
        if (_currentMode.value == MusicSourceMode.FOCUS_SOUNDS) {
            startSynthAudio()
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
        }
    }

    fun pause() {
        _isPlaying.value = false
        stopSynthAudio()
    }

    fun nextTrack(context: Context) {
        if (_currentMode.value == MusicSourceMode.FOCUS_SOUNDS) {
            _currentTrackIndex.value = (_currentTrackIndex.value + 1) % focusTracks.size
            if (_isPlaying.value) {
                stopSynthAudio()
                startSynthAudio()
            }
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        }
    }

    fun prevTrack(context: Context) {
        if (_currentMode.value == MusicSourceMode.FOCUS_SOUNDS) {
            val prev = _currentTrackIndex.value - 1
            _currentTrackIndex.value = if (prev < 0) focusTracks.size - 1 else prev
            if (_isPlaying.value) {
                stopSynthAudio()
                startSynthAudio()
            }
        } else {
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        }
    }

    fun selectTrack(index: Int) {
        _currentMode.value = MusicSourceMode.FOCUS_SOUNDS
        _currentTrackIndex.value = index.coerceIn(focusTracks.indices)
        if (_isPlaying.value) {
            stopSynthAudio()
            startSynthAudio()
        }
    }

    fun setMode(mode: MusicSourceMode) {
        _currentMode.value = mode
        if (mode == MusicSourceMode.SYSTEM_MEDIA && _isPlaying.value) {
            stopSynthAudio()
        }
    }

    fun setVolume(vol: Float) {
        val clamped = vol.coerceIn(0f, 1f)
        _volume.value = clamped
        try {
            audioTrack?.setVolume(clamped)
        } catch (_: Exception) {}
    }

    fun openMusicApp(context: Context) {
        // Try Spotify first
        val spotifyIntent = context.packageManager.getLaunchIntentForPackage("com.spotify.music")
        if (spotifyIntent != null) {
            spotifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(spotifyIntent)
            return
        }

        // Try YouTube Music
        val ytMusicIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.youtube.music")
        if (ytMusicIntent != null) {
            ytMusicIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(ytMusicIntent)
            return
        }

        // Fallback to general music player intent
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // General audio intent fallback
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun dispatchMediaKey(context: Context, keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val eventDown = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
            val eventUp = KeyEvent(KeyEvent.ACTION_UP, keyCode)
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
        } catch (_: Exception) {}
    }

    private fun startSynthAudio() {
        stopSynthAudio()
        val track = getCurrentTrack()
        audioJob = scope.launch {
            try {
                val sampleRate = 22050
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(4096)

                val trackAudio = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = trackAudio
                trackAudio.setVolume(_volume.value)
                trackAudio.play()

                val buffer = ShortArray(bufferSize / 2)
                var phase = 0.0
                val freq = track.baseFreq

                while (isActive && _isPlaying.value) {
                    val twoPi = 2.0 * Math.PI
                    val delta = twoPi * freq / sampleRate

                    for (i in buffer.indices) {
                        // Soothing harmonic chords + gentle white/pink noise for rain or lofi texture
                        val wave1 = sin(phase) * 0.4
                        val wave2 = sin(phase * 1.5) * 0.25
                        val wave3 = sin(phase * 0.5) * 0.15
                        val noise = if (track.id.contains("rain") || track.id.contains("cafe")) {
                            (Random.nextDouble(-0.15, 0.15))
                        } else 0.0

                        val combined = (wave1 + wave2 + wave3 + noise) * 0.35
                        buffer[i] = (combined * Short.MAX_VALUE).toInt().toShort()

                        phase += delta
                        if (phase > twoPi) phase -= twoPi
                    }

                    trackAudio.write(buffer, 0, buffer.size)
                }

                trackAudio.stop()
                trackAudio.release()
            } catch (_: Exception) {
            } finally {
                audioTrack = null
            }
        }
    }

    private fun stopSynthAudio() {
        audioJob?.cancel()
        audioJob = null
        try {
            audioTrack?.let {
                if (it.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        audioTrack = null
    }
}
