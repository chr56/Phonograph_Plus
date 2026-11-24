/*
 * Copyright (c) 2022~2026 chr_56
 */

package player.phonograph.ui.modules.playlist

import player.phonograph.mechanism.playlist.DynamicPlaylists
import player.phonograph.mechanism.playlist.PlaylistSongsActions
import player.phonograph.mechanism.scanner.MediaStoreScanner
import player.phonograph.model.QueueSong
import player.phonograph.model.Song
import player.phonograph.model.playlist.FilePlaylistLocation
import player.phonograph.model.playlist.Playlist
import player.phonograph.model.playlist.VirtualPlaylistLocation
import player.phonograph.model.ui.UIMode
import player.phonograph.repo.loader.Playlists
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PlaylistDetailViewModel(initialPlaylist: Playlist, initialSongs: List<QueueSong>) : ViewModel() {

    private val targetLocation = initialPlaylist.location

    private val _playlist: MutableStateFlow<Playlist?> = MutableStateFlow(initialPlaylist)
    val playlist = _playlist.asStateFlow()

    private val _currentMode: MutableStateFlow<UIMode> = MutableStateFlow(UIMode.Common)
    val currentMode get() = _currentMode.asStateFlow()

    private val _songs: MutableStateFlow<List<QueueSong>> = MutableStateFlow(initialSongs)
    val songs get() = _songs.asStateFlow()

    private val _query: MutableStateFlow<String> = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _loadErrors = MutableSharedFlow<String>()
    val loadErrors = _loadErrors.asSharedFlow()

    val items: Flow<List<QueueSong>> = combine(songs, query, currentMode) { songs, query, mode ->
        if (mode == UIMode.Search) songs.filter { it.song.title.contains(query) } else songs
    }

    val totalCount: Flow<Int> = songs.map { it.size }
    val totalDuration: Flow<Long> = songs.map { it.fold(0L) { acc: Long, queueSong -> acc + queueSong.song.duration } }

    private val mutex = Mutex()

    suspend fun execute(context: Context, action: PlaylistAction): Boolean {
        if (_playlist.value == null) return false
        val applicationContext = context.applicationContext
        return when (action) {
            is PlaylistAction.Load        -> requestLoad(applicationContext, action.scan)
            is PlaylistAction.Search      -> requestSearch(applicationContext, action.keyword)
            is PlaylistAction.UpdateMode  -> updateMode(applicationContext, action.mode)
            is PlaylistAction.Edit.Delete -> deleteItem(applicationContext, action.song, action.position)
            is PlaylistAction.Edit.Move   -> moveItem(applicationContext, action.from, action.to)
        }
    }

    private var loadJob: Job? = null

    private fun requestLoad(context: Context, scan: Boolean): Boolean {
        if (_currentMode.value == UIMode.Editor) return false

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (scan) scanPlaylist(context)
            reloadPlaylist(context)
        }
        return true
    }

    private suspend fun scanPlaylist(context: Context) {
        val location = _playlist.value?.location as? FilePlaylistLocation ?: return
        try {
            withContext(Dispatchers.IO) { MediaStoreScanner(context, reportResult = false).scan(location.path) }
        } catch (_: Exception) {
        }
    }

    private suspend fun reloadPlaylist(context: Context): Unit = mutex.withLock {
        val playlist = try {
            withContext(Dispatchers.IO) { loadPlaylist(context) }
        } catch (e: Exception) {
            _loadErrors.emit(e.message ?: e.toString())
            return
        }

        _playlist.value = playlist
        if (playlist == null) return

        val songs = try {
            withContext(Dispatchers.IO) { loadSongs(context, playlist) }
        } catch (e: Exception) {
            _loadErrors.emit(e.message ?: e.toString())
            return
        }

        _songs.value = songs
    }

    private suspend fun loadPlaylist(context: Context): Playlist? =
        when (val location = targetLocation) {
            VirtualPlaylistLocation.Favorite   -> DynamicPlaylists.favorites(context.resources)
            VirtualPlaylistLocation.History    -> DynamicPlaylists.history(context.resources)
            VirtualPlaylistLocation.LastAdded  -> DynamicPlaylists.lastAdded(context.resources)
            VirtualPlaylistLocation.MyTopTrack -> DynamicPlaylists.myTopTrack(context.resources)
            VirtualPlaylistLocation.Random     -> DynamicPlaylists.random(context.resources)
            else                               -> Playlists.of(context, location)
        }

    private suspend fun loadSongs(context: Context, playlist: Playlist): List<QueueSong> {
        val reader = PlaylistSongsActions.reader(playlist).also { it.refresh(context) }
        return QueueSong.fromQueue(reader.allSongs(context))
    }

    private fun requestSearch(context: Context, keyword: String): Boolean {
        _query.value = keyword
        return true
    }

    private fun updateMode(context: Context, mode: UIMode): Boolean {
        val previousMode = _currentMode.value
        _currentMode.value = mode
        if (mode == UIMode.Editor) {
            loadJob?.cancel()
        } else if (previousMode == UIMode.Editor) {
            requestLoad(context, scan = true)
        }
        return true
    }

    private suspend fun deleteItem(context: Context, song: Song, position: Int): Boolean = mutex.withLock {
        val currentSongs = _songs.value
        val currentPlaylist = playlist.value ?: return false
        if (currentSongs.getOrNull(position)?.song != song) return false

        withContext(Dispatchers.IO) {
            val writer = PlaylistSongsActions.writer(currentPlaylist)
            if (writer?.removeSong(context, song, position.toLong()) != true) return@withContext false

            val newSongs = currentSongs.map { it.song }.toMutableList()
            newSongs.removeAt(position)
            _songs.value = QueueSong.fromQueue(newSongs)
            true
        }
    }

    private suspend fun moveItem(context: Context, from: Int, to: Int): Boolean = mutex.withLock {
        val currentSongs = _songs.value
        val currentPlaylist = playlist.value ?: return false
        if (from !in currentSongs.indices || to !in currentSongs.indices) return false
        if (from == to) return true

        withContext(Dispatchers.IO) {
            val writer = PlaylistSongsActions.writer(currentPlaylist)
            if (writer?.moveSong(context, from, to) != true) return@withContext false

            val newSongs = currentSongs.map { it.song }.toMutableList()
            newSongs.add(to, newSongs.removeAt(from))
            _songs.value = QueueSong.fromQueue(newSongs)
            true
        }
    }
}