package com.musicin.app

import android.app.Application
import android.content.ComponentName
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx = app.applicationContext
    private val store = PlaylistStore(ctx)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _playlists = MutableStateFlow(store.load())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _currentId = MutableStateFlow<Long?>(null)
    val currentId: StateFlow<Long?> = _currentId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeat = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeat: StateFlow<Int> = _repeat.asStateFlow()

    val currentSong: StateFlow<Song?> = combine(_songs, _currentId) { list, id ->
        if (id == null) null else list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private var controller: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            refresh(player)
        }
    }

    init {
        val token = SessionToken(ctx, ComponentName(ctx, PlaybackService::class.java))
        val future = MediaController.Builder(ctx, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val c = future.get()
                controller = c
                c.addListener(listener)
                refresh(c)
                pending.forEach { it(c) }
                pending.clear()
            } catch (e: Exception) {
                // não conseguiu conectar ao serviço
            }
        }, ContextCompat.getMainExecutor(ctx))

        // atualiza a barra de progresso enquanto toca
        viewModelScope.launch {
            while (true) {
                val c = controller
                if (c != null && c.isPlaying) {
                    _position.value = c.currentPosition.coerceAtLeast(0L)
                }
                delay(500)
            }
        }
    }

    private fun refresh(p: Player) {
        _isPlaying.value = p.isPlaying
        _currentId.value = p.currentMediaItem?.mediaId?.toLongOrNull()
        _duration.value = if (p.duration == C.TIME_UNSET) 0L else p.duration.coerceAtLeast(0L)
        _position.value = p.currentPosition.coerceAtLeast(0L)
        _shuffle.value = p.shuffleModeEnabled
        _repeat.value = p.repeatMode
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending += block
    }

    // ---------- biblioteca ----------

    fun loadSongs() {
        viewModelScope.launch {
            if (_songs.value.isEmpty()) _loading.value = true
            val list = withContext(Dispatchers.IO) { MusicRepository.load(ctx) }
            _songs.value = list
            _loading.value = false
        }
    }

    private fun toItem(song: Song): MediaItem =
        MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .build(),
            )
            .build()

    // ---------- reprodução ----------

    /** Toca uma lista a partir de [startIndex]. Se [shuffle] for nulo, mantém o modo aleatório atual. */
    fun playAll(list: List<Song>, startIndex: Int = 0, shuffle: Boolean? = null) {
        if (list.isEmpty()) return
        withController { c ->
            if (shuffle != null) c.shuffleModeEnabled = shuffle
            c.setMediaItems(list.map(::toItem), startIndex.coerceIn(0, list.lastIndex), 0L)
            c.prepare()
            c.play()
        }
    }

    fun shuffleAll(list: List<Song>) {
        if (list.isEmpty()) return
        playAll(list, Random.nextInt(list.size), shuffle = true)
    }

    fun togglePlay() = withController { c ->
        if (c.isPlaying) {
            c.pause()
        } else {
            if (c.playbackState == Player.STATE_ENDED) c.seekToDefaultPosition()
            c.play()
        }
    }

    fun next() = withController { it.seekToNext() }
    fun previous() = withController { it.seekToPrevious() }
    fun seekTo(ms: Long) = withController {
        it.seekTo(ms)
        _position.value = ms
    }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeat() = withController {
        it.repeatMode = when (it.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    fun playNext(song: Song) {
        withController { c ->
            if (c.mediaItemCount == 0) {
                playAll(listOf(song))
            } else {
                c.addMediaItem(c.currentMediaItemIndex + 1, toItem(song))
            }
        }
    }

    fun addToQueue(song: Song) {
        withController { c ->
            if (c.mediaItemCount == 0) playAll(listOf(song)) else c.addMediaItem(toItem(song))
        }
    }

    // ---------- playlists ----------

    private fun savePlaylists(list: List<Playlist>) {
        _playlists.value = list
        store.save(list)
    }

    fun createPlaylist(name: String, firstSong: Song? = null): String {
        val p = Playlist(PlaylistStore.newId(), name.trim(), listOfNotNull(firstSong?.id))
        savePlaylists(_playlists.value + p)
        return p.id
    }

    fun renamePlaylist(id: String, name: String) {
        savePlaylists(_playlists.value.map { if (it.id == id) it.copy(name = name.trim()) else it })
    }

    fun deletePlaylist(id: String) {
        savePlaylists(_playlists.value.filterNot { it.id == id })
    }

    /** Devolve false se a música já estava na playlist. */
    fun addToPlaylist(playlistId: String, songId: Long): Boolean {
        val p = _playlists.value.firstOrNull { it.id == playlistId } ?: return false
        if (songId in p.songIds) return false
        savePlaylists(_playlists.value.map { if (it.id == playlistId) it.copy(songIds = it.songIds + songId) else it })
        return true
    }

    fun removeFromPlaylist(playlistId: String, songId: Long) {
        savePlaylists(
            _playlists.value.map {
                if (it.id == playlistId) it.copy(songIds = it.songIds.filterNot { id -> id == songId }) else it
            },
        )
    }

    override fun onCleared() {
        controller?.removeListener(listener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        super.onCleared()
    }
}
