package com.nororoutine.batu.audio

import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class DeviceTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUriString: String,
    val albumId: Long = 0L,
    val coverUrl: String? = null,
    val spotifyUri: String? = null,
    val isSpotify: Boolean = false
)

object DeviceMusicManager {

    private const val TAG = "DeviceMusicManager"

    // Default Curated Spotify Focus & Routine Tracks (Real Spotify CDNs & URIs)
    private val DEFAULT_SPOTIFY_TRACKS = listOf(
        DeviceTrack(
            id = -1L,
            title = "Weightless (Derin Odak)",
            artist = "Marconi Union",
            album = "Ambient Focus",
            durationMs = 485000L,
            contentUriString = "spotify:track:6kkwzB6hXLIONkEk9JciA6",
            albumId = 0L,
            coverUrl = "https://i.scdn.co/image/ab67616d0000b273574c82b09a4732cb6c138b0f",
            spotifyUri = "spotify:track:6kkwzB6hXLIONkEk9JciA6",
            isSpotify = true
        ),
        DeviceTrack(
            id = -2L,
            title = "Lofi Chill Beats (Sabah Rutini)",
            artist = "Lofi Fruits Music",
            album = "Morning Chill & Focus",
            durationMs = 154000L,
            contentUriString = "spotify:track:0VjIjW4GlUZAMYd2vXMi3b",
            albumId = 0L,
            coverUrl = "https://i.scdn.co/image/ab67616d0000b273297a7a13c3aa4dcf3fa98e09",
            spotifyUri = "spotify:track:0VjIjW4GlUZAMYd2vXMi3b",
            isSpotify = true
        ),
        DeviceTrack(
            id = -3L,
            title = "Experience (Verimlilik)",
            artist = "Ludovico Einaudi",
            album = "In A Time Lapse",
            durationMs = 315000L,
            contentUriString = "spotify:track:1BncfTJAWxWvOTytuqMN4b",
            albumId = 0L,
            coverUrl = "https://i.scdn.co/image/ab67616d0000b27341efcbbaebf73cbe1da476a2",
            spotifyUri = "spotify:track:1BncfTJAWxWvOTytuqMN4b",
            isSpotify = true
        ),
        DeviceTrack(
            id = -4L,
            title = "Clair de Lune (Akşam Sakinliği)",
            artist = "Claude Debussy",
            album = "Classical Chill",
            durationMs = 300000L,
            contentUriString = "spotify:track:6N7gEjfGds24RtFBP7m31t",
            albumId = 0L,
            coverUrl = "https://i.scdn.co/image/ab67616d0000b2739a896d8f512ffadff1df9866",
            spotifyUri = "spotify:track:6N7gEjfGds24RtFBP7m31t",
            isSpotify = true
        ),
        DeviceTrack(
            id = -5L,
            title = "Peaceful Piano Meditation",
            artist = "Relaxing Piano Music",
            album = "Deep Relaxation",
            durationMs = 240000L,
            contentUriString = "spotify:track:1vYxhdCizn0hLd3n4G5FhO",
            albumId = 0L,
            coverUrl = "https://i.scdn.co/image/ab67616d0000b2738734268e3dfaa84ee7c0cf59",
            spotifyUri = "spotify:track:1vYxhdCizn0hLd3n4G5FhO",
            isSpotify = true
        )
    )

    private val _tracks = MutableStateFlow<List<DeviceTrack>>(DEFAULT_SPOTIFY_TRACKS)
    val tracks: StateFlow<List<DeviceTrack>> = _tracks.asStateFlow()

    private val _currentTrackIndex = MutableStateFlow(0)
    val currentTrackIndex: StateFlow<Int> = _currentTrackIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _volume = MutableStateFlow(0.75f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _hasQueriedTracks = MutableStateFlow(false)
    val hasQueriedTracks: StateFlow<Boolean> = _hasQueriedTracks.asStateFlow()

    // Live Spotify track detected from Spotify's Android Broadcasts
    private val _liveSpotifyTrack = MutableStateFlow<DeviceTrack?>(null)
    val liveSpotifyTrack: StateFlow<DeviceTrack?> = _liveSpotifyTrack.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var isSpotifyReceiverRegistered = false

    fun getCurrentTrack(): DeviceTrack? {
        val live = _liveSpotifyTrack.value
        if (live != null) return live

        val list = _tracks.value
        val idx = _currentTrackIndex.value
        return if (list.isNotEmpty() && idx in list.indices) {
            list[idx]
        } else {
            DEFAULT_SPOTIFY_TRACKS.firstOrNull()
        }
    }

    /**
     * Registers Spotify BroadcastReceiver to listen to real-time track and playback state from Spotify.
     */
    private fun ensureSpotifyReceiver(context: Context) {
        if (isSpotifyReceiverRegistered) return
        val appContext = context.applicationContext

        val filter = IntentFilter().apply {
            addAction("com.spotify.music.metadatachanged")
            addAction("com.spotify.music.playbackstatechanged")
            addAction("com.spotify.music.queuechanged")
        }

        val spotifyReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent == null) return
                val action = intent.action ?: return
                Log.d(TAG, "Spotify Broadcast received: $action")

                val trackName = intent.getStringExtra("track")
                val artistName = intent.getStringExtra("artist")
                val albumName = intent.getStringExtra("album")
                val trackId = intent.getStringExtra("id") // e.g. "spotify:track:4cOdK2wGLETKBW3PvgPWqT"
                val length = intent.getIntExtra("length", 0).toLong()
                val isPlayingNow = intent.getBooleanExtra("playing", false)

                if (action == "com.spotify.music.playbackstatechanged") {
                    _isPlaying.value = isPlayingNow
                }

                if (!trackName.isNullOrBlank()) {
                    _isPlaying.value = true
                    val currentLive = _liveSpotifyTrack.value

                    // If it's a new track, update and fetch album cover
                    if (currentLive?.title != trackName || currentLive.artist != artistName) {
                        val initialTrack = DeviceTrack(
                            id = System.currentTimeMillis(),
                            title = trackName,
                            artist = artistName ?: "Spotify",
                            album = albumName ?: "Spotify",
                            durationMs = length,
                            contentUriString = trackId ?: "",
                            coverUrl = currentLive?.coverUrl,
                            spotifyUri = trackId,
                            isSpotify = true
                        )
                        _liveSpotifyTrack.value = initialTrack

                        // Fetch high-res album cover from Spotify oEmbed
                        if (!trackId.isNullOrBlank() && trackId.contains("spotify:track:")) {
                            val rawId = trackId.substringAfter("spotify:track:")
                            fetchSpotifyAlbumCover(rawId) { coverUrl ->
                                _liveSpotifyTrack.value = initialTrack.copy(coverUrl = coverUrl)
                            }
                        }
                    }
                }
            }
        }

        try {
            ContextCompat.registerReceiver(
                appContext,
                spotifyReceiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )
            isSpotifyReceiverRegistered = true
        } catch (e: Exception) {
            Log.w(TAG, "Could not register Spotify receiver: ${e.message}")
        }
    }

    /**
     * Fetches official album cover art from Spotify's oEmbed endpoint.
     */
    private fun fetchSpotifyAlbumCover(trackId: String, onCoverFetched: (String) -> Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://open.spotify.com/oembed?url=https://open.spotify.com/track/$trackId")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    val json = JSONObject(response)
                    if (json.has("thumbnail_url")) {
                        val coverUrl = json.getString("thumbnail_url")
                        scope.launch(Dispatchers.Main) {
                            onCoverFetched(coverUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to fetch Spotify oEmbed cover: ${e.message}")
            }
        }
    }

    /**
     * Queries MediaStore for audio tracks present on the physical device.
     * Also registers Spotify receiver.
     */
    fun refreshDeviceTracks(context: Context) {
        ensureSpotifyReceiver(context)

        scope.launch(Dispatchers.IO) {
            val list = mutableListOf<DeviceTrack>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

            try {
                val cursor = context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    selection,
                    null,
                    sortOrder
                )

                cursor?.use { c ->
                    val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)

                    while (c.moveToNext()) {
                        val id = c.getLong(idCol)
                        val title = c.getString(titleCol) ?: "Bilinmeyen Parça"
                        val artistRaw = c.getString(artistCol)
                        val artist = if (artistRaw == null || artistRaw == "<unknown>") "Bilinmeyen Sanatçı" else artistRaw
                        val album = c.getString(albumCol) ?: ""
                        val duration = c.getLong(durCol)
                        val albumId = if (albumIdCol >= 0) c.getLong(albumIdCol) else 0L
                        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                        list.add(
                            DeviceTrack(
                                id = id,
                                title = title,
                                artist = artist,
                                album = album,
                                durationMs = duration,
                                contentUriString = uri.toString(),
                                albumId = albumId,
                                isSpotify = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load device audio tracks from MediaStore", e)
            }

            // If device has no local MP3 tracks, provide curated Spotify tracks
            if (list.isEmpty()) {
                _tracks.value = DEFAULT_SPOTIFY_TRACKS
            } else {
                _tracks.value = list + DEFAULT_SPOTIFY_TRACKS
            }
            _hasQueriedTracks.value = true
        }
    }

    /**
     * Plays a track. If it's a local media file, plays via MediaPlayer.
     * Never opens external applications.
     */
    fun playTrack(context: Context, index: Int) {
        val list = _tracks.value
        if (list.isEmpty() || index !in list.indices) return

        _currentTrackIndex.value = index
        val track = list[index]

        if (track.isSpotify || !track.contentUriString.startsWith("content://")) {
            // Background control via media keys (no external app launched)
            _isPlaying.value = true
            _currentPositionMs.value = 0L
            startProgressTracker()
            dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY)
            dispatchSpotifyMediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY)
            return
        }

        // Local media playback via MediaPlayer
        try {
            stopPlayer()
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, Uri.parse(track.contentUriString))
                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    startProgressTracker()
                }
                setOnCompletionListener {
                    nextTrack(context)
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    _isPlaying.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing local track: ${track.title}", e)
            _isPlaying.value = false
        }
    }

    /**
     * Toggles play/pause:
     * Controls device media playback in the background silently.
     * Never launches Spotify or any external app.
     */
    fun togglePlayPause(context: Context) {
        ensureSpotifyReceiver(context)

        val mp = mediaPlayer
        if (mp != null) {
            if (mp.isPlaying) {
                mp.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                mp.start()
                _isPlaying.value = true
                startProgressTracker()
            }
            return
        }

        val willPlay = !_isPlaying.value
        _isPlaying.value = willPlay

        if (willPlay) {
            startProgressTracker()
        }

        // Send hardware media key silently to active audio player (Spotify / device media)
        dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
        dispatchSpotifyMediaButton(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    fun nextTrack(context: Context) {
        val list = _tracks.value
        if (list.isNotEmpty()) {
            val nextIdx = (_currentTrackIndex.value + 1) % list.size
            _currentTrackIndex.value = nextIdx
            val track = list[nextIdx]
            _currentPositionMs.value = 0L
            if (mediaPlayer != null && !track.isSpotify) {
                playTrack(context, nextIdx)
            }
        }
        dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_NEXT)
        dispatchSpotifyMediaButton(context, KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    fun prevTrack(context: Context) {
        val list = _tracks.value
        if (list.isNotEmpty()) {
            val prevIdx = if (_currentTrackIndex.value - 1 < 0) list.size - 1 else _currentTrackIndex.value - 1
            _currentTrackIndex.value = prevIdx
            val track = list[prevIdx]
            _currentPositionMs.value = 0L
            if (mediaPlayer != null && !track.isSpotify) {
                playTrack(context, prevIdx)
            }
        }
        dispatchMediaKey(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        dispatchSpotifyMediaButton(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    /**
     * Launches the track directly in Spotify app or web fallback.
     */
    fun launchSpotifyTrack(context: Context, track: DeviceTrack) {
        val uriString = track.spotifyUri ?: track.contentUriString
        try {
            // 1. Try Spotify URI intent (spotify:track:...)
            val spotifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                setPackage("com.spotify.music")
            }
            context.startActivity(spotifyIntent)
            _isPlaying.value = true
        } catch (_: Exception) {
            // 2. Try generic view intent with web link fallback
            try {
                val webUrl = if (uriString.startsWith("spotify:track:")) {
                    "https://open.spotify.com/track/" + uriString.substringAfter("spotify:track:")
                } else if (uriString.startsWith("spotify:playlist:")) {
                    "https://open.spotify.com/playlist/" + uriString.substringAfter("spotify:playlist:")
                } else {
                    "https://open.spotify.com"
                }

                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(browserIntent)
                _isPlaying.value = true
            } catch (ex: Exception) {
                Log.w(TAG, "Could not open Spotify track", ex)
                openMusicApp(context)
            }
        }
    }

    fun openMusicApp(context: Context) {
        val packages = listOf(
            "com.spotify.music",
            "com.google.android.apps.youtube.music",
            "com.sec.android.app.music",
            "com.apple.android.music",
            "deezer.android.app"
        )

        for (pkg in packages) {
            val intent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                return
            }
        }

        // Generic music player intent
        try {
            val intent = Intent(MediaStore.INTENT_ACTION_MUSIC_PLAYER).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Log.w(TAG, "Could not launch music application", ex)
            }
        }
    }

    private fun dispatchMediaKey(context: Context, keyCode: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val eventDown = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, keyCode, 0)
            val eventUp = KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0)
            audioManager.dispatchMediaKeyEvent(eventDown)
            audioManager.dispatchMediaKeyEvent(eventUp)
        } catch (e: Exception) {
            Log.e(TAG, "Error dispatching media key", e)
        }
    }

    private fun dispatchSpotifyMediaButton(context: Context, keyCode: Int) {
        try {
            val intentDown = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setPackage("com.spotify.music")
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
            }
            context.sendOrderedBroadcast(intentDown, null)

            val intentUp = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
                setPackage("com.spotify.music")
                putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, keyCode))
            }
            context.sendOrderedBroadcast(intentUp, null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not dispatch targeted Spotify media key", e)
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val mp = mediaPlayer
                if (mp != null) {
                    if (mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                    }
                } else {
                    val track = getCurrentTrack()
                    val total = track?.durationMs ?: 0L
                    if (total > 0) {
                        val next = _currentPositionMs.value + 1000L
                        _currentPositionMs.value = if (next >= total) 0L else next
                    } else {
                        _currentPositionMs.value = _currentPositionMs.value + 1000L
                    }
                }
                delay(1000L)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun stopPlayer() {
        stopProgressTracker()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(positionMs.toInt())
                _currentPositionMs.value = positionMs
            } catch (e: Exception) {
                Log.e(TAG, "Seek error", e)
            }
        }
    }

    fun setVolume(context: Context, vol: Float) {
        _volume.value = vol.coerceIn(0f, 1f)
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = (vol * maxVol).toInt().coerceIn(0, maxVol)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    fun formatTime(ms: Long): String {
        val totalSecs = (ms / 1000).coerceAtLeast(0)
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "%d:%02d".format(mins, secs)
    }
}
